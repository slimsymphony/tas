package com.nokia.ci.tas.service;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;

import java.util.Collection;
import java.util.List;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.logging.Logger;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.Util;

import com.nokia.ci.tas.commons.message.Message;
import com.nokia.ci.tas.commons.message.TestOperation;

import com.nokia.ci.tas.commons.statistics.Statistics;

/**
 * Handler of a single Test performed by the Testing Automation Service.
 */
public class TestHandler extends Thread {

    /**
     * Current instance of the Test Automation Service.
     */
    private TestAutomationService testAutomationService;

    /**
     * Hostname of the Test Automation Service.
     */
    private String testAutomationServiceHostname;

    /**
     * Port number of the Test Automation Service.
     */
    private int testAutomationServicePort;

    /**
     * Instance of the test monitor.
     */
    private TestMonitor testMonitor;

    /**
     * Instance of the test to be performed
     */
    private Test test;

    /**
     * Test node responsible for performing specified test.
     */
    private TestNode reservedTestNode;

    /**
     * Message pool for enabling asynchronous messaging.
     */
    private ConcurrentLinkedQueue<Message> messagePool;

    /**
     * Path to test workspace on the side of Test Automation Service.
     */
    private File testWorkspace;

    /**
     * File containing statistics information for test execution.
     */
    private File statisticsFile;

    /**
     * Statistics writer for the test handler.
     */
    private PrintWriter statisticsWriter;

    /**
     * A list of product reserved for the test.
     */
    private List<Product> reservedProducts;

    /**
     * Variable for keeping test handler performing the test.
     */
    private boolean isRunning = true;

    /**
     * Tells whenever test is failed or not.
     */
    private boolean testHasFailed = false;

    /**
     * Tells the reason of test failure.
     */
    private String reasonOfTestFailure = null;

    /**
     * Keeps a list of file names which should be received by test handler.
     */
    private ConcurrentLinkedQueue<String> listOfFilesToBeReceived;

    /**
     * Keeps a list of file names which should be send by test handler.
     */
    private ConcurrentLinkedQueue<String> listOfFilesToBeSend;

    /**
     * Moment of time when this handler started working.
     */
    private long testHandlingStartedAt = 0L;

    /**
     * Moment of time when test execution has actually started on the test node.
     */
    private long testExecutionOnTestNodeStartedAt = 0L;

    /**
     * Current configuration of the Test Automation Service.
     */
    private Configuration configuration;

    /**
     * Instance of the remote client that has issued specified test.
     */
    private RemoteClient remoteClient;

    /**
     * Instance of the Test Automation Service's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationService.GLOBAL_LOGGER_NAME);

    /**
     * Creates a test handler for specified test.
     * All the messages regarding test performances will be also delivered to specified listener.
     * If testNode will be not specified, the test handler will try to discover the best node by itself.
     *
     * @param testAutomationService Instance of the Test Automation Service
     * @param testMonitor Instance of the test monitor
     * @param test A test to be handled
     * @param reservedTestNode Instance of the test node reserved for specified test
     * @param reservedProducts List of the products reserved fro specified test
     */
    public TestHandler(TestAutomationService testAutomationService,
                       TestMonitor testMonitor,
                       Test test,
                       TestNode reservedTestNode,
                       List<Product> reservedProducts) {

        super(test.getRuntimeId()); // Test handlers are always associated with test's runtime ids

        this.testAutomationService = testAutomationService;
        this.testMonitor = testMonitor;
        this.test = test;
        this.reservedTestNode = reservedTestNode;
        this.reservedProducts = reservedProducts; // The same list of reserved products may be found in the test instance
        this.configuration = testAutomationService.getConfiguration();
        this.remoteClient = testMonitor.getRemoteClient();

        messagePool = new ConcurrentLinkedQueue();

        listOfFilesToBeReceived = new ConcurrentLinkedQueue();
        listOfFilesToBeSend = new ConcurrentLinkedQueue();

        // Extract the following parameters to eliminate unnecessary method calls
        testAutomationServiceHostname = testAutomationService.getHostname();
        testAutomationServicePort = testAutomationService.getPort();

        setPriority(MIN_PRIORITY); // Always run with minimal priority
    }

