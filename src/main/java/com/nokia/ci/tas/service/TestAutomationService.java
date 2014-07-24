package com.nokia.ci.tas.service;

import java.io.File;
import java.io.FileOutputStream;

import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Calendar;
import java.util.Timer;
import java.util.Collections;
import java.util.GregorianCalendar;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.regex.Pattern;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.TestAutomationServiceListener;
import com.nokia.ci.tas.commons.TestNodeDescription;
import com.nokia.ci.tas.commons.TestPackage;
import com.nokia.ci.tas.commons.Util;

import com.nokia.ci.tas.commons.log.ConsoleFormatter;
import com.nokia.ci.tas.commons.log.LogFileFormatter;
import com.nokia.ci.tas.commons.log.LogFileSwitcher;

import com.nokia.ci.tas.commons.message.ProductOperation;
import com.nokia.ci.tas.commons.message.RegistryOperation;
import com.nokia.ci.tas.commons.message.TestOperation;
import com.nokia.ci.tas.commons.message.TextMessage;

import com.nokia.ci.tas.commons.statistics.Statistics;
import com.nokia.ci.tas.commons.statistics.TestEntry;
import com.nokia.ci.tas.service.monitor.MonitorListener;

/**
 * Test Automation Service.
 */
public class TestAutomationService extends Thread {

    /**
     * Singleton instance of the Test Automation Service.
     */
    private static TestAutomationService self;

    /**
     * Name of the host where Test Automation Service is running.
     */
    private String serviceHostname;

    /**
     * Port number that Test Automation Service is using for listening incoming messages.
     */
    private int servicePort = -1;

    /**
     * Description of this Test Automation Service instance, if specified.
     */
    private String description = "";

    /**
     * Detailed desctiption of this Test Automation Service instance,
     * which usually will contain hostname, port number and specified description.
     */
    private String serviceDetailedDescription = "";

    /**
     * Variable which keeps Test Automation Service running.
     */
    private boolean isRunning = true;

    /**
     * Variable which remembers the start time for this Test Automation Service instance.
     */
    private long startTime = 0L;

    /**
     * List of the running test handlers.
     */
    private ConcurrentLinkedQueue<TestMonitor> testMonitors;

    /**
     * List of the test handler waiting for any testing resources.
     */
    private ConcurrentLinkedQueue<TestMonitor> testMonitorsWaitingForTestingResources;

	/**
	 * List of the test handler waiting for test restarts.
	 */
	private ConcurrentLinkedQueue<TestMonitor> testMonitorsWaitingForTestRestarts;

    /**
     * List of test nodes registered to this instance of the Test Automation Service.
     */
    private ConcurrentLinkedQueue<TestNode> testNodes;

    /**
     * List of Test Automation Clients registered to this instance of the Test Automation Service.
     */
    private ConcurrentLinkedQueue<RemoteClient> remoteClients;

    /**
     * Handler of all incoming messages.
     */
    private Receiver receiver;

    /**
     * Configuration handler.
     */
    private Configuration configuration;

    /**
     * A moment when the product requests were tried to be resolved last time.
     */
    private long timeOfLastProductRequestResolvings = 0L;

    /**
     * Date and time format used for timestamps in logging prints.
     */
    private SimpleDateFormat timestampFormat;

    /**
     * Date format used in various locations.
     */
    private SimpleDateFormat dateFormat;

    /**
     * Hour format used in various locations.
     */
    private SimpleDateFormat hourFormat;

    /**
     * Name of the directory where Test Automation Service keeps all log files.
     */
    private static final String SERVICE_LOGS_DIRECTORY = "logs";

    /**
     * Reference to a logs directory used by Test Automation Service.
     */
    private File logsDirectory;

    /**
     * Format used in names of the log files. Here they follow the Java standard date and time patterns.
     */
    private String logFilenameFormat = "yyyy-MM-dd";

    /**
     * Name of the directory where Test Automation Service keeps all its statistics.
     */
    private static final String SERVICE_STATISTICS_DIRECTORY = "statistics";

    /**
     * Reference to statistics directory used by Test Automation Service.
     */
    private File statisticsDirectory;

    /**
     * Statistics module.
     */
    private Statistics statistics;

    /**
     * Name of the directory where Test Automation Service keeps all its maintenance messages.
     */
    private static final String SERVICE_MESSAGES_DIRECTORY = "messages";

    /**
     * Reference to a messages directory used by Test Automation Service.
     */
    private File messagesDirectory;

    /**
     * Timer used for switching log files.
     */
    private Timer timer;

    /**
     * Keeps a copy of web-page representing current status of the Test Automation Service.
     */
    private String currentStatus = "Loading...";
    
    /**
     * Keeps a copy of descriptions for all products handled by the Test Automation Service.
     */
    private String productDescriptions = "";

    /**
     * Name associated with the global Logger.
     */
    public static final String GLOBAL_LOGGER_NAME = "TestAutomationService";

    /**
     * Global logger used in all prints and messaging.
     */
    private static Logger logger;

    /**
     * Creates an instance of the Test Automation Service.
     * The constructor is made private for allowing
     * only the single static instance of the service on a hosting machine.
     *
     * @param port A port number on which this instance of Test Automation Service should be launched
     */
    private TestAutomationService(int port) {

        super(); // Start as anonymous thread

        // Remember the moment when this instance was created
        startTime = System.currentTimeMillis();

        // Create a date and time formatter
        timestampFormat = new SimpleDateFormat(Constant.TIMESTAMP_FORMAT);
        dateFormat = new SimpleDateFormat(Constant.DATE_FORMAT);
        hourFormat = new SimpleDateFormat(Constant.HOUR_FORMAT);

        // Init logging
        try {
            logger = Logger.getLogger(GLOBAL_LOGGER_NAME);
            logger.setLevel(Level.ALL);

            // Create and append console formatter
            ConsoleFormatter consoleFormatter = new ConsoleFormatter();

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(consoleFormatter);
            logger.addHandler(consoleHandler);

            // Create and append log file formatter
            logsDirectory = new File(SERVICE_LOGS_DIRECTORY);
            if (!logsDirectory.exists()) {
                if (logsDirectory.mkdirs()) {
                    p("Test Automation Service's log directory was successfully created at " + logsDirectory.getAbsolutePath());
                }
            } else {
                p("Test Automation Service's log directory was successfully initialized at " + logsDirectory.getAbsolutePath());
            }

            // Create formatter for the log file
            LogFileFormatter logFileFormatter = new LogFileFormatter(new SimpleDateFormat(Constant.TIMESTAMP_FORMAT));

            // Create timer and init current log
            timer = new Timer(true);
            timer.schedule(new LogFileSwitcher(GLOBAL_LOGGER_NAME, logsDirectory, logFileFormatter, logFilenameFormat, LogFileSwitcher.NO_CLEANUP), new Date());

            // Schedule changes of log files each 24 hours
            Calendar calendar = Calendar.getInstance();

            // Enable log file changes right at the next day's 00:00:00.000
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));
            calendar.add(Calendar.DATE, 1);

            Date nextLogChangeDate = calendar.getTime();

            // Schedule timer to switch log files each 24 hours
            timer.scheduleAtFixedRate(new LogFileSwitcher(GLOBAL_LOGGER_NAME, logsDirectory, logFileFormatter, logFilenameFormat, LogFileSwitcher.NO_CLEANUP), nextLogChangeDate, 24L * Constant.ONE_HOUR);

