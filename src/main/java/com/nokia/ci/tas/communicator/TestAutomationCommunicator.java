package com.nokia.ci.tas.communicator;

import java.io.File;
import java.io.PrintWriter;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.SimCard;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.TestNodeDescription;

import com.nokia.ci.tas.commons.Util;
import com.nokia.ci.tas.commons.message.ProductOperation;
import com.nokia.ci.tas.commons.message.RegistryOperation;
import com.nokia.ci.tas.commons.message.TestOperation;

import com.nokia.ci.tas.commons.log.ConsoleFormatter;
import com.nokia.ci.tas.commons.log.LogFileFormatter;
import com.nokia.ci.tas.commons.log.LogFileSwitcher;
import com.nokia.ci.tas.communicator.monitor.MonitorListener;

/**
 * Test Automation Communicator for CI 2.0 Testing Automation Service.
 */
public class TestAutomationCommunicator extends Thread {

    /**
     * Static instance of the Test Automation Communicator.
     */
    private static TestAutomationCommunicator self;

    /**
     * Hostname of the Test Automation Service.
     */
    private String testAutomationServiceHostname = null;

    /**
     * Port number of the Test Automation Service for incoming connections.
     */
    private int testAutomationServicePort = Constant.TEST_AUTOMATION_SERVICE_DEFAULT_PORT_NUMBER;

    /**
     * Name of the Test Automation Communicator's hosting machine.
     */
    private String communicatorHostname = null;

    /**
     * Port number used by Test Automation Communicator for incoming connections.
     */
    private int communicatorPort = Constant.TEST_AUTOMATION_COMMUNICATOR_DEFAULT_PORT_NUMBER;

    /**
     * Description of the test node.
     */
    private TestNodeDescription testNodeDescription;

    /**
     * Name of the directory where Test Automation Communicator keeps all tests artifacts and tools.
     */
    private static final String COMMUNICATOR_WORKSPACE_DIRECTORY = "workspace";

    /**
     * Startup flag which enables keeping workspaces of all tests, no matter if they failed or succeeded.
     */
    private static final String KEEP_WORKSPACES_OF_ALL_TESTS = "--keep-workspaces-of-all-tests";

    /**
     * Setting which enables keeping workspaces of all tests, no matter if they has failed or succeeded.
     */
    private static boolean keepWorkspacesOfAllTests = false;

    /**
     * Startup flag which enables keeping workspaces of failed tests only.
     */
    private static final String KEEP_WORKSPACES_OF_FAILED_TESTS = "--keep-workspaces-of-failed-tests";

    /**
     * Setting which enables keeping workspaces of failed tests only.
     */
    private static boolean keepWorkspacesOfFailedTests = false;

    /**
     * Startup setting for defining the number of days that test artifacts and log files may be preserved by the Test Automation Communicator.
     */
    private static final String CLEANUP_PERIOD_IN_DAYS = "--cleanup-period-in-days";

    /**
     * Constant which defines the default number of days that test artifacts and log files may be preserved by the Test Automation Communicator.
     */
    private static final int DEFAULT_CLEANUP_PERIOD_IN_DAYS = 7;

    /**
     * Cleanup period in milliseconds.
     */
    private static long cleanupPeriod = DEFAULT_CLEANUP_PERIOD_IN_DAYS * Constant.ONE_DAY;

    /**
     * Reference to a workspace directory of the Test Automation Communicator.
     */
    private File communicatorWorkspace;

    /**
     * Name of the directory where Test Automation Communicator keeps all product specific configuration files.
     */
    private static final String COMMUNICATOR_PRODUCTS_DIRECTORY = "products";

    /**
     * Reference to a product configurations directory used by Test Automation Communicator.
     */
    private File productConfigurationsDirectory;

    /**
     * Name of the directory where Test Automation Communicator keeps all log files.
     */
    private static final String COMMUNICATOR_LOGS_DIRECTORY = "logs";

    /**
     * Reference to a logs directory used by Test Automation Communicator.
     */
    private File logsDirectory;

    /**
     * Format used in names of the log files. Here they follow the Java standard date and time patterns.
     */
    private String logFilenameFormat = "yyyy-MM-dd";

    /**
     * Name associated with the global Logger.
     */
    public static final String GLOBAL_LOGGER_NAME = "TestAutomationCommunicator";

    /**
     * Timer used for switching log files.
     */
    private Timer timer;

    /**
     * Dynamic list of currently available test executors.
     * Each test executor takes care about a single test
     * and works as an independend thread having its own workspace directory.
     */
    private ConcurrentLinkedQueue<TestExecutor> testExecutors;

    /**
     * Receiver is Test Automation Communicator's component responsible
     * for caching and processing all incoming connections.
     */
    private Receiver receiver;

    /**
     * Sender is Test Automation Communicator's component responsible
     * for caching and processing all outcoming messages.
     */
    private Sender sender;

    /**
     * Instance of the file caching utility.
     */
    private FileCache fileCache;

    /**
     * Product explorer is responsible for getting information
     * and current status out of all products and prototypes
     * connected to the testing node through various communication ports.
     */
    private ProductExplorer productExplorer;

    /**
     * Running variable keeps Communator working.
     */
    private boolean isRunning = true;

