package com.nokia.ci.tas.communicator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.FileDescription;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.Util;

import com.nokia.ci.tas.commons.message.FileOperation;
import com.nokia.ci.tas.commons.message.TestOperation;
import com.nokia.ci.tas.commons.message.TextMessage;

/**
 * Executor of a single Test issued by the Testing Automation Service on a testing machine.
 */
public class TestExecutor extends Thread {

    /**
     * Supported operations.
     */
    private enum Operation {
        INIT_TEST, // Create local workspace and copy test artifacts
        EXECUTE_TEST, // Launch test executor script
        FINALIZE_TEST // Send test results and delete local workspace
    };

    /**
     * A test to be perfomed.
     */
    private Test test;

    /**
     * Instance of the message sending facilities.
     */
    private Sender sender;

    /**
     * Path to test's workspace.
     */
    private File testWorkspace;

    /**
     * A pool of received messages related to test handling.
     */
    private ConcurrentLinkedQueue<Object> messagePool;

    /**
     * Variable that keeps test executor running.
     */
    private boolean isRunning = true;

    /**
     * Indication of a started and running test execution.
     */
    private boolean isTestRunning = false;

    /**
     * Indication of whenever the test has failed.
     */
    private boolean isTestFailed = false;

    /**
     * Result code that test execution script has returned.
     */
    private int testExecutionResultCode = -1;

    /**
     * Holder of textual explanation why test has failed.
     */
    private String reasonOfTestFailure;

    /**
     * Keeps a list of file names which should be received by test handler.
     */
    private ConcurrentLinkedQueue<String> listOfFilesToBeReceived;

    /**
     * Keeps a list of file names which should be send by test handler.
     */
    private ConcurrentLinkedQueue<String> listOfFilesToBeSend;

    /**
     * Instance of the Test Automation Communicator.
     */
    private TestAutomationCommunicator testAutomationCommunicator;

    /**
     * Hostname of remote client for which a test will be executed.
     */
    private String remoteClientHostname;

    /**
     * Port number of remote client for which a test will be executed.
     */
    private int remoteClientPort;

    /**
     * Hostname of the Test Automation Service for which this communicator is working.
     */
    private String testAutomationServiceHostname;

    /**
     * Port number of the Test Automation Service for which this communicator is working.
     */
    private int testAutomationServicePort;

    /**
     * Hostname of the communicator itself.
     */
    private String testAutomationCommunicatorHostname;

    /**
     * Port number of the communicator itself.
     */
    private int testAutomationCommunicatorPort;

    /**
     * Moment of time when the last message was send.
     */
    private long timeOfLastSuccessfulFileTransfer = 0L;

    /**
     * Tells what test executor is doing currently
     */
    private Operation currentOperation;

    /**
     * A pool containing all the operations that test executor should handle.
     */
    private ConcurrentLinkedQueue<Operation> operations;

    /**
     * Instance of test process which does the actual testing.
     */
    private Process testProcess;

    /**
     * A reference to the directory where all product configurations are stored.
     */
    private File productConfigurationsDirectory;

    /**
     * Reference to the log file associated with this test executor.
     */
    private File logFile;

    /**
     * Log writer for the executor.
     */
    private PrintWriter logWriter;

    /**
     * Date format used in logging.
     */
    private SimpleDateFormat dateFormat;

    /**
     * Dynamic buffer for log messages.
     */
    private StringBuffer logBuffer;

    /**
     * Timer used for various tasks.
     */
    private Timer timer;

    /**
     * Timer task for flushing log buffer.
     */
    private TimerTask flushLogBuffer;

    /**
     * Indicator of a failed file receive.
     */
    private boolean hasGotFailureInFileReceive = false;

    /**
     * Indicator of a failed file send.
     */
    private boolean hasGotFailureInFileSend = false;

    /**
     * Instance of file caching utility.
     */
    private FileCache fileCache;

    /**
     * File separator used by hosting system.
     */
    private String fileSeparator;

    /**
     * Timer task for checking existency of the external test execution process.
     */
    private TimerTask checkExistencyOfTestExecutionProcess;

    /**
     * Variable for indicating if external test execution process is alive or not.
     */
    private boolean isTestExecutionProcessAlive = false;

    /**
     * Reader of outputs from the external test execution process.
     */
    private BufferedReader testProcessOutputReader = null;

    /**
     * Instance of the Test Automation Communicator's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationCommunicator.GLOBAL_LOGGER_NAME);

    /**
     * Constructor.
     *
     * @param test A test to be performed
     * @param sender Sender of all messages
     * @param fileCache Instance of a local file cache
     * @param remoteClientHostname Hostname of the remote client responsible for the test
     * @param remoteClientPort Port number of the remote client responsible for the test
     */
    //public TestExecutor(Test test, Sender sender, FileCache fileCache, String remoteClientHostname, int remoteClientPort) {
    public TestExecutor(Test test, Sender sender, FileCache fileCache, String remoteClientHostname, int remoteClientPort) {
        super(test.getRuntimeId()); // Executor's id is the same as test's runtime id

        this.test = test;
        this.sender = sender;
        this.fileCache = fileCache;
        this.remoteClientHostname = remoteClientHostname;
        this.remoteClientPort = remoteClientPort;

        messagePool = new ConcurrentLinkedQueue();

        listOfFilesToBeReceived = new ConcurrentLinkedQueue();
        listOfFilesToBeSend = new ConcurrentLinkedQueue();

        testAutomationCommunicator = TestAutomationCommunicator.getInstance();
        testAutomationServiceHostname = testAutomationCommunicator.getTestAutomationServiceHostname();
        testAutomationServicePort = testAutomationCommunicator.getTestAutomationServicePort();

        testAutomationCommunicatorHostname = testAutomationCommunicator.getHostname();
        testAutomationCommunicatorPort = testAutomationCommunicator.getPort();

        productConfigurationsDirectory = testAutomationCommunicator.getProductConfigurationsDirectory();

        fileSeparator = testAutomationCommunicator.getFileSeparator();

        p("Successfully created for test '" + test.getRuntimeId() + "' and client " + remoteClientHostname + ":" + remoteClientPort);

        operations = new ConcurrentLinkedQueue();

        logBuffer = new StringBuffer("");

        timer = new Timer();

        // Create log flushing task
        flushLogBuffer = new TimerTask() {
            @Override
            public void run() {
                flushLogBuffer();
            }
        };

        // Flush log buffer each 5 seconds
        timer.scheduleAtFixedRate(flushLogBuffer, 0L, Constant.FIVE_SECONDS);

        // Basic workflow of the test
        operations.add(Operation.INIT_TEST);
        operations.add(Operation.EXECUTE_TEST);
        operations.add(Operation.FINALIZE_TEST);

        // Always put executor's thread into minimal priority
        setPriority(MIN_PRIORITY);
    }