    /**
     * Returns a test node on which this test is running.
     *
     * @return A test node on which this test is running
     */
    public synchronized TestNode getReservedTestNode() {
        return reservedTestNode;
    }

    /**
     * Returns a list of reserved products.
     *
     * @return A list of reserved products
     */
    public synchronized List<Product> getReservedProducts() {
        return reservedProducts;
    }

    /**
     * Returns a test associated with this test handler.
     *
     * @return A test associated with this test handler
     */
    public synchronized Test getTest() {
        return test;
    }

    /**
     * Run is implementation of a normal test handling on the side of Test Automation Service.
     */
    @Override
    public void run() {
        p("Started handling test '" + test.getRuntimeId() + "':\n" + test.toString());

        // Always ensure test timeout
        if (test.getTimeout() <= 0L) {
            p("Test '" + test.getRuntimeId() + "' hasn't any specified timeout. The default timeout will be used");
            test.setTimeout(configuration.getTestDefaultTimeout());
        }

        // Always get synchronized with the test monitor about the time when test handling has started
        testHandlingStartedAt = testMonitor.getTestHandlingStartTime();

        statisticsFile = testAutomationService.createStatisticsFile(test);

        if (statisticsFile != null) {
            try {
                statisticsWriter = new PrintWriter(statisticsFile);
            } catch (Exception e) {
                p("Got troubles while tried to create a statistics file writer for the handler: " + e.getClass() + " - " + e.getMessage());
                statisticsWriter = null;
            }
        } else {
            p("Couldn't obtain a reference to statistics file");
            statisticsWriter = null;
        }

        // At this point test is considered to be officially started
        test.setStatus(Test.Status.STARTED);

        // Write test initialization data into statistics file
        writeStatistics(Statistics.TEST_CREATION_TIME_LABEL + Statistics.STATISTICS_LABEL_AND_TIME_SEPARATOR + testHandlingStartedAt);
        writeStatistics(Statistics.CREATED_TEST_DESCRIPTION_LABEL + "\n" + test.toXML());
        writeStatistics(Statistics.TEST_INITIALIZATION_TIME_LABEL + Statistics.STATISTICS_LABEL_AND_TIME_SEPARATOR + System.currentTimeMillis());
        writeStatistics(Statistics.TEST_RESOURCES_SUCCESSFUL_RESERVATION_LABEL);

        // 1. Send a "start test" message to the test node and wait for "test started" or "test failed" messages
        if (isRunning) {
            p("Ensuring the minimal execution time for the test");

            // Time left for execution is timeout minus the time spent for test resources allocation
            long timeLeft = test.getTimeout() - (System.currentTimeMillis() - testHandlingStartedAt);

            // Prevent any negative values
            if (timeLeft < 0L) {
                timeLeft = 0L;
            }

            // If left time is greater than minimal limit, start the test
            if (timeLeft > configuration.getTestMinimalExecutionTime()) {
                p("The remaining test time is " + Util.convert(timeLeft) + ". Test '" + test.getRuntimeId() + "' can be started");

                TestOperation startTestMessage = new TestOperation(TestOperation.Id.START, test);
                startTestMessage.setSender(remoteClient.getClientHostname(), remoteClient.getClientPort());

                if (startTestMessage != null) {
                    if (reservedTestNode != null) {
                        startTestMessage.setReceiver(reservedTestNode.getHostname(), reservedTestNode.getPort());
                        reservedTestNode.handle(startTestMessage);
                        notifyMonitor("Trying to start test '" + test.getRuntimeId() + "' on the test node " + reservedTestNode.getHostnameAndPort());
                        writeStatistics(System.currentTimeMillis(), Statistics.INITIALIZED_TEST_DESCRIPTION_LABEL + "\n" + test.toXML());
                    }
                }

                // Wait for any responses from the test node
                while (isRunning) {
                    try {
                        if (!messagePool.isEmpty()) {
                            Message message = messagePool.poll(); // Always get the first message

                            if (message instanceof TestOperation) {
                                TestOperation testOperation = (TestOperation) message;
                                Test receivedTestUpdate = testOperation.getTest();

                                if (receivedTestUpdate != null) {
                                    if (receivedTestUpdate.getStatus() == Test.Status.STARTED) {
                                        // Remember the time when test has started on the test node
                                        testExecutionOnTestNodeStartedAt = System.currentTimeMillis();
                                        test.setStartTime(testExecutionOnTestNodeStartedAt);
                                        test.setStatus(Test.Status.STARTED, "");
                                        isRunning = true;

                                        p("Test '" + test.getRuntimeId() + "' has started on test node " + reservedTestNode.getHostnameAndPort());
                                        testMonitor.notifyMonitorAboutStartedTest(test);

                                        writeStatistics(Statistics.TEST_START_TIME_LABEL + Statistics.STATISTICS_LABEL_AND_TIME_SEPARATOR + testExecutionOnTestNodeStartedAt);
                                        writeStatistics(Statistics.TEST_STARTUP_LABEL + reservedTestNode.getHostnameAndPort());
                                    } else if (receivedTestUpdate.getStatus() == Test.Status.FAILED) {
                                        isRunning = false;
                                        testHasFailed = true;
                                        reasonOfTestFailure = receivedTestUpdate.getStatusDetails();
                                        test.setStatus(Test.Status.FAILED, reasonOfTestFailure);
                                        p(reasonOfTestFailure);
                                        writeStatistics(System.currentTimeMillis(), reasonOfTestFailure);
                                    } else {
                                        p("Got an unsupported message: " + message);
                                    }
                                }
                            }

                            break;
                        }

                        // Check timeout
                        if (test.isExpired(testHandlingStartedAt)) {
                            isRunning = false;
                            testHasFailed = true;
                            reasonOfTestFailure = "Got expiration of test's timeout (" + Util.convert(test.getTimeout())
                                                  + ") during waiting for notification about test's start on the test node " + reservedTestNode.getHostnameAndPort();
                            test.setStatus(Test.Status.FAILED, reasonOfTestFailure);
                            p(reasonOfTestFailure);
                        }

                        sleep(Constant.ONE_SECOND); // Wait for any updates

                    } catch (Exception e) {
                        p("Got troubles while trying to start executing test '" + test.getRuntimeId() + "' on the test node " + reservedTestNode.getHostnameAndPort());
                        e.printStackTrace();
                    }
                }
            } else {
                // Fail the test
                isRunning = false;
                testHasFailed = true;
                reasonOfTestFailure = "The remaining test time is " + Util.convert(timeLeft)
                                      + ". That is less than minimal time for test execution ("
                                      + Util.convert(configuration.getTestMinimalExecutionTime())
                                      + "). Test '" + test.getRuntimeId() + "' is not allowed to be started";
                test.setStatus(Test.Status.FAILED, reasonOfTestFailure);
                p(reasonOfTestFailure);
            }
        }

        // 2. If test was successfully started, wait until test executor on test node will send us a "test finished" or "test failed" messages
        if (isRunning) {
            // Wait for any responses from the test node
            while (isRunning) {
                try {
                    if (!messagePool.isEmpty()) {
                        Message message = messagePool.poll(); // Always get the first message

                        if (message instanceof TestOperation) {
                            TestOperation testOperation = (TestOperation) message;
                            Test receivedTestUpdate = testOperation.getTest();

                            if (receivedTestUpdate != null) {
                                if (receivedTestUpdate.getStatus() == Test.Status.FINISHED) {
                                    test.setStatus(Test.Status.FINISHED, "");
                                    isRunning = true;
                                    // Move to the next step
                                    break;
                                } else if (receivedTestUpdate.getStatus() == Test.Status.FAILED) {
                                    isRunning = false;
                                    testHasFailed = true;
                                    reasonOfTestFailure = receivedTestUpdate.getStatusDetails();
                                    test.setStatus(Test.Status.FAILED, reasonOfTestFailure);
                                    p(reasonOfTestFailure);
                                    // Move to the next step
                                    break;
                                } else {
                                    p("Got an unsupported message: " + message);
                                }
                            }
                        }
                    }

                    // Check timeout
                    if (test.isExpired(testHandlingStartedAt)) {
                        isRunning = false;
                        testHasFailed = true;
                        reasonOfTestFailure = "Got expiration of test's timeout (" + Util.convert(test.getTimeout())
                                              + ") during test execution on the test node " + reservedTestNode.getHostnameAndPort();
                        test.setStatus(Test.Status.FAILED, reasonOfTestFailure);
                        p(reasonOfTestFailure);
                    }

                    sleep(Constant.ONE_SECOND); // Wait for updates

                } catch (Exception e) {
                    p("Got troubles while trying to wait for test's '" + test.getRuntimeId() + "' results from the test node " + reservedTestNode.getHostnameAndPort());
                    e.printStackTrace();
                }
            }
        }

        // At this point test is either finished or failed
        p("Handler of the test '" + test.getRuntimeId() + "' is finishing its work");

        if (testHasFailed) {
            if (reasonOfTestFailure == null || reasonOfTestFailure.isEmpty()) {
                reasonOfTestFailure = Constant.UNSPECIFIED_REASON_OF_FAILURE;
                test.setStatus(Test.Status.FAILED, reasonOfTestFailure);
                p(reasonOfTestFailure);
            }

            writeStatistics(Statistics.TEST_FAILURE_LABEL + reasonOfTestFailure);

            // Automatically release all reserved products
            if (reservedProducts != null && !reservedProducts.isEmpty()) {
                p("Since test '" + test.getRuntimeId() + "' has failed, Test Automation Service will automatically release all reserved products,"
                    + " no matter what the releasing mode was...");

                if (reservedTestNode != null) {
                    reservedTestNode.freeProducts(reservedProducts);

                    // Notify listener about automatically released products
                    for (Product reservedProduct : reservedProducts) {
                        notifyMonitor("The following product was automatically released due to failure of the test '" + test.getRuntimeId() + "': " + reservedProduct);
                    }

                    reservedProducts = null;
                }
            }

            notifyAboutFailedTest(reasonOfTestFailure);

        } else {
            writeStatistics(Statistics.TEST_SUCCESS_LABEL + reservedTestNode.getHostnameAndPort());

            // Free any reserved products if releasing mode was automatic
            if (test.getProductReleasingMode() == Test.ProductReleasingMode.MANUALLY_RELEASE_RESERVED_PRODUCTS) {
                p("Skipping automatic releasing of reserved products, since test's product releasing mode was MANUAL");

                if (reservedProducts != null && !reservedProducts.isEmpty()) {
                    for (Product reservedProduct : reservedProducts) {
                        notifyMonitor("Note, a product which should be manually released after test '" + test.getRuntimeId() + "' is: " + reservedProduct);
                    }
                }
            } else {
                p("Automatically releasing all reserved products, since test's product releasing mode was AUTOMATIC");

                if (reservedTestNode != null) {
                    p("Trying to notify the test node " + reservedTestNode.getHostnameAndPort() + " about released products...");
                    reservedTestNode.freeProducts(reservedProducts);
                } else {
                    p("Error: Reserved test node is NULL! Couldn't notify it about released products...");
                }
            }

            notifyAboutFinishedTest();
        }

        // One running test less
        if (reservedTestNode != null) {
            reservedTestNode.decreaseNumberOfRunningTests(test, testHasFailed);
        }

        writeStatistics(Statistics.TEST_END_TIME_LABEL + Statistics.STATISTICS_LABEL_AND_TIME_SEPARATOR + System.currentTimeMillis());

        if (statisticsWriter != null) {
            statisticsWriter.close();
        }

        testMonitor.removeTestHandler(this, testHasFailed);
        try {
	        Field f = testAutomationService.getClass().getField( "remoteClients" );
	        @SuppressWarnings( "unchecked" )
			Collection<RemoteClient> remoteClients = (Collection<RemoteClient>)f.get( testAutomationService );
	        if(remoteClients!=null)
	        	remoteClients.remove( remoteClient );
        }catch( Exception e ) {
        	p( "Remove remote client from service side failed." + e.getMessage() );
        }
    }