            p("Log file's switcher was successfully scheduled");

        } catch (Exception e) {
            p("Got troubles while tried to initialize logging:" + e.toString());
            e.printStackTrace();
            return;
        }

        // Init configuration handler
        try {
            configuration = new Configuration();
            configuration.start();
        } catch (Exception e) {
            p("Got troubles while tried to initialize configuration handler:" + e.toString());
            e.printStackTrace();
            return;
        }

        // Init statistics directory
        try {
            // Get access to statistics directory
            statisticsDirectory = new File(SERVICE_STATISTICS_DIRECTORY);
            if (!statisticsDirectory.exists()) {
                if (statisticsDirectory.mkdirs()) {
                    p("Test Automation Service's statistics directory was successfully created at " + statisticsDirectory.getAbsolutePath());
                }
            } else {
                p("Test Automation Service's statistics directory was successfully initialized at " + statisticsDirectory.getAbsolutePath());
            }

            // Create and init statistics module
            statistics = new Statistics(SERVICE_STATISTICS_DIRECTORY, configuration);
            statistics.start();

            p("Statistics module was successfully initialized");

        } catch (Exception e) {
            p("Got troubles while tried to initialize statistics: " + e.toString());
            e.printStackTrace();
            return;
        }

        // Init messages directory
        try {
            messagesDirectory = new File(SERVICE_MESSAGES_DIRECTORY);

            if (!messagesDirectory.exists()) {
                if (messagesDirectory.mkdirs()) {
                    p("Test Automation Service's messages directory was successfully created at " + messagesDirectory.getAbsolutePath());
                }
            } else {
                p("Test Automation Service's messages directory was successfully initialized at " + messagesDirectory.getAbsolutePath());
            }

        } catch (Exception e) {
            p("Got troubles while tried to initialize messages directory: " + e.toString());
            e.printStackTrace();
            return;
        }

        p("Launching Test Automation Service v" + Constant.TEST_AUTOMATION_RELEASE_VERSION);

        // Set the port number
        if (port < 0 || port > 0xFFFF) {
            servicePort = Constant.TEST_AUTOMATION_CLIENT_DEFAULT_PORT_NUMBER;
            p("Provided port number " + port + " is out of valid ranges. Launching Test Automation Service on default port number " + servicePort);
        } else {
            servicePort = port;
            p("Launching Test Automation Service on port number " + servicePort);
        }

        // Get the hostname
        if (serviceHostname == null || serviceHostname.isEmpty()) {
            p("Trying to get the hostname of Test Automation Service");

            try {
                serviceHostname = Util.getValidHostIp();//InetAddress.getLocalHost().getCanonicalHostName();
                p("Extracted hostname is " + serviceHostname);
            } catch (Exception e) {
                p("Got troubles while trying to extract local hostname: " + e.getClass() + " - " + e.getMessage());
                p("Will try to extract hostname from the environment variables");

                serviceHostname = (String) System.getenv().get("HOST"); // By default we are running on Linux

                p("HOST environment variable returned " + serviceHostname);

                if (serviceHostname == null || serviceHostname.isEmpty()) {
                    serviceHostname = (String) System.getenv().get("HOSTNAME"); // Try another Linux environment variable
                    p("HOSTNAME environment variable returned " + serviceHostname);
                }

                if (serviceHostname == null || serviceHostname.isEmpty()) {
                    serviceHostname = (String) System.getenv().get("COMPUTERNAME"); // Otherwise we are probably running on Windows
                    p("COMPUTERNAME environment variable returned " + serviceHostname);
                }

                if (serviceHostname == null || serviceHostname.isEmpty()) {
                    p("Warning: Couldn't extract a hostname for the Test Automation Service out of environment variables. Please specify one either in HOST, HOSTNAME or in COMPUTERNAME system variables.");
                    serviceHostname = "localhost";
                }
            }
        }

        p("Trying to launch Test Automation Service on " + serviceHostname + ":" + servicePort);

        setPriority(Thread.MAX_PRIORITY);
    }

    /**
     * Returns an instance of the Test Automation Service which runs on current hostname and on specified port
     * or creates one on specified port if it was not yet created.
     *
     * @param port Port number that an instance of Test Automation Service should use
     * @param description Optional description specified for this instance of Test Automation Service
     * @return A singleton instance of the Test Automation Service
     */
    public synchronized static TestAutomationService getInstance(int port, String description, int monitorPort) {

        if (self == null) {
            self = new TestAutomationService(port);
            self.setDescription(description);
            self.start();
        }
        
        MonitorListener ml = new MonitorListener( monitorPort, self );
        ml.start();
        
        return self;
    }

    /**
     * Main routine of the Test Automation Service.
     */
    @Override
    public void run() {
        // Create and init everything

        // There could be as many test monitors as tests were issued
        testMonitors = new ConcurrentLinkedQueue();

        // This queue will contain copies of the test monitors waiting for some testing resources
        testMonitorsWaitingForTestingResources = new ConcurrentLinkedQueue();

		// This queue will contain copies of the test monitors waiting for test restarts
		testMonitorsWaitingForTestRestarts = new ConcurrentLinkedQueue();

        // Test nodes will be dynamically added and removed
        testNodes = new ConcurrentLinkedQueue();

        // Test Automation Clients will be dynamically added and removed
        remoteClients = new ConcurrentLinkedQueue();
        
        // Create server socket and launch receiver
        try {
            ServerSocket listener = new ServerSocket(servicePort);

            p("Testing Automation Service started working on hostname " + serviceHostname + " and port " + servicePort);

            // Build a detailed service description
            serviceDetailedDescription = serviceHostname + ":" + servicePort;

            if (description != null && !description.isEmpty()) {
                serviceDetailedDescription += " (\"" + description + "\")";
            }
            
            // Create and start Receiver which will handle all incoming connections
            receiver = new Receiver(self, listener);
            receiver.start();

            // Enter into the main loop
            while (isRunning) {
                // Try to resolve product requests each 15 seconds
                if ((System.currentTimeMillis() - timeOfLastProductRequestResolvings) > Constant.FIFTEEN_SECONDS) {
                    resolveRequestsForTestRestarts();
                    resolveRequestsForTestResources();
                    timeOfLastProductRequestResolvings = System.currentTimeMillis();
                    updateCurrentStatus();
                }

                sleep(Constant.DECISECOND); // Wait for any updates
            }
        } catch (Exception e) {
            if (e instanceof BindException) {
                p("Cannot start Test Automation Service on hostname " + serviceHostname + " and port " + servicePort + " because of already occupied port number.");
                p("Please try to launch Test Automation Service on another port or shutdown already running instance of Test Automation Service on this hostname and required port number.");
            } else {
                p("Couldn't run Test Automation Service on hostname " + serviceHostname + " and port " + servicePort + " because of: " + e.getMessage());
                e.printStackTrace();
            }

            if (receiver != null) {
                receiver.stopWorking();
            }

            isRunning = false;
        }
    }

    /**
     * Handles specified test operation.
     *
     * @param testOperation Test operation to handle
     */
    protected void handleTestOperation(TestOperation testOperation) {
        if (testOperation.getId() == TestOperation.Id.START) {
            startTest(testOperation);
        } else if (testOperation.getId() == TestOperation.Id.STOP) {
            stopTest(testOperation);
        } else if (testOperation.getId() == TestOperation.Id.UPDATE) {
            forward(testOperation);
        } else if (testOperation.getId() == TestOperation.Id.CHECK) {
            checkTest(testOperation);
        }
    }

    /**
     * Starts test for remote Test Automation Client according to provided information.
     *
     * @param startTestMessage Message containing information about a test to be started
     */
    protected void startTest(TestOperation startTestMessage) {
        p("Trying to start a new test from the Start Test message");

        // Check if test can be started properly

        String clientHostname = startTestMessage.getSenderHostname();
        if (clientHostname == null || clientHostname.isEmpty()) {
            p("Client hostname is not specified. Cannot start test");
            return;
        }

        int clientPort = startTestMessage.getSenderPort();
        if (clientPort <= 0) {
            p("Client port number has invalid value " + clientPort + ". Cannot start test");
            return;
        }

        Test test = startTestMessage.getTest();
        if (test == null) {
            p("Test specification is not provided. Cannot start test.");
            return;
        }

        String testId = test.getId();
        if (testId == null || testId.isEmpty()) {
            p("Test id is not provided. Cannot start test.");
            return;
        }

        // At this point all most important parameters seems to have some values
        // Try to figure out if we already have specified remote client

        RemoteClient remoteClient = getRemoteClient(clientHostname, clientPort);

        if (remoteClient == null) {
            // We don't have such client, so we can add it

            remoteClient = new RemoteClient(self, clientHostname, clientPort, serviceHostname, servicePort);
            remoteClient.start();

            remoteClients.add(remoteClient);

            p("Successfully registered a new Test Automation Client at " + remoteClient.getClientHostnameAndPort());
        } else {
            p("Have a successful registration with Test Automation Client at " + remoteClient.getClientHostnameAndPort());
        }

        // Check if this remote client already has a running test with the same id
        if (remoteClient.hasRunningTestWithId(testId)) {
            // Test cannot be started, but shouldn't be failed
            String text = "Test '" + testId + "' is already under execution for the client at " + clientHostname + ":" + clientPort;
            TextMessage textMessage = new TextMessage(test, text);
            textMessage.setSender(serviceHostname, servicePort);
            textMessage.setReceiver(clientHostname, clientPort);
            remoteClient.handle(textMessage);
        } else {
            // Test can be started
            // Add this test to the list of tests issued by this remote client
            remoteClient.addTest(test);
            // Try to create a handler for the test
            startTest(test, remoteClient, true);
        }
    }

    /**
     * Starts specified test on any capable testing node registered to this Test Automation Service.
     * The listener specified in this method will receive all messages regarding the test execution.
     *
     * @param test Test to be performed
     * @param listener Listener of all messages regarding the test execution
     * @param isRemoteListener True when listener is a remote client of Test Automation Service
     */
    public void startTest(Test test, TestAutomationServiceListener listener, boolean isRemoteListener) {

        // Ensure that test is specified
        if (test != null) {
            // Create a new test handler
            String testId = test.getId();

            if (testId != null && !testId.isEmpty()) {
                // Check that test id doesn't contain any bad characters
                String failureReason = Util.checkTestId(testId);

                if (failureReason != null) {
                    failureReason += " Test cannot be executed.";
                    p(failureReason);

                    if (isRemoteListener) {
                        RemoteClient client = (RemoteClient) listener;
                        client.testFailed(test, failureReason);
                    } else {
                        listener.testFailed(test, failureReason);
                    }
                } else {
                    if (getTestMonitor(testId) != null) {
                        // We already have a test handler with specified Id
                        if (isRemoteListener) {
                            RemoteClient client = (RemoteClient) listener;
                            client.testFailed(test, "Test Automation Service at " + serviceDetailedDescription
                                                    + " is already performing a test with the same id '" + testId + "'");
                        } else {
                            listener.testFailed(test, "Test Automation Service at " + serviceDetailedDescription
                                                      + " is already performing a test with the same id '" + testId + "'");
                        }
                    } else {
                        // Start the new test
                        TestMonitor testMonitor = new TestMonitor(self, test, listener, isRemoteListener);
                        testMonitors.add(testMonitor);
                        p("Successfully created a new monitor for the test '" + testId + "'");

                        // Add this test handler into the list of testing resource requesters
                        issueRequestForTestingResources(testMonitor);

                        // Start test handler
                        testMonitor.start();

                        // Notify test listener
                        if (isRemoteListener) {
                            RemoteClient client = (RemoteClient) listener;
                            client.messageFromTestAutomationService(testMonitor.getTest(), "Test handling has started on Test Automation Service at " + serviceDetailedDescription);
                        } else {
                            listener.messageFromTestAutomationService(testMonitor.getTest(), "Test handling has started on Test Automation Service at " + serviceDetailedDescription);
                        }
                    }
                }
            } else {
                if (isRemoteListener) {
                    RemoteClient client = (RemoteClient) listener;
                    client.testFailed(test, "Test Automation Service at " + serviceDetailedDescription + " is not able to start a test with unspecified id");
                } else {
                    listener.testFailed(test, "Test Automation Service at " + serviceDetailedDescription + " is not able to start a test with unspecified id");
                }
            }
        } else {
            if (isRemoteListener) {
                RemoteClient client = (RemoteClient) listener;
                client.testFailed(test, "Test description was not provided");
            } else {
                listener.testFailed(test, "Test description was not provided");
            }
        }
    }

    /**
     * Stops test for remote Test Automation Client according to provided information.
     *
     * @param stopTestMessage Message containing information about a test to be stopped
     */
    protected void stopTest(TestOperation stopTestMessage) {
        p("Trying to stop a test from the stop test message:\n" + stopTestMessage);

        String clientHostname = stopTestMessage.getSenderHostname();
        int clientPort = stopTestMessage.getSenderPort();
        Test test = stopTestMessage.getTest();

        // Check if test can be stopped properly

        if (clientHostname == null || clientHostname.isEmpty()) {
            p("Client hostname is not specified. Cannot stop test");
            return;
        }

        if (clientPort <= 0) {
            p("Client port number has invalid value " + clientPort + ". Cannot stop test");
            return;
        }

        if (test == null) {
            p("Test specification is not provided. Cannot stop test.");
            return;
        }

        String testId = test.getId();

        if (testId == null || testId.isEmpty()) {
            p("Test id is not provided. Cannot stop test.");
            return;
        }

        // At this point all parameters have some values, now will need to check them

        // Try to figure out if we already have specified remote client

        RemoteClient remoteClient = getRemoteClient(clientHostname, clientPort);

        if (remoteClient == null) {
            // We don't have such client, so we can add it

            remoteClient = new RemoteClient(self, clientHostname, clientPort, serviceHostname, servicePort);
            remoteClient.start();

            remoteClients.add(remoteClient);

            p("Successfully registered a new Test Automation Client at " + remoteClient.getClientHostnameAndPort());
        } else {
            p("Already have a registration with Test Automation Client at " + remoteClient.getClientHostnameAndPort());
        }

        // Check if this remote client already has a running test with the same id
        if (remoteClient.hasRunningTestWithId(testId)) {

            stopTest(test, remoteClient, true);

        } else {
            // Test cannot be stopped, since it is not under execution by this Test Automation Service
            p("Sending a STOP test operation with test in FAILED status to remote client at " + remoteClient.getClientHostnameAndPort() + " regarding the test '" + test.getId() + "'");

            String reasonOfFailure = "Test '" + testId + "' is not under execution by Test Automation Service at " + serviceDetailedDescription;

            // Test should be set to the FAILED status, since here we have a clear misconfiguration of the test execution flow
            test.setStatus(Test.Status.FAILED, reasonOfFailure);

            // Stop test operation will ensure that remote part will stop executing this test
            TestOperation stopTestOperation = new TestOperation(TestOperation.Id.STOP, test);
            stopTestOperation.setSender(serviceHostname, servicePort);
            stopTestOperation.setReceiver(remoteClient.getClientHostname(), remoteClient.getClientPort());

            remoteClient.handle(stopTestOperation);
        }
        remoteClients.remove( remoteClient );
    }

    /**
     * Checks if test from specified message is still handled by this Test Automation Service.
     * And if it does, the UPDATE test operation will be send with test's current status and data.
     * Otherwise it should send back the STOP test operation message with the same test in FAILED status.
     *
     * @param message Test operation message containing a test to be checked
     */
    protected void checkTest(TestOperation message) {

        Test test = message.getTest();

        if (test != null) {
            p("Performing a check for the test '" + test.getId() + "'");
            TestMonitor testMonitor = getTestMonitor(test.getId());
            RemoteClient remoteClient = getRemoteClient(message.getSenderHostname(), message.getSenderPort());

            if (testMonitor != null && remoteClient != null) {
                p("Has a test handler and remote client for the test '" + test.getId() + "'");
                // We have a test under handling
                // Reply with UPDATE test operation message
                Test currentTestUpdate = testMonitor.getTest();
                TestOperation responseMessage = new TestOperation(TestOperation.Id.UPDATE, currentTestUpdate);
                responseMessage.setSender(serviceHostname, servicePort);
                responseMessage.setReceiver(remoteClient.getClientHostname(), remoteClient.getClientPort());

                remoteClient.handle(responseMessage);

            } else {
                // We have a test which we are not handling here for some reason
                p("Hasn't a monitor for the test '" + test.getId() + "'");

                if (remoteClient == null) {
                    // Create a new remote client
                    // Alhough this might be looking too excessive to create a new remote client instance for the unknow test,
                    // we can never be sure if the same remote client is handling some other tests which will be checked very soon here

                    remoteClient = new RemoteClient(self, message.getSenderHostname(), message.getSenderPort(), serviceHostname, servicePort);
                    remoteClient.start();
                    remoteClients.add(remoteClient);
                }

                if (remoteClient != null) {
                    // Test cannot be checked, since it is not under execution by this Test Automation Service
                    p("Sending a STOP test operation with test in FAILED status to remote client at " + remoteClient.getClientHostnameAndPort() + " regarding the test '" + test.getId() + "'");

                    String reasonOfFailure = "Test '" + test.getId() + "' is not under execution by Test Automation Service at " + serviceDetailedDescription;

                    // Test should be set to the FAILED status, since here we have a clear misconfiguration of the test execution flow
                    test.setStatus(Test.Status.FAILED, reasonOfFailure);

                    // Stop test operation will ensure that remote part will stop executing this test
                    TestOperation stopTestOperation = new TestOperation(TestOperation.Id.STOP, test);
                    stopTestOperation.setSender(serviceHostname, servicePort);
                    stopTestOperation.setReceiver(remoteClient.getClientHostname(), remoteClient.getClientPort());

                    remoteClient.handle(stopTestOperation);
                }
            }
        }
    }

    /**
     * Forwards specfied test operation message to the test handler.
     *
     * @param message Test operation message to be forwarded
     */
    protected void forward(TestOperation message) {

        Test test = message.getTest();

        if (test != null) {
            TestMonitor testMonitor = getTestMonitor(test.getId());

            if (testMonitor != null) {
                testMonitor.handle(message);
            }
        }
    }

    /**
     * Stops specified test.
     *
     * @param test Test to be stopped
     * @param listener Listener of events related to the test performances
     * @param isRemoteListener True when listener is a remote client of Test Automation Service
     */
    public void stopTest(Test test, TestAutomationServiceListener listener, boolean isRemoteListener) {
        if (test != null) {
            p("Stopping test '" + test.getId() + "'");

            // Get a handler of the test
            TestMonitor testMonitor = getTestMonitor(test.getId());

            if (testMonitor != null) {
                // Remove test handler
                testMonitor.stopTest("Listener of the test '" + test.getId() + "' has stopped the test");
                testMonitors.remove(testMonitor);

                // Also remove it from the list of testing resource requesters
                if (removeRequestForTestingResources(testMonitor)) {
                    p("Monitor of the test '" + test.getId() + "' was removed from the list of test resource requesters");
                }

                testMonitor = null;

                p("Test '" + test.getId() + "' was successfully stopped");
                p("Finally having " + testMonitors.size() + " test monitors");

                if (isRemoteListener) {
                    RemoteClient client = (RemoteClient) listener;
                    client.messageFromTestAutomationService(test, "Test '" + test.getId() + "' was successfully stopped");
                    client.removeTest(test);
                } else {
                    listener.messageFromTestAutomationService(test, "Test '" + test.getId() + "' was successfully stopped");
                }

                System.gc();

            } else {
                if (isRemoteListener) {
                    RemoteClient client = (RemoteClient) listener;
                    client.messageFromTestAutomationService(test, "Couldn't stop test '"
                            + test.getId() + "' in Test Automation Service because such test is no more under execution");
                } else {
                    listener.messageFromTestAutomationService(test, "Couldn't stop test '"
                            + test.getId() + "' in Test Automation Service because such test is no more under execution");
                }
            }
        } else {
            if (isRemoteListener) {
                RemoteClient client = (RemoteClient) listener;
                client.messageFromTestAutomationService(test, "Couldn't stop test at Test Automation Service because specified test object was NULL");
            } else {
                listener.messageFromTestAutomationService(test, "Couldn't stop test at Test Automation Service because specified test object was NULL");
            }
        }
    }

    /**
     * Removes a handler for specified test.
     *
     * @param test A test the handler is working on
     */
    protected void removeTestMonitor(Test test) {
        if (test != null) {
            // Get a handler of the test
            TestMonitor testMonitor = getTestMonitor(test.getId());

            if (testMonitor != null) {
                // Remove test handler
                testMonitors.remove(testMonitor);

                // Also remove it from the list of testing resource requesters
                if (removeRequestForTestingResources(testMonitor)) {
                    p("Monitor of the test '" + test.getId() + "' was removed from the list of test resource requesters");
                }

                testMonitor = null;

                p("Monitor of the test '" + test.getId() + "' was successfully stopped and removed");
                p("Finally having " + testMonitors.size() + " test monitors");

                System.gc();
            }
        }
    }

    /**
     * Returns statistics file for specified test from the Test Automation Service workspace.
     *
     * @param test Test issued to the Test Automation Service
     * @return Reference to the statistics file related to issued test or null if such reference couldn't be opened
     */
    protected File createStatisticsFile(Test test) {
        File file = null;

        try {
            file = new File(statisticsDirectory + System.getProperty("file.separator") + test.getRuntimeId());

            // Delete old version of file
            if (file.exists()) {
                if (file.delete()) {
                    p("Has deleted already existing statistics file for the test '" + test.getRuntimeId() + "' at " + file.getAbsolutePath());
                }
            }

            // Create a new file
            if (!file.exists()) {
                if (file.createNewFile()) {
                    p("Has created a statistics file for the test '" + test.getRuntimeId() + "' at " + file.getAbsolutePath());
                } else {
                    p("Couldn't create statistics file for the test '" + test.getRuntimeId() + "' at " + file.getAbsolutePath());
                    file = null;
                }
            }
        } catch (Exception e) {
            p("Couldn't create statistics file for the test '" + test.getRuntimeId() + "' due to " + e.getClass() + " - " + e.getMessage());
            file = null;
        }

        return file;
    }

    /**
     * Updates a list of products for some test node, mentioned in the message.
     *
     * @param message Message containing a list of products for specified test node.
     */
    protected void handleProductOperation(ProductOperation message) {

        // Find responsible test node and update its list of products
        TestNode node = getTestNode(message.getSenderHostname(), message.getSenderPort());

        if (node != null) {
            //p("Updating a product on test node " + node.getHostnameAndPort());
            node.handleProductOperation(message);
        } else {
            // Broadcast message to all available test nodes
            Product product = message.getProduct();

            if (product != null) {
                p("Got a product operation from remote part at " + message.getSenderHostname() + ":" + message.getSenderPort()
                    + " concerning the product with IMEI " + product.getIMEI() + " and of type " + product.getRMCode());
                String imei = product.getIMEI();

                if (imei != null && !imei.isEmpty()) {
                    for (TestNode testNode : testNodes) {
                        if (testNode.holdsProduct(imei)) {

                            if (product.isFree() && message.getId() == ProductOperation.Id.UPDATE) {
                                p("A product operation message about product with IMEI " + product.getIMEI()
                                    + " and of type " + product.getRMCode() + " is considered as a product releasing message...");
                                testNode.freeProduct(product);
                            } else {
                                testNode.handleProductOperation(message);
                            }

                            p("Message about product with IMEI " + imei + " and of type " + product.getRMCode()
                                + " was forwarded to the test node " + testNode.getHostnameAndPort());

                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Deletes specified filepath recursively.
     *
     * @param path A path to be deleted
     * @return Trye if deletion was successfull or false otherwise
     */
    protected synchronized boolean deleteRecursively(File path) {
        boolean result = true;

        if (path.exists()) {
            if (path.isDirectory()) {

                // Delete each sub-element in the directory
                for (File pathElement : path.listFiles()) {
                    result = deleteRecursively(pathElement);
                }
            }

            // Delete the element itself
            result = result & path.delete();
        }

        return result;
    }

    /**
     * Returns a handler of the test with specified id, or null if such handler is not existing.
     *
     * @param testId Id of the test that handler is working on
     * @return A handler which works on test with specified test id, or null if such handler is not existing
     */
    protected TestMonitor getTestMonitor(String testId) {

        TestMonitor result = null;

        if (testId != null && !testId.isEmpty()) {
            for (TestMonitor current : testMonitors) {
                if (current.getName().equals(testId)) {
                    result = current;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Handles a registry operation.
     *
     * @param registryOperation Registry operation
     */
    protected void handleRegistryOperation(RegistryOperation registryOperation) {

        RegistryOperation.Remote remote = registryOperation.getRemote();
        RegistryOperation.Id operation = registryOperation.getId();

        if (remote == RegistryOperation.Remote.TEST_NODE) {
            // Registry operation relates to a test node
            TestNodeDescription testNodeDescription = registryOperation.getTestNodeDescription();

            if (testNodeDescription == null) {
                // Create a test node description, assuming that sender is a test node
                testNodeDescription = new TestNodeDescription();
                testNodeDescription.setHostname(registryOperation.getSenderHostname());
                testNodeDescription.setPort(registryOperation.getSenderPort());
                testNodeDescription.setTestAutomationSoftwareVersion("");
            }

            // Try to find the corresponding test node
            TestNode testNode = getTestNode(testNodeDescription.getHostname(), testNodeDescription.getPort());

            if (operation == RegistryOperation.Id.UPDATE) {

                if (testNode != null) {
                    // Normal update of a test node
                    testNode.setDescription(testNodeDescription);
                } else {
                    // We have a connected but not yet registered test node

                    testNode = new TestNode(this, testNodeDescription.getHostname(), testNodeDescription.getPort());
                    testNode.setDescription(testNodeDescription);
                    testNode.start();

                    testNodes.add(testNode);

                    String message = "Test node " + testNode.getHostnameAndPort();

                    if (!testNodeDescription.getDescription().isEmpty()) {
                        message += " (\"" + testNodeDescription.getDescription() + "\")";
                    }

                    message += " has registered to the Test Automation Service " + serviceHostname + ":" + servicePort
                            + " at " + timestampFormat.format(new Date(System.currentTimeMillis()));

                    p(message);
                    createMessage( "Update_"+testNodeDescription.getHostname(), message );
                }

            } else if (operation == RegistryOperation.Id.REGISTER) {

                if (testNode == null) {
                    // We have a connected but not yet registered test node
                    p("Registering a new test node " + testNodeDescription.getHostname() + ":" + testNodeDescription.getPort());

                    testNode = new TestNode(this, testNodeDescription.getHostname(), testNodeDescription.getPort());
                    testNode.setDescription(testNodeDescription);
                    testNode.start();

                    testNodes.add(testNode);
                    
                    String message = "Test node " + testNode.getHostnameAndPort();

                    if (!testNodeDescription.getDescription().isEmpty()) {
                        message += " (\"" + testNodeDescription.getDescription() + "\")";
                    }

                    message += " has registered to the Test Automation Service " + serviceHostname + ":" + servicePort
                            + " at " + timestampFormat.format(new Date(System.currentTimeMillis()));

                    p(message);
                    createMessage("Register_"+testNodeDescription.getHostname(), message);
                }

            } else if (operation == RegistryOperation.Id.DEREGISTER) {

                handleDisconnectedTestNode(testNodeDescription.getHostname(), testNodeDescription.getPort());

            } else {
                p("Warning: Has received unsupported test node registry operation:\n" + registryOperation);
                // Ignore any unsupported operations
            }

        } else if (remote == RegistryOperation.Remote.CLIENT) {
            // Registry operation relates to a client

            String remoteClientHostname = registryOperation.getSenderHostname();
            int remoteClientPort = registryOperation.getSenderPort();

            // Try to find the corresponding remote client
            RemoteClient client = getRemoteClient(remoteClientHostname, remoteClientPort);

            if (operation == RegistryOperation.Id.UPDATE) {

                if (client == null) {
                    // We have a connected but not yet registered remote client
                    p("Registering a new remote client at " + remoteClientHostname + ":" + remoteClientPort);

                    client = new RemoteClient(self, remoteClientHostname, remoteClientPort, serviceHostname, servicePort);
                    client.start();

                    remoteClients.add(client);
                    p("Successfully registered a new Test Automation Client at " + client.getClientHostnameAndPort());
                }

            } else if (operation == RegistryOperation.Id.REGISTER) {

                if (client == null) {
                    // We have a connected but not yet registered remote client
                    p("Registering a new remote client at " + remoteClientHostname + ":" + remoteClientPort);

                    client = new RemoteClient(self, remoteClientHostname, remoteClientPort, serviceHostname, servicePort);
                    client.start();

                    remoteClients.add(client);
                    p("Successfully registered a new Test Automation Client at " + client.getClientHostnameAndPort());
                }
            } else if (operation == RegistryOperation.Id.DEREGISTER) {

                // Handle a proper disconnection of remote client
                handleDisconnectedRemoteClient(remoteClientHostname, remoteClientPort, false);

            } else {
                p("Warning: Has received unsupported remote client registry operation:\n" + registryOperation);
                // Ignore any unsupported operations
            }

        } else {
            // Ignore registry operations related to some unsupported remote parts
        }
    }

    /**
     * Gets a Test Node with specified hostname and port or null if such node is not existing.
     *
     * @param hostname Hostname to be searched for
     * @param port Port number to be searched for
     * @return Test Node with specified hostname and port or null if such node is not existing
     */
    protected TestNode getTestNode(String hostname, int port) {

        TestNode result = null;

        if (hostname != null && !hostname.isEmpty() && port > 0) {
            for (TestNode current : testNodes) {
                if (current.getHostname().equals(hostname)) {
                    if (current.getPort() == port) {
                        result = current;
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Gets a Test Node with specified hostname or null if such node is not existing.
     *
     * @param hostname Hostname to be searched for.
     * @return Test Node with specified hostname or null if such node is not existing
     */
    protected TestNode getTestNode(String hostname) {

        TestNode result = null;

        if (hostname != null && !hostname.isEmpty()) {
            for (TestNode current : testNodes) {
                if (current.getHostname().equals(hostname)) {
                    result = current;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Gets an instance of Test Automation Service under specified hostname or null if such client is not existing
     *
     * @param hostname Hostname associated with requested client
     * @param port Port number associated with requested client
     * @return Test Automation Service with specified hostname or null if such client is not existing
     */
    protected RemoteClient getRemoteClient(String hostname, int port) {

        RemoteClient result = null;

        if (hostname != null && !hostname.isEmpty() && port > 0) {
            for (RemoteClient current : remoteClients) {
                if (current.getClientHostname().equals(hostname)) {
                    if (current.getClientPort() == port) {
                        result = current;
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Return hostname associated with current instance of service.
     *
     * @return Hostname associated with current instance of service
     */
    public String getHostname() {
        return serviceHostname;
    }

    /**
     * Return port number associated with current instance of service.
     *
     * @return Port number associated with current instance of service
     */
    public int getPort() {
        return servicePort;
    }

    /**
     * Returns a hostname and port of Test Automation Service
     * in canonical form "hostname:port".
     *
     * @return A hostname and port of Test Automation Service in canonical form "hostname:port"
     */
    public String getHostnameAndPort() {
        return serviceHostname + ":" + servicePort;
    }

    /**
     * Handles a situation with disconnected test node.
     *
     * @param hostname Hostname of disconnected test node
     * @param port Port number of disconnected test node
     */
    protected void handleDisconnectedTestNode(String hostname, int port) {
        p("Handling a diconnected test node " + hostname + ":" + port);

        TestNode disconnectedTestNode = getTestNode(hostname, port);

        if (disconnectedTestNode != null) {
            // Remove disconnected test node from the list of available test nodes
            if (testNodes.remove(disconnectedTestNode)) {
                p("Disconnected test node " + disconnectedTestNode.getHostnameAndPort()
                        + " was successfully removed from the list of available test nodes");
            } else {
                p("Couldn't properly remove disconnected test node " + disconnectedTestNode.getHostnameAndPort()
                        + " from the list of available test nodes");
            }

            // Notify test monitors about a disconnected test node
            for (TestMonitor testMonitor : testMonitors) {
                testMonitor.handleDisconnectedTestNode(disconnectedTestNode);
            }

            // Stop the instance of disconnected test node
            disconnectedTestNode.shutdown();
            
            String message = "Emergency! Test node " + disconnectedTestNode.getHostnameAndPort();
            
            TestNodeDescription testNodeDescription = disconnectedTestNode.getDescription();
            
            if (testNodeDescription != null && !testNodeDescription.getDescription().isEmpty()) {
                message += " (\"" + testNodeDescription.getDescription() + "\")";
            }

            message += " has disconnected from the Test Automation Service " + serviceHostname + ":" + servicePort
                    + " at " + timestampFormat.format(new Date(System.currentTimeMillis()));

            p(message);
            createMessage( "TestNode_Disconn_" + disconnectedTestNode.getHostname(), message );
        }
    }

    /**
     * Handles a situation with disconnected remote client.
     *
     * @param hostname Hostname of disconnected remote client
     * @param port Port number of disconnected remote client
     * @param isFailure True if there were some failures occured with remote client
     */
    protected void handleDisconnectedRemoteClient(String hostname, int port, boolean isFailure) {
        p("Handling a diconnected remote client at " + hostname + ":" + port);

        RemoteClient disconnectedRemoteClient = getRemoteClient(hostname, port);

        if (disconnectedRemoteClient != null) {
            String message = "Remote client " + hostname + ":" + port
                    + " has disconnected from the Test Automation Service " + serviceHostname + ":" + servicePort
                    + " at " + timestampFormat.format(new Date(System.currentTimeMillis()));

            if (remoteClients.remove(disconnectedRemoteClient)) {
                p("Disconnected remote client at " + disconnectedRemoteClient.getClientHostnameAndPort()
                        + " was successfully removed from the list of connected remote clients");

                // Check test resource requesters
                for (TestMonitor current : testMonitorsWaitingForTestingResources) {
                    if (current.getRemoteClient().getClientHostnameAndPort().equals(disconnectedRemoteClient.getClientHostnameAndPort())) {
                        if (testMonitorsWaitingForTestingResources.remove(current)) {
                            p("Monitor of the test '" + current.getName() + "' was removed from the list of test resource requesters,"
                                + " since it was issued from a disconnected remote client at " + disconnectedRemoteClient.getClientHostnameAndPort());
                            message += "\nMonitor of the test '" + current.getName() + "' was removed from the list of test resource requesters due to remote client's disconnection.";
                        } else {
                            p("Error: Couldn't remove monitor of the test '" + current.getName() + "' from the list of test resource requesters"
                                + " up on disconnection of its remote client at " + disconnectedRemoteClient.getClientHostnameAndPort());

                        }
                    }
                }
                
                // Check test restarters
                for (TestMonitor current : testMonitorsWaitingForTestRestarts) {
                    if (current.getRemoteClient().getClientHostnameAndPort().equals(disconnectedRemoteClient.getClientHostnameAndPort())) {
                        if (testMonitorsWaitingForTestRestarts.remove(current)) {
                            p("Monitor of the test '" + current.getName() + "' was removed from the list of test restarters,"
                                + " since it was issued from a disconnected remote client at " + disconnectedRemoteClient.getClientHostnameAndPort());
                            message += "\nMonitor of the test '" + current.getName() + "' was removed from the list of test restarters due to remote client's disconnection.";
                        } else {
                            p("Error: Couldn't remove monitor of the test '" + current.getName() + "' from the list of test restarters"
                                + " up on disconnection of its remote client at " + disconnectedRemoteClient.getClientHostnameAndPort());

                        }
                    }
                }

                // Check running test monitors
                for (TestMonitor current : testMonitors) {
                    if (current.getRemoteClient().getClientHostnameAndPort().equals(disconnectedRemoteClient.getClientHostnameAndPort())) {

                        current.stopTest("Remote client " + disconnectedRemoteClient.getClientHostnameAndPort()
                            + " has disconnected from the Test Automation Service " + serviceHostname + ":" + servicePort);

                        if (testMonitors.remove(current)) {
                            p("Monitor of the test '" + current.getName() + "' was removed from the list of running test monitors,"
                                + " since it was issued from a disconnected remote client at " + disconnectedRemoteClient.getClientHostnameAndPort());
                            message += "\nMonitor of the test '" + current.getName() + "' was removed from the list of running test monitors due to remote client's disconnection.";
                        } else {
                            p("Error: Couldn't remove monitor of the test '" + current.getName() + "' from the list of running test monitors"
                                + " up on disconnection of its remote client at " + disconnectedRemoteClient.getClientHostnameAndPort());

                        }
                    }
                }
            } else {
                p("Couldn't properly remove disconnected remote client at " + disconnectedRemoteClient.getClientHostnameAndPort()
                        + " from the list of connected test nodes");
            }

            if (isFailure) {
                message = "Network failure! " + message;
                createMessage("RemoteClient_Disconn_" + hostname + "_" + port, message);
            }
        }
    }

    /**
     * Issue a request for any testing resources from specified test handler.
     *
     * @param testHandler Issuer of the new request for testing resources
     * @return True if request was successfully issued or false otherwise
     */
    public boolean issueRequestForTestingResources(TestMonitor testMonitor) {
        boolean success = false;

        if (!testMonitorsWaitingForTestingResources.contains(testMonitor)) {
            // Create a list expressions that will match all resources required to execute this test
            Test test = testMonitor.getTest();

            if (test != null) {
                if (test.getTarget() == Test.Target.FLASH) {
                    // Flash targets always require physical devices
                    p("Trying to create test resources matching expressions for the test '" + test.getId() + "'");

                    boolean successfullyExtractedMatchingExpressions = true;
                    List<String> matchingNameValueGroups = new ArrayList<String>(0);
                    List<Product> requiredProducts = test.getRequiredProducts();

                    // Try to create name-value groups from the list of required products
                    if (requiredProducts != null && !requiredProducts.isEmpty()) {
                        for (Product currentRequiredProduct : requiredProducts) {
                            String currentValuableNameValuePairs = currentRequiredProduct.getNameValuePairs();

                            if (currentValuableNameValuePairs != null && !currentValuableNameValuePairs.isEmpty()) {
                                List<String> normalizedNameValueGroups = Util.getNormalizedNameValueGroups(currentValuableNameValuePairs);

                                if (normalizedNameValueGroups != null && !normalizedNameValueGroups.isEmpty()) {
                                    // Each required product may get only a single normalized group of name-value pairs
                                    String normalizedNameValueGroup = normalizedNameValueGroups.get(0);

                                    if (normalizedNameValueGroup != null && !normalizedNameValueGroup.isEmpty()) {
                                        matchingNameValueGroups.add(normalizedNameValueGroup);
                                    } else {
                                        p("Got troubles with extracting normalized name-value groups for a required product: " + currentRequiredProduct);
                                        successfullyExtractedMatchingExpressions = false;
                                        break;
                                    }
                                }
                            }
                        }

                    } else {
                        // Try to use already specified expression
                        String requiredProductsExpression = test.getRequiredEnvironment();

                        if (requiredProductsExpression != null && !requiredProductsExpression.isEmpty()) {
                            matchingNameValueGroups = Util.getNormalizedNameValueGroups(requiredProductsExpression);

                            if (matchingNameValueGroups != null && !matchingNameValueGroups.isEmpty()) {
                                successfullyExtractedMatchingExpressions = true;
                            }
                        }
                    }

                    if (successfullyExtractedMatchingExpressions) {
                        // Sort exressions so that the longests (and so, most exotic) expressions will appear first,
                        // since that can save a lot of time during product allocations

                        // After this step the shortests expressions will appear first
                        Collections.sort(matchingNameValueGroups, Collections.reverseOrder());
                        // After this step the longests expression will appear first
                        Collections.reverse(matchingNameValueGroups);

                        if (!matchingNameValueGroups.isEmpty()) {
                            p("Test '" + test.getId() + "' will use the following list of matching expressions for required products:");
                            for (String matchingNameValueGroup : matchingNameValueGroups) {
                                p("\t " + matchingNameValueGroup);
                            }
                        }

                        testMonitor.setNameValueGroupsForRequiredProducts(matchingNameValueGroups);

                        success = testMonitorsWaitingForTestingResources.add(testMonitor);
                        p("A request for product sets was successfully issued for the test '" + testMonitor.getTest().getId() + "'");

                    } else {
                        p("Couldn't extract expressions for required resources regarding the test '" + test.getId() + "'");
                        testMonitor.stopTest("Couldn't extract expressions for required resources");
                    }
                } else {
                    // Nose targets are not requiring any physical products
                    success = testMonitorsWaitingForTestingResources.add(testMonitor);
                    p("A request for test nodes was successfully issued for the test '" + testMonitor.getTest().getId() + "'");
                }
            }
        }

        return success;
    }
    
    /**
     * Issue a request for test restart.
     *
     * @param testHandler Issuer of the request for test restart
     * @return True if request was successfully issued or false otherwise
     */
    public boolean issueRequestForTestRestart(TestMonitor testMonitor) {
        boolean success = false;

        if (!testMonitorsWaitingForTestRestarts.contains(testMonitor)) {
            success = testMonitorsWaitingForTestRestarts.add(testMonitor);
            p("Monitor of the test '" + testMonitor.getTest().getId() + "' was successfully added to the list for test restarters");
        } else {
            p("Monitor of the test '" + testMonitor.getTest().getId() + "' is already on the list for test restarters");
            success = true;
        }

        return success;
    }

    /**
     * Remove a request for any testing resources from specified test handler.
     *
     * @param testHandler Issuer of the request for testing resources
     * @return True if request was successfully removed or false otherwise
     */
    public boolean removeRequestForTestingResources(TestMonitor testMonitor) {
        boolean success = false;

        Test test = testMonitor.getTest();

        if (test != null) {
            String testId = test.getId();

            for (TestMonitor requestingTestMonitor : testMonitorsWaitingForTestingResources) {
                Test requestingTest = requestingTestMonitor.getTest();

                if (requestingTest.getId().equals(testId)) {
                    success = testMonitorsWaitingForTestingResources.remove(requestingTestMonitor);
                    break;
                }
            }
        }

        return success;
    }

    /**
     * Resolves all current requests for products.
     */
    private void resolveRequestsForTestResources() {
        if (!testMonitorsWaitingForTestingResources.isEmpty()) {

            if (configuration == null || configuration.isMaintenanceMode()) {
                // Notify test monitors about maintenance mode
                String notification = "Test Automation Service at " + serviceHostname + ":" + servicePort + " is currently in the maintenace mode."
                                        +" No tests will be allocated until maintenance mode will be switched off...";

                p(notification);

                for (TestMonitor testMonitor : testMonitorsWaitingForTestingResources) {
                    testMonitor.notifyListener(notification);
                }

                return;
            }

            // Extract all current configuration settings
            long maximalNumberOfTestsPerNode = configuration.getMaximalNumberOfTestsPerNode();
            long testResourcesExpectationTimeout = configuration.getTestResourcesExpectationTimeout();

            p("Trying to resolve all current requests for test resources...");

            // Sort test nodes according to their workloads
            // Test nodes will less number of executed tests will appear first
            List<TestNode> tempTestNodes = new ArrayList<TestNode>(testNodes);
            Collections.sort(tempTestNodes);
            testNodes = new ConcurrentLinkedQueue<TestNode>(tempTestNodes);

            List<TestMonitor> resolvedTestingResourceRequests = new ArrayList<TestMonitor>(0);

            // Check all currently issued requests for test resources
            for (TestMonitor testMonitor : testMonitorsWaitingForTestingResources) {
                Test test = testMonitor.getTest();
				p("Trying to reserve resources for the test '" + test.getId() + "'...");

                Test.Target target = test.getTarget();
                List<String> requiredEnvironments = testMonitor.getRequiredEnvironments();
                List<List<Pattern>> patternsForRequiredEnvironments = testMonitor.getPatternsForRequiredEnvironments();

                if (patternsForRequiredEnvironments.isEmpty()) {
                    // Flash targets always require environment specifications
                    if (target == Test.Target.FLASH) {
                        testMonitor.stopTest("Test '" + testMonitor.getTest().getId() + "' has specified target " + target
                                                + ", but hasn't given any of proper environment specifications.");
                        resolvedTestingResourceRequests.add(testMonitor);
                    }
                }

                // Allocate test node and product set matches for flashing test
                List<List<TestNode>> finalTestNodeMatches = new ArrayList<List<TestNode>>(0);
                List<List<List<Product>>> finalProductSetMatches = new ArrayList<List<List<Product>>>(0);

                // Allocate matching test node for nose test
                TestNode finalTestNodeMatch = null;

                if (target == Test.Target.FLASH) {
                    // Flash target always requires some physical environments: product sets, complementary devices, etc.

                    // Find out all test nodes which has some free products
                    List<TestNode> availableTestNodes = new ArrayList<TestNode>(0);
                    List<List<Product>> availableFreeProducts = new ArrayList<List<Product>>(0);

                    for (TestNode testNode : testNodes) {
                        if (!testNode.isMaintenanceMode()) {
                            long capacity = maximalNumberOfTestsPerNode - testNode.getNumberOfRunningTests();

                            if (capacity > 0) {
                                // This test node is able to execute some more tests
                                List<Product> freeProducts = new ArrayList<Product>(testNode.getFreeProducts());

                                if (!freeProducts.isEmpty()) {
                                    // This test node has some free products
                                    availableTestNodes.add(testNode);
                                    availableFreeProducts.add(freeProducts);
                                }
                            }
                        } else {
                            p("Test node " + testNode.getHostnameAndPort() + " is in the maintenance mode and cannot be used");
                        }
                    }

                    p("Test farm has " + availableTestNodes.size() + " capable test nodes");

                    // Test farm has some capable test nodes
                    if (!availableTestNodes.isEmpty()) {
                        // Try to search for each of environments required by the test
                        for (int i = 0; i < patternsForRequiredEnvironments.size(); i++) {
                            List<Pattern> patterns = patternsForRequiredEnvironments.get(i);
                            String requiredEnvironment = requiredEnvironments.get(i);

                            p("Test '" + test.getId() + "' has requested a product set pattern #" + (i + 1) + "/" + patternsForRequiredEnvironments.size() + ": '" + requiredEnvironment + "'");

                            // Right now a single environment set must be allocated from the same test node
                            // But since the same test may require many different environment sets,
                            // the same test could be splitted across many different test nodes

                            // Find out the number of test packages requiring current environment
                            int maximalNumberOfRequestedEnvironments = 0;
                            int totalNumberOfAvailableEnvironments = 0;

                            if (test.getNumberOfTestPackages() > 0) {
                                List<TestPackage> testPackages = test.getTestPackages();
                                for (TestPackage testPackage : testPackages) {
                                    if (testPackage.getRequiredEnvironment().equalsIgnoreCase(requiredEnvironment)) {
                                        // One more package has requested current environment
                                        maximalNumberOfRequestedEnvironments++;
                                    }
                                }
                            } else {
                                maximalNumberOfRequestedEnvironments = 1; // One and the same environment for the whole test
                            }

                            p("Test '" + test.getId() + "' has requested environment '" + requiredEnvironment + "' in " + maximalNumberOfRequestedEnvironments + " packages");

                            // Find test nodes and products sets that match current environment
                            List<TestNode> matchingTestNodes = new ArrayList<TestNode>(0);
                            List<List<Product>> matchingProductSets = new ArrayList<List<Product>>(0);

                            // Sort available test nodes according to their current workloads,
                            // so that less buzy test nodes will appear first
                            Collections.sort(availableTestNodes);

                            // Go through all capable and available test nodes
                            for (int tn = 0; tn < availableTestNodes.size(); tn++) {
                                TestNode testNode = availableTestNodes.get(tn);
                                List<Product> freeProducts = availableFreeProducts.get(tn);

                                // Each pattern stands for a single required product or some complementary physical device
                                if (freeProducts.size() >= patterns.size()) {
                                    // Get current capacity once more
                                    long capacity = maximalNumberOfTestsPerNode - testNode.getNumberOfRunningTests();

                                    do {
                                        // Try to find a matching set of all required products or complementary physical devices
                                        List<Product> currentMatchingProductSet = new ArrayList<Product>(0);

                                        // Check each of required product patterns
                                        for (int j = 0; j < patterns.size(); j++) {
                                            Pattern pattern = patterns.get(j);

                                            for (Product product : freeProducts) {
                                                if (pattern.matcher(product.getNameValuePairs()).matches()) {
                                                    // We've got a product which matches required pattern
                                                    // Now we must ensure that product isn't already taken by some other matching set
                                                    boolean canAdd = true;

                                                    for (List<List<Product>> finalProductSetMatch : finalProductSetMatches) {
                                                        for (List<Product> finalProductSet : finalProductSetMatch) {
                                                            if (finalProductSet.contains(product)) {
                                                                // This product is already taken by some product set
                                                                canAdd = false;
                                                                break;
                                                            }
                                                        }
                                                    }

                                                    if (canAdd) {
                                                        // If this product isn't yet taken, ensure that we don't have any dublicates
                                                        if (!currentMatchingProductSet.contains(product)) {
                                                            currentMatchingProductSet.add(product);
                                                            // A match for current pattern is found, now move to the next pattern
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (currentMatchingProductSet.size() == patterns.size()) {
                                            // Current product set has got maches for all required patterns

                                            // Remove matching products from the list of free products
                                            for (Product match : currentMatchingProductSet) {
                                                if (freeProducts.contains(match)) {
                                                    freeProducts.remove(match);
                                                }
                                            }

                                            // Add current match to the list of final matches
                                            matchingProductSets.add(currentMatchingProductSet);
                                            // Also remember on which test node this product set is available
                                            matchingTestNodes.add(testNode);

                                            // A single matching environment set means a single test: either the whole test or a splitted one
                                            totalNumberOfAvailableEnvironments++;
                                            capacity--;

                                            if (totalNumberOfAvailableEnvironments >= maximalNumberOfRequestedEnvironments) {
                                                // We've discovered just enough of required product sets
                                                p("Test '" + test.getId() + "' has got " + totalNumberOfAvailableEnvironments
                                                    + " (enough) product sets for required product set pattern #" + (i + 1) + "/" + patternsForRequiredEnvironments.size()
                                                    + ": '" + requiredEnvironment + "' on test node " + testNode.getHostnameAndPort());
                                                break;
                                            }
                                        } else {
                                            // Move to the next test node
                                            p("Test '" + test.getId() + "' couldn't get enough matches for required environment '"
                                                + requiredEnvironment + "' on test node " + testNode.getHostnameAndPort());
                                            break;
                                        }
                                    } while (capacity > 0 && freeProducts.size() >= patterns.size());
                                } else {
                                    p("Test '" + test.getId() + "' couldn't get enough of products from the test node " + testNode.getHostnameAndPort()
                                        + " - Test node has " + freeProducts.size() + " free products and " + patterns.size() + " were required at minimum");
                                }

                                if (totalNumberOfAvailableEnvironments >= maximalNumberOfRequestedEnvironments) {
                                    // We've discovered just enough of required product sets
                                    p("Test '" + test.getId() + "' got " + totalNumberOfAvailableEnvironments + " (enough) required product sets."
                                        + " Stop scanning test farm for a required product set pattern #" + (i + 1) + "/" + patternsForRequiredEnvironments.size()
                                        + ": '" + requiredEnvironment + "'");
                                    break;
                                }
                            }

                            // Store discovered product sets and the corresponding test nodes
                            p("Test '" + test.getId() + "' finally got " + totalNumberOfAvailableEnvironments + " available product sets out of "
                                + maximalNumberOfRequestedEnvironments + " requested, concerning the product set pattern #" + (i + 1) + "/" + patternsForRequiredEnvironments.size()
                                + ": '" + requiredEnvironment + "'");

                            if (totalNumberOfAvailableEnvironments > 0) {
                                // We've discovered at least one environment for the test
                                // Store matches for required environment
                                finalTestNodeMatches.add(matchingTestNodes);
                                finalProductSetMatches.add(matchingProductSets);
                            } else {
                                // Nothing was discovered at this time

                                if (!finalProductSetMatches.isEmpty()) {
                                    // At least test farm has something to offer
                                    p("Test '" + test.getId() + "' didn't get any matches for required environment '" + requiredEnvironment + "'. Stop scanning the test farm...");
                                    // Stop any other discoveries
                                    break;
                                }

                                // Otherwise just continue searches till at least something will be discovered
                            }
                        }
                    } else {
                        p("Test farm hasn't any capable test nodes. Stop scanning the test farm...");
                        break;
                    }
                } else if (target == Test.Target.NOSE) {
                    // Nose target doesn't require any physical products or complementary physical devices, but just test nodes
                    for (TestNode testNode : testNodes) {
                        if (!testNode.isMaintenanceMode()) {
                            long capacity = maximalNumberOfTestsPerNode - testNode.getNumberOfRunningTests();

                            if (capacity > 0) {
                                // This test node can run one more test
                                // At this point nose targets are not specifying any environment requirements
                                finalTestNodeMatch = testNode;
                                break; // Stop scanning the test farm
                            }
                        } else {
                            p("Test node " + testNode.getHostnameAndPort() + " is in the maintenance mode and cannot be used");
                        }
                    }
                } else {
                    // Unsupported target. Fail the test
                    testMonitor.stopTest("Test '" + testMonitor.getTest().getId() + "' has specified unsupported target " + target + ".");
                    resolvedTestingResourceRequests.add(testMonitor);
                }

                // At this point the test farm is scanned, so all discovered environments could be used for test allocations

                if (target == Test.Target.FLASH) {
                    p("Test '" + test.getId() + "' has got " + finalProductSetMatches.size() + " product set matches for " + requiredEnvironments.size() + " required environments");

                    if (!finalProductSetMatches.isEmpty()) {
                        // Test has got product set matches for all required environments
                        // Create a list for all splitted tests
                        List<Test> splittedTests = new ArrayList<Test>(0);
                        // Remember on which test nodes they are supposed to be started
                        List<TestNode> reservedTestNodes = new ArrayList<TestNode>(0);
                        // Remember which product sets were reserved for them
                        List<List<Product>> reservedProductSets = new ArrayList<List<Product>>(0);
                        // And what test packages has been handled
                        List<TestPackage> handledTestPackages = new ArrayList<TestPackage>(0);

                        // Try to split test as much as possible across each of required environments and available product set matches
                        for (int i = 0; i < finalProductSetMatches.size(); i++) {
                            // Get current environment
                            String requiredEnvironment = requiredEnvironments.get(i);
                            // Get the list of test nodes that are matching this environment
                            List<TestNode> matchingTestNodes = finalTestNodeMatches.get(i);
                            // Get the list of product sets that are matching this environment
                            List<List<Product>> matchingProductSets = finalProductSetMatches.get(i);

                            /*p("Test '" + test.getId() + "' has requested environment '" + requiredEnvironment + "' and has got it on:");
                            for (int t = 0; t < matchingTestNodes.size(); t++) {
                                TestNode testNode = matchingTestNodes.get(t);
                                List<Product> productSet = matchingProductSets.get(t);
                                p("Test node " + testNode.getHostnameAndPort());
                                for (Product product : productSet) {
                                    p("Has matching product with IMEI " + product.getIMEI() + " and of type " + product.getRMCode());
                                }
                            }*/

                            // Find out how much of packages has requested this environment
                            List<TestPackage> involvedTestPackages = new ArrayList<TestPackage>(0);

                            if (test.getNumberOfTestPackages() > 0) {
                                List<TestPackage> testPackages = test.getTestPackages();

                                for (TestPackage testPackage : testPackages) {
                                    if (testPackage.getRequiredEnvironment().equalsIgnoreCase(requiredEnvironment)) {
                                        // This package will be executed on current product set matches
                                        involvedTestPackages.add(testPackage);
                                    }
                                }
                            } else {
                                // Since test hasn't any packages, create a single virtual package that will simply contain all test artifacts
                                involvedTestPackages.add(new TestPackage(test.getId(), test.getArtifacts(), requiredEnvironment));
                            }

                            if (!involvedTestPackages.isEmpty()) {
                                // Study how much of test packages should get each of matching product sets
                                p("Test '" + test.getId() + "' has " + involvedTestPackages.size() + " test packages for requested environment '" + requiredEnvironment + "'");

                                int numberOfAvailableEnvironments = matchingProductSets.size();
                                int numberOfRequiredEnvironments = involvedTestPackages.size();

                                // Check if test could be splitted across available resource sets
                                double dR = (double) numberOfRequiredEnvironments / numberOfAvailableEnvironments;

                                // dR could be a rational or even irrational number
                                // So, here we're rounding up towards the next integer number
                                // Now nR will be the optimal number of test packages per each of available product sets
                                int nR = (int) Math.ceil(dR);

                                p("Test '" + test.getId() + "' has got " + numberOfAvailableEnvironments + " available environment sets for " + numberOfRequiredEnvironments + " of required."
                                    + " That makes " + numberOfRequiredEnvironments + "/" + numberOfAvailableEnvironments + " = " + dR + " of average number of packages per environment set"
                                    + " or " + nR + " if rounded up");

                                // Split test packages according to discovered optimal splitting coefficient nR
                                int tsIndex = 0;
                                List<List<TestPackage>> splittedTestPackageSets = new ArrayList<List<TestPackage>>(0);

                                do {
                                    List<TestPackage> splittedTestPackageSet = new ArrayList<TestPackage>(0);

                                    for (int t = tsIndex; t < (tsIndex + nR); t++) {
                                        if (t < involvedTestPackages.size()) {
                                            splittedTestPackageSet.add(involvedTestPackages.get(t));
                                        }
                                    }

                                    splittedTestPackageSets.add(splittedTestPackageSet);
                                    tsIndex += splittedTestPackageSet.size();

                                } while (tsIndex < involvedTestPackages.size());

                                // Finally create a set of splitted test
                                for (int s = 0; s < splittedTestPackageSets.size(); s++) {
                                    // Each splitted test will be allocated on each of available product sets with optimal number of test packages
                                    List<TestPackage> splittedTestPackageSet = splittedTestPackageSets.get(s);
                                    Test splittedTest = new Test(test); // Let's copy all important parameters from the original test

                                    // Add all files from the test packages as artifacts of this splitted test
                                    for (TestPackage testPackage : splittedTestPackageSet) {
                                        handledTestPackages.add(testPackage);

                                        List<String> files = testPackage.getFiles();

                                        for (String file : files) {
                                            splittedTest.addArtifact(file);
                                        }
                                    }

                                    // Clean the list of test packages
                                    splittedTest.setTestPackages(new ArrayList<TestPackage>(0));

                                    // Store the environment required by all current test packages
                                    splittedTest.setRequiredEnvironment(requiredEnvironment);

                                    // Store current splitted test
                                    splittedTests.add(splittedTest);
                                    // Also remember on which test node this test will be started
                                    reservedTestNodes.add(matchingTestNodes.get(s));
                                    // And which product set will be reserved for this test
                                    reservedProductSets.add(matchingProductSets.get(s));
                                }
                            }
                        }

                        // Try to launch all created splitted tests
                        p("Test '" + test.getId() + "' will finally try to launch " + splittedTests.size() + " test handlers");

                        if (!splittedTests.isEmpty()) {
                            // Each of splitted test will get a single test handler
                            List<TestHandler> testHandlers = new ArrayList<TestHandler>(0);
                            int subIdIndex = testMonitor.getSubIdIndex();
                            boolean useSubIds = true;

                            // Check if sub-id is needed at all
                            if (splittedTests.size() == 1) {
                                // We have only a single splitted test, check if it has taken all handled test packages

                                // Create a copy of all original test packages
                                List<TestPackage> originalTestPackages = new ArrayList<TestPackage>(test.getTestPackages());

                                if (!originalTestPackages.isEmpty()) {
                                    // Remove all handled packages
                                    for (TestPackage handledTestPackage : handledTestPackages) {
                                        for (TestPackage originalTestPackage : originalTestPackages) {
                                            if (originalTestPackage.getRequiredEnvironment().equals(handledTestPackage.getRequiredEnvironment())) {
                                                originalTestPackages.remove(originalTestPackage);
                                                break;
                                            }
                                        }
                                    }
                                }

                                // Now if all original test packages are handled and there weren't any other tests, there is no need for a sub-id
                                if (originalTestPackages.isEmpty() && subIdIndex == 1) {
                                    p("Test '" + test.getId() + "' will use all test packages inside the one test. Sub-id usage will be skipped");
                                    useSubIds = false;
                                } else {
                                    useSubIds = true;
                                }
                            }

                            // Specify sub-ids for splitted tests and ensure proper product reservations and reserve matching products

                            // Calculate the proper products reservation time, which is the remaining time for the whole test
                            long reservationTimeout = test.getTimeout() - (System.currentTimeMillis() - testMonitor.getTestHandlingStartTime());

                            for (int i = 0; i < splittedTests.size(); i++) {
                                Test splittedTest = splittedTests.get(i);

                                // Each of splitted test should get some sub-id, so that its full id will be: "original test id" + "sub-id"
                                if (useSubIds) {
                                    splittedTest.setSubId("_" + subIdIndex);
                                    subIdIndex++;
                                } else {
                                    splittedTest.setSubId("");
                                }

                                // Get the test node on which this test will be started
                                TestNode reservedTestNode = reservedTestNodes.get(i);
                                // Get the product set that will be reserved for this test
                                List<Product> reservedProductSet = reservedProductSets.get(i);

                                // Perform operation of reserving products for this test and for calculated remaining time
                                List<Product> reservedProducts = reservedTestNode.reserveProducts(splittedTest, reservedProductSet, reservationTimeout);

                                // Ensure that reserving was fine
                                if (!reservedProducts.isEmpty() && reservedProducts.size() == reservedProductSet.size()) {
                                    // If product reserving was fine, we can create a test handler
                                    splittedTest.setReservedProducts(reservedProducts);
                                    for(Product p : reservedProducts) {
                                    	this.createMessage( "Usage_"+p.getRole()+"_"+p.getRMCode()+"_"+p.getSn(), "Product:" + p.toString()+" have been reserved and used by "+splittedTest.getId() );
                                    }
                                    TestHandler testHandler = new TestHandler(self, testMonitor, splittedTest, reservedTestNode, reservedProducts);
                                    testHandlers.add(testHandler);
                                    // Notify test node about the test it will run
                                    reservedTestNode.increaseNumberOfRunningTests(splittedTest);
                                    p("Test '" + test.getId() + "' will start a splitted test '" + splittedTest.getRuntimeId() + "'");
                                } else {
                                    // Otherwise stop allocating splitted tests
                                    p("Test '" + test.getId() + "' got an error while tried to reserve products from the test node " + reservedTestNode.getHostnameAndPort());
                                    if (reservedProducts == null) {
                                        p("Test '" + test.getId() + "' has got NULL instead of the list of reserved products");
                                    } else {
                                        p("Test '" + test.getId() + "' has got " + reservedProducts.size() + " reserved products out of " + reservedProductSet.size() + " required");
                                    }
                                    break;
                                }
                            }

                            // Ensure that each of splitted tests has got a test handler
                            if (!testHandlers.isEmpty() && testHandlers.size() == splittedTests.size()) {
                                /*p("Test '" + test.getId() + "' will finally launch the following test handlers:");
                                for (TestHandler testHandler : testHandlers) {
                                    p("A test handler '" + testHandler.getName() + "' for the test '" + test.getId() + "'");
                                }*/

                                // Notify test monitor about created test handlers
                                testMonitor.addTestHandlers(testHandlers);

                                // Remove splitted packages from the test
                                List<TestPackage> testPackages = test.getTestPackages();

                                if (!testPackages.isEmpty()) {
                                    for (TestPackage handledTestPackage : handledTestPackages) {
                                        for (TestPackage originalTestPackage : testPackages) {
                                            if (originalTestPackage.getRequiredEnvironment().equals(handledTestPackage.getRequiredEnvironment())) {
                                                p("Test '" + test.getId() + "' contained package '" + handledTestPackage.getId() + "'");
                                                if (testPackages.remove(originalTestPackage)) {
                                                    p("Test '" + test.getId() + "' has removed the package '" + handledTestPackage.getId() + "' as the handled one");
                                                }
                                                break;
                                            }
                                        }
                                    }

                                    // Update the list of test packages in test
                                    /*p("Test '" + test.getId() + "' has got the following updated list of test packages:");
                                    for (TestPackage testPackage : testPackages) {
                                        p("Test '" + test.getId() + "' now will have a test package: " + testPackage);
                                    }*/

                                    test.setTestPackages(testPackages);

                                    //p("Test '" + test.getId() + "' is now updated to:\n" + test);

                                    testMonitor.updateMonitoredTest(test);
                                }

                                // This test has got all required environments from the test, so it can be removed from the list of resource requesters
                                if (test.getTestPackages().isEmpty()) {
                                    p("Test '" + test.getId() + "' has proceeded all of its test packages and so, can be removed from the list of resource requesters");
                                    resolvedTestingResourceRequests.add(testMonitor);
                                }

                            } else {
                                // Something went wrong, so all product sets should be released
                                p("Test '" + test.getId() + "' has got a problem during resevation of product sets. All involved product sets will be now automatically released...");

                                for (TestHandler testHandler : testHandlers) {
                                    TestNode reservedTestNode = testHandler.getReservedTestNode();
                                    List<Product> reservedProducts = testHandler.getReservedProducts();

                                    if (reservedTestNode != null) {
                                        reservedTestNode.freeProducts(reservedProducts);
                                        reservedTestNode.decreaseNumberOfRunningTests(testHandler.getTest(), true);
                                    }
                                }

                                // Check test timeouts
                                if ((System.currentTimeMillis() - testMonitor.getTestHandlingStartTime()) > testResourcesExpectationTimeout) {
                                    // Stop the test due to expired product expectation
                                    testMonitor.stopTest("Test '" + testMonitor.getTest().getId()
                                                            + "' hasn't seen any suitable products from this Test Automation Service"
                                                            + " for more than a test resource expectation timeout has allowed ("
                                                            + Util.convert(testResourcesExpectationTimeout) + ")");
                                    resolvedTestingResourceRequests.add(testMonitor);
                                } else if (testMonitor.isTestExpired()) {
                                    // Stop the test due to timeout expiration
                                    testMonitor.stopTest("Test '" + testMonitor.getTest().getId() + "' has got expiration of its timeout"
                                                            + " before suitable test resources were available");
                                    resolvedTestingResourceRequests.add(testMonitor);
                                }
                            }
                        } else {
                            p("Test '" + test.getId() + "' couldn't create splitted tests. The test description is the following:\n" + test);
                        }
                    } else {
                        // Test hasn't got all of required environments
                        p("Test '" + test.getId() + "' CANNOT be started due to unavailability of all required environments");
                        // Check timeouts for this test
                        if ((System.currentTimeMillis() - testMonitor.getTestHandlingStartTime()) > testResourcesExpectationTimeout) {
                            // Stop the test due to expired product expectation
                            testMonitor.stopTest("Test '" + testMonitor.getTest().getId()
                                                    + "' hasn't seen any suitable products from this Test Automation Service"
                                                    + " for more than a test resource expectation timeout has allowed ("
                                                    + Util.convert(testResourcesExpectationTimeout) + ")");
                            resolvedTestingResourceRequests.add(testMonitor);
                        } else if (testMonitor.isTestExpired()) {
                            // Stop the test due to timeout expiration
                            testMonitor.stopTest("Test '" + testMonitor.getTest().getId() + "' has got expiration of its timeout"
                                                    + " before suitable test resources were available");
                            resolvedTestingResourceRequests.add(testMonitor);
                        }
                    }
                } else if (target == Test.Target.NOSE) {
                    // Nose test is requiring a single test node

                    if (finalTestNodeMatch != null) {
                        // Create a single handler for a single test
                        p("Test '" + test.getId() + "' CAN be started on the test node " + finalTestNodeMatch.getHostnameAndPort());

                        // Create a new instance of the test for easier handling
                        Test noseTest = new Test(test);

                        // Add files from all test packages as artifacts of this test
                        List<TestPackage> testPackages = test.getTestPackages();

                        for (TestPackage testPackage : testPackages) {
                            List<String> files = testPackage.getFiles();

                            for (String file : files) {
                                if (file != null && !file.isEmpty()) {
                                    noseTest.addArtifact(file);
                                }
                            }
                        }

                        // Clear the list of test packages
                        noseTest.setTestPackages(new ArrayList<TestPackage>(0));

                        // Since there are not splitted tests, there are now sub-ids
                        noseTest.setSubId("");

                        // Create a test handler for this test
                        List<TestHandler> testHandlers = new ArrayList<TestHandler>(0);

                        TestHandler testHandler = new TestHandler(self, testMonitor, noseTest, finalTestNodeMatch, new ArrayList<Product>(0));
                        testHandlers.add(testHandler);
                        // Notify test node about the test it will run
                        finalTestNodeMatch.increaseNumberOfRunningTests(noseTest);

                        /*p("Test '" + test.getId() + "' will finally launch the following test handlers:");
                        for (TestHandler testHandler : testHandlers) {
                            p("A test handler '" + testHandler.getName() + "' for the test '" + test.getId() + "'");
                        }*/

                        // Notify test monitor about successful test allocation
                        testMonitor.addTestHandlers(testHandlers);

                        // Remove test monitor from the list of resouce requesters
                        resolvedTestingResourceRequests.add(testMonitor);

                    } else {
                        // Test farm hasn't any capable test nodes
                        p("Test '" + test.getId() + "' CANNOT be started due to unavailability of all required environments");
                        // Check test timeouts
                        if ((System.currentTimeMillis() - testMonitor.getTestHandlingStartTime()) > testResourcesExpectationTimeout) {
                            // Stop the test due to expired product expectation
                            testMonitor.stopTest("Test '" + testMonitor.getTest().getId()
                                                    + "' hasn't seen any suitable products from this Test Automation Service"
                                                    + " for more than a test resource expectation timeout has allowed ("
                                                    + Util.convert(testResourcesExpectationTimeout) + ")");
                            resolvedTestingResourceRequests.add(testMonitor);
                        } else if (testMonitor.isTestExpired()) {
                            // Stop the test due to timeout expiration
                            testMonitor.stopTest("Test '" + testMonitor.getTest().getId() + "' has got expiration of its timeout"
                                                    + " before suitable test resources were available");
                            resolvedTestingResourceRequests.add(testMonitor);
                        }
                    }
                } else {
                    // Test has specified unsupported target and so, will be failed
                    testMonitor.stopTest("Test '" + testMonitor.getTest().getId() + "' has specified unsupported target " + target + ".");
                    resolvedTestingResourceRequests.add(testMonitor);
                }
            }

            for (TestMonitor resolvedProductRequest : resolvedTestingResourceRequests) {
                if (removeRequestForTestingResources(resolvedProductRequest)) {
                    // Notify test monitor that all its requests are now handled
                    resolvedProductRequest.setIsReservingTestResources(false);
					p("Monitor of the test '" + resolvedProductRequest.getName() + "' was successfully removed from the list of test resource requesters");
                } else {
                    p("Error: Couldn't remove monitor of the test '" + resolvedProductRequest.getName() + "' from the list of test resource requesters");
                }
            }

            /*
            // DEBUG: Show current test farm
            String testFarmDescription = "\nCurrent test farm:\n";

            for (TestNode testNode : testNodes) {
                //p(testNode.getHostnameAndPort() + " with current load " + testNode.getNumberOfRunningTests() + "/" + maximalNumberOfTestsPerNode + ":");
                testFarmDescription += "\n" + testNode.getHostnameAndPort() + ":";
                List<Product> products = testNode.getProducts();
                if (!products.isEmpty()) {
                    for (Product product : products) {
                        testFarmDescription += "\n\t" + product.getIMEI() + " - " + product.getRMCode() + " " + product.getStatusDetails();
                    }
                } else {
                    testFarmDescription += "\n\t Has NO products";
                }
            }

            p("Current test farm: " + testFarmDescription + "\n");
            //END DEBUG*/
        }
    }
    
    /**
     * Resolves all current requests for test restarts.
     */
    private void resolveRequestsForTestRestarts() {
        if (!testMonitorsWaitingForTestRestarts.isEmpty()) {
            if (configuration == null || configuration.isMaintenanceMode()) {
                // Notify test monitors about maintenance mode
                String notification = "Test Automation Service at " + serviceHostname + ":" + servicePort + " is currently in the maintenace mode."
                                        +" No tests will be restarted until maintenance mode will be switched off...";

                p(notification);

                for (TestMonitor testMonitor : testMonitorsWaitingForTestRestarts) {
                    testMonitor.notifyListener(notification);
                }

                return;
            }

            // Extract all current configuration settings
            long maximalNumberOfTestsPerNode = configuration.getMaximalNumberOfTestsPerNode();
            long testResourcesExpectationTimeout = configuration.getTestResourcesExpectationTimeout();

            p("Trying to resolve all current requests for test restarts...");

            // Sort test nodes according to their workloads
            // Test nodes will less number of executed tests will appear first
            List<TestNode> tempTestNodes = new ArrayList<TestNode>(testNodes);
            Collections.sort(tempTestNodes);
            testNodes = new ConcurrentLinkedQueue<TestNode>(tempTestNodes);

            List<TestMonitor> resolvedTestRestartRequests = new ArrayList<TestMonitor>(0);

            // Check all currently issued requests for test restarts
            for (TestMonitor testMonitor : testMonitorsWaitingForTestRestarts) {
                List<Test> testsToBeRestarted = testMonitor.getTestsToBeRestarted();
                
                // Always check that test are on the list
                if (testsToBeRestarted.isEmpty()) {
                    resolvedTestRestartRequests.add(testMonitor);
                }

                for (Test testToBeRestarted : testsToBeRestarted) {
                    p("Trying to reserve resources for the test '" + testToBeRestarted.getRuntimeId() + "'...");

                    Test.Target target = testToBeRestarted.getTarget();
                    List<String> requiredEnvironments = new ArrayList<String>(0);
                    requiredEnvironments.add(testToBeRestarted.getRequiredEnvironment());

                    //List<List<Pattern>> patternsForRequiredEnvironments = testMonitor.getPatternsForRequiredEnvironments();
                    List<List<Pattern>> patternsForRequiredEnvironments = new ArrayList<List<Pattern>>(0);
                    
                    // Create lists of regular expressions describing all required environments
                    for (String environment : requiredEnvironments) {
                        List<String> regularExpressions = Util.createRegularExpressions(environment);
                        if (regularExpressions != null && !regularExpressions.isEmpty()) {

                            // Turn regular expressions into patterns
                            List<Pattern> patterns = new ArrayList<Pattern>(0);

                            for (String regularExpression : regularExpressions) {
                                patterns.add(Pattern.compile(regularExpression));
                            }

                            patternsForRequiredEnvironments.add(patterns);
                        }
                    }

                    if (patternsForRequiredEnvironments.isEmpty()) {
                        // Flash targets always require environment specifications
                        if (target == Test.Target.FLASH) {
                            testMonitor.stopTest("Test '" + testToBeRestarted.getRuntimeId() + "' has specified target " + target
                                                    + ", but hasn't given any of proper environment specifications.");
                            resolvedTestRestartRequests.add(testMonitor);
                        }
                    }

                    // Allocate test node and product set matches for flashing test
                    List<List<TestNode>> finalTestNodeMatches = new ArrayList<List<TestNode>>(0);
                    List<List<List<Product>>> finalProductSetMatches = new ArrayList<List<List<Product>>>(0);

                    // Allocate matching test node for nose test
                    TestNode finalTestNodeMatch = null;

                    if (target == Test.Target.FLASH) {
                        // Flash target always requires some physical environments: product sets, complementary devices, etc.

                        // Find out all test nodes which has some free products
                        List<TestNode> availableTestNodes = new ArrayList<TestNode>(0);
                        List<List<Product>> availableFreeProducts = new ArrayList<List<Product>>(0);

                        for (TestNode testNode : testNodes) {
                            if (!testNode.isMaintenanceMode()) {
                                long capacity = maximalNumberOfTestsPerNode - testNode.getNumberOfRunningTests();

                                if (capacity > 0) {
                                    // This test node is able to execute some more tests
                                    List<Product> freeProducts = new ArrayList<Product>(testNode.getFreeProducts());

                                    if (!freeProducts.isEmpty()) {
                                        // This test node has some free products
                                        availableTestNodes.add(testNode);
                                        availableFreeProducts.add(freeProducts);
                                    }
                                }
                            } else {
                                p("Test node " + testNode.getHostnameAndPort() + " is in the maintenance mode and cannot be used");
                            }
                        }

                        p("Test farm has " + availableTestNodes.size() + " capable test nodes");

                        // Test farm has some capable test nodes
                        if (!availableTestNodes.isEmpty()) {
                            // Try to search for each of environments required by the test
                            for (int i = 0; i < patternsForRequiredEnvironments.size(); i++) {
                                List<Pattern> patterns = patternsForRequiredEnvironments.get(i);
                                String requiredEnvironment = requiredEnvironments.get(i);

                                p("Test '" + testToBeRestarted.getRuntimeId() + "' has requested a product set pattern #" + (i + 1) + "/" + patternsForRequiredEnvironments.size() + ": '" + requiredEnvironment + "'");

                                // Right now a single environment set must be allocated from the same test node
                                // But since the same test may require many different environment sets,
                                // the same test could be splitted across many different test nodes

                                // Find out the number of test packages requiring current environment
                                int maximalNumberOfRequestedEnvironments = 0;
                                int totalNumberOfAvailableEnvironments = 0;

                                if (testToBeRestarted.getNumberOfTestPackages() > 0) {
                                    List<TestPackage> testPackages = testToBeRestarted.getTestPackages();
                                    for (TestPackage testPackage : testPackages) {
                                        if (testPackage.getRequiredEnvironment().equalsIgnoreCase(requiredEnvironment)) {
                                            // One more package has requested current environment
                                            maximalNumberOfRequestedEnvironments++;
                                        }
                                    }
                                } else {
                                    maximalNumberOfRequestedEnvironments = 1; // One and the same environment for the whole test
                                }

                                p("Test '" + testToBeRestarted.getRuntimeId() + "' has requested environment '" + requiredEnvironment + "' in " + maximalNumberOfRequestedEnvironments + " packages");

                                // Find test nodes and products sets that match current environment
                                List<TestNode> matchingTestNodes = new ArrayList<TestNode>(0);
                                List<List<Product>> matchingProductSets = new ArrayList<List<Product>>(0);

                                // Sort available test nodes according to their current workloads,
                                // so that less buzy test nodes will appear first
                                Collections.sort(availableTestNodes);

                                // Go through all capable and available test nodes
                                for (int tn = 0; tn < availableTestNodes.size(); tn++) {
                                    TestNode testNode = availableTestNodes.get(tn);
                                    List<Product> freeProducts = availableFreeProducts.get(tn);

                                    // Each pattern stands for a single required product or some complementary physical device
                                    if (freeProducts.size() >= patterns.size()) {
                                        // Get current capacity once more
                                        long capacity = maximalNumberOfTestsPerNode - testNode.getNumberOfRunningTests();

                                        do {
                                            // Try to find a matching set of all required products or complementary physical devices
                                            List<Product> currentMatchingProductSet = new ArrayList<Product>(0);

                                            // Check each of required product patterns
                                            for (int j = 0; j < patterns.size(); j++) {
                                                Pattern pattern = patterns.get(j);

                                                for (Product product : freeProducts) {
                                                    if (pattern.matcher(product.getNameValuePairs()).matches()) {
                                                        // We've got a product which matches required pattern
                                                        // Now we must ensure that product isn't already taken by some other matching set
                                                        boolean canAdd = true;

                                                        for (List<List<Product>> finalProductSetMatch : finalProductSetMatches) {
                                                            for (List<Product> finalProductSet : finalProductSetMatch) {
                                                                if (finalProductSet.contains(product)) {
                                                                    // This product is already taken by some product set
                                                                    canAdd = false;
                                                                    break;
                                                                }
                                                            }
                                                        }

                                                        if (canAdd) {
                                                            // If this product isn't yet taken, ensure that we don't have any dublicates
                                                            if (!currentMatchingProductSet.contains(product)) {
                                                                currentMatchingProductSet.add(product);
                                                                // A match for current pattern is found, now move to the next pattern
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (currentMatchingProductSet.size() == patterns.size()) {
                                                // Current product set has got maches for all required patterns

                                                // Remove matching products from the list of free products
                                                for (Product match : currentMatchingProductSet) {
                                                    if (freeProducts.contains(match)) {
                                                        freeProducts.remove(match);
                                                    }
                                                }

                                                // Add current match to the list of final matches
                                                matchingProductSets.add(currentMatchingProductSet);
                                                // Also remember on which test node this product set is available
                                                matchingTestNodes.add(testNode);

                                                // A single matching environment set means a single test: either the whole test or a splitted one
                                                totalNumberOfAvailableEnvironments++;
                                                capacity--;

                                                if (totalNumberOfAvailableEnvironments >= maximalNumberOfRequestedEnvironments) {
                                                    // We've discovered just enough of required product sets
                                                    p("Test '" + testToBeRestarted.getRuntimeId() + "' has got " + totalNumberOfAvailableEnvironments
                                                        + " (enough) product sets for required product set pattern #" + (i + 1) + "/" + patternsForRequiredEnvironments.size()
                                                        + ": '" + requiredEnvironment + "' on test node " + testNode.getHostnameAndPort());
                                                    break;
                                                }
                                            } else {
                                                // Move to the next test node
                                                p("Test '" + testToBeRestarted.getRuntimeId() + "' couldn't get enough matches for required environment '"
                                                    + requiredEnvironment + "' on test node " + testNode.getHostnameAndPort());
                                                break;
                                            }
                                        } while (capacity > 0 && freeProducts.size() >= patterns.size());
                                    } else {
                                        p("Test '" + testToBeRestarted.getRuntimeId() + "' couldn't get enough of products from the test node " + testNode.getHostnameAndPort()
                                            + " - Test node has " + freeProducts.size() + " free products and " + patterns.size() + " were required at minimum");
                                    }

                                    if (totalNumberOfAvailableEnvironments >= maximalNumberOfRequestedEnvironments) {
                                        // We've discovered just enough of required product sets
                                        p("Test '" + testToBeRestarted.getRuntimeId() + "' got " + totalNumberOfAvailableEnvironments + " (enough) required product sets."
                                            + " Stop scanning test farm for a required product set pattern #" + (i + 1) + "/" + patternsForRequiredEnvironments.size()
                                            + ": '" + requiredEnvironment + "'");
                                        break;
                                    }
                                }

                                // Store discovered product sets and the corresponding test nodes
                                p("Test '" + testToBeRestarted.getRuntimeId() + "' finally got " + totalNumberOfAvailableEnvironments + " available product sets out of "
                                    + maximalNumberOfRequestedEnvironments + " requested, concerning the product set pattern #" + (i + 1) + "/" + patternsForRequiredEnvironments.size()
                                    + ": '" + requiredEnvironment + "'");

                                if (totalNumberOfAvailableEnvironments > 0) {
                                    // We've discovered at least one environment for the test
                                    // Store matches for required environment
                                    finalTestNodeMatches.add(matchingTestNodes);
                                    finalProductSetMatches.add(matchingProductSets);
                                } else {
                                    // Nothing was discovered at this time

                                    if (!finalProductSetMatches.isEmpty()) {
                                        // At least test farm has something to offer
                                        p("Test '" + testToBeRestarted.getRuntimeId() + "' didn't get any matches for required environment '" + requiredEnvironment + "'. Stop scanning the test farm...");
                                        // Stop any other discoveries
                                        break;
                                    }

                                    // Otherwise just continue searches till at least something will be discovered
                                }
                            }
                        } else {
                            p("Test farm hasn't any capable test nodes. Stop scanning the test farm...");
                            break;
                        }
                    } else if (target == Test.Target.NOSE) {
                        // Nose target doesn't require any physical products or complementary physical devices, but just test nodes
                        for (TestNode testNode : testNodes) {
                            if (!testNode.isMaintenanceMode()) {
                                long capacity = maximalNumberOfTestsPerNode - testNode.getNumberOfRunningTests();

                                if (capacity > 0) {
                                    // This test node can run one more test
                                    // At this point nose targets are not specifying any environment requirements
                                    finalTestNodeMatch = testNode;
                                    break; // Stop scanning the test farm
                                }
                            } else {
                                p("Test node " + testNode.getHostnameAndPort() + " is in the maintenance mode and cannot be used");
                            }
                        }
                    } else {
                        // Unsupported target. Fail the test
                        testMonitor.stopTest("Test '" + testMonitor.getTest().getId() + "' has specified unsupported target " + target + ".");
                        resolvedTestRestartRequests.add(testMonitor);
                    }

                    // At this point the test farm is scanned, so all discovered environments could be used for test allocations

                    if (target == Test.Target.FLASH) {
                        p("Test '" + testToBeRestarted.getRuntimeId() + "' has got " + finalProductSetMatches.size() + " product set matches for "
                                + requiredEnvironments.size() + " required environments");

                        if (!finalProductSetMatches.isEmpty()) {
                            // Test has got product set matches for all required environments

                            // Remember on which test nodes they are supposed to be started
                            List<TestNode> reservedTestNodes = new ArrayList<TestNode>(0);
                            // Remember which product sets were reserved for them
                            List<List<Product>> reservedProductSets = new ArrayList<List<Product>>(0);

                            // Try to split test as much as possible across each of required environments and available product set matches
                            for (int i = 0; i < finalProductSetMatches.size(); i++) {
                                // Get current environment
                                String requiredEnvironment = requiredEnvironments.get(i);
                                // Get the list of test nodes that are matching this environment
                                List<TestNode> matchingTestNodes = finalTestNodeMatches.get(i);
                                // Get the list of product sets that are matching this environment
                                List<List<Product>> matchingProductSets = finalProductSetMatches.get(i);

                                p("Test '" + testToBeRestarted.getRuntimeId() + "' has requested environment '" + requiredEnvironment + "' and has got it on:");
                                for (int t = 0; t < matchingTestNodes.size(); t++) {
                                    TestNode testNode = matchingTestNodes.get(t);
                                    List<Product> productSet = matchingProductSets.get(t);
                                    p("Test node " + testNode.getHostnameAndPort());
                                    for (Product product : productSet) {
                                        p("And on its product with IMEI " + product.getIMEI() + " and of type " + product.getRMCode());
                                    }
                                }
                                
                                // Take the very first mathing test node and product set
                                reservedTestNodes.add(matchingTestNodes.get(0));
                                reservedProductSets.add(matchingProductSets.get(0));

                                break;
                            }

                            // Try to restart this test
                            p("Test '" + testToBeRestarted.getRuntimeId() + "' will try to launch a new test handler");

                            // Calculate the proper products reservation time, which is the remaining time for the whole test
                            long reservationTimeout = testToBeRestarted.getTimeout() - (System.currentTimeMillis() - testMonitor.getTestHandlingStartTime());
                            
                            List<TestHandler> testHandlers = new ArrayList<TestHandler>(0);

                            for (int i = 0; i < reservedTestNodes.size(); i++) {
                                // Get the test node on which this test will be restarted
                                TestNode reservedTestNode = reservedTestNodes.get(i);
                                // Get the product set that will be reserved for this test
                                List<Product> reservedProductSet = reservedProductSets.get(i);

                                // Perform operation of reserving products for this test and for calculated remaining time
                                List<Product> reservedProducts = reservedTestNode.reserveProducts(testToBeRestarted, reservedProductSet, reservationTimeout);

                                // Ensure that reserving was fine
                                if (!reservedProducts.isEmpty() && reservedProducts.size() == reservedProductSet.size()) {
                                    // If product reserving was fine, we can create a test handler
                                    testToBeRestarted.setReservedProducts(reservedProducts);

                                    TestHandler testHandler = new TestHandler(self, testMonitor, testToBeRestarted, reservedTestNode, reservedProducts);
                                    testHandlers.add(testHandler);
                                    // Notify test node about the test it will run
                                    reservedTestNode.increaseNumberOfRunningTests(testToBeRestarted);
                                    p("Test '" + testToBeRestarted.getRuntimeId() + "' will be restarted on test node " + reservedTestNode.getHostnameAndPort());
                                } else {
                                    // Otherwise stop allocating splitted tests
                                    p("Test '" + testToBeRestarted.getRuntimeId() + "' got an error while tried to reserve products from the test node " + reservedTestNode.getHostnameAndPort());
                                    if (reservedProducts == null) {
                                        p("Test '" + testToBeRestarted.getRuntimeId() + "' has got NULL instead of the list of reserved products");
                                    } else {
                                        p("Test '" + testToBeRestarted.getRuntimeId() + "' has got " + reservedProducts.size() + " reserved products out of " + reservedProductSet.size() + " required");
                                    }
                                    break;
                                }
                            }

                            // Ensure that we have created test handler
                            if (!testHandlers.isEmpty()) {
                                /*p("Test '" + test.getId() + "' will finally launch the following test handlers:");
                                for (TestHandler testHandler : testHandlers) {
                                    p("A test handler '" + testHandler.getName() + "' for the test '" + test.getId() + "'");
                                }*/

                                // Notify test monitor about restarted test and created test handlers
                                testMonitor.removeTestToBeRestarted(testToBeRestarted);
                                testMonitor.addTestHandlers(testHandlers);

                            } else {
                                // Something went wrong, so all product sets should be released
                                p("Test '" + testToBeRestarted.getId() + "' has got a problem during resevation of product sets. All involved product sets will be now automatically released...");

                                for (TestHandler testHandler : testHandlers) {
                                    TestNode reservedTestNode = testHandler.getReservedTestNode();
                                    List<Product> reservedProducts = testHandler.getReservedProducts();

                                    if (reservedTestNode != null) {
                                        reservedTestNode.freeProducts(reservedProducts);
                                        reservedTestNode.decreaseNumberOfRunningTests(testHandler.getTest(), true);
                                    }
                                }

                                // Check test timeouts
                                if ((System.currentTimeMillis() - testMonitor.getTestHandlingStartTime()) > testResourcesExpectationTimeout) {
                                    // Stop the test due to expired product expectation
                                    testMonitor.stopTest("Test '" + testMonitor.getTest().getId()
                                                            + "' hasn't seen any suitable products from this Test Automation Service"
                                                            + " for more than a test resource expectation timeout has allowed ("
                                                            + Util.convert(testResourcesExpectationTimeout) + ")");
                                    resolvedTestRestartRequests.add(testMonitor);
                                } else if (testMonitor.isTestExpired()) {
                                    // Stop the test due to timeout expiration
                                    testMonitor.stopTest("Test '" + testMonitor.getTest().getId() + "' has got expiration of its timeout"
                                                            + " before suitable test resources were available");
                                    resolvedTestRestartRequests.add(testMonitor);
                                }
                            }
                        } else {
                            // Test hasn't got all of required environments
                            p("Test '" + testToBeRestarted.getRuntimeId() + "' CANNOT be started due to unavailability of all required environments");
                            // Check timeouts for this test
                            if ((System.currentTimeMillis() - testMonitor.getTestHandlingStartTime()) > testResourcesExpectationTimeout) {
                                // Stop the test due to expired product expectation
                                testMonitor.stopTest("Test '" + testMonitor.getTest().getId()
                                                        + "' hasn't seen any suitable products from this Test Automation Service"
                                                        + " for more than a test resource expectation timeout has allowed ("
                                                        + Util.convert(testResourcesExpectationTimeout) + ")");
                                resolvedTestRestartRequests.add(testMonitor);
                            } else if (testMonitor.isTestExpired()) {
                                // Stop the test due to timeout expiration
                                testMonitor.stopTest("Test '" + testMonitor.getTest().getId() + "' has got expiration of its timeout"
                                                        + " before suitable test resources were available");
                                resolvedTestRestartRequests.add(testMonitor);
                            }
                        }
                    } else if (target == Test.Target.NOSE) {
                        // Nose test is requiring a single test node

                        if (finalTestNodeMatch != null) {
                            // Create a single handler for a single test
                            p("Test '" + testToBeRestarted.getRuntimeId() + "' CAN be started on the test node " + finalTestNodeMatch.getHostnameAndPort());

                            // Create a new instance of the test for easier handling
                            Test noseTest = new Test(testToBeRestarted);

                            // Clear the list of test packages
                            noseTest.setTestPackages(new ArrayList<TestPackage>(0));

                            // Since there are not splitted tests, there are now sub-ids
                            noseTest.setSubId("");

                            // Create a test handler for this test
                            List<TestHandler> testHandlers = new ArrayList<TestHandler>(0);

                            TestHandler testHandler = new TestHandler(self, testMonitor, noseTest, finalTestNodeMatch, new ArrayList<Product>(0));
                            testHandlers.add(testHandler);
                            // Notify test node about the test it will run
                            finalTestNodeMatch.increaseNumberOfRunningTests(noseTest);

                            /*p("Test '" + test.getId() + "' will finally launch the following test handlers:");
                            for (TestHandler testHandler : testHandlers) {
                                p("A test handler '" + testHandler.getName() + "' for the test '" + test.getId() + "'");
                            }*/

                            // Notify test monitor about successful test allocation
                            testMonitor.addTestHandlers(testHandlers);

                            // Remove test monitor from the list of resouce requesters
                            resolvedTestRestartRequests.add(testMonitor);

                        } else {
                            // Test farm hasn't any capable test nodes
                            p("Test '" + testToBeRestarted.getRuntimeId() + "' CANNOT be started due to unavailability of all required environments");
                            // Check test timeouts
                            if ((System.currentTimeMillis() - testMonitor.getTestHandlingStartTime()) > testResourcesExpectationTimeout) {
                                // Stop the test due to expired product expectation
                                testMonitor.stopTest("Test '" + testMonitor.getTest().getId()
                                                        + "' hasn't seen any suitable products from this Test Automation Service"
                                                        + " for more than a test resource expectation timeout has allowed ("
                                                        + Util.convert(testResourcesExpectationTimeout) + ")");
                                resolvedTestRestartRequests.add(testMonitor);
                            } else if (testMonitor.isTestExpired()) {
                                // Stop the test due to timeout expiration
                                testMonitor.stopTest("Test '" + testMonitor.getTest().getId() + "' has got expiration of its timeout"
                                                        + " before suitable test resources were available");
                                resolvedTestRestartRequests.add(testMonitor);
                            }
                        }
                    } else {
                        // Test has specified unsupported target and so, will be failed
                        testMonitor.stopTest("Test '" + testMonitor.getTest().getId() + "' has specified unsupported target " + target + ".");
                        resolvedTestRestartRequests.add(testMonitor);
                    }
                }
            }

            for (TestMonitor resolvedTestRestartRequest : resolvedTestRestartRequests) {
                if (testMonitorsWaitingForTestRestarts.contains(resolvedTestRestartRequest)) {
                    if (testMonitorsWaitingForTestRestarts.remove(resolvedTestRestartRequest)) {
                        p("Monitor of the test '" + resolvedTestRestartRequest.getName() + "' was successfully removed from the list of test restarters");
                    } else {
                        p("Error: Couldn't remove monitor of the test '" + resolvedTestRestartRequest.getName() + "' from the list of test restarters");
                    }
                } else {
                    p("Error: The list of test restarters doesn't contain monitor of the test '" + resolvedTestRestartRequest.getName() + "'");
                }
            }

            /*
            // DEBUG: Show current test farm
            String testFarmDescription = "\nCurrent test farm:\n";

            for (TestNode testNode : testNodes) {
                //p(testNode.getHostnameAndPort() + " with current load " + testNode.getNumberOfRunningTests() + "/" + maximalNumberOfTestsPerNode + ":");
                testFarmDescription += "\n" + testNode.getHostnameAndPort() + ":";
                List<Product> products = testNode.getProducts();
                if (!products.isEmpty()) {
                    for (Product product : products) {
                        testFarmDescription += "\n\t" + product.getIMEI() + " - " + product.getRMCode() + " " + product.getStatusDetails();
                    }
                } else {
                    testFarmDescription += "\n\t Has NO products";
                }
            }

            p("Current test farm: " + testFarmDescription + "\n");
            //END DEBUG*/
        }
    }

    /**
     * Sets optional description for this instance of Test Automation Service.
     *
     * @param description Optional description for this instance of Test Automation Service
     */
    private void setDescription(String description) {
        if (description != null && !description.isEmpty()) {
            this.description = description;
        }
    }

    /**
     * Returns a description specified for this instance of Test Automation Service.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns current configuration for the Test Automation Service.
     *
     * @return Current configuration for the Test Automation Service
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Returns current statistics from the Test Automation Service.
     *
     * @return Current statistics from the Test Automation Service
     */
    public Statistics getStatistics() {
        return statistics;
    }

    /**
     * Returns current status of the whole Test Automation Service in textual form.
     *
     * @return Current status of the whole Test Automation Service in textual form
     */
    protected String getCurrentStatus() {
        return currentStatus;
    }
    
    /**
     * Returns JSON descriptions of all products handled by this instance of Test Automation Service.
     *
     * @return JSON descriptions of all products handled by this instance of Test Automation Service
     */
    protected String getProductDescriptions() {
        return productDescriptions;
    }

    /**
     * Updates current status of this Test Automation Service.
     */
    protected void updateCurrentStatus() {
        try {
            StringBuffer status = new StringBuffer();
            StringBuffer productDescriptionsInJSON = new StringBuffer();

            status.append("<!DOCTYPE html>\n");
            status.append("<html>\n");
            status.append("<head>\n");
            status.append("<title>");
            status.append("Test Automation Service " + serviceHostname + ":" + servicePort);
            if (!description.isEmpty()) {
                status.append(" - " + description);
            }
            status.append("</title>\n");
            status.append("<link rel=\"shortcut icon\" href=\"http://wikis.in.nokia.com/pub/CI20Development/TASGuide/favicon.ico\" type=\"image/x-icon\" />\n");
            status.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
            status.append("<script type=\"text/javascript\">\n");
            status.append("    function toggle(testId) {\n");
            status.append("        var element = document.getElementById(testId);\n");
            status.append("        var text = document.getElementById(\"detailed \" + testId);\n");
            status.append("        if (element.style.display == \"block\") {\n");
            status.append("            element.style.display = \"none\";\n");
            status.append("            text.innerHTML = \"Show details\";\n");
            status.append("        } else {\n");
            status.append("            element.style.display = \"block\";\n");
            status.append("            text.innerHTML = \"Hide details\";\n");
            status.append("        }\n");
            status.append("    }\n");
            status.append("</script>\n");
            status.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
            status.append("</head>\n");
            status.append("<body>\n<table border=\"0\">\n\n");

            // Print out basic information about this Test Automation Service
            status.append("<tr><td>Test Automation Service v" + Constant.TEST_AUTOMATION_RELEASE_VERSION);

            if (!description.isEmpty()) {
                status.append(" for \"<b>" + description + "</b>\"");
            }

            status.append(", running on ");
            status.append(serviceHostname + ":" + servicePort);
            status.append(", status at " + timestampFormat.format(new Date(System.currentTimeMillis())));
            status.append(" (uptime " + Util.convert(System.currentTimeMillis() - startTime) + ")");
            status.append("</td></tr>\n\n");

            status.append("<tr><td>&nbsp;</td></tr>\n\n");

            // Show statistics
            status.append("<tr><td>Test execution statistics:</td></tr>\n\n");
            status.append("<tr><td>\n<blockquote>\n");
            if (statistics != null) {
                status.append("Number of tests in statistics: " + statistics.getTotalNumberOfTests() + "<br/><br/>");

                status.append("Average test allocation time: " + Util.convert(statistics.getAverageTestAllocationTime()) + "<br/>");
                status.append("Average test preparation time: " + Util.convert(statistics.getAverageTestPreparationTime()) + "<br/>");
                status.append("Average test execution time: " + Util.convert(statistics.getAverageTestExecutionTime()) + "<br/><br/>");

                status.append("Maximal test allocation time: " + Util.convert(statistics.getMaximalTestAllocationTime()) + "<br/>");
                status.append("Maximal test preparation time: " + Util.convert(statistics.getMaximalTestPreparationTime()) + "<br/>");

                long maximalTestExecutionTimeForSuccessfulTests = statistics.getMaximalTestExecutionTimeForSuccessfulTests();
                long maximalTestExecutionTimeForFailedTests = statistics.getMaximalTestExecutionTimeForFailedTests();

                TestEntry longestSuccessfulTest = statistics.getLongestSuccessfulTest();
                TestEntry longestFailedTest = statistics.getLongestFailedTest();

                if (longestSuccessfulTest != null) {
                    status.append("Maximal execution time of successful tests: " + Util.convert(maximalTestExecutionTimeForSuccessfulTests));

                    status.append("&nbsp;(<a id=\"detailed " + longestSuccessfulTest.getTestRuntimeId()
                        + "\" href=\"javascript:toggle('" + longestSuccessfulTest.getTestRuntimeId() + "');\">Show details</a>)\n");

                    status.append("<div id=\"" + longestSuccessfulTest.getTestRuntimeId() + "\" style=\"display: none\">\n");
                    status.append("<pre>\n");
                    status.append("\n\nThe longest successful test:\n\n" + longestSuccessfulTest.toString());
                    status.append("</pre>\n");
                    status.append("</div>\n");

                    status.append("<br/>");

                } else {
                    status.append("Maximal execution time of successful tests: " + Util.convert(maximalTestExecutionTimeForSuccessfulTests) + "<br/>");
                }

                if (longestFailedTest != null) {
                    status.append("Maximal execution time of failed tests: " + Util.convert(maximalTestExecutionTimeForFailedTests));

                    status.append("&nbsp;(<a id=\"detailed " + longestFailedTest.getTestRuntimeId()
                        + "\" href=\"javascript:toggle('" + longestFailedTest.getTestRuntimeId() + "');\">Show details</a>)\n");

                    status.append("<div id=\"" + longestFailedTest.getTestRuntimeId() + "\" style=\"display: none\">\n");
                    status.append("<pre>\n");
                    status.append("\n\nThe longest failed test:\n\n" + longestFailedTest.toString());
                    status.append("</pre>\n");
                    status.append("</div>\n");

                    status.append("<br/><br/>");

                } else {
                    status.append("Maximal execution time of failed tests: " + Util.convert(maximalTestExecutionTimeForFailedTests) + "<br/><br/>");
                }
            } else {
                status.append("<font color=\"#ee0000\">Couldn't obtain statistics data. Please wait a little bit for updates</font></br>");
            }

            if (configuration != null) {
                status.append("Threshold period: " + Util.convert(configuration.getStatisticsThresholdPeriod()) + "<br/>");
                status.append("\n</blockquote>\n</td></tr>\n\n");

                status.append("<tr><td>&nbsp;</td></tr>\n\n");

                // Show current configuration
                status.append("<tr><td>Current configuration:</td></tr>\n\n");
                status.append("<tr><td>\n<blockquote>\n");
                status.append("Default timeout for a test: " + Util.convert(configuration.getTestDefaultTimeout()) + "<br/>");
                status.append("Minimal execution time for a test: " + Util.convert(configuration.getTestMinimalExecutionTime()) + "<br/>");
                status.append("Test resources expectation timeout: " + Util.convert(configuration.getTestResourcesExpectationTimeout()) + "<br/>");

                status.append("Maximal number of retries for a failed test: " + configuration.getMaximalNumberOfRetriesForFailedTest() + "<br/>");
                status.append("Maximal allowed number of tests per test node: " + configuration.getMaximalNumberOfTestsPerNode() + "<br/>");

                if (configuration.isMaintenanceMode()) {
                    status.append("<font color=\"#ee0000\">Maintenance mode is on</font></br>");
                }
            } else {
                status.append("<font color=\"#ee0000\">Couldn't obtain configuration settings. Please wait a little bit for updates</font></br>");
            }

            status.append("\n</blockquote>\n</td></tr>\n\n");
            status.append("<tr><td>&nbsp;</td></tr>\n\n");

            // Tell what tests are waiting for execution
            status.append("<tr><td>Tests waiting for execution:</td></tr>\n\n");

            if (testMonitorsWaitingForTestingResources.isEmpty()) {
                status.append("<tr><td>\n<blockquote>\n<b>None</b>\n</blockquote>\n</td></tr>\n\n");
            } else {
                status.append("<tr><td>\n<blockquote>\n");

                for (TestMonitor current : testMonitorsWaitingForTestingResources) {
                    Test currentTest = current.getTest();

                    if (!currentTest.getURL().isEmpty()) {
                        status.append("<a href=\"" + currentTest.getURL() + "\" target=\"_blank\">" + currentTest.getId() + "</a><br/>\n");
                    } else {
                        status.append("<b>" + currentTest.getId() + "</b><br/>\n");
                    }
                }

                status.append("\n</blockquote>\n</td></tr>\n\n");
            }

            status.append("<tr><td>&nbsp;</td></tr>\n\n");

            // Show utilization statistics
            long currentTime = System.currentTimeMillis();

            // Get current total number of available products
            long currentNumberOfProductsInFarm = 0L;
            if (testNodes != null) {
                for (TestNode currentTestNode : testNodes) {
                    currentNumberOfProductsInFarm += currentTestNode.getNumberOfProducts();
                }
            }

            // Get duration of workload history in days
            int workloadHistoryPeriodInDays = 7; // Default value

            if (configuration != null) {
                workloadHistoryPeriodInDays = (int) configuration.getWorkloadHistoryPeriod();
            }

            // Prevent negative values
            if (workloadHistoryPeriodInDays < 0) {
                workloadHistoryPeriodInDays = -workloadHistoryPeriodInDays;
            }
            // Show history for a least one date back
            if (workloadHistoryPeriodInDays < 1) {
                workloadHistoryPeriodInDays = 1;
            }

            GregorianCalendar calendar = (GregorianCalendar) Calendar.getInstance();
            calendar.setLenient(true); // Make it much smarter in day rolls

            // Extract test farm's utilization entries for the whole history period and current date
            long workloadHistoryStartTime = currentTime - (workloadHistoryPeriodInDays * Constant.ONE_DAY);

            List<TestEntry> testEntries = new ArrayList<TestEntry>(0);
            if (statistics != null) {
                testEntries = statistics.getTestEntriesForPeriod(workloadHistoryStartTime, currentTime);
            }

            // Figure out test farm's utilization for this date
            calendar.setTimeInMillis(currentTime);
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

            workloadHistoryStartTime = calendar.getTimeInMillis();
            long workloadHistoryEndTime = currentTime;

            StringBuffer sampleDescription = new StringBuffer();
            StringBuffer sampleLabels = new StringBuffer();
            StringBuffer sampleData = new StringBuffer();

            calendar.setTimeInMillis(workloadHistoryStartTime);

            // Slice workload time
            long sampleDurationInMilliseconds = Constant.FIVE_MINUTES; // Default value
            if (configuration != null) {
                sampleDurationInMilliseconds = configuration.getDailyWorkloadHistorySliceDuration();
            }
            int numberOfSamples = (int) (Constant.ONE_DAY / sampleDurationInMilliseconds);
            long[] samples = new long[numberOfSamples];

            // Create sample description
            sampleDescription.append(Util.convert(sampleDurationInMilliseconds));

            // Create hourly labels
            sampleLabels.append("var sampleLabels = [\n");

            for (int i = 0; i < 24; i++) {
                sampleLabels.append("\t'" + hourFormat.format(new Date(workloadHistoryStartTime + i * Constant.ONE_HOUR)) + "',\n");
            }

            sampleLabels.append("];\n");

            // Create samples for current date
            for (int j = 0; j < numberOfSamples; j++) {
                long currentStartTime = calendar.getTimeInMillis();
                calendar.add(Calendar.MILLISECOND, (int) sampleDurationInMilliseconds);
                long currentEndTime = calendar.getTimeInMillis();

                for (TestEntry testEntry : testEntries) {
                    long testInitializationTime = testEntry.getTestInitializationTime();
                    long testEndTime = testEntry.getTestEndTime();

                    if (testInitializationTime > currentStartTime && testInitializationTime < currentEndTime) {

                        Test test = testEntry.getTest();

                        if (test != null) {
                            int numberOfReservedProducts = test.getReservedProducts().size();

                            if (numberOfReservedProducts > 0) {
                                // Calculate end index from the end of test's execution
                                long endIndex = (testEndTime - workloadHistoryStartTime) / sampleDurationInMilliseconds;

                                if (endIndex > 0 && endIndex < numberOfSamples) {
                                    for (int i = j; i < endIndex; i++) {
                                        samples[i] += numberOfReservedProducts;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Convert samples into two-dimensional array with time labels and samples themselves
            sampleData.append("var sampleData = [\n");

            long sampleTimestamp = 0L;

            for (int i = 0; i < samples.length; i++) {
                sampleTimestamp = workloadHistoryStartTime + i * sampleDurationInMilliseconds;
                sampleData.append("['" + timestampFormat.format(new Date(sampleTimestamp)) + "', " + samples[i] + "], ");
            }

            sampleData.append("];\n");

            // Generate background samples for the whole period of workload history

            // Get test farm's utilization for specified number of days
            workloadHistoryStartTime = currentTime - (workloadHistoryPeriodInDays * Constant.ONE_DAY);
            workloadHistoryEndTime = currentTime - Constant.ONE_DAY;

            // Set calendar to the beginning of that date
            calendar.setTimeInMillis(workloadHistoryStartTime);
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));
            // Get time from the beginning of that date
            workloadHistoryStartTime = calendar.getTimeInMillis();

            // Set calendar to the end of that date
            calendar.setTimeInMillis(workloadHistoryEndTime);
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getMaximum(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));
            // Get time from the beginning of that date
            workloadHistoryEndTime = calendar.getTimeInMillis();

            StringBuffer backgroundSampleData = new StringBuffer();

            calendar.setTimeInMillis(workloadHistoryStartTime);

            // Slice workload time
            long[] backgroundSamples = new long[numberOfSamples];

            long currentHistoryDayStartTime = 0L;
            long currentHistoryDayEndTime = 0L;

            // Create background samples
            for (int h = 0; h < workloadHistoryPeriodInDays; h++) {

                currentHistoryDayStartTime = workloadHistoryStartTime + h * Constant.ONE_DAY;
                currentHistoryDayEndTime = workloadHistoryStartTime + (h + 1) * Constant.ONE_DAY - 1L;

                long[] currentHistoryDayBackgroundSamples = new long[numberOfSamples];

                for (TestEntry testEntry : testEntries) {
                    long testInitializationTime = testEntry.getTestInitializationTime(); // When test has got all requested products
                    long testEndTime = testEntry.getTestEndTime();

                    if (testInitializationTime > currentHistoryDayStartTime && testInitializationTime < currentHistoryDayEndTime) {
                        Test test = testEntry.getTest();

                        if (test != null) {
                            int numberOfReservedProducts = test.getReservedProducts().size();

                            if (numberOfReservedProducts > 0) {
                                // Calculate start and end indexes from the test's execution timestamps
                                int startIndex = (int) ((testInitializationTime - currentHistoryDayStartTime) / sampleDurationInMilliseconds);
                                int endIndex = (int) ((testEndTime - currentHistoryDayStartTime) / sampleDurationInMilliseconds);

                                if (startIndex < 0) {
                                    startIndex = 0;
                                }

                                if (endIndex < 0) {
                                    endIndex = 0;
                                }

                                if (endIndex >= currentHistoryDayBackgroundSamples.length) {
                                    endIndex = currentHistoryDayBackgroundSamples.length - 1;
                                }

                                if (startIndex < endIndex) {
                                    // Update usage statistics only if they represent some maximal value
                                    for (int i = startIndex; i <= endIndex; i++) {
                                        currentHistoryDayBackgroundSamples[i] += numberOfReservedProducts;
                                    }
                                }
                            }
                        }
                    }
                }

                // Transfer history day's maximal samples into the final background samples
                for (int i = 0; i < numberOfSamples; i++) {
                    if (backgroundSamples[i] < currentHistoryDayBackgroundSamples[i]) {
                        backgroundSamples[i] = currentHistoryDayBackgroundSamples[i];
                    }
                }
            }

            // Convert samples into two-dimensional array with time labels and samples themselves
            backgroundSampleData.append("var backgroundSampleData = [\n");

            sampleTimestamp = 0L;

            for (int i = 0; i < backgroundSamples.length; i++) {
                sampleTimestamp = workloadHistoryStartTime + i * sampleDurationInMilliseconds;
                backgroundSampleData.append("\t['', " + backgroundSamples[i] + "], ");
            }

            backgroundSampleData.append("];\n");

            // Append results to the final web page

            status.append("<tr><td>Test farm's utilization in " + sampleDescription.toString()
                    + " slices for the current and the last " + workloadHistoryPeriodInDays + " days:</td></tr>\n");
            status.append("<tr><td>\n");
            status.append("<blockquote>\n");
            status.append("<canvas id=\"testFarmDayUtilization\" width=\"896\" height=\"256\">\n");
            status.append("Unfortunately your browser is not supporting HTML 5.<br/>\n");
            status.append("Please consider updating to a capable one.\n");
            status.append("</canvas>\n");
            status.append("<script type=\"text/javascript\">\n");
            status.append("var canvas = document.getElementById(\"testFarmDayUtilization\");\n");
            status.append("var context = canvas.getContext(\"2d\");\n");
            status.append("var canvasWidth = canvas.width;\n");
            status.append("var canvasHeight = canvas.height;\n");
            status.append("var gradient = context.createLinearGradient(0, 0, 0, canvasHeight);\n");
            status.append("gradient.addColorStop(0, \"#ee0000\");\n");
            status.append("gradient.addColorStop(1 / 3, \"#007700\");\n");
            status.append("gradient.addColorStop(1, \"#007700\");\n\n");
            status.append("var sampleDescription = '" + sampleDescription.toString() + "';\n");
            status.append(sampleLabels.toString() + "\n");
            status.append(sampleData.toString() + "\n");
            status.append(backgroundSampleData.toString() + "\n");
            status.append("var largestNumberOfSamples = 0;\n");
            status.append("for (var i = 0; i < sampleData.length; i++) {\n");
            status.append("    if (largestNumberOfSamples < sampleData[i][1]) {\n");
            status.append("        largestNumberOfSamples = sampleData[i][1];\n");
            status.append("    }\n");
            status.append("}\n");
            status.append("for (var i = 0; i < backgroundSampleData.length; i++) {\n");
            status.append("    if (largestNumberOfSamples < backgroundSampleData[i][1]) {\n");
            status.append("        largestNumberOfSamples = backgroundSampleData[i][1];\n");
            status.append("    }\n");
            status.append("}\n");
            status.append("// Prevent zero or negative values\n");
            status.append("if (largestNumberOfSamples <= 0) {\n");
            status.append("    largestNumberOfSamples = 1;\n");
            status.append("}\n");
            status.append("var numberOfSampleLabels = sampleLabels.length;\n");
            status.append("// Prevent zero or negative values\n");
            status.append("if (numberOfSampleLabels <= 0) {\n");
            status.append("    numberOfSampleLabels = 1;\n");
            status.append("}\n");
            status.append("var numberOfSamples = sampleData.length;\n");
            status.append("// Prevent zero or negative values\n");
            status.append("if (numberOfSamples <= 0) {\n");
            status.append("    numberOfSamples = 1;\n");
            status.append("}\n");
            status.append("var maxWidthForSample = canvasWidth / numberOfSampleLabels;\n");
            status.append("var singleSampleWidth = canvasWidth / numberOfSamples;\n");
            status.append("var currentNumberOfProductsInFarm = " + currentNumberOfProductsInFarm + ";\n");
            status.append("var numberOfGuidingLines = 1;\n");
            status.append("var singleSampleHeight = 1;\n");
            status.append("if (largestNumberOfSamples > currentNumberOfProductsInFarm) {\n");
            status.append("    singleSampleHeight = canvasHeight / largestNumberOfSamples;\n");
            status.append("    numberOfGuidingLines = largestNumberOfSamples;\n");
            status.append("} else {\n");
            status.append("    singleSampleHeight = canvasHeight / (currentNumberOfProductsInFarm + 1);\n");
            status.append("    numberOfGuidingLines = currentNumberOfProductsInFarm + 1;\n");
            status.append("}\n");
            status.append("if (singleSampleHeight <= 0) {\n");
            status.append("    singleSampleHeight = 1;\n");
            status.append("}\n");
            status.append("var fontSize = 10;\n");
            status.append("context.font = fontSize + \"px Verdana\";\n");
            status.append("// Show guiding lines\n");
            status.append("var guidingStep = 1;\n");
            status.append("var gradientScale = currentNumberOfProductsInFarm / canvasHeight;\n");
            status.append("var guidingLineFillStyle = \"#eeeeee\";\n");
            status.append("if (gradientScale >= 0.5) {\n");
            status.append("    guidingLineFillStyle = \"#ffffff\";\n");
            status.append("} else if (gradientScale >= 0.25) {\n");
            status.append("    guidingLineFillStyle = \"#f5f5f5\";\n");
            status.append("}\n");
            status.append("for (var i = guidingStep; i <= numberOfGuidingLines; i += guidingStep) {\n");
            status.append("    context.beginPath();\n");
            status.append("    context.fillStyle = guidingLineFillStyle;\n");
            status.append("    context.fillRect(0, canvasHeight - 1 - (i * singleSampleHeight), canvasWidth, 1);\n");
            status.append("    context.fillText(\"\" + i, 0, canvasHeight - 2 - (i * singleSampleHeight));\n");
            status.append("    context.closePath();\n");
            status.append("}\n");
            status.append("// Show current capacity\n");
            status.append("context.beginPath();\n");
            status.append("context.fillStyle = \"#000000\";\n");
            status.append("context.fillRect(0, canvasHeight - 1 - (currentNumberOfProductsInFarm * singleSampleHeight), canvasWidth, 1);\n");
            status.append("context.closePath();\n");
            status.append("// Draw sample labels\n");
            status.append("var sampleLabelX = 0;\n");
            status.append("for (var i = 0; i < numberOfSampleLabels; i++) {\n");
            status.append("    context.beginPath();\n");
            status.append("    context.fillStyle = \"#cccccc\";\n");
            status.append("    context.fillText(\" \" + sampleLabels[i], sampleLabelX, fontSize, maxWidthForSample);\n");
            status.append("    context.fillRect(sampleLabelX, fontSize, 1, canvasHeight);\n");
            status.append("    context.closePath();\n");
            status.append("    sampleLabelX = sampleLabelX + maxWidthForSample;\n");
            status.append("}\n");
            status.append("// Draw samples\n");
            status.append("var sampleStartX = 0;\n");
            status.append("for (var i = 0; i < numberOfSamples; i++) {\n");
            status.append("    var currentNumberOfSamples = sampleData[i][1] * singleSampleHeight;\n");
            status.append("    var currentNumberOfBackgroundSamples = backgroundSampleData[i][1] * singleSampleHeight;\n");
            status.append("    if (currentNumberOfSamples > currentNumberOfBackgroundSamples) {\n");
            status.append("        // Draw today's samples before background samples\n");
            status.append("        context.beginPath();\n");
            status.append("        context.fillStyle = gradient;\n");
            status.append("        context.fillRect(sampleStartX, canvasHeight - currentNumberOfSamples, singleSampleWidth + 1, currentNumberOfSamples);\n");
            status.append("        context.fillStyle = \"#eeeeee\";\n");
            status.append("        context.fillRect(sampleStartX, canvasHeight - currentNumberOfBackgroundSamples, singleSampleWidth + 1, currentNumberOfBackgroundSamples);\n");
            status.append("        context.closePath();\n");
            status.append("    } else {\n");
            status.append("        // Draw background samples before today's samples\n");
            status.append("        context.beginPath();\n");
            status.append("        context.fillStyle = \"#eeeeee\";\n");
            status.append("        context.fillRect(sampleStartX, canvasHeight - currentNumberOfBackgroundSamples, singleSampleWidth + 1, currentNumberOfBackgroundSamples);\n");
            status.append("        context.fillStyle = gradient;\n");
            status.append("        context.fillRect(sampleStartX, canvasHeight - currentNumberOfSamples, singleSampleWidth + 1, currentNumberOfSamples);\n");
            status.append("        context.closePath();\n");
            status.append("    }\n");
            status.append("    sampleStartX += singleSampleWidth;\n");
            status.append("}\n");
            status.append("// Tell about maximal usage so far and current capacity\n");
            status.append("context.beginPath();\n");
            status.append("context.fillStyle = \"#cccccc\";\n");
            status.append("context.fillText(\"Test farm's maximal load: \" + largestNumberOfSamples + \" devices in \" + sampleDescription, 0, fontSize * 2, canvasWidth);\n");
            status.append("context.fillText(\"Test farm's current capacity: \" + currentNumberOfProductsInFarm + \" devices\", 0, fontSize * 3, canvasWidth);\n");
            status.append("context.closePath();\n");
            status.append("</script>\n");
            status.append("</blockquote>\n");
            status.append("</td></tr>\n\n");

            status.append("<tr><td>&nbsp;</td></tr>\n\n");

            // Show test farm's daily workloads for specified number of days
            workloadHistoryStartTime = currentTime - (workloadHistoryPeriodInDays * Constant.ONE_DAY);

            // Set to the exact beginning of the date
            calendar.setTimeInMillis(workloadHistoryStartTime);
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

            workloadHistoryStartTime = calendar.getTimeInMillis();
            workloadHistoryEndTime = currentTime;

            // Test entries are already extracted at this point

            calendar.setTimeInMillis(currentTime);

            String workloadDataStatus = "";
            List<String> workloadDates = new ArrayList<String>(0);

            StringBuffer workloadData = new StringBuffer();
            workloadData.append("var data = [\n");

            if (testEntries.isEmpty()) {
                workloadDataStatus = "<b>No data currently</b>";
            } else {
                // Parse test entries
                long currentWorkloadDayStartTime = 0L;
                long endTime = 0L;
                String dateLabel;
                int numberOfSuccessfulTests = 0;
                int numberOfFailedTests = 0;

                for (int i = workloadHistoryPeriodInDays - 1; i > 0; i--) {

                    currentWorkloadDayStartTime = currentTime - (i * Constant.ONE_DAY);

                    // Set calendar to the exact beginning of the date
                    calendar.setTimeInMillis(currentWorkloadDayStartTime);
                    calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
                    calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
                    calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

                    // Take exact beginning of the date in milliseconds
                    currentWorkloadDayStartTime = calendar.getTimeInMillis();

                    // Set calendar to the end of the same day
                    calendar.set(Calendar.HOUR_OF_DAY, calendar.getMaximum(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
                    calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
                    calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));

                    endTime = calendar.getTimeInMillis();

                    dateLabel = dateFormat.format(new Date(currentWorkloadDayStartTime));
                    numberOfSuccessfulTests = 0;
                    numberOfFailedTests = 0;

                    for (TestEntry current : testEntries) {
                        Long currentTestCreationTime = current.getTestCreationTime();

                        if (currentTestCreationTime > currentWorkloadDayStartTime && currentTestCreationTime < endTime) {
                            // The corresponding test entry is within the specified range
                            if (current.testWasSuccessful()) {
                                numberOfSuccessfulTests++;
                            } else {
                                numberOfFailedTests++;
                            }
                        }
                    }

                    workloadData.append("\t['" + dateLabel + "', " + numberOfSuccessfulTests + ", " + numberOfFailedTests + "],\n");
                    workloadDates.add(dateLabel);
                }

                // Add workload statistics for the current day as well
                // Reset calendar
                calendar.setTimeInMillis(currentTime);
                calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
                calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
                calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
                calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

                currentWorkloadDayStartTime = calendar.getTimeInMillis();
                endTime = currentTime;

                dateLabel = dateFormat.format(new Date(currentWorkloadDayStartTime));
                numberOfSuccessfulTests = 0;
                numberOfFailedTests = 0;

                for (TestEntry current : testEntries) {
                    Long currentTestCreationTime = current.getTestCreationTime();

                    if (currentTestCreationTime > currentWorkloadDayStartTime && currentTestCreationTime < endTime) {
                        // The corresponding test entry is within the specified range

                        if (current.testWasSuccessful()) {
                            numberOfSuccessfulTests++;
                        } else {
                            numberOfFailedTests++;
                        }
                    }
                }

                workloadData.append("\t['" + dateLabel + "', " + numberOfSuccessfulTests + ", " + numberOfFailedTests + "]\n");
                workloadDates.add(dateLabel);
            }

            workloadData.append("];\n");

            // Add daily workloads to the HTML
            status.append("<tr><td>Daily workloads: " + workloadDataStatus + "</td></tr>\n");
            status.append("<tr><td>\n");
            status.append("<blockquote>\n");
            if (workloadDataStatus.isEmpty()) {
                // Everything is fine, we have some data to show
                status.append("<canvas id=\"dailyWorkloads\" width=\"896\" height=\"256\">\n");
            } else {
                // Otherwise make canvas smaller
                status.append("<canvas id=\"dailyWorkloads\" width=\"896\" height=\"10\">\n");
            }
            status.append("Unfortunately your browser is not supporting HTML 5.<br/>\n");
            status.append("Please consider updating to a capable one.\n");
            status.append("</canvas>\n");
            status.append("<script type=\"text/javascript\">\n");
            status.append("var canvas = document.getElementById(\"dailyWorkloads\");\n");
            status.append("var context = canvas.getContext(\"2d\");\n");
            status.append("var canvasWidth = canvas.width;\n");
            status.append("var canvasHeight = canvas.height;\n");
            status.append("// The date, the number of successful tests and the number of failed tests on that date\n");
            status.append(workloadData);
            status.append("var numberOfDays = data.length;\n");
            status.append("var maxNumberOfTestsPerDay = 0;\n");
            status.append("for (var i = 0; i < numberOfDays; i++) {\n");
            status.append("    if (data[i][1] > maxNumberOfTestsPerDay) {\n");
            status.append("        maxNumberOfTestsPerDay = data[i][1];\n");
            status.append("    }\n");
            status.append("    if (data[i][2] > maxNumberOfTestsPerDay) {\n");
            status.append("        maxNumberOfTestsPerDay = data[i][2];\n");
            status.append("    }\n");
            status.append("}\n");
            status.append("maxNumberOfTestsPerDay = maxNumberOfTestsPerDay + (maxNumberOfTestsPerDay * 0.2); // For a nicer printouts\n");
            status.append("var maxWidthForDay = canvasWidth / numberOfDays;\n");
            status.append("var maxWidthForColumn = maxWidthForDay / 2;\n");
            status.append("var maxHeightForSingleTest = canvasHeight / maxNumberOfTestsPerDay;\n");
            status.append("if (maxHeightForSingleTest <= 0) {\n");
            status.append("    maxHeightForSingleTest = 1;\n");
            status.append("}\n");
            status.append("var fontSize = maxWidthForColumn / 4;\n");
            status.append("var startX = 0;\n");
            status.append("var endX = maxWidthForDay;\n");
            status.append("var startY = 0;\n");
            status.append("var endY = canvasHeight;\n");
            status.append("var currentColumnHeight = 0;\n");
            status.append("context.font = fontSize + \"px Verdana\";\n");
            status.append("context.lineWidth = 1;\n");
            status.append("for (var i = 0; i < numberOfDays; i++) {\n");
            status.append("    // Draw column of successful tests\n");
            status.append("    currentColumnHeight = maxHeightForSingleTest * data[i][1];\n");
            status.append("    if (data[i][1] > 0) {\n");
            status.append("        if (currentColumnHeight < fontSize) {\n");
            status.append("            currentColumnHeight = 1.1 * fontSize;\n");
            status.append("        }\n");
            status.append("    }\n");
            status.append("    context.beginPath();\n");
            status.append("    context.fillStyle = \"#007700\";\n");
            status.append("    context.fillRect(startX, startY + canvasHeight - currentColumnHeight, maxWidthForColumn, canvasHeight);\n");
            status.append("    context.fillStyle = \"#ffffff\";\n");
            status.append("    context.fillText(\" \" + data[i][1], startX, startY + fontSize + canvasHeight - currentColumnHeight, maxWidthForColumn);\n");
            status.append("    context.closePath();\n");
            status.append("    // Draw column of failed tests\n");
            status.append("    currentColumnHeight = maxHeightForSingleTest * data[i][2];\n");
            status.append("    if (data[i][2] > 0) {\n");
            status.append("        if (currentColumnHeight < fontSize) {\n");
            status.append("            currentColumnHeight = 1.1 * fontSize;\n");
            status.append("        }\n");
            status.append("    }\n");
            status.append("    context.beginPath();\n");
            status.append("    context.fillStyle = \"#ee0000\";\n");
            status.append("    context.fillRect(startX + maxWidthForColumn, startY + canvasHeight - currentColumnHeight, maxWidthForColumn, canvasHeight);\n");
            status.append("    context.fillStyle = \"#ffffff\";\n");
            status.append("    context.fillText(\" \" + data[i][2], startX + maxWidthForColumn, startY + fontSize + canvasHeight - currentColumnHeight, maxWidthForColumn);\n");
            status.append("    context.closePath();\n");
            status.append("    // Draw date\n");
            status.append("    context.beginPath();\n");
            status.append("    context.fillStyle = \"#cccccc\";\n");
            status.append("    context.fillText(\" \" + data[i][0], startX, startY + fontSize, maxWidthForColumn);\n");
            status.append("    context.closePath();\n");
            status.append("    startX = startX + maxWidthForDay;\n");
            status.append("}\n");
            status.append("</script>\n");
            status.append("</blockquote>\n");
            status.append("</td></tr>\n");

            status.append("\n\n<tr><td>&nbsp;</td></tr>\n\n");

            status.append("<tr><td>Detailed workloads by day: " + workloadDataStatus + "</td></tr>\n");

            if (!workloadDates.isEmpty()) {
                status.append("<tr><td>\n<blockquote>\n");

                for (String currentWorkloadDate : workloadDates) {
                    if (currentWorkloadDate != null && !currentWorkloadDate.isEmpty()) {
                        status.append("<a href=\"" + HttpHandler.getDateStatisticsURI(currentWorkloadDate) + "\">" + currentWorkloadDate + "</a>&nbsp;\n");
                    }
                }

                status.append("</blockquote>\n</td></tr>\n\n");
            }

            status.append("<tr><td>&nbsp;</td></tr>\n\n");

            // Tell what are currently available test nodes and their products
            status.append("<tr><td>Available test nodes:</td></tr>\n\n");

            // Put JSON product descriptions into a proper JSON "products" array
            productDescriptionsInJSON.append("{ \"" + HttpHandler.PRODUCTS_TOKEN + "\": [\n");

            if (testNodes == null || testNodes.isEmpty()) {
                status.append("<tr><td>\n<blockquote>\n<b>None</b>\n</blockquote>\n</td></tr>\n\n");
            } else {
                int numberOfTestNodes = testNodes.size();
                int numberOfAddedTestNodes = 0;

                for (TestNode currentTestNode : testNodes) {
                    status.append(currentTestNode.getCurrentStatus());
                    status.append("\n\n<tr><td>&nbsp;</td></tr>\n\n");

                    productDescriptionsInJSON.append(currentTestNode.getProductDescriptions());

                    numberOfAddedTestNodes += 1;
                    
                    if (numberOfAddedTestNodes < numberOfTestNodes) {
                        productDescriptionsInJSON.append(",");
                    }

                    productDescriptionsInJSON.append("\n");
                }
            }

            // Add symbols of JSON array's end
            productDescriptionsInJSON.append("] }\n");

            status.append("<tr><td>&nbsp;</td></tr>\n\n");

            status.append("\n</table>\n</body>\n</html>\n");

            currentStatus = status.toString();
            productDescriptions = productDescriptionsInJSON.toString();
        } catch (Exception e) {
            p("Got troubles with updating current status: " + e.getClass() + " - " + e.getMessage() + " - " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Returns detailed status for specified date.
     *
     * @param date Date to use
     * @return Detailed status for specified date
     */
    protected StringBuffer getDateStatus(String date) {
        try {
            StringBuffer dateStatus = new StringBuffer();
            Date requestedDate = null;

            try {
                requestedDate = dateFormat.parse(date);
                // Reformat the date just to make it appear in correct form
                date = dateFormat.format(new Date(requestedDate.getTime()));
            } catch (Exception e) {
                // Requested date is invalid or has invalid format
                requestedDate = null;
            }

            dateStatus.append("<!DOCTYPE html>\n");
            dateStatus.append("<html>\n");
            dateStatus.append("<head>\n");
            dateStatus.append("<title>");
            dateStatus.append("Detailed status for date " + date);
            dateStatus.append("</title>\n");
            dateStatus.append("<link rel=\"shortcut icon\" href=\"http://wikis.in.nokia.com/pub/CI20Development/TASGuide/favicon.ico\" type=\"image/x-icon\" />\n");
            dateStatus.append("<script type=\"text/javascript\">\n");
            dateStatus.append("    function toggle(testId) {\n");
            dateStatus.append("        var element = document.getElementById(testId);\n");
            dateStatus.append("        var text = document.getElementById(\"detailed \" + testId);\n");
            dateStatus.append("        if (element.style.display == \"block\") {\n");
            dateStatus.append("            element.style.display = \"none\";\n");
            dateStatus.append("            text.innerHTML = \"Show details\";\n");
            dateStatus.append("        } else {\n");
            dateStatus.append("            element.style.display = \"block\";\n");
            dateStatus.append("            text.innerHTML = \"Hide details\";\n");
            dateStatus.append("        }\n");
            dateStatus.append("    }\n");
            dateStatus.append("</script>\n");
            dateStatus.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
            dateStatus.append("</head>\n");
            dateStatus.append("<body>\n<table border=\"0\">\n\n");

            // Print out basic information about this Test Automation Service and selected date
            dateStatus.append("<tr><td>Test Automation Service v" + Constant.TEST_AUTOMATION_RELEASE_VERSION);

            if (!description.isEmpty()) {
                dateStatus.append(" for \"<b>" + description + "</b>\"");
            }

            dateStatus.append(", running on ");
            dateStatus.append(serviceHostname + ":" + servicePort);
            dateStatus.append(", detailed status for date " + date);
            dateStatus.append("</td></tr>\n\n");

            dateStatus.append("<tr><td>&nbsp;</td></tr>\n\n");

            if (requestedDate == null) {
                // If requested date is invalid, stop processings here
                dateStatus.append("<tr><td>The specified date \"" + date + "\" has invalid format.");
                dateStatus.append(" Please use the default format \"" + Constant.DATE_FORMAT + "\" for specifying dates.</td></tr>\n\n");

                dateStatus.append("<tr><td>&nbsp;</td></tr>\n\n");
                dateStatus.append("\n</table>\n</body>\n</html>\n");

                return dateStatus;
            }

            // Show statistics for the date
            GregorianCalendar calendar = (GregorianCalendar) Calendar.getInstance();

            // Set calendar to the beginning of the day
            calendar.setTime(requestedDate);
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

            long workloadHistoryStartTime = calendar.getTimeInMillis();

            // Set calendar to the end of the day
            calendar.setTime(requestedDate);
            calendar.set(Calendar.HOUR_OF_DAY, calendar.getMaximum(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));

            long workloadHistoryEndTime = calendar.getTimeInMillis();

            // Extract day statistics
            List<TestEntry> testEntries = new ArrayList<TestEntry>(0);
            if (statistics != null) {
                testEntries = statistics.getTestEntriesForPeriod(workloadHistoryStartTime, workloadHistoryEndTime);
            }

            dateStatus.append("<tr><td>Test execution statistics for date " + date + ":</td></tr>\n\n");
            dateStatus.append("<tr><td>\n<blockquote>\n");
            dateStatus.append("Number of executed tests: " + testEntries.size() + "<br/><br/>");

            if (!testEntries.isEmpty()) {
                // Calculate average and maximal values
                long totalNumberOfTests = 0L;

                long averageTestAllocationTime = 0L;
                long averageTestPreparationTime = 0L;
                long averageTestExecutionTime = 0L;

                long maximalTestAllocationTime = 0L;
                long maximalTestPreparationTime = 0L;
                long maximalTestExecutionTimeForSuccessfulTests = 0L;
                long maximalTestExecutionTimeForFailedTests = 0L;

                TestEntry longestSuccessfulTest = null;
                TestEntry longestFailedTest = null;

                for (TestEntry currentTestEntry : testEntries) {
                    long testCreationTime = currentTestEntry.getTestCreationTime();
                    long testInitializationTime = currentTestEntry.getTestInitializationTime();
                    long testStartTime = currentTestEntry.getTestStartTime();
                    long testEndTime = currentTestEntry.getTestEndTime();

                    long testAllocationTime = 0L; // Time difference between test creation and initialization
                    long testPreparationTime = 0L; // Time difference between test initialization and start
                    long testExecutionTime = 0L; // Time difference between test start and end

                    if (testCreationTime > 0L) {
                        //p("Test was created at " + simpleDateFormat.format(new Date(testCreationTime)));

                        if (testInitializationTime > testCreationTime) {
                            //p("Test was initialized at " + simpleDateFormat.format(new Date(testInitializationTime)));
                            testAllocationTime = testInitializationTime - testCreationTime;

                            if (testStartTime > testInitializationTime) {
                                //p("Test was started at " + simpleDateFormat.format(new Date(testInitializationTime)));
                                testPreparationTime = testStartTime - testInitializationTime;

                                if (testEndTime > testStartTime) {
                                    //p("Test was ended at " + simpleDateFormat.format(new Date(testEndTime)));
                                    testExecutionTime = testEndTime - testStartTime;

                                    if (testExecutionTime > 0L) {
                                        // Put this test data into statistics

                                        totalNumberOfTests++;

                                        averageTestAllocationTime += testAllocationTime;
                                        averageTestPreparationTime += testPreparationTime;
                                        averageTestExecutionTime += testExecutionTime;

                                        if (testAllocationTime > maximalTestAllocationTime) {
                                            maximalTestAllocationTime = testAllocationTime;
                                        }

                                        if (testPreparationTime > maximalTestPreparationTime) {
                                            maximalTestPreparationTime = testPreparationTime;
                                        }

                                        if (currentTestEntry.testWasSuccessful()) {
                                            if (testExecutionTime > maximalTestExecutionTimeForSuccessfulTests) {
                                                maximalTestExecutionTimeForSuccessfulTests = testExecutionTime;
                                                longestSuccessfulTest = currentTestEntry;
                                            }
                                        } else {
                                            if (testExecutionTime > maximalTestExecutionTimeForFailedTests) {
                                                maximalTestExecutionTimeForFailedTests = testExecutionTime;
                                                longestFailedTest = currentTestEntry;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (totalNumberOfTests > 0L) {
                    // Calculate average values
                    averageTestAllocationTime /= totalNumberOfTests; // Average time difference between test creation and initialization
                    averageTestPreparationTime /= totalNumberOfTests; // Average time difference between test initialization and start
                    averageTestExecutionTime /= totalNumberOfTests; // Average time difference between test start and end

                    dateStatus.append("Average test allocation time: " + Util.convert(averageTestAllocationTime) + "<br/>");
                    dateStatus.append("Average test preparation time: " + Util.convert(averageTestPreparationTime) + "<br/>");
                    dateStatus.append("Average test execution time: " + Util.convert(averageTestExecutionTime) + "<br/><br/>");

                    dateStatus.append("Maximal test allocation time: " + Util.convert(maximalTestAllocationTime) + "<br/>");
                    dateStatus.append("Maximal test preparation time: " + Util.convert(maximalTestPreparationTime) + "<br/>");

                    if (longestSuccessfulTest != null) {
                        // Since an HTML div with the same id might be presented on this page, we need to add more id uniquiness here
                        long additionalId = System.currentTimeMillis();

                        dateStatus.append("Maximal execution time of successful tests: " + Util.convert(maximalTestExecutionTimeForSuccessfulTests));

                        dateStatus.append("&nbsp;(<a id=\"detailed " + longestSuccessfulTest.getTestRuntimeId() + additionalId
                            + "\" href=\"javascript:toggle('" + longestSuccessfulTest.getTestRuntimeId() + additionalId + "');\">Show details</a>)\n");

                        dateStatus.append("<div id=\"" + longestSuccessfulTest.getTestRuntimeId() + additionalId + "\" style=\"display: none\">\n");
                        dateStatus.append("<pre>\n");
                        dateStatus.append("\n\nThe longest successful test:\n\n" + longestSuccessfulTest.toString());
                        dateStatus.append("</pre>\n");
                        dateStatus.append("</div>\n");

                        dateStatus.append("<br/>");

                    } else {
                        dateStatus.append("Maximal execution time of successful tests: " + Util.convert(maximalTestExecutionTimeForSuccessfulTests) + "<br/>");
                    }

                    if (longestFailedTest != null) {
                        // Since an HTML div with the same id might be presented on this page, we need to add more id uniquiness here
                        long additionalId = System.currentTimeMillis();

                        dateStatus.append("Maximal execution time of failed tests: " + Util.convert(maximalTestExecutionTimeForFailedTests));

                        dateStatus.append("&nbsp;(<a id=\"detailed " + longestFailedTest.getTestRuntimeId() + additionalId
                            + "\" href=\"javascript:toggle('" + longestFailedTest.getTestRuntimeId() + additionalId + "');\">Show details</a>)\n");

                        dateStatus.append("<div id=\"" + longestFailedTest.getTestRuntimeId() + additionalId + "\" style=\"display: none\">\n");
                        dateStatus.append("<pre>\n");
                        dateStatus.append("\n\nThe longest failed test:\n\n" + longestFailedTest.toString());
                        dateStatus.append("</pre>\n");
                        dateStatus.append("</div>\n");

                        dateStatus.append("<br/><br/>");

                    } else {
                        dateStatus.append("Maximal execution time of failed tests: " + Util.convert(maximalTestExecutionTimeForFailedTests) + "<br/><br/>");
                    }
                }
            }

            dateStatus.append("\n</blockquote>\n</td></tr>\n\n");

            dateStatus.append("<tr><td>&nbsp;</td></tr>\n\n");

            // Show current configuration
            if (configuration != null) {
                dateStatus.append("<tr><td>Current configuration:</td></tr>\n\n");
                dateStatus.append("<tr><td>\n<blockquote>\n");
                dateStatus.append("Default timeout for a test: " + Util.convert(configuration.getTestDefaultTimeout()) + "<br/>");
                dateStatus.append("Maximal allowed number of tests per test node: " + configuration.getMaximalNumberOfTestsPerNode() + "<br/>");
                dateStatus.append("Minimal execution time for a test: " + Util.convert(configuration.getTestMinimalExecutionTime()) + "<br/>");
                dateStatus.append("Test resources expectation timeout: " + Util.convert(configuration.getTestResourcesExpectationTimeout()) + "<br/>");
                if (configuration.isMaintenanceMode()) {
                    dateStatus.append("<font color=\"#ee0000\">Maintenance mode is on</font></br>");
                }
                dateStatus.append("\n</blockquote>\n</td></tr>\n\n");
            } else {
                dateStatus.append("<font color=\"#ee0000\">Couldn't obtain configuration settings. Please wait a little bit for updates</font></br>");
            }

            dateStatus.append("<tr><td>&nbsp;</td></tr>\n\n");

            // Show hourly workloads
            String workloadDataStatus = "";

            StringBuffer workloadData = new StringBuffer();
            workloadData.append("var data = [\n");

            if (testEntries.isEmpty()) {
                workloadDataStatus = "<b>No data</b>";
            } else {
                // Parse test entries
                SimpleDateFormat hoursFormat = new SimpleDateFormat("HH:00");
                String hourLabel;
                int numberOfSuccessfulTests = 0;
                int numberOfFailedTests = 0;


                // Set calendar to the beginning of the day
                calendar.setTime(requestedDate);
                calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
                calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
                calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
                calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

                for (int i = 0; i < 24; i++) { // For 24 hours

                    calendar.set(Calendar.HOUR_OF_DAY, i);
                    calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
                    calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
                    calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

                    workloadHistoryStartTime = calendar.getTimeInMillis();

                    calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
                    calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
                    calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));

                    workloadHistoryEndTime = calendar.getTimeInMillis();

                    hourLabel = hoursFormat.format(new Date(workloadHistoryStartTime));
                    numberOfSuccessfulTests = 0;
                    numberOfFailedTests = 0;

                    for (TestEntry current : testEntries) {
                        Long currentTestCreationTime = current.getTestCreationTime();

                        if (currentTestCreationTime > workloadHistoryStartTime && currentTestCreationTime < workloadHistoryEndTime) {
                            // The corresponding test entry is within the specified range
                            if (current.testWasSuccessful()) {
                                numberOfSuccessfulTests++;
                            } else {
                                numberOfFailedTests++;
                            }
                        }
                    }

                    if (numberOfSuccessfulTests > 0 || numberOfFailedTests > 0) {
                        workloadData.append("\t['" + hourLabel + "', " + numberOfSuccessfulTests + ", " + numberOfFailedTests + "],\n");
                    }
                }
            }

            workloadData.append("];\n");

            // Add daily workloads to the HTML
            dateStatus.append("<tr><td>Day workloads: " + workloadDataStatus + "</td></tr>\n");
            dateStatus.append("<tr><td>\n");
            dateStatus.append("<blockquote>\n");
            if (workloadDataStatus.isEmpty()) {
                // Everything is fine, we have some data to show
                dateStatus.append("<canvas id=\"dayWorkloads\" width=\"896\" height=\"256\">\n");
            } else {
                // Otherwise make canvas smaller
                dateStatus.append("<canvas id=\"dayWorkloads\" width=\"896\" height=\"10\">\n");
            }

            dateStatus.append("Unfortunately your browser is not supporting HTML 5.<br/>\n");
            dateStatus.append("Please consider updating to a capable one.\n");
            dateStatus.append("</canvas>\n");
            dateStatus.append("<script type=\"text/javascript\">\n");
            dateStatus.append("var canvas = document.getElementById(\"dayWorkloads\");\n");
            dateStatus.append("var context = canvas.getContext(\"2d\");\n");
            dateStatus.append("var canvasWidth = canvas.width;\n");
            dateStatus.append("var canvasHeight = canvas.height;\n");
            dateStatus.append("// The hour, the number of successful tests and the number of failed tests on that hour\n");
            dateStatus.append(workloadData);
            dateStatus.append("var numberOfHours = data.length;\n");
            dateStatus.append("var maxNumberOfTestsPerHour = 0;\n");
            dateStatus.append("for (var i = 0; i < numberOfHours; i++) {\n");
            dateStatus.append("    if (data[i][1] > maxNumberOfTestsPerHour) {\n");
            dateStatus.append("        maxNumberOfTestsPerHour = data[i][1];\n");
            dateStatus.append("    }\n");
            dateStatus.append("    if (data[i][2] > maxNumberOfTestsPerHour) {\n");
            dateStatus.append("        maxNumberOfTestsPerHour = data[i][2];\n");
            dateStatus.append("    }\n");
            dateStatus.append("}\n");
            dateStatus.append("maxNumberOfTestsPerHour = maxNumberOfTestsPerHour + (maxNumberOfTestsPerHour * 0.2); // For a nicer printouts\n");
            dateStatus.append("var maxWidthForHour = canvasWidth / data.length;\n");
            dateStatus.append("// Don't allow hour column to be too wide\n");
            dateStatus.append("if (data.length < 6) {\n");
            dateStatus.append("    maxWidthForHour = canvasWidth / 6;\n");
            dateStatus.append("}\n");
            dateStatus.append("var maxWidthForColumn = maxWidthForHour / 2;\n");
            dateStatus.append("var maxHeightForSingleTest = canvasHeight / maxNumberOfTestsPerHour;\n");
            dateStatus.append("var fontSize = maxWidthForColumn / 4;\n");
            dateStatus.append("var startX = 0;\n");
            dateStatus.append("var endX = maxWidthForHour;\n");
            dateStatus.append("var startY = 0;\n");
            dateStatus.append("var endY = canvasHeight;\n");
            dateStatus.append("var currentColumnHeight = 0;\n");
            dateStatus.append("context.font = fontSize + \"px Verdana\";\n");
            dateStatus.append("context.lineWidth = 1;\n");
            dateStatus.append("for (var i = 0; i < numberOfHours; i++) {\n");
            dateStatus.append("    var hourLabel = data[i][0];\n");
            dateStatus.append("    var numberOfSuccessfulTests = data[i][1];\n");
            dateStatus.append("    var numberOfFailedTests = data[i][2];\n");
            dateStatus.append("    // Draw column of successful tests\n");
            dateStatus.append("    currentColumnHeight = maxHeightForSingleTest * numberOfSuccessfulTests;\n");
            dateStatus.append("    if (numberOfSuccessfulTests > 0) {\n");
            dateStatus.append("        if (currentColumnHeight < fontSize) {\n");
            dateStatus.append("            currentColumnHeight = 1.1 * fontSize;\n");
            dateStatus.append("        }\n");
            dateStatus.append("    }\n");
            dateStatus.append("    context.beginPath();\n");
            dateStatus.append("    context.fillStyle = \"#007700\";\n");
            dateStatus.append("    context.fillRect(startX, startY + canvasHeight - currentColumnHeight, maxWidthForColumn, canvasHeight);\n");
            dateStatus.append("    context.fillStyle = \"#ffffff\";\n");
            dateStatus.append("    context.fillText(\" \" + numberOfSuccessfulTests, startX, startY + fontSize + canvasHeight - currentColumnHeight, maxWidthForColumn);\n");
            dateStatus.append("    context.closePath();\n");
            dateStatus.append("    // Draw column of failed tests\n");
            dateStatus.append("    currentColumnHeight = maxHeightForSingleTest * numberOfFailedTests;\n");
            dateStatus.append("    if (numberOfFailedTests > 0) {\n");
            dateStatus.append("        if (currentColumnHeight < fontSize) {\n");
            dateStatus.append("            currentColumnHeight = 1.1 * fontSize;\n");
            dateStatus.append("        }\n");
            dateStatus.append("    }\n");
            dateStatus.append("    context.beginPath();\n");
            dateStatus.append("    context.fillStyle = \"#ee0000\";\n");
            dateStatus.append("    context.fillRect(startX + maxWidthForColumn, startY + canvasHeight - currentColumnHeight, maxWidthForColumn, canvasHeight);\n");
            dateStatus.append("    context.fillStyle = \"#ffffff\";\n");
            dateStatus.append("    context.fillText(\" \" + numberOfFailedTests, startX + maxWidthForColumn, startY + fontSize + canvasHeight - currentColumnHeight, maxWidthForColumn);\n");
            dateStatus.append("    context.closePath();\n");
            dateStatus.append("    // Draw hour label\n");
            dateStatus.append("    context.beginPath();\n");
            dateStatus.append("    context.fillStyle = \"#cccccc\";\n");
            dateStatus.append("    context.fillText(\" \" + hourLabel, startX, startY + fontSize, maxWidthForColumn);\n");
            dateStatus.append("    context.closePath();\n");
            dateStatus.append("    startX = startX + maxWidthForHour;\n");
            dateStatus.append("}\n");
            dateStatus.append("</script>\n");
            dateStatus.append("</blockquote>\n");
            dateStatus.append("</td></tr>\n");

            dateStatus.append("<tr><td>&nbsp;</td></tr>\n\n");

            dateStatus.append("<tr><td>Executed tests:</td></tr>\n");

            if (testEntries.isEmpty()) {
                dateStatus.append("<tr><td>\n<blockquote>\n<b>None</b>\n</blockquote>\n</td></tr>\n\n");
            } else {
                dateStatus.append("<tr><td>\n<blockquote>\n");

                SimpleDateFormat testCreationTimeFormat = new SimpleDateFormat(Constant.TIMESTAMP_FORMAT);

                for (TestEntry currentTestEntry : testEntries) {
                    StringBuffer currentTestData = new StringBuffer();
                    String testRuntimeId = currentTestEntry.getTestRuntimeId();
                    String testURL = currentTestEntry.getTestURL();
                    long testCreationTime = currentTestEntry.getTestCreationTime();
                    boolean testWasSuccessful = currentTestEntry.testWasSuccessful();
                    String reasonOfFailure = currentTestEntry.getReasonOfFailure();

                    currentTestData.append(testCreationTimeFormat.format(new Date(testCreationTime)) + " - ");

                    if (testURL != null && !testURL.isEmpty()) {
                        currentTestData.append("<a href=\"" + testURL + "\" target=\"_blank\">");
                    }

                    if (testWasSuccessful) {
                        currentTestData.append("<font color=\"#006600\">");
                        currentTestData.append(testRuntimeId);
                        currentTestData.append("</font>");
                    } else {
                        currentTestData.append("<font color=\"#ee0000\">");
                        currentTestData.append(testRuntimeId);
                        currentTestData.append("</font>");
                    }

                    if (testURL != null && !testURL.isEmpty()) {
                        currentTestData.append("</a>");
                    }

                    if (!testWasSuccessful) {
                        if (reasonOfFailure != null && !reasonOfFailure.isEmpty()) {
                            currentTestData.append(" - " + reasonOfFailure);
                        } else {
                            currentTestData.append(" - " + Constant.UNSPECIFIED_REASON_OF_FAILURE);
                        }

                        Test currentTestDetailedDescription = currentTestEntry.getTest();

                        if (currentTestDetailedDescription != null) {
                            currentTestData.append("&nbsp;(<a id=\"detailed " + currentTestDetailedDescription.getRuntimeId()
                                    + "\" href=\"javascript:toggle('" + currentTestDetailedDescription.getRuntimeId() + "');\">Show details</a>)\n");

                            currentTestData.append("<div id=\"" + currentTestDetailedDescription.getRuntimeId() + "\" style=\"display: none\">\n");
                            currentTestData.append("<pre>\n");
                            currentTestData.append("\n\nDetailed test description on startup:\n" + currentTestDetailedDescription.toString());
                            currentTestData.append("\n\nTest was executed on test node " + currentTestEntry.getTestNodeHostname() + ":" + currentTestEntry.getTestNodePort());

                            currentTestData.append("\nTest was delivered to the Test Automation Service at " + timestampFormat.format(new Date(currentTestEntry.getTestCreationTime())));

                            if (currentTestEntry.getTestInitializationTime() > 0L) {
                                currentTestData.append("\nTest has got all required resources from the test farm at " + timestampFormat.format(new Date(currentTestEntry.getTestInitializationTime())));
                            } else {
                                currentTestData.append("\nTest couldn't get any of required resources from the test farm");
                            }

                            if (currentTestEntry.getTestStartTime() > 0L) {
                                currentTestData.append("\nTest has started on test node at " + timestampFormat.format(new Date(currentTestEntry.getTestStartTime())));
                            } else {
                                currentTestData.append("\nTest couldn't be properly started on the test node");
                            }

                            if (currentTestEntry.getTestEndTime() > 0L) {
                                currentTestData.append("\nTest has ended execution on test node at " + timestampFormat.format(new Date(currentTestEntry.getTestEndTime())));
                            } else {
                                currentTestData.append("\nTest couldn't end properly its execution on the test node");
                            }

                            currentTestData.append("</pre>\n");
                            currentTestData.append("</div>\n");
                        }
                    }

                    currentTestData.append("<br/>\n");

                    dateStatus.append(currentTestData);
                }

                dateStatus.append("</blockquote>\n</td></tr>\n\n");
            }

            dateStatus.append("<tr><td>&nbsp;</td></tr>\n\n");

            dateStatus.append("\n</table>\n</body>\n</html>\n");

            return dateStatus;

        } catch (Exception e) {
            p("Got troubles with getting status for a date '" + date + "': " + e.getClass() + " - " + e.getMessage() + " - " + e.toString());
            e.printStackTrace();
        }

        return new StringBuffer("Got troubles with extracting the status for date '" + date + "'");
    }

    /**
     * Creates a new file in the messages directory and stores specified message in it.
     *
     * @param message Message text to be used
     */
    public void createMessage(String fileName, String message) {
        if (message != null && !message.isEmpty()) {
            try {
                File messageFile = new File(messagesDirectory.getAbsolutePath() + System.getProperty("file.separator") + fileName+"_"+System.currentTimeMillis() + ".msg");
                messageFile.createNewFile();

                // Write message into craeted file
                if (messageFile != null && messageFile.exists()) {
                    FileOutputStream fileOutputStream = new FileOutputStream(messageFile);
                    fileOutputStream.write(message.getBytes());
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
            } catch (Exception e) {
                p("Got troubles while tried to create a file for message '" + message + "': " + e.toString());
                e.printStackTrace();
            }
        }
    }

    /**
     * Prints help message to console.
     */
    private static void printHelp() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\nTest Automation Service v" + Constant.TEST_AUTOMATION_RELEASE_VERSION + "\n");
        stringBuilder.append("\nThe default way to launch an instance of Test Automation Service:\n\n");
        stringBuilder.append("java -jar TestAutomationService.jar "
                + Constant.TEST_AUTOMATION_SERVICE_PORT_NUMBER_ARGUMENT + "=" + Constant.TEST_AUTOMATION_SERVICE_DEFAULT_PORT_NUMBER + "\n\n");
        stringBuilder.append("Here the \"" + Constant.TEST_AUTOMATION_SERVICE_PORT_NUMBER_ARGUMENT
                + "\" parameter stands for a port number that Test Automation Service will use for all incoming communications.");
        stringBuilder.append("\n\nTyping just \"java -jar TestAutomationService.jar\" will automatically launch Test Automation Service on its default port "
                + Constant.TEST_AUTOMATION_SERVICE_DEFAULT_PORT_NUMBER + ". ");
        stringBuilder.append("\n\nPlease note that Test Automation Service might be launched and used anywhere where at least the Java 6 runtime environment is presented.");
        stringBuilder.append("\n\nThe hostname of the machine (and so the Test Automation Service) will be automatically extracted by Java. ");
        stringBuilder.append("However, if Java will fail on extracting the hostname of the machine, it will try to use the \"HOST\", \"HOSTNAME\" or \"COMPUTERNAME\" environment variables.\n");

        stringBuilder.append("\nLaunching Test Automation Service with a description:\n\n");

        stringBuilder.append("java -jar TestAutomationService.jar "
                + Constant.TEST_AUTOMATION_SERVICE_PORT_NUMBER_ARGUMENT + "=" + Constant.TEST_AUTOMATION_SERVICE_DEFAULT_PORT_NUMBER );
        stringBuilder.append(" ").append(Constant.TEST_AUTOMATION_SERVICE_MONITOR_PORT_NUMBER_ARGUMENT).append("=5451").append("\n");
        stringBuilder.append(" " + Constant.TEST_AUTOMATION_SERVICE_DESCRIPTION_ARGUMENT + "=\"Some description for this instance, if any\"\n\n");
        stringBuilder.append("Here the \"" + Constant.TEST_AUTOMATION_SERVICE_DESCRIPTION_ARGUMENT
                + "\" parameter stands for a short description assigned to the Test Automation Service instance.\n");

        System.out.println(stringBuilder.toString());
    }

    /**
     * Used for debug or informal message prints to console.
     *
     * @param text A message to be printed on console
     */
    private static void p(String text) {
        logger.log(Level.ALL, "TAS: " + text);
    }

    /**
     * Launch Test Automation Service as a standalone application
     *
     * @param arguments Startup arguments used for Test Automation Service configuration
     */
    public static void main(String[] arguments) {

        int port = -1;
        int mPort = -1;
        String description = "";

        if (arguments.length > 0) {

            for (int i = 0; i < arguments.length; i++) {

                String parameter = arguments[i];

                if (parameter != null && !parameter.isEmpty()) {

                    // Check for the help request
                    if (parameter.indexOf("help") != -1 || parameter.indexOf("HELP") != -1) {

                        printHelp();

                        return; // Nothing else to do

                    } else if (parameter.indexOf(Constant.TEST_AUTOMATION_SERVICE_PORT_NUMBER_ARGUMENT) != -1) {
                        try {
                            // Parse port number of the Test Automation Service
                            port = Integer.parseInt(parameter.substring(parameter.indexOf("=") + 1));

                            if (port <= 0) {
                                System.out.println(Constant.TEST_AUTOMATION_SERVICE_PORT_NUMBER_ARGUMENT + " parameter has invalid value " + port + ". Please specify a valid one or type command \"java -jar TestAutomationService.jar -help\" for getting more information.");
                                return;
                            }
                        } catch (Exception e) {
                            System.out.println(Constant.TEST_AUTOMATION_SERVICE_PORT_NUMBER_ARGUMENT + " parameter is probably incorrectly specified. Please type command \"java -jar TestAutomationService.jar -help\" for getting more information.");
                            return;
                        }
                    } else if (parameter.indexOf(Constant.TEST_AUTOMATION_SERVICE_MONITOR_PORT_NUMBER_ARGUMENT) != -1 ){
                    	try {
                    		// Parse port number of the Test Automation Service
                            mPort = Integer.parseInt(parameter.substring(parameter.indexOf("=") + 1));

                            if (mPort <= 0) {
                                System.out.println(Constant.TEST_AUTOMATION_SERVICE_MONITOR_PORT_NUMBER_ARGUMENT + " parameter has invalid value " + port + ". Please specify a valid one or type command \"java -jar TestAutomationService.jar -help\" for getting more information.");
                                return;
                            }
                        } catch (Exception e) {
                            System.out.println(Constant.TEST_AUTOMATION_SERVICE_MONITOR_PORT_NUMBER_ARGUMENT + " parameter is probably incorrectly specified. Please type command \"java -jar TestAutomationService.jar -help\" for getting more information.");
                            return;
                        }
                    } else if (parameter.indexOf(Constant.TEST_AUTOMATION_SERVICE_DESCRIPTION_ARGUMENT) != -1) {
                        try {
                            int index = parameter.indexOf("=");

                            if (index != -1) {
                                description = parameter.substring(index + 1);
                            }
                        } catch (Exception e) {
                            System.out.println(Constant.TEST_AUTOMATION_SERVICE_DESCRIPTION_ARGUMENT + " parameter is probably incorrectly specified. Please type command \"java -jar TestAutomationService.jar -help\" for getting more information.");
                            return;
                        }
                    } else {
                        System.out.println("Specified parameter " + parameter + " is not supported and will be ignored.");
                    }
                }
            }
        } else {
            System.out.println("Port number for the Test Automation Service was not specified. Default port number " + Constant.TEST_AUTOMATION_SERVICE_DEFAULT_PORT_NUMBER + " will be used.");
            port = Constant.TEST_AUTOMATION_SERVICE_DEFAULT_PORT_NUMBER;
        }

        if (port > 0) {
            TestAutomationService tas = getInstance(port, description, mPort);
        }
    }
}