    /**
     * Test executor's main routine.
     */
    @Override
    public void run() {
        p("Started working");

        // Remember the moment when executor started working
        long testExecutionStartedAt = System.currentTimeMillis();

        while (isRunning) {
            try {
                if (!operations.isEmpty()) { // If executor has got something to do
                    currentOperation = operations.poll(); // Get the next operation to do

                    // Handle it
                    if (currentOperation == Operation.INIT_TEST) {
                        p("Initializing the test");

                        // Create a workspace for the test
                        try {
                            testWorkspace = testAutomationCommunicator.createWorkspaceForTest(test.getRuntimeId());

                            if (testWorkspace != null) {
                                // Create log file and log writer

                                // The reason why we don't use the official Java Logger or Log4j is fairly simple:
                                // The log file will be deleted as soon as the test is successfully executed
                                // Since we could have many test executors running in parallel,
                                // we should simply let only the local thread to update its log file
                                // without interferencing any other executors through static Loggers

                                try {
                                    logFile = new File(testWorkspace.getAbsolutePath() + fileSeparator + test.getRuntimeId() + ".log");

                                    if (!logFile.exists()) {
                                        logFile.createNewFile();
                                    }

                                    logWriter = new PrintWriter(logFile);

                                    dateFormat = new SimpleDateFormat(Constant.TIMESTAMP_FORMAT);

                                } catch (Exception e) {
                                    p("Got troubles with creation logger for the test: " + e.getClass() + " " + e.getMessage());
                                }

                                p("Workspace for the test " + test.getRuntimeId() +
                                    " was successfully created at " + testWorkspace.getAbsolutePath()
                                    + " on test node " + testAutomationCommunicatorHostname + ":" + testAutomationCommunicatorPort);
                                isRunning = true;
                                isTestFailed = false;
                            } else {
                                p("Error: Couldn't create a workspace for the test '" + test.getRuntimeId() + "'"
                                    + " on test node " + testAutomationCommunicatorHostname + ":" + testAutomationCommunicatorPort);

                                isRunning = false;
                                isTestFailed = true;
                                reasonOfTestFailure = "Couldn't create a workspace for the test '" + test.getRuntimeId() + "'";
                            }
                        } catch (Exception e) {
                            p("Got troubles while tried to create a workspace for the test: "
                                    + e.getClass() + " " + e.getMessage());
                            isRunning = false;
                            isTestFailed = true;
                            reasonOfTestFailure = "Couldn't create a workspace for the test '" + test.getRuntimeId() + "'";
                        }

                        // Request copies of all test artifacts
                        if (!isTestFailed) {
                            p("Trying to copy all required test artifacts from remote client " + remoteClientHostname + ":" + remoteClientPort
                                + " to the test node " + testAutomationCommunicatorHostname + ":" + testAutomationCommunicatorPort
                                + " and into test workspace at " + testWorkspace.getAbsolutePath());

                            // Issue a transfer request for each of required files and ensure that such transfers were tried for at least Constant.NUMBER_OF_RETRIES
                            List<String> fileNamesToReceive = test.getArtifacts();

                            for (String fileName : fileNamesToReceive) {
                                // Create a file transfer request
                                FileDescription fileDescription = new FileDescription();
                                fileDescription.setFileName(fileName);
                                fileDescription.setFileSize(FileDescription.UNKNOWN_FILE_SIZE);

                                FileOperation fileTransferRequest = new FileOperation(FileOperation.Id.GET, test, fileDescription);
                                fileTransferRequest.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);
                                fileTransferRequest.setReceiver(remoteClientHostname, remoteClientPort);

                                // Remember what file we should now receive
                                listOfFilesToBeReceived.add(fileName);

                                FileCacheEntry fileCacheEntry = new FileCacheEntry(fileName, test, testWorkspace.getAbsolutePath() + fileSeparator + fileName, fileTransferRequest);

                                fileCache.add(fileCacheEntry);

                                // Sender will check later file cache for all file transfer requests

                                p("Issued a request for file '" + fileName + "'");
                            }

                            // Now simply wait until all requested files will be delivered
                            if (!listOfFilesToBeReceived.isEmpty()) {

                                long timeOfLastTestListenerNotification = 0L;

                                while (isRunning) {
                                    try {
                                        if (listOfFilesToBeReceived.isEmpty()) {
                                            if (!isTestFailed) {
                                                p("All requested artifacts were successfully received. Launching test execution");
                                            }
                                            break;
                                        } else {
                                            if ((System.currentTimeMillis() - timeOfLastTestListenerNotification) > Constant.FIVE_MINUTES) {

                                                // Tell test listener what files this executor is waiting for
                                                StringBuffer filesToBeDelivered = new StringBuffer("");

                                                for (String fileName : listOfFilesToBeReceived) {
                                                    filesToBeDelivered.append(fileName + " ");
                                                }

                                                p("Waiting for requested files on test node "
                                                    + testAutomationCommunicatorHostname + ":" + testAutomationCommunicatorPort
                                                    + ": " + filesToBeDelivered.toString());

                                                timeOfLastTestListenerNotification = System.currentTimeMillis();
                                            }
                                        }

                                        sleep(Constant.ONE_SECOND);

                                    } catch (Exception e) {
                                        isRunning = false;
                                        isTestFailed = true;
                                        reasonOfTestFailure = "Got troubles while waited for test artifacts: " + e.getClass() + " " + e.getMessage();
                                        p("Stopping test due to " + reasonOfTestFailure);
                                    }
                                }
                            }

                            if (isTestFailed) {
                                // Don't request artifacts anymore
                                p("Stopping test due to occured failure in test artifacts delivery");
                                break;
                            } else {
                                p("All test artifacts are finally delivered. Launching test execution...");
                            }
                        }

                    } else if (currentOperation == Operation.EXECUTE_TEST) {

                        p("Executing the test '" + test.getRuntimeId() + "'");

                        // Notify Test Automation Service about the started test
                        test.setStatus(Test.Status.STARTED, "");

                        TestOperation testStartedMessage = new TestOperation(TestOperation.Id.UPDATE, test);
                        testStartedMessage.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
                        testStartedMessage.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);

                        sender.handle(testStartedMessage);

                        // In order to make product flashings safe, we need to keep a short pause between parallel tests
                        long timeIntervalBetweenThisAndLastTest = System.currentTimeMillis() - testAutomationCommunicator.getTimeWhenLastTestHasStarted();

                        if (timeIntervalBetweenThisAndLastTest < Constant.FIFTEEN_SECONDS) {
                            p("A time interval between this and the last time is less than 15 seconds");
                            p("Will wait for about "
                                    + ((Constant.FIFTEEN_SECONDS - timeIntervalBetweenThisAndLastTest) / 1000L)
                                    + " seconds before launching the test");

                            try {
                                sleep(Constant.FIFTEEN_SECONDS - timeIntervalBetweenThisAndLastTest);
                            } catch (Exception e) {
                                // Ignore
                            }
                        }

                        p("Launching executor script for test '" + test.getRuntimeId() + "'");

                        // Create JSON files for all products reserved for this test
                        // If there is only one product reserved, create a "product.json" file
                        // Otherwise create a "product.json" file for the main product
                        // and "remoteProduct.json" and "referenceProduct.json" files
                        // for the remote and reference products respectively
                        List<Product> reservedProducts = test.getReservedProducts();

                        if (reservedProducts != null && !reservedProducts.isEmpty()) {
                            // Create "Main.json" file for the main product
                            // and also "Remote.json" and "Reference.json" files for the remote and reference products respectively
                            for (Product reservedProduct : reservedProducts) {
                                try {
                                    if (reservedProduct != null) {
                                        String fileName = "Main.json";

                                        if (reservedProduct.getRole() == Product.Role.MAIN) {
                                            // A product in main role should always get the filename "Main.json"
                                            fileName = "Main.json";
                                        } else if (reservedProduct.getRole() == Product.Role.REMOTE) {
                                            // A product in remove role should always get the filename "Remote.json"
                                            fileName = "Remote.json";
                                        } else if (reservedProduct.getRole() == Product.Role.REFERENCE) {
                                            // A product in reference role should always get the filename "Reference.json"
                                            fileName = "Reference.json";
                                        }

                                        String productJSONDescription = reservedProduct.toJSON();

                                        FileOutputStream fileOutputStream = new FileOutputStream(testWorkspace.getAbsolutePath() + fileSeparator + fileName, false);
                                        fileOutputStream.write(productJSONDescription.getBytes("UTF-8"));
                                        fileOutputStream.flush();
                                        fileOutputStream.close();

                                        p("Reserved product's JSON description was successfully stored in the file "
                                                + testWorkspace.getAbsolutePath() + fileSeparator + fileName);
                                    }
                                } catch (Exception e) {
                                    // Don't stop test
                                    p("Couldn't create JSON description files for a product with IMEI " + reservedProduct.getIMEI()
                                            + " and of type " + reservedProduct.getRMCode()
                                            + " because of " + e.getClass() + " - " + e.getMessage());
                                }
                            }

                            // Also create now deprecated "product.json" file for the main product
                            // and deprecated "<role>Product.json" files for remote and reference products respectively
                            // Such files generation is now deprecated and will be removed in upcoming versions of Test Automation Communicator
                            for (Product reservedProduct : reservedProducts) {
                                try {
                                    if (reservedProduct != null) {
                                        String fileName = "Product.json";

                                        if (reservedProduct.getRole() == Product.Role.MAIN) {
                                            // A product in main role should always get the filename "product.json"
                                            fileName = "product.json";
                                        } else if (reservedProduct.getRole() == Product.Role.REMOTE) {
                                            // A product in remove role should always get the filename "remoteProduct.json"
                                            fileName = Product.ROLE_REMOTE + fileName;
                                        } else if (reservedProduct.getRole() == Product.Role.REFERENCE) {
                                            // A product in reference role should always get the filename "referenceProduct.json"
                                            fileName = Product.ROLE_REFERENCE + fileName;
                                        }

                                        String productJSONDescription = reservedProduct.toDeprecatedJSON();

                                        FileOutputStream fileOutputStream = new FileOutputStream(testWorkspace.getAbsolutePath() + fileSeparator + fileName, false);
                                        fileOutputStream.write(productJSONDescription.getBytes("UTF-8"));
                                        fileOutputStream.flush();
                                        fileOutputStream.close();

                                        p("Reserved product's JSON description was successfully stored in the file "
                                                + testWorkspace.getAbsolutePath() + fileSeparator + fileName);
                                    }
                                } catch (Exception e) {
                                    // Don't stop test
                                    p("Couldn't create JSON description files for a product with IMEI " + reservedProduct.getIMEI()
                                            + " and of type " + reservedProduct.getRMCode()
                                            + " because of " + e.getClass() + " - " + e.getMessage());
                                }
                            }
                        }

                        // Tests are performed with specified command and parameters
                        List<String> executorArguments = new ArrayList<String>(0);

                        // Always nofity communicator about the moment a new test is started
                        testAutomationCommunicator.newTestStarted();

                        try {
                            //List<String> executionList = new ArrayList<String>(0);
                        	StringBuffer executionList = new StringBuffer();

                            //executionList.add(test.getExecutorApplication());
                        	executionList.append(test.getExecutorApplication()).append( " " );
                            //executionList.add(testWorkspace.getAbsolutePath() + fileSeparator + test.getExecutorScript());
                        	if((test.getExecutorApplication()==null||test.getExecutorApplication().trim().isEmpty()) && !Util.isWindows()) {
                        		executionList.append( "chmod +x \"" ).append( testWorkspace.getAbsolutePath() + fileSeparator + test.getExecutorScript() ).append( "\" && " );
                        	}
                        	executionList.append( "\"" ).append(testWorkspace.getAbsolutePath() + fileSeparator + test.getExecutorScript()).append( "\" " );

                        	for (Product reservedProduct : reservedProducts) {
	                            StringBuilder productArguments = new StringBuilder("");
	
	                            // The role is always at the first place
	                            /*if (reservedProduct.getRole() == Product.Role.MAIN) {
	                                productArguments.append(Product.ROLE_MAIN);
	                            } else if (reservedProduct.getRole() == Product.Role.REMOTE) {
	                                productArguments.append(Product.ROLE_REMOTE);
	                            } else if (reservedProduct.getRole() == Product.Role.REFERENCE) {
	                                productArguments.append(Product.ROLE_REFERENCE);
	                            }*/
	
	                            // A path to product's configuration file is following the role
	                            //productArguments.append(Constant.PRODUCT_PARAMETERS_SEPARATOR_FOR_EXECUTOR_SCRIPT);
	                            //productArguments.append(productConfigurationsDirectory.getAbsolutePath() + fileSeparator + reservedProduct.getIMEI() + ".xml");
	
	                            // Add product parameters into the list of arguments
	                            executorArguments.add(reservedProduct.getSn());
                            }
                        	
                            for (String currentArgument : executorArguments) {
                                // Embrace each argument with "" marks, since they might contain white spaces
                                //executionList.add("\"" + currentArgument + "\"");
                                executionList.append(" \"" + currentArgument + "\" ");
                            }
                            
                            // Try to backup startup parameters in the /restart_test.bat file for possible re-launches of test in the future
                            /*try {
                                File backupLauncherFile = new File(testWorkspace.getAbsolutePath() + fileSeparator + "restart_test.bat");

                                if (!backupLauncherFile.exists()) {
                                    // Continue only when such file is not existing
                                    if (backupLauncherFile.createNewFile()) {
                                        PrintWriter printWriter = new PrintWriter(new FileWriter(backupLauncherFile));
                                        StringBuilder outputs = new StringBuilder("");

                                        // Startup parameters will be "called" from the generated .bat file
                                        outputs.append("call");
                                        outputs.append( " " ).append( executionList.toString() );
                                        

                                        printWriter.write(outputs.toString());
                                        printWriter.flush();

                                        p("Test's startup parameters were successfully backuped in the file " + backupLauncherFile.getAbsolutePath());
                                    }
                                }
                            } catch (Exception e) {
                                // Don't stop test
                                p("Couldn't backup test's startup parameters because of " + e.getClass() + " - " + e.getMessage());
                            }*/

                            p("Executing command list:");
                            /*for (String current : executionList) {
                                p("\t" + current);
                            }*/
                            p(executionList.toString());

                            // Create a process builder for the test execution script
                            //ProcessBuilder processBuilder = new ProcessBuilder(executionList);
                            
                            // Set a working directory for this process
                            File processWorkingDirectory = new File(testWorkspace.getAbsolutePath());
                            //processBuilder.directory(processWorkingDirectory);

                            //p("Process run directory will be: " + processBuilder.directory().getAbsolutePath());

                            // Redirect errors to the standard output stream
                            //processBuilder.redirectErrorStream(true);*/
                            
                            p("Launching process...");

                            isTestFailed = false;
                            isTestRunning = true;

                            // Launch process
                            //testProcess = processBuilder.start();
                            testProcess = Util.exec( executionList.toString(), processWorkingDirectory, test.getExecutorEnvparams() );

                            p("Process starts executing...");
                            p("Launching a task for checking existency of the external test execution process...");

                            isTestExecutionProcessAlive = true; // Assuming so

                            // Create external test execution checking task
                            checkExistencyOfTestExecutionProcess = new TimerTask() {
                                @Override
                                public void run() {
                                    isTestExecutionProcessAlive();
                                }
                            };

                            // Activate checks of external test process existency each 5 minutes (but postpond the first check by 5 minutes as well)
                            timer.scheduleAtFixedRate(checkExistencyOfTestExecutionProcess, Constant.FIVE_MINUTES, Constant.FIVE_MINUTES);

                            // Read outputs and print them out
                            InputStream inputStream = testProcess.getInputStream();
                            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                            testProcessOutputReader = new BufferedReader(inputStreamReader);

                            String processOutputLine = null;

                            if (testProcessOutputReader != null) {
                                try {
                                    while (isTestExecutionProcessAlive) {
                                        processOutputLine = testProcessOutputReader.readLine();

                                        if (processOutputLine != null) {
                                            if (!processOutputLine.isEmpty()) {
                                                p(processOutputLine);
                                            }
                                        } else {
                                            break;
                                        }

                                        // Always go to sleep to prevent any possible deadlocks here
                                        sleep(Constant.MILLISECOND);
                                    }
                                } catch (Exception e) {
                                    // Ignore
                                }
                            }

                            if (isTestRunning) {
                                p("Checking exit code from the external test execution process...");

                                // Get the result code of test process
                                testExecutionResultCode = testProcess.exitValue();

                                if (testExecutionResultCode == 0)  {
                                    p("Process has successfully finished its work");
                                } else {
                                    isTestFailed = true;
                                    reasonOfTestFailure = "Test execution process has finished its work with exit code " + testExecutionResultCode;
                                    p(reasonOfTestFailure);
                                }
                            } else {
                                p("Skipping checks of exit code from the external test execution process...");
                            }

                        } catch (Exception e) {
                            p("Got troubles while tried to execute test script: " + e.getClass() + " " + e.getMessage());
                        }

                        isTestRunning = false; // Test is over after its execution

                        flushLogBuffer();

                        // Remove all unhandled operations
                        operations.clear();
                        // And proceed only to test finalization
                        operations.add(Operation.FINALIZE_TEST);

                    } else if (currentOperation == Operation.FINALIZE_TEST) {

                        p("Finalizing the test");

                        // Always try to send an archive with results
                        p("Trying to send a file " + test.getResultsFilename() + " with test results...");

                        // Check if test results file is existing and could be send
                        String testResultsFileName = test.getResultsFilename();
                        File testResultsFile = null;
                        boolean testFileCanBeSend = false;

                        try {
                            testResultsFile = new File(testWorkspace.getAbsolutePath() + fileSeparator + testResultsFileName);

                            if (testResultsFile.exists() && testResultsFile.canRead()) {
                                p("Test results file exists at " + testResultsFile.getAbsolutePath() + " and can be send");
                                testFileCanBeSend = true;
                            }

                            // If test was splitted, rename results file according to sub-id
                            // For example, rename "results.zip" into "results_7.zip" if test's sub-id was "_7"
                            if (!test.getSubId().isEmpty()) {
                                String renamedTestResultsFileName = testResultsFileName;

                                if (testResultsFileName.contains(".")) {
                                    // If test results filename contains extension, add sub id just in front of it
                                    renamedTestResultsFileName = testResultsFileName.substring(0, testResultsFileName.lastIndexOf("."));
                                    renamedTestResultsFileName += test.getSubId();
                                    renamedTestResultsFileName += testResultsFileName.substring(testResultsFileName.lastIndexOf("."));
                                } else {
                                    // Else add sub id to the end of filename
                                    renamedTestResultsFileName = testResultsFileName + test.getId();
                                }

                                File renamedTestResultsFile = new File(testWorkspace.getAbsolutePath() + fileSeparator + renamedTestResultsFileName);

                                p("Trying to rename test results file from " + testResultsFile.getAbsolutePath() + " to " + renamedTestResultsFile.getAbsolutePath());

                                if (testResultsFile.renameTo(renamedTestResultsFile)) {
                                    testResultsFileName = renamedTestResultsFileName;
                                    testResultsFile = renamedTestResultsFile;
                                    p("Successfully renamed test results file to " + testResultsFile.getAbsolutePath());
                                }
                            }

                            //DEBUG:
                            // Add test's name to the test results's filename:
                            /*
                                String renamedTestResultsFileName = test.getId() + "-" + testResultsFileName;
                                File renamedTestResultsFile = new File(testWorkspace.getAbsolutePath() + fileSeparator + renamedTestResultsFileName);

                                p("DEBUG: Trying to rename test results file from " + testResultsFile.getAbsolutePath() + " to " + renamedTestResultsFile.getAbsolutePath());

                                if (testResultsFile.renameTo(renamedTestResultsFile)) {
                                    testResultsFileName = renamedTestResultsFileName;
                                    testResultsFile = renamedTestResultsFile;
                                    p("DEBUG: Successfully renamed test results file to " + testResultsFile.getAbsolutePath());
                                }
                            */
                            //END DEBUG

                        } catch (Exception e) {
                            p("Got troubles while tried to send test results file " + testResultsFileName
                              + " of the test '" + test.getRuntimeId() + "': " + e.getClass() + " " + e.getMessage());
                        }

                        // Send test results file back to remote client
                        if (testFileCanBeSend) {
                            for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                                p("Trying to send a test results file " + testResultsFileName);
                                p("Issuing a file transfer for file " + testResultsFileName);

                                hasGotFailureInFileSend = false;

                                FileDescription fileDescription = new FileDescription();
                                fileDescription.setFileName(testResultsFileName);
                                fileDescription.setFileSize(testResultsFile.length());

                                FileOperation fileTransfer = new FileOperation(FileOperation.Id.PUT, test, fileDescription);
                                fileTransfer.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);
                                fileTransfer.setReceiver(remoteClientHostname, remoteClientPort);

                                // Remember what file we should now transfer
                                listOfFilesToBeSend.clear();
                                listOfFilesToBeSend.add(testResultsFileName);

                                // Remember the moment when we've tried to send a file
                                timeOfLastSuccessfulFileTransfer = System.currentTimeMillis();
                                long timeOfLastNotification = System.currentTimeMillis();

                                // Send file transfer message
                                sender.handle(fileTransfer);

                                // Start waiting for files
                                while (!listOfFilesToBeSend.isEmpty()) {
                                    try {
                                        if (listOfFilesToBeSend.isEmpty()) {
                                            if (!hasGotFailureInFileSend) {
                                                p("Has successfully send a test results file " + testResultsFileName + " to remote client at " + remoteClientHostname + ":" + remoteClientPort);
                                            } else {
                                                p("Has failed to send a test results file " + testResultsFileName + " to remote client at " + remoteClientHostname + ":" + remoteClientPort);
                                            }
                                            break;
                                        } else {
                                            if ((System.currentTimeMillis() - timeOfLastNotification) > Constant.ONE_MINUTE) {
                                                for (String fileName : listOfFilesToBeSend) {
                                                    p("Trying to send a file " + fileName);
                                                }
                                                timeOfLastNotification = System.currentTimeMillis();
                                            }

                                            if ((System.currentTimeMillis() - timeOfLastSuccessfulFileTransfer) > Constant.FIFTEEN_MINUTES) {
                                                isTestFailed = true;
                                                reasonOfTestFailure = "Couldn't send test results file " + testResultsFileName + " to remote client at "
                                                    + remoteClientHostname + ":" + remoteClientPort + " for more than " + Util.convert(Constant.FIFTEEN_MINUTES);
                                                p("Stopping test due to " + reasonOfTestFailure);
                                                listOfFilesToBeSend.clear();
                                                break;
                                            }
                                        }

                                        sleep(Constant.DECISECOND);

                                    } catch (Exception e) {
                                        isTestFailed = true;
                                        reasonOfTestFailure = "Got troubles while tried to send test results file " + testResultsFileName
                                            + " to remote client at " + remoteClientHostname + ":" + remoteClientPort + ": " + e.getClass() + " " + e.getMessage();
                                        p("Stopping test due to " + reasonOfTestFailure);
                                    }
                                }

                                if (isTestFailed) {
                                    break; // Stop any other attempts
                                } else {
                                    if (!hasGotFailureInFileSend) {
                                        // Specified file was successfully send
                                        break; // Stop any other tries
                                    } else {
                                        p("Got a failure on attempt #" + (i + 1) + " to send file " + fileDescription.getFileName());
                                    }
                                }
                            }
                        }

                        // Clean up test workspace
                        if (testAutomationCommunicator.canDeleteWorkspace(isTestFailed)) {
                            p("Trying to delete test's workspace...");

                            // Close logger for this test executor
                            if (logWriter != null) {
                                logWriter.close();
                            }

                            if (deleteRecursively(testWorkspace)) {
                                p("Workspace of the test '" + test.getRuntimeId() + "' was successfully deleted");
                            }
                        } else {
                            p("Workspace deletion is not allowed");

                            if (isTestFailed) {
                                // Create a special file for indicating that test has failed
                                try {
                                    File failureFile = new File(testWorkspace.getAbsolutePath() + fileSeparator + "TEST_HAS_FAILED.txt");
                                    failureFile.createNewFile();

                                    // Write the reason of failure into this file
                                    if (reasonOfTestFailure != null) {
                                        FileOutputStream fileOutputStream = new FileOutputStream(failureFile);
                                        fileOutputStream.write(reasonOfTestFailure.getBytes());
                                        fileOutputStream.flush();
                                        fileOutputStream.close();
                                    }
                                } catch (Exception e) {
                                    // Ignore
                                }
                            }
                        }

                        // Test is over
                        isRunning = false;
                        break;

                    } else {
                        p("Having a not supported operation " + currentOperation + "!");
                    }
                }

                sleep(Constant.DECISECOND); // Wait for any updates

                p("Waiting any updates...");

            } catch (InterruptedException e) {
                p("Test executor was interrupted. Stop working");
                isRunning = false;
            }
        }

        // Notify Test Automation Service about success or failure
        if (isTestFailed) {
            if (reasonOfTestFailure == null || reasonOfTestFailure.isEmpty()) {
                reasonOfTestFailure = Constant.UNSPECIFIED_REASON_OF_FAILURE;
            }

            p("Ended work. Test execution took totally " + Util.convert(System.currentTimeMillis() - testExecutionStartedAt)
                + ". Test execution was unsuccessful and has failed becase: " + reasonOfTestFailure);

            test.setStatus(Test.Status.FAILED, reasonOfTestFailure);

            TestOperation testFailedMessage = new TestOperation(TestOperation.Id.UPDATE, test);
            testFailedMessage.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
            testFailedMessage.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);

            sender.handle(testFailedMessage);

            p("Has send a message about test failure:\n" + testFailedMessage);

        } else {
            // Check what was the executor's return code
            if (testExecutionResultCode == 0) {
                p("Ended work. Test execution took totally " + Util.convert(System.currentTimeMillis() - testExecutionStartedAt)
                    + ". Test execution was successful");

                test.setStatus(Test.Status.FINISHED, "");

                TestOperation testFinishedMessage = new TestOperation(TestOperation.Id.UPDATE, test);
                testFinishedMessage.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
                testFinishedMessage.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);

                sender.handle(testFinishedMessage);

                p("Has send a test finished message:\n" + testFinishedMessage);

            } else {
                reasonOfTestFailure = "Test executor returned code " + testExecutionResultCode;

                p("Ended work. Test execution took totally " + Util.convert(System.currentTimeMillis() - testExecutionStartedAt)
                    + ". Test execution was unsuccessful and has failed becase: " + reasonOfTestFailure);

                test.setStatus(Test.Status.FAILED, reasonOfTestFailure);

                TestOperation testFailedMessage = new TestOperation(TestOperation.Id.UPDATE, test);
                testFailedMessage.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
                testFailedMessage.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);

                sender.handle(testFailedMessage);

                p("Has send a message about test failure:\n" + testFailedMessage);
            }
        }

        // Remove all cache entries refering to artifacts of this test
        List<String> testArtifacts = test.getArtifacts();

        for (String testArtifact : testArtifacts) {
            fileCache.removeEntryFor(testWorkspace.getAbsolutePath() + fileSeparator + testArtifact);
        }

        // So far we are running exclusively on Windows
        cleanupWindowsEnvironment();

        flushLogBuffer();

        // Stop logging for this test executor
        if (logWriter != null) {
            logWriter.close();
        }

        if (timer != null) {
            timer.cancel();
        }

        testAutomationCommunicator.stopTest(test, isTestFailed);
    }

    /**
     * Handles specified message.
     *
     * @param message A message to be handled
     */
    public synchronized void handle(Object message) {
        messagePool.add(message);
        notify();
    }

    /**
     * Returns a link to test workspace.
     *
     * @return A link to test workspace
     */
    public synchronized File getTestWorkspace() {
        return testWorkspace;
    }

    /**
     * Returns a test under execution.
     *
     * @return A test under execution
     */
    public synchronized Test getTest() {
        return test;
    }

    /**
     * Returns current status of the test executor in textual form.
     *
     * @return Current status of the test executor in textual form
     */
    public synchronized String getStatus() {
        return currentOperation.name();
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
     * Checks that test has a corresponding execution process in test node's memory.
     * Returns true if such process is existing or false otherwise.
     *
     * @return True if the test execution process is existing in test node's memory or false otherwise
     */
    private synchronized void isTestExecutionProcessAlive() {
        p("Checking existency of the external test execution process for '" + test.getRuntimeId() + "'...");
        List<Integer> pids = new ArrayList<Integer>(0);
        String windowsCommandLineElement = test.getRuntimeId() + "\\";
        String linuxCommandLineElement = test.getRuntimeId() + "/";

        try {
            // Search for any PID which has a command lines containing id of the current test
            List<String> commands = new ArrayList<String>(0);
            commands.add("WMIC"); // Use Windows Management Instrumentation for command line
            commands.add("PROCESS"); // Get all processes
            commands.add("GET");
            commands.add("CommandLine,ProcessId"); // With their command lines and PIDs
            commands.add("/VALUE"); // In a line-after-line format

            ProcessBuilder discoveryProcessBuilder = new ProcessBuilder(commands);
            discoveryProcessBuilder.redirectErrorStream(true);

            Process discoveryProcess = discoveryProcessBuilder.start();

            // Read outputs and print them out
            InputStream inputStream = discoveryProcess.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String processOutputLine = null;
            boolean shouldHaveProcessID = false;

            try {
                do {
                    processOutputLine = bufferedReader.readLine();

                    if (processOutputLine != null && !processOutputLine.isEmpty()) {
                        if (shouldHaveProcessID) {
                            // Check that we actually have a ProcessId
                            if (processOutputLine.startsWith("ProcessId")) {
                                try {
                                    String pidValue = processOutputLine.substring(processOutputLine.indexOf("=") + 1);
                                    Integer pid = Integer.decode(pidValue);
                                    if (pid.intValue() > 0) {
                                        pids.add(pid);
                                    }
                                } catch (Exception e) {
                                    //e.printStackTrace();
                                }
                            }

                            shouldHaveProcessID = false;
                        }

                        if (processOutputLine.startsWith("CommandLine")) {
                            // Always verify a proper runtime id with Windows and Linux file separating symbols
                            if (processOutputLine.indexOf(windowsCommandLineElement) != -1) {
                                // The next line should be process id belonging to this test execution
                                shouldHaveProcessID = true;
                            } else if (processOutputLine.indexOf(linuxCommandLineElement) != -1) {
                                // The next line should be process id belonging to this test execution
                                shouldHaveProcessID = true;
                            }
                        }
                    }
                } while (processOutputLine != null);

            } catch (Exception e) {
                //e.printStackTrace();
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }

        // If at least one process ID was discovered, the test execution is assumed to be still running
        if (!pids.isEmpty()) {
            p("External test execution process for '" + test.getRuntimeId() + "' is still presented");

            isTestExecutionProcessAlive = true;

        } else {
            p("External test execution process for '" + test.getRuntimeId() + "' is not presented");

            isTestExecutionProcessAlive = false;

            // Close output reader as well
            try {
                if (testProcessOutputReader != null) {
                    testProcessOutputReader.close();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Checks that no process has left for executed test.
     * If it will find one, it will try to kill it along with its sub-processes.
     */
    private synchronized void cleanupWindowsEnvironment() {
        p("Cleaning up Windows environment after test execution...");
        List<Integer> pids = new ArrayList<Integer>(0);
        String windowsCommandLineElement = test.getRuntimeId() + "\\";
        String linuxCommandLineElement = test.getRuntimeId() + "/";

        // Find out FUSE connection ids of reserved products
        List<String> fuseConnectionIds = new ArrayList<String>(0);

        if (test != null) {
            List<Product> reservedProducts = test.getReservedProducts();

            if (reservedProducts != null && !reservedProducts.isEmpty()) {
                for (Product reservedProduct : reservedProducts) {
                    String fuseConnectionId = reservedProduct.getFuseConnectionId();

                    if (fuseConnectionId != null && !fuseConnectionId.isEmpty()) {
                        fuseConnectionIds.add(fuseConnectionId);
                    }
                }
            }
        }

        try {
            // Search for any PID which has a command lines containing id of the current test
            List<String> commands = new ArrayList<String>(0);
            commands.add("WMIC"); // Use Windows Management Instrumentation for command line
            commands.add("PROCESS"); // Get all processes
            commands.add("GET");
            commands.add("CommandLine,ProcessId"); // With their command lines and PIDs
            commands.add("/VALUE"); // In a line-after-line format

            ProcessBuilder discoveryProcessBuilder = new ProcessBuilder(commands);
            discoveryProcessBuilder.redirectErrorStream(true);

            p("Launching cleanup discovery process...");
            Process discoveryProcess = discoveryProcessBuilder.start();

            // Read outputs and print them out
            InputStream inputStream = discoveryProcess.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String processOutputLine = null;
            boolean shouldHaveProcessID = false;

            try {
                do {
                    processOutputLine = bufferedReader.readLine();

                    if (processOutputLine != null && !processOutputLine.isEmpty()) {
                        if (shouldHaveProcessID) {
                            // Check that we actually have a ProcessId
                            if (processOutputLine.startsWith("ProcessId")) {
                                p("Its process ID is " + processOutputLine);

                                try {
                                    String pidValue = processOutputLine.substring(processOutputLine.indexOf("=") + 1);
                                    Integer pid = Integer.decode(pidValue);
                                    if (pid.intValue() > 0) {
                                        pids.add(pid);
                                        p("Discovered a process ID to kill: " + pid);
                                    }
                                } catch (Exception e) {
                                    //e.printStackTrace();
                                }
                            }

                            shouldHaveProcessID = false;
                        }

                        if (processOutputLine.startsWith("CommandLine")) {
                            // Always verify a proper runtime id with Windows and Linux file separating symbols
                            if (processOutputLine.indexOf(windowsCommandLineElement) != -1) {
                                p("Discovered a process to kill: " + processOutputLine);
                                // The next line should be process id to be killed
                                shouldHaveProcessID = true;
                            } else if (processOutputLine.indexOf(linuxCommandLineElement) != -1) {
                                p("Discovered a process to kill: " + processOutputLine);
                                // The next line should be process id to be killed
                                shouldHaveProcessID = true;
                            } else {
                                // Verify that no reserved FUSE communication ids were mentioned as well
                                if (!fuseConnectionIds.isEmpty()) {
                                    for (String fuseConnectionId : fuseConnectionIds) {
                                        if (processOutputLine.indexOf(fuseConnectionId) != -1) {
                                            p("Discovered a process to kill: " + processOutputLine);
                                            // The next line should be process id to be killed
                                            shouldHaveProcessID = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } while (processOutputLine != null);

            } catch (Exception e) {
                //e.printStackTrace();
            }

        } catch (Exception e) {
            //e.printStackTrace();
        }

        if (pids.isEmpty()) {
            p("Hasn't discovered any remained sub-processes to be killed after this test");
        }

        // Kill discovered processes
        for (Integer pid : pids) {
            try {
                // Kill a process with specified PID along with its sub-processes
                List<String> commands = new ArrayList<String>(0);
                commands.add("taskkill"); // Use Windows taskkill utility
                commands.add("/PID"); // Kill processes by their PIDs
                commands.add(pid.toString());
                commands.add("/F"); // Do forced kills
                commands.add("/T"); // Kill all possible sub-processes as well

                ProcessBuilder killProcessBuilder = new ProcessBuilder(commands);
                killProcessBuilder.redirectErrorStream(true);

                p("Trying to kill a process with PID: " + pid.intValue());

                Process killProcess = killProcessBuilder.start();

                // Read outputs and print them out
                InputStream inputStream = killProcess.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String processOutputLine = null;

                try {
                    do {
                        processOutputLine = bufferedReader.readLine();

                        if (processOutputLine != null && !processOutputLine.isEmpty()) {
                            p(processOutputLine);
                        }
                    } while (processOutputLine != null);

                } catch (Exception e) {
                    //e.printStackTrace();
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    /**
     * Callback method about failed message sending.
     *
     * @param reasonOfFailure A reason behind test stopping
     */
    public synchronized void stopWorking(String reasonOfFailure) {

        if (reasonOfFailure != null && !reasonOfFailure.isEmpty()) {
            reasonOfTestFailure = reasonOfFailure;
            isTestFailed = true;
            p("Got a request to stop handler of the test '" + test.getRuntimeId() + "' due to failure: " + reasonOfTestFailure);

            test.setStatus(Test.Status.FAILED, reasonOfTestFailure);

            TestOperation testFailedMessage = new TestOperation(TestOperation.Id.UPDATE, test);
            testFailedMessage.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
            testFailedMessage.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);

            sender.handle(testFailedMessage);

            p("Has send a test failed message: " + testFailedMessage);

            // Create a special file for indicating that test has failed
            try {
                File failureFile = new File(testWorkspace.getAbsolutePath() + fileSeparator + "TEST_HAS_FAILED.txt");
                failureFile.createNewFile();

                // Write the reason of failure into this file
                FileOutputStream fileOutputStream = new FileOutputStream(failureFile);
                fileOutputStream.write(reasonOfFailure.getBytes());
                fileOutputStream.flush();
                fileOutputStream.close();

            } catch (Exception e) {
                // Ignore
            }

            if (testAutomationCommunicator.canDeleteWorkspace(isTestFailed)) {
                p("Trying to delete test's workspace...");
                if (deleteRecursively(testWorkspace)) {
                    p("Workspace of the test '" + test.getRuntimeId() + "' was successfully deleted");
                }
            } else {
                p("Workspace deletion is not allowed");
            }
        } else {
            p("Got a request to stop handler of the test '" + test.getRuntimeId() + "'");
        }

        if (isTestRunning) {
            if (testProcess != null) {
                testProcess.destroy();
                cleanupWindowsEnvironment();
                p("Test process is killed");
            }

            isTestRunning = false;
            isTestExecutionProcessAlive = false;

            flushLogBuffer();
        }

        listOfFilesToBeReceived.clear();
        listOfFilesToBeSend.clear();

        operations.clear();
        operations.add(Operation.FINALIZE_TEST);

        notify();
    }

    /**
     * Callback method about failed message sending.
     */
    public synchronized void messageSendFailed() {
        p("Having a failed send of message regarding test " + test.getRuntimeId());
        stopWorking("Remote client or network has failed");
    }

    /**
     * Callback method about finished file receiving.
     *
     * @param fileName Name of the received file
     * @param success True if file was successfully received or false otherwise
     * @return True if notification was handled or false otherwise
     */
    public synchronized boolean fileReceived(String fileName, boolean success) {
        boolean handled = false;

        if (success) {
            p("Got a notification about successful receive of file " + fileName);
            for (String expectedFileName : listOfFilesToBeReceived) {
                if (expectedFileName.equals(fileName)) {
                    if (listOfFilesToBeReceived.remove(expectedFileName)) {
                        p("Confirming a successfull receive of file '" + fileName + "'");
                        hasGotFailureInFileReceive = false;
                    }
                    timeOfLastSuccessfulFileTransfer = System.currentTimeMillis();
                }
            }
        } else {
            p("Got a notification about failed receive of file " + fileName);
            for (String expectedFileName : listOfFilesToBeReceived) {
                if (expectedFileName.equals(fileName)) {
                    if (listOfFilesToBeReceived.remove(expectedFileName)) {
                        p("Confirming a failed receive of file '" + fileName + "'");
                        hasGotFailureInFileReceive = true;
                        isTestFailed = true;
                        reasonOfTestFailure = "Failed to receive file '" + fileName + "' on test node "
                            + testAutomationCommunicatorHostname + ":" + testAutomationCommunicatorPort;
                        listOfFilesToBeReceived.clear();
                        break;
                    }
                }
            }
        }

        notify();

        handled = true;

        return handled;
    }

    /**
     * Callback method about finished file sending.
     *
     * @param fileName Name of the send file
     * @param success True if file was successfully sended or false otherwise
     * @return True if notification was handled or false otherwise
     */
    public synchronized boolean fileSend(String fileName, boolean success) {
        boolean handled = false;

        if (success) {
            p("Got a notification about successful send of file " + fileName);
            for (String expectedFileName : listOfFilesToBeSend) {
                if (expectedFileName.equals(fileName)) {
                    if (listOfFilesToBeSend.remove(expectedFileName)) {
                        p("Confirming a successfull send of file " + fileName);
                    }
                    timeOfLastSuccessfulFileTransfer = System.currentTimeMillis();
                }
            }
        } else {
            p("Got a notification about failed send of file " + fileName);
            for (String expectedFileName : listOfFilesToBeSend) {
                if (expectedFileName.equals(fileName)) {
                    if (listOfFilesToBeSend.remove(expectedFileName)) {
                        p("Confirming a failure in send of file " + fileName);
                        hasGotFailureInFileSend = true;
                    }
                }
            }
        }

        notify();

        handled = true;

        return handled;
    }

    /**
     * Returns a list of products reserved for the test.
     *
     * @return A list of products reserved for the test
     */
    public synchronized List<Product> getReservedProducts() {

        if (test != null) {
            return test.getReservedProducts();
        }

        return null;
    }

    /**
     * Handles a situation of permanently disconnected product.
     *
     * @param product Instance of a permanently disconnected product
     */
    public synchronized void handlePermanentlyDisconnectedProduct(Product product) {
        if (product != null) {
            p("Got a notification about permanently disconnected product with IMEI "
                    + product.getIMEI() + " and of type " + product.getRMCode());

            stopWorking("Got a permanently disconnected product during test execution. Disconnected product had IMEI "
                        + product.getIMEI() + " and was of type " + product.getRMCode());
        }

        notify();
    }

    /**
     * Flushes the content of log buffer into a text message and send it over the sender.
     */
    private synchronized void flushLogBuffer() {
        if (logBuffer != null && logBuffer.length() > 0) {
            // Send contents of log buffer in a text message
            try {
                TextMessage textMessage = new TextMessage(test, logBuffer.toString());
                textMessage.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);
                textMessage.setReceiver(remoteClientHostname, remoteClientPort);

                sender.handle(textMessage);

                // Reset buffer
                logBuffer = new StringBuffer("");

            } catch (Exception e) {
                // Ignore any failures, since log messaging is only of informative nature
            }
        }
    }

    /**
     * Prints test to console.
     *
     * @param text A test to be printed
     */
    private synchronized void p(String text) {

        // Print message to test's own log
        if (logWriter != null) {
            String logLine = dateFormat.format(new Date()) + " " + test.getRuntimeId() + ": " + text + "\n";

            // Store line in the log file
            logWriter.append(logLine);

            // Store line in the log buffer for text messaging
            logBuffer.append(logLine);
        }

        // And to the global
        logger.log(Level.ALL, test.getRuntimeId() + ": " + text);
    }
}