    /**
     * Stops test handler and performs all related cleanups.
     *
     * @param reason Reason behind test's stop
     */
    public synchronized void stopTest(String reason) {

        if (reason != null && !reason.isEmpty()) {
            p("Got a request to STOP the test '" + test.getRuntimeId() + "' because: " + reason);
            reasonOfTestFailure = reason;
        } else {
            p("Got a request to STOP the test '" + test.getRuntimeId() + "' because of unspecified reason");
            reasonOfTestFailure = Constant.UNSPECIFIED_REASON_OF_FAILURE;
        }

        // Send reserved test node a "stop test" message and immediately release all reserved products
        if (reservedTestNode != null) {
            p("Notifying test node " + reservedTestNode.getHostnameAndPort() + " about failed test '" + test.getRuntimeId() + "'");
            TestOperation stopTestOperation = new TestOperation(TestOperation.Id.STOP, test);
            stopTestOperation.setSender(testAutomationServiceHostname, testAutomationServicePort);
            stopTestOperation.setReceiver(reservedTestNode.getHostname(), reservedTestNode.getPort());
            reservedTestNode.handle(stopTestOperation);
            reservedTestNode.decreaseNumberOfRunningTests(test, true);

            // Automatically release all reserved products
            if (reservedProducts != null && !reservedProducts.isEmpty()) {
                p("Automatically releasing all reserved products, no matter what the releasing mode was...");

                reservedTestNode.freeProducts(reservedProducts);

                // Notify listener about automatically released products
                for (Product reservedProduct : reservedProducts) {
                    notifyMonitor("The following product was automatically released due to test failure: " + reservedProduct);
                }

                reservedProducts = null;
            }
        }

        isRunning = false;
        testHasFailed = true;

        listOfFilesToBeReceived.clear();
        listOfFilesToBeSend.clear();

        notify();
    }