    /**
     * Listener of all incoming messages.
     */
    private ServerSocket listener;

    /**
     * Tells the moment of time when the last test was started.
     */
    private long timeWhenLastTestHasStarted = 0L;

    /**
     * A symbol used by the hosting system for delimiting files and directories in paths.
     */
    private String fileSeparator;

    /**
     * Global logger used in all prints and messaging.
     */
    private static Logger logger;

    /**
     * Creates an instance of the Test Automation Communicator.
     * The constructor is made private for allowing
     * only the single static instance of the communicator on a hosting machine.
     */
    private TestAutomationCommunicator(String[] arguments) {

        super(); // Start as anonymous thread

        self = this;

        fileSeparator = System.getProperty("file.separator");

        if (fileSeparator == null || fileSeparator.isEmpty()) {
            // Assume Windows environment by default
            fileSeparator = "\\";
        }

        boolean communicatorCanBeStarted = parseArguments(arguments);

        if (communicatorCanBeStarted) {
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
                logsDirectory = new File(COMMUNICATOR_LOGS_DIRECTORY);
                if (!logsDirectory.exists()) {
                    if (logsDirectory.mkdirs()) {
                        p("Test Automation Communicator's log directory was successfully created at " + logsDirectory.getAbsolutePath());
                    }
                } else {
                    p("Test Automation Communicator's log directory was successfully initialized at " + logsDirectory.getAbsolutePath());
                }

                // Create formatter for the log file
                LogFileFormatter logFileFormatter = new LogFileFormatter(new SimpleDateFormat(Constant.TIMESTAMP_FORMAT));

                // Create timer and init current log
                timer = new Timer(true);
                timer.schedule(new LogFileSwitcher(TestAutomationCommunicator.GLOBAL_LOGGER_NAME, logsDirectory, logFileFormatter, logFilenameFormat, cleanupPeriod), new Date());

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
                timer.scheduleAtFixedRate(new LogFileSwitcher(TestAutomationCommunicator.GLOBAL_LOGGER_NAME, logsDirectory, logFileFormatter, logFilenameFormat, cleanupPeriod),
                    nextLogChangeDate, 24L * Constant.ONE_HOUR);

                p("Log file's switcher was successfully scheduled");

            } catch (Exception e) {
                p("Got troubles while tried to initialize logging:" + e.toString());
                e.printStackTrace();
                return;
            }

            p("Test Automation Communicator v" + Constant.TEST_AUTOMATION_RELEASE_VERSION);
            p("Launching Test Automation Communicator on " + communicatorHostname + ":" + communicatorPort
              + " for the Test Automation Service at " + testAutomationServiceHostname + ":" + testAutomationServicePort);

            if (!testNodeDescription.getDescription().isEmpty()) {
                p("Test node description: " + testNodeDescription.getDescription());
            }

            setPriority(Thread.MAX_PRIORITY);

            start();
            
            MonitorListener ml = new MonitorListener( this );
            ml.start();
        }
    }

    /**
     * Communicator's main routine.
     */
    @Override
    public void run() {

        long communicatorLaunchedAt = System.currentTimeMillis();

        // Create Test Automation Communicator's workspace directory
        try {
            communicatorWorkspace = new File(COMMUNICATOR_WORKSPACE_DIRECTORY);

            // Empty workspace directory if it exists
            if (communicatorWorkspace.exists()) {
                p("Workspace directory is already exising. Deleting old workspace data...");
                deleteRecursively(communicatorWorkspace);
                p("Old workspace was successfully deleted");
            }

            // Create workspace if it is not existing
            if (!communicatorWorkspace.exists()) {
                communicatorWorkspace.mkdirs();

                if (communicatorWorkspace.exists()) {
                    p("Test Automation Communicator's workspace was successfully created at " + communicatorWorkspace.getAbsolutePath());
                }
            } else {
                p("Test Automation Communicator's workspace exists at " + communicatorWorkspace.getAbsolutePath());
            }

            // Create a directory for products configuration files if it is not existing, but never erase it!
            try {
                productConfigurationsDirectory = new File(COMMUNICATOR_PRODUCTS_DIRECTORY);

                if (!productConfigurationsDirectory.exists()) {
                    productConfigurationsDirectory.mkdirs();

                    if (productConfigurationsDirectory.exists()) {
                        p("Test Automation Communicator's product configurations directory was successfully created at " + productConfigurationsDirectory.getAbsolutePath());
                    }
                } else {
                    p("Test Automation Communicator's product configurations directory exists at " + productConfigurationsDirectory.getAbsolutePath());
                }

                // Always ensure that a reference example of a product configuration file is existing in this directory
                String referenceFilename = "IMEI.xml";

                File referenceFile = new File(productConfigurationsDirectory.getAbsolutePath() + fileSeparator + referenceFilename);

                if (!referenceFile.exists()) {
                    if (referenceFile.createNewFile()) {
                        // Write example contents into this file
                        try {
                            PrintWriter exampleConfiguration = new PrintWriter(referenceFile, "UTF-8");

                            Product exampleProduct = new Product();

                            exampleProduct.setIMEI("000000000000001");
                            exampleProduct.setRMCode("RM-123");
                            exampleProduct.setFuseConnectionName("NFPD USB_FPS21");
                            exampleProduct.setFuseConnectionId("Guid_0123456789");
                            exampleProduct.setIPAddress("172.23.127.26");
                            exampleProduct.setPort("15001");
                            exampleProduct.setHardwareType("X1");
                            exampleProduct.setRole(Product.Role.MAIN);

                            SimCard simCard1 = new SimCard(SimCard.XML_ELEMENT_SIM_CARD_1);
                            simCard1.setPhoneNumber("+3585550000001");
                            simCard1.setPin1Code("1234");
                            simCard1.setPin2Code("2222");
                            simCard1.setPuk1Code("11111111");
                            simCard1.setPuk2Code("22222222");
                            exampleProduct.setSim1(simCard1);

                            SimCard simCard2 = new SimCard(SimCard.XML_ELEMENT_SIM_CARD_2);
                            simCard2.setPhoneNumber("+3585550000002");
                            simCard2.setPin1Code("1234");
                            simCard2.setPin2Code("2222");
                            simCard2.setPuk1Code("11111111");
                            simCard2.setPuk2Code("22222222");
                            exampleProduct.setSim2(simCard2);

                            exampleProduct.setTestAutomationService("test-automation-service.nokia.com", 12345);

                            exampleConfiguration.append(Constant.XML_DECLARATION + "\n" + exampleProduct.toXML());

                            exampleConfiguration.flush();
                            p("A reference configuration file is created at " + referenceFile.getAbsolutePath());

                        } catch (Exception e) {
                            // This issue is not a critical one, but we should explain
                            p("Warning: Couldn't create a reference configuration file at " + referenceFile.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception e) {
                p("Error: Test Automation Communicator has got a problem during initialization of directory for product configurations:");
                e.printStackTrace();
                return;
            }
        } catch (Exception e) {
            p("Error: Test Automation Communicator has got a problem during its workspace initialization:");
            e.printStackTrace();
            return;
        }

        // Create a list of test executors
        testExecutors = new ConcurrentLinkedQueue();

        // Initialize and start file cache
        fileCache = new FileCache();

        // Initialize and start Receiver
        receiver = new Receiver(fileCache);
        receiver.start();

        // Initialize and start Sender
        sender = new Sender(fileCache);
        sender.start();

        // Initalize and start Product Explorer
        productExplorer = new ProductExplorer(sender);
        productExplorer.start();
        
     // Let the FUSE and CI Proxy wake up during this minimal interval
        if ((System.currentTimeMillis() - communicatorLaunchedAt) < Constant.TEN_SECONDS) {
            try {
                p("Will wait for Product Detector initializations for 10 seconds...");
                sleep(Constant.TEN_SECONDS);
            } catch (Exception e) {
                // Ignore
            }
        }

        // Register a handler of the shutdown event
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });

        p("Trying to start Test Automation Communicator on " + communicatorHostname + ":" + communicatorPort);

        // Find a free port number and create listening socket
        while (isRunning) {
            try {
                listener = new ServerSocket(communicatorPort);
                p("Test Automation Communicator will work on " + communicatorHostname + ":" + communicatorPort);
                break;
            } catch (Exception e) {
                p("Got a problem during getting a free port: " + e.getClass() + " " + e.getMessage());
                p("Port " + communicatorPort + " is probably already occupied");
                communicatorPort += 1;

                if (communicatorPort > 0xffff) {
                    communicatorPort = Constant.TEST_AUTOMATION_COMMUNICATOR_DEFAULT_PORT_NUMBER;
                }

                p("Will try to use port " + communicatorPort);
            }
        }

        p("Communicator started listening for all incoming messages on port " + communicatorPort);

        // Start listening to incoming connections
        try {
            p("Test Automation Communicator successfully started woring on " + communicatorHostname + ":" + communicatorPort);

            // Try to register to the specified Test Automation Service
            RegistryOperation registryOperation = new RegistryOperation(RegistryOperation.Id.REGISTER, RegistryOperation.Remote.TEST_NODE);
            registryOperation.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
            registryOperation.setSender(communicatorHostname, communicatorPort);
            registryOperation.setTestNodeDescription(testNodeDescription);

            sender.handle(registryOperation);

            // Enter the main loop
            while (isRunning) {
                // Accept incoming request
                Socket connection = listener.accept();

                // And process it asynchronously
                receiver.handle(connection);

                sleep(Constant.MILLISECOND); // Wait for any updates
            }

            if (listener != null) {
                listener.close();
                p("Listener or incoming messages was successfully closed");
            }

        } catch (Exception e) {
            p("Test Automation Communicator has got an exception during its work:");
            e.printStackTrace();

            p("Got troubles while listening to incoming messages. Stop working");
            for (TestExecutor current : testExecutors) {
                stopTest(current.getTest(), true);
            }

            System.gc();
            p("Stopped working");
        }

        p("Ended work");
    }

    /**
     * Returns a static instance of itself.
     *
     * @return A static instance of itself
     */
    public static synchronized TestAutomationCommunicator getInstance() {
        return self;
    }

    /**
     * Returns a hostname of remote Test Automation Service.
     *
     * @return A hostname of remote Test Automation Service
     */
    public synchronized String getTestAutomationServiceHostname() {
        return testAutomationServiceHostname;
    }

    /**
     * Returns a port number of remote Test Automation Service.
     *
     * @return A port number of remote Test Automation Service
     */
    public synchronized int getTestAutomationServicePort() {
        return testAutomationServicePort;
    }

    /**
     * Returns a hostname of this Test Automation Communicator.
     *
     * @return A hostname of this Test Automation Communicator
     */
    public synchronized String getHostname() {
        return communicatorHostname;
    }

    /**
     * Returns a port number of this Test Automation Communicator.
     *
     * @return A port number of this Test Automation Communicator
     */
    public synchronized int getPort() {
        return communicatorPort;
    }

    /**
     * Returns test node's current description.
     *
     * @return Test node's current description
     */
    public synchronized TestNodeDescription getTestNodeDescription() {
        return testNodeDescription;
    }

    /**
     * Forwards specified message to a test executor with mentioned id.
     *
     * @param message A message to be forwarded
     * @param executorId Id of test executor
     */
    public synchronized void forward(Object message, String executorId) {

        TestExecutor testExecutor = getTestExecutor(executorId);

        if (testExecutor != null) {
            testExecutor.handle(message);
        }
    }

    /**
     * Returns a test executor with specified id or null if such executor is not existing.
     *
     * @param executorId Id of executor
     * @return A test executor with specified id or null if such executor is not existing
     */
    public synchronized TestExecutor getTestExecutor(String executorId) {

        TestExecutor result = null;

        if (executorId != null && !executorId.isEmpty()) {
            for (TestExecutor current : testExecutors) {
                if (current.getName().equals(executorId)) {
                    result = current;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Returns current list of test executors.
     *
     * @return Current list of test executors
     */
    public synchronized ConcurrentLinkedQueue<TestExecutor> getTestExecutors() {
        return testExecutors;
    }

    public ProductExplorer getProductExplorer() {
    	return productExplorer;
    }
    
    /**
     * Remember the moment of time when a new test has started.
     */
    public synchronized void newTestStarted() {
        timeWhenLastTestHasStarted = System.currentTimeMillis();
    }

    /**
     * Returns a moment of time when the last test has started.
     *
     * @return A moment of time when the last test has started
     */
    public synchronized long getTimeWhenLastTestHasStarted() {
        return timeWhenLastTestHasStarted;
    }

    /**
     * Handles a test related operation.
     *
     * @param message A test related operation
     */
    public synchronized void handleTestOperation(TestOperation message) {
        Test test = message.getTest();

        if (test != null) {
            String testRuntimeId = test.getRuntimeId();

            if (testRuntimeId != null && !testRuntimeId.isEmpty()) {
                if (message.getId() == TestOperation.Id.START) {
                    TestExecutor testExecutor = getTestExecutor(testRuntimeId);

                    if (testExecutor == null) {
                        // Test can be started
                        p("Trying to start the new test: " + test);
                        testExecutor = new TestExecutor(test, sender, fileCache, message.getSenderHostname(), message.getSenderPort());
                        testExecutors.add(testExecutor);
                        testExecutor.start();
                        p("Successfully created a new executor for the test '" + testRuntimeId + "'");
                    } else {
                        // We have already a running test with the same id
                        p("Cannot create and start executor for the test '" + testRuntimeId + "', since a test with the same id is already running");
                    }
                } else if (message.getId() == TestOperation.Id.STOP) {
                    TestExecutor testExecutor = getTestExecutor(testRuntimeId);

                    if (testExecutor != null) {
                        // We can stop the test
                        p("Trying to stop the test '" + test.getRuntimeId() + "' up from the received test operation message...");

                        testExecutor.stopWorking("Stopped up on the external request from " + message.getSenderHostname() + ":" + message.getSenderPort());
                        testExecutors.remove(testExecutor);
                        testExecutor = null;

                        p("Test executor of the test '" + testRuntimeId + "' was successfully stopped");
                        p("Finally having " + testExecutors.size() + " test executors");

                        System.gc();

                    } else {
                        // We don't have a running test with specified id
                        p("Cannot stop a test '" + testRuntimeId + "', since a test with specified id is not handled by this test node");
                    }
                } else {
                    p("Got UNSUPPORTED test operation message: " + message);
                }
            }
        }
    }

    /**
     * Stops specified test.
     *
     * @param test A test to be stopped
     * @param isTestFailed True if test execution has failed for some reason, or true otherwise
     */
    public synchronized void stopTest(Test test, boolean isTestFailed) {

        String testRuntimeId = test.getRuntimeId();

        if (isTestFailed) {
            p("Stopping executor of failed test '" + testRuntimeId + "'");
        } else {
            p("Stopping executor of successful test '" + testRuntimeId + "'");
        }

        // Get executor of the test
        TestExecutor testExecutor = getTestExecutor(testRuntimeId);

        if (testExecutor != null) {
            if (testExecutors.remove(testExecutor)) {
                testExecutor = null;

                p("Test executor of the test '" + testRuntimeId + "' was successfully stopped and removed");
            } else {
                p("Error: Couldn't stop and remove executor of the test '" + testRuntimeId + "'");
            }

            p("Finally having " + testExecutors.size() + " test executors");

            System.gc();
        }

        if (testExecutors.isEmpty()) {
            // Since this communicator is not running tests anymore, perform automatic cleanup of the workspace
            // The reason why we don't use timer for such task is quite simple: for debugging reasons
            // For example, if some test execution will be jammed, none of its artifacts will be deleted
            try {
                p("Trying to perform automatic cleanup of the workspace directory. All test artifacts older than " + Util.convert(cleanupPeriod) + " will be deleted...");
                long currentTime = System.currentTimeMillis();

                File[] workspaceDirectoryEntries = communicatorWorkspace.listFiles();

                if (workspaceDirectoryEntries != null && workspaceDirectoryEntries.length > 0) {
                    for (File workspaceDirectoryEntry : workspaceDirectoryEntries) {
                        long lastModified = workspaceDirectoryEntry.lastModified();

                        if ((currentTime - lastModified) >= cleanupPeriod) {
                            p("Workspace directory contains an entry " + workspaceDirectoryEntry.getAbsolutePath()
                                + " which was last modified at " + new Date(lastModified) + " and so, can be deleted");
                            if (deleteRecursively(workspaceDirectoryEntry)) {
                                p("Log directory entry " + workspaceDirectoryEntry.getAbsolutePath() + " was successfully deleted");
                            }
                        }
                    }
                } else {
                    p("Couldn't list entries in workspace directory. Will skip automatic cleanup");
                }
            } catch (Exception e) {
                p("Got troubles while tried to perform automatic cleanup of the workspace directory: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates a workspace (a separated directory on local file system) for specified test id.
     *
     * @param testId Id of the test to be used
     * @return File object to created test workspace directory
     */
    public synchronized File createWorkspaceForTest(String testId) {

        String testDirectory = communicatorWorkspace.getAbsolutePath() + fileSeparator + testId;
        p("Test directory will be: " + testDirectory);

        File testWorkspace = new File(testDirectory);

        if (testWorkspace.exists()) {
            // Remove old contents
            if (deleteRecursively(testWorkspace)) {
                p("Already existing test directory was successfully cleaned at " + testWorkspace.getAbsolutePath());
            }
        }

        if (!testWorkspace.exists()) {
            testWorkspace.mkdirs();

            if (testWorkspace.exists()) {
                p("Test directory successfully created at " + testWorkspace.getAbsolutePath());
            } else {
                p("Error: Cannot crate a directory for test at " + testWorkspace.getAbsolutePath());
                testWorkspace = null;
            }
        }

        return testWorkspace;
    }

    /**
     * Returns a reference to the directory containing all products configurations.
     *
     * @return A reference to the directory containing all products configurations
     */
    protected File getProductConfigurationsDirectory() {
        return productConfigurationsDirectory;
    }

    /**
     * Handles update of products currently connected to this node.
     *
     * @param message A message with a list of updated products
     */
    public synchronized void handleProductOperation(ProductOperation message) {
        if (productExplorer != null) {
            productExplorer.handle(message);
        }
    }

    /**
     * Handles a situation of permanently disconnected product.
     *
     * @param product Instance of a permanently disconnected product
     */
    public synchronized void handlePermanentlyDisconnectedProduct(Product product) {
        if (product != null) {
            // Find out who was using this product
            for (TestExecutor testExecutor : testExecutors) {
                p("Checking test executor " + testExecutor.getName());

                boolean executorUsesThisProduct = false;
                List<Product> reservedProducts = testExecutor.getReservedProducts();

                p("Test executor " + testExecutor.getName() + " has " + reservedProducts.size() + " reserved products");

                if (reservedProducts != null && !reservedProducts.isEmpty()) {
                    for (Product reservedProduct : reservedProducts) {
                        if (reservedProduct.getIMEI().equals(product.getIMEI())) {
                            p("Test executor " + testExecutor.getName() + " is currently using a disconnected product");
                            executorUsesThisProduct = true;
                            break;
                        }
                    }
                }

                if (executorUsesThisProduct) {
                    p("Notifying test executor " + testExecutor.getName() + " about a disconnected product");
                    testExecutor.handlePermanentlyDisconnectedProduct(product);
                }
            }
        }
    }

    /**
     * Handles a failed message sending.
     *
     * @param message Message which failed to be send
     */
    public synchronized void messageSendFailed(Object message) {
        // A failed message send means that remote Test Automation Service is not working anymore
        p("A connection to remote Test Automation Service at " + testAutomationServiceHostname + ":" + testAutomationServicePort + " was lost");

        // Stop all test executors
        for (TestExecutor current : testExecutors) {
            p("Stopping executor of the test '" + current.getTest().getRuntimeId() + "' due to occured network failure");
            current.messageSendFailed();
        }

        // Set all products free
        productExplorer.setAllProductsFree();

        // Try to register to the Test Automation Service once again
        RegistryOperation registryOperation = new RegistryOperation(RegistryOperation.Id.REGISTER, RegistryOperation.Remote.TEST_NODE);
        registryOperation.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
        registryOperation.setSender(communicatorHostname, communicatorPort);
        registryOperation.setTestNodeDescription(testNodeDescription);
        sender.handle(registryOperation);
    }

    /**
     * Returns available instance of sender.
     *
     * @return Available instance of sender
     */
    protected synchronized Sender getSender() {
        return sender;
    }

    /**
     * Performs all necessary cleanups to stop this Test Automation Communicator properly.
     */
    protected synchronized void shutdown() {
        p("Shutting down Test Automation Communicator...");

        // Stop all running tests
        try {
            if (testExecutors != null && !testExecutors.isEmpty()) {
                p("Trying to stop all running test executors...");
                for (TestExecutor current : testExecutors) {
                    current.stopWorking("Got a request to stop execution up on Test Automation Communicator's shutdown");
                }
            }
        } catch (Exception e) {
            p("Got troubles while tried to stop all executing tests: " + e.getClass() + " - " + e.getMessage());
            e.printStackTrace();
        }

        // Send deregistering message
        try {
            p("Trying to send a deregistration message to the Test Automation Service at " + testAutomationServiceHostname + ":" + testAutomationServicePort);

            // Send message straight over a socket, since sender and listener will be interpreted
            RegistryOperation deregistryOperation = new RegistryOperation(RegistryOperation.Id.DEREGISTER, RegistryOperation.Remote.TEST_NODE);
            deregistryOperation.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
            deregistryOperation.setSender(communicatorHostname, communicatorPort);
            deregistryOperation.setTestNodeDescription(testNodeDescription);

            Socket socket = new Socket(InetAddress.getByName(deregistryOperation.getReceiverHostname()), deregistryOperation.getReceiverPort());
            OutputStream output = socket.getOutputStream();

            // Send message
            output.write(deregistryOperation.toXML().getBytes("UTF-8"));
            output.flush();
            output.close();
            socket.close();

        } catch (Exception e) {
            p("Got troubles while tried to send a deregistration message to the Test Automation Service: " + e.getClass() + " - " + e.getMessage());
            e.printStackTrace();
        }

        // Close message sender and listener
        try {
            if (sender != null) {
                sender.shutdown();
            }

            if (listener != null) {
                p("Trying to close message listener...");
                listener.close();
                listener = null;
            }
        } catch (Exception e) {
            p("Got troubles while tried to close message listener and sender: " + e.getClass() + " - " + e.getMessage());
            e.printStackTrace();
        }

        isRunning = false;
    }

    /**
     * Returns cleanup period milliseconds.
     *
     * @return Cleanup period milliseconds
     */
    protected synchronized long getCleanupPeriod() {
        return cleanupPeriod;
    }

    /**
     * Tells whenever a workspace of the test is allowed to be deleted or not.
     *
     * @param testHasFailed Indication of a successful or failed test
     * @return True if workspace can be deleted or false otherwise
     */
    protected synchronized boolean canDeleteWorkspace(boolean testHasFailed) {
        boolean canDeleteWorkspace = true;

        // Check cleanup parameters
        if (keepWorkspacesOfAllTests) {
            canDeleteWorkspace = false;
        } else if (keepWorkspacesOfFailedTests) {
            if (testHasFailed) {
                canDeleteWorkspace = false;
            }
        }

        return canDeleteWorkspace;
    }

    /**
     * Deletes all files and directories in specified directory path.
     *
     * @param path Directory path to be deleted
     * @return True if deletion was successful or false otherwise
     */
    private synchronized boolean deleteRecursively(File path) {
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
     * Returns file separator symbol used by the hosting system.
     *
     * @return File separator symbol used by the hosting system
     */
    public String getFileSeparator() {
        return fileSeparator;
    }

    /**
     * Parses communicator's startup arguments.
     * Returns true if Test Automation Communicator may continue its work, or false otherwise.
     *
     * @param arguments Startup arguments of the Test Automation Communicator
     * @return True if Test Automation Communicator may continue its work, or false otherwise
     */
    private synchronized boolean parseArguments(String[] arguments) {

        boolean communicatorCanBeStarted = true;
        testNodeDescription = new TestNodeDescription();

        if (arguments != null && arguments.length > 0) {
            for (int i = 0; i < arguments.length; i++) {
                String parameter = arguments[i];

                if (parameter != null && !parameter.isEmpty()) {

                    // Check for the help request
                    if (parameter.indexOf("help") != -1 || parameter.indexOf("HELP") != -1) {

                        printHelp();

                        communicatorCanBeStarted = false; // Nothing else to do
                        break;

                    } else if (parameter.indexOf(Constant.TEST_AUTOMATION_SERVICE_HOSTNAME_ARGUMENT) != -1) {

                        // Parse hostname of the Test Automation Service
                        try {
                            testAutomationServiceHostname = parameter.substring(parameter.indexOf("=") + 1);

                            if (testAutomationServiceHostname == null || testAutomationServiceHostname.isEmpty()) {
                                System.out.println("Specified Test Automation Service hostname " + testAutomationServiceHostname + " is invalid. Please specify a proper value.");
                                communicatorCanBeStarted = false;
                                break;
                            }

                        } catch (Exception e) {
                            System.out.println(Constant.TEST_AUTOMATION_SERVICE_HOSTNAME_ARGUMENT + " parameter is probably incorrectly specified. Please type command \"java -jar TestAutomationCommunicator.jar -help\" for getting more information.");
                            communicatorCanBeStarted = false;
                            break;
                        }
                    } else if (parameter.indexOf(Constant.TEST_AUTOMATION_SERVICE_PORT_NUMBER_ARGUMENT) != -1) {

                        // Parse port number of the Test Automation Service
                        try {
                            testAutomationServicePort = Integer.parseInt(parameter.substring(parameter.indexOf("=") + 1));

                            if (testAutomationServicePort < 0 || testAutomationServicePort > 0xffff) {
                                System.out.println("Specified Test Automation Service port " + testAutomationServicePort + " is out of allowed range. Please specify a proper value.");
                                communicatorCanBeStarted = false;
                                break;
                            }


                        } catch (Exception e) {
                            System.out.println(Constant.TEST_AUTOMATION_SERVICE_PORT_NUMBER_ARGUMENT + " parameter is probably incorrectly specified. Please type command \"java -jar TestAutomationCommunicator.jar -help\" for getting more information.");
                            communicatorCanBeStarted = false;
                            break;
                        }
                    } else if (parameter.indexOf(Constant.TEST_AUTOMATION_COMMUNICATOR_PORT_NUMBER_ARGUMENT) != -1) {

                        // Parse listening port number for the Test Automation Communicator itself
                        try {
                            communicatorPort = Integer.parseInt(parameter.substring(parameter.indexOf("=") + 1));

                            if (communicatorPort < 0 || communicatorPort > 0xffff) {
                                System.out.println("Specified communicator port " + communicatorPort + " is out of allowed range.");
                                communicatorPort = Constant.TEST_AUTOMATION_COMMUNICATOR_DEFAULT_PORT_NUMBER;
                                System.out.println("Communicator port was changed to its default value " + communicatorPort);
                            }

                        } catch (Exception e) {
                            System.out.println(Constant.TEST_AUTOMATION_COMMUNICATOR_PORT_NUMBER_ARGUMENT + " parameter is probably incorrectly specified. Please type command \"java -jar TestAutomationCommunicator.jar -help\" for getting more information.");
                            communicatorCanBeStarted = false;
                            break;
                        }
                    } else if (parameter.indexOf(Constant.TEST_AUTOMATION_COMMUNICATOR_DESCRIPTION_ARGUMENT) != -1) {

                        // Parse description about this Test Automation Communicator
                        try {
                            String description = parameter.substring(parameter.indexOf("=") + 1);

                            if (description == null || description.isEmpty()) {
                                System.out.println("Specified description is invalid. Please specify a proper value.");
                                communicatorCanBeStarted = false;
                                break;
                            } else {
                                testNodeDescription.setDescription(description);
                            }

                        } catch (Exception e) {
                            System.out.println(Constant.TEST_AUTOMATION_COMMUNICATOR_DESCRIPTION_ARGUMENT + " parameter is probably incorrectly specified. Please type command \"java -jar TestAutomationCommunicator.jar -help\" for getting more information.");
                            communicatorCanBeStarted = false;
                            break;
                        }
                    } else if (parameter.indexOf(CLEANUP_PERIOD_IN_DAYS) != -1) {

                        // Parse number of days for a cleanup period
                        try {
                            cleanupPeriod = Integer.parseInt(parameter.substring(parameter.indexOf("=") + 1));

                            if (cleanupPeriod <= 0) {
                                System.out.println("Specified cleanup period " + cleanupPeriod + " cannot be zero or negative.");
                                cleanupPeriod = DEFAULT_CLEANUP_PERIOD_IN_DAYS * Constant.ONE_DAY;
                                System.out.println("Cleanup period will be changed to its default value: " + (Util.convert(cleanupPeriod)));
                            } else {
                                // Otherwise the value is OK, turn days into milliseconds
                                cleanupPeriod = cleanupPeriod * Constant.ONE_DAY;
                            }
                        } catch (Exception e) {
                            System.out.println(CLEANUP_PERIOD_IN_DAYS + " parameter is probably incorrectly specified. Please type command \"java -jar TestAutomationCommunicator.jar -help\" for getting more information.");
                            communicatorCanBeStarted = false;
                            break;
                        }
                    } else {
                        if (parameter.equalsIgnoreCase(KEEP_WORKSPACES_OF_ALL_TESTS)) {
                            System.out.println("Will keep workspaces of all tests");
                            keepWorkspacesOfAllTests = true;
                        } else if (parameter.equalsIgnoreCase(KEEP_WORKSPACES_OF_FAILED_TESTS)) {
                            System.out.println("Will keep workspaces of failed tests only");
                            keepWorkspacesOfFailedTests = true;
                        } else {
                            System.out.println("Specified parameter " + parameter + " is not supported!");
                            communicatorCanBeStarted = false;
                            printHelp();
                            break;
                        }
                    }
                }
            }
        } else { // Otherwise just print help and exit
            printHelp();
            communicatorCanBeStarted = false; // Nothing else to do
        }

        if (communicatorCanBeStarted) {
            System.out.println("Will preserve test artifacts and log files for at least " + (Util.convert(cleanupPeriod)));

            // Get the hostname of test node
            if (communicatorHostname == null || communicatorHostname.isEmpty()) {
                System.out.println("Trying to get the hostname of Test Automation Communicator");

                // Some Nokia networks cannot resolve correctly IP addresses of machines from their short hostnames
                // In such cases if Test Automation Communicator will run on Windows,
                // it is safe to extract a fully qualified hostname of the machine from Java
                // On Linux machines fully qualified hostnames should be extracted
                // from their "HOST" or "HOSTNAME" environment variables only

                String osName = System.getProperty("os.name");

                if (osName != null && !osName.isEmpty()) {
                    osName = osName.toLowerCase();

                    if (osName.indexOf("win") != -1) {
                        // Communicator is started on Windows
                        try {
                            // Ask Java about a fully qualified hostname
                            //InetAddress localhost = InetAddress.getLocalHost();
                            communicatorHostname = Util.getValidHostIp();//localhost.getCanonicalHostName();
                        } catch (Exception e) {
                            communicatorHostname = null;
                        }
                    }
                }

                if (communicatorHostname == null || communicatorHostname.isEmpty()) {
                    // Try to extract hostname from the system variables
                    communicatorHostname = (String) System.getenv().get("COMPUTERNAME"); // By default we are running on Windows

                    if (communicatorHostname == null || communicatorHostname.isEmpty()) {
                        communicatorHostname = (String) System.getenv().get("HOST"); // Otherwise we are probably running on Linux
                    }

                    if (communicatorHostname == null || communicatorHostname.isEmpty()) {
                        communicatorHostname = (String) System.getenv().get("HOSTNAME"); // Try another possible environment variable on Linux
                    }

                    if (communicatorHostname == null || communicatorHostname.isEmpty()) {
                        System.out.println("Warning: Couldn't extract a hostname for the Test Automation Communicator out of environment. Please specify one either in HOST, HOSTNAME or in COMPUTERNAME system variables.");
                        communicatorCanBeStarted = false;
                    }
                }
            }
        }

        // Update test node description
        if (communicatorCanBeStarted) {
            testNodeDescription.setHostname(communicatorHostname);
            testNodeDescription.setPort(communicatorPort);
            testNodeDescription.setTestAutomationSoftwareVersion(Constant.TEST_AUTOMATION_RELEASE_VERSION);
        }

        return communicatorCanBeStarted;
    }

    /**
     * Prints help message to the console.
     */
    private static void printHelp() {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\nTest Automation Communicator v" + Constant.TEST_AUTOMATION_RELEASE_VERSION + "\n\n");
        stringBuilder.append("Default usage:\n\n");
        stringBuilder.append(" java -jar TestAutomationCommunicator.jar "
                             + Constant.TEST_AUTOMATION_SERVICE_HOSTNAME_ARGUMENT + "=jamesbond007.europe.nokia.com "
                             + Constant.TEST_AUTOMATION_SERVICE_PORT_NUMBER_ARGUMENT + "=12345\n\n");
        stringBuilder.append("Here you will start and connect you Communicator to the Test Automation Service running at address jamesbond007.europe.nokia.com:12345\n\n");
        stringBuilder.append("The other supported parameters:\n\n");
        stringBuilder.append(" " + Constant.TEST_AUTOMATION_SERVICE_HOSTNAME_ARGUMENT + "=<host.name> - Hostname of the machine where Test Automation Service is running\n\n");
        stringBuilder.append(" " + Constant.TEST_AUTOMATION_SERVICE_PORT_NUMBER_ARGUMENT + "=<number> - Port number that Test Automation Service is using for incoming communications\n\n");
        stringBuilder.append(" " + Constant.TEST_AUTOMATION_COMMUNICATOR_PORT_NUMBER_ARGUMENT + "=<number> - Specify port number to be used by Communicator, if the default port 27182 is not available\n\n");
        stringBuilder.append(" " + Constant.TEST_AUTOMATION_COMMUNICATOR_DESCRIPTION_ARGUMENT + "=\"<string>\" - A short description about this Communicator instance\n\n");

        stringBuilder.append(" " + KEEP_WORKSPACES_OF_FAILED_TESTS + " - Will force Communicator to preserve workspaces of failed tests only\n\n");
        stringBuilder.append(" " + KEEP_WORKSPACES_OF_ALL_TESTS + " - Will force Communicator to preserve workspaces of all tests ever issued on this test node\n\n");
        stringBuilder.append(" " + CLEANUP_PERIOD_IN_DAYS + "=<number of days> - Specify a number of days that test artifacts and log files will be preserved by this Communicator\n\n");
        stringBuilder.append("If no cleanup flags are specified, the test workspaces will be always deleted.\n\n");
        stringBuilder.append("Please remember that Test Automation Communicator will always clean its workspace up on restart.\n");

        System.out.println(stringBuilder.toString());
    }

    /**
     * Print debug or information messages to console.
     *
     * @param text Text to be printed out
     */
    private static void p(String text) {
        logger.log(Level.ALL, "COM: " + text);
    }

    /**
     * Starts Test Automation Communicator as a standalone application.
     *
     * @param arguments All given startup arguments
     */
    public static void main(String[] arguments) {
        TestAutomationCommunicator communicator = new TestAutomationCommunicator(arguments);
    }
}