    /**
     * Asynchronously handles specified message.
     *
     * @param message A message to be handled
     */
    public synchronized void handle(Message message) {
        if (messagePool.add(message)) {
            p("Test Handler for '" + getName() + "' got a message to handle:\n" + message);
        } else {
            p("Error: Couldn't add a message for handling:\n" + message);
        }
        notify();
    }

    /**
     * Returns a workspace associated with performing test.
     *
     * @return A workspace associated with performing test
     */
    public synchronized File getTestWorkspace() {
        return testWorkspace;
    }

    /**
     * Returns the time when test handling has started.
     *
     * @return Time when test handling has started
     */
    public long getTestHandlingStartTime() {
        return testHandlingStartedAt;
    }

    /**
     * Returns the time when test execution has started on test node.
     *
     * @return Time when test execution has started on test node
     */
    public long getTestExecutionStartTime() {
        return testExecutionOnTestNodeStartedAt;
    }

    /**
     * Sends test monitor specified message.
     *
     * @param message Message to be send to the test monitor
     */
    private synchronized void notifyMonitor(String message) {
        p(message);
        testMonitor.notifyListener(message);
    }

    /**
     * Notifies listener about failed test.
     *
     * @param reason Reason behind test's failure
     */
    private synchronized void notifyAboutFailedTest(String reason) {
        // Always ensure that the reason is specified
        if (reason != null && !reason.isEmpty()) {
            reasonOfTestFailure = reason;
        } else {
            reasonOfTestFailure = Constant.UNSPECIFIED_REASON_OF_FAILURE;
        }

        p("Notifying monitor about FAILED test '" + test.getRuntimeId() + "' because: " + reasonOfTestFailure);

        test.setStatus(Test.Status.FAILED, reasonOfTestFailure);

        testMonitor.notifyMonitorAboutFailedTest(test, reasonOfTestFailure);
    }

    /**
     * Notifies listener about finished test.
     */
    private synchronized void notifyAboutFinishedTest() {
        p("Notifying test monitor about FINISHED test '" + test.getRuntimeId() + "'");

        test.setStatus(Test.Status.FINISHED);
        testMonitor.notifyMonitorAboutFinishedTest(test);
    }

    /**
     * Returns true if test has expired or false otherwise.
     *
     * @return True if test has expired or false otherwise
     */
    public boolean isTestExpired() {
        return test.isExpired(testHandlingStartedAt);
    }

    /**
     * Writes specified message to the statistics file.
     *
     * @param message Message to be written into statistics file
     */
    private synchronized void writeStatistics(String message) {
        if (statisticsWriter != null) {
            statisticsWriter.append(message);
            statisticsWriter.append("\n");
            statisticsWriter.flush();
        }
    }

    private synchronized void writeStatistics(long timestamp, String message) {
        writeStatistics(timestamp + " - " + message);
    }

    /**
     * Prints specified text to output stream.
     *
     * @param text A text to be printed to output stream
     */
    protected synchronized void p(String text) {
        // All messages are forwarded to test monitor
        testMonitor.p(getName() + ": " + text);
    }
}
