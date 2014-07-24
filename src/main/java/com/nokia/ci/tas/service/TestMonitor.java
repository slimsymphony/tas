package com.nokia.ci.tas.service;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.regex.Pattern;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.TestAutomationServiceListener;
import com.nokia.ci.tas.commons.TestPackage;
import com.nokia.ci.tas.commons.Util;

import com.nokia.ci.tas.commons.message.Message;
import com.nokia.ci.tas.commons.message.TestOperation;

/**
 * Monitor of a single test performed by the Test Automation Service.
 */
public class TestMonitor extends Thread {

    /**
     * Current instance of the Test Automation Service.
     */
    private TestAutomationService testAutomationService;

    /**
     * Instance of the test to be performed
     */
    private Test test;

    /**
     * Listener of all events related to executing the test.
     */
    private TestAutomationServiceListener listener;

    /**
     * Message pool for asynchronous messaging.
     */
    private ConcurrentLinkedQueue<Message> messagePool;

    /**
     * A list of tests to be restarted.
     */
    private List<Test> testsToBeRestarted;
    
    /**
     * A list of products that were successfully used during this test.
     */
    private List<Product> usedProducts;

    /**
     * A list of groups for name-value pairs that are matching all products specified
     * as required in the test description. Each required product will have exactly one group
     * of name-value pairs. All required products are currently assumed to be in logical 'and' relationship.
     */
    private List<String> nameValueGroupsForRequiredProducts;

    /**
     * Variable for keeping test monitor running.
     */
    private boolean isRunning = true;

    /**
     * Tells whenever the test has failed or not.
     */
    private boolean testHasFailed = false;

    /**
     * Tells the reason of test failure.
     */
    private String reasonOfTestFailure = null;

    /**
     * Tells whenever test monitor is waiting for successful reservations of all required test resources.
     */
    private boolean isReservingTestResources = true;

    /**
     * Tells whenever a listener of the test is a remote client or local application.
     */
    private boolean isRemoteListener = true;

    /**
     * Moment of time when test handling has started.
     */
    private long testHandlingStartedAt = 0L;

    /**
     * Current configuration of the Test Automation Service.
     */
    private Configuration configuration;

    /**
     * A list of test handlers created by this monitor.
     */
    private List<TestHandler> testHandlers;

    /**
     * Keeps number of test handlers initially given for monitoring.
     */
    private int subIdIndex = 1;

    /**
     * A list of specifications for environments to be used during the test.
     * Each environment may specify some set of products or/and a test machine.
     */
    private List<String> requiredEnvironments;

    /**
     * Each required environment may be described by a list of regular expressions.
     * All required environments will be stored in a Perl analogy as a list of lists.
     */
    private List<List<String>> regexesForRequiredEnvironments;

    /**
     * Container of compiled patterns for each of extracted regular expressions.
     */
    private List<List<Pattern>> patternsForRequiredEnvironments;

    /**
     * Keeps the total number of occured failures during executing this test.
     */
    private int totalNumberOfFailures = 0;

    /**
     * Keeps the maximal number of retries for a single failed test.
     */
    private long maximalNumberOfRetriesForFailedTest = Constant.MAXIMAL_NUMBER_OF_RETRIES_FOR_FAILED_TEST;

    /**
     * The moment of time when listener of issued test was notified about something.
     */
    private long timeOfLastListenerNotification = 0L;

    /**
     * Minimal durations for test executions are used in order to prevent "dead" products in the farm
     * due to interrupted updates of their software and firmwares.
     */
    private long minimalExecutionTimeForTest = Constant.FIVE_MINUTES;
    
    /**
     * A list of short summary reports about all issues or failures happened during test execution.
     */
    private List<String> testExecutionSummary;
    
    /**
     * Date and time format used for timestamps in logging prints.
     */
    private SimpleDateFormat timestampFormat;

    /**
     * Instance of the Test Automation Service's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationService.GLOBAL_LOGGER_NAME);

    /**
     * Creates a test monitor for specified test.
     * All the messages regarding test execution will be delivered to specified listener.
     *
     * @param testAutomationService Instance of Test Automation Service
     * @param test A test to be monitored
     * @param listener Listener of all events related to test executions
     * @param isRemoteListener True when listenener is a remote client connected to the Test Automation Service
     */
    public TestMonitor(TestAutomationService testAutomationService,
                       Test test,
                       TestAutomationServiceListener listener,
                       boolean isRemoteListener) {

        super(test.getId()); // Test monitor's name will be the same as test's id

        this.testAutomationService = testAutomationService;
        this.test = test;
        this.listener = listener;
        this.isRemoteListener = isRemoteListener;
        this.configuration = testAutomationService.getConfiguration();

        messagePool = new ConcurrentLinkedQueue();

        nameValueGroupsForRequiredProducts = new ArrayList<String>(0);
        testHandlers = new ArrayList<TestHandler>(0);

        testsToBeRestarted = new ArrayList<Test>(0);
        
        usedProducts = new ArrayList<Product>(0);

        testExecutionSummary = new ArrayList<String>(0);

        timestampFormat = new SimpleDateFormat(Constant.TIMESTAMP_FORMAT);

        // In case of NoSE test targets it means reserving a test node
        isReservingTestResources = true;
        timeOfLastListenerNotification = System.currentTimeMillis();

        extractEnvironmentRequirements();

        setPriority(Thread.MIN_PRIORITY); // Always run with minimal priority
    }

    /**
     * Implementation of the monitor's workflow.
     */
    @Override
    public void run() {
        // Remember the moment when test handling has started
        testHandlingStartedAt = System.currentTimeMillis();

        p("Started monitoring test '" + test.getId() + "':\n" + test.toString());

        // Pending the test resources allocation
        test.setStatus(Test.Status.PENDING);

        // Always ensure that test timeouts are specified
        if (test.getTimeout() <= 0L) {
            if (configuration != null) {
                test.setTimeout(configuration.getTestDefaultTimeout());
                p("Test '" + test.getId() + "' hasn't any specified timeout. The default timeout of " + Util.convert(test.getTimeout()) + " will be used");
            }
        }

        // Always get the minimal execution time for tests and the maximal number of retries for failed tests
        if (configuration != null) {
            long minimalDuration = configuration.getTestMinimalExecutionTime();

            if (minimalDuration > 0L) {
                minimalExecutionTimeForTest = minimalDuration;
            }

            maximalNumberOfRetriesForFailedTest = configuration.getMaximalNumberOfRetriesForFailedTest();
        }

        p("Entering the main loop...");

        while (isRunning) {
            try {
                // Check if any message has arrived
                if (!messagePool.isEmpty()) {
                    Message message = messagePool.poll(); // Always get the first message

                    if (message != null) {
                        p("Got a message for processing: " + message);

                        if (message instanceof TestOperation) {
                            TestOperation testOperation = (TestOperation) message;
                            Test updatedTest = testOperation.getTest();

                            if (updatedTest != null) {
                                // Forward test operation message to the corresponding test handler
                                String testRuntimeId = updatedTest.getRuntimeId();

                                // Each handler is identified by test's runtime id
                                for (TestHandler testHandler : testHandlers) {
                                    if (testHandler.getName().equals(testRuntimeId)) {
                                        testHandler.handle(message);
                                        p("Message forwarded to " + testHandler.getName());
                                        break;
                                    }
                                }
                            }
                        } else {
                            p("Got an unsupported message: " + message);
                        }
                    }
                }

                // Check test timeout
                if (test.isExpired(testHandlingStartedAt)) {
                    isRunning = false;
                    testHasFailed = true;
                    reasonOfTestFailure = "Got expiration of test's timeout (" + Util.convert(test.getTimeout()) + ") during test execution";
                    test.setStatus(Test.Status.FAILED, reasonOfTestFailure);
                    notifyListener(reasonOfTestFailure);

                    break;
                } else {
                    if ((System.currentTimeMillis() - timeOfLastListenerNotification) > Constant.FIVE_MINUTES) {

                        long currenTime = System.currentTimeMillis();
                        long remainingTime = test.getTimeout() - (currenTime - testHandlingStartedAt);
                        timeOfLastListenerNotification = System.currentTimeMillis();

                        if (isReservingTestResources) {
                            // Notify listener about waiting for test resources
                            if (remainingTime > 0L) {
                                notifyListener("Still waiting for required testing resources from the farm."
                                    + " The remaining timeout is " + Util.convert(remainingTime));
                            }
                        } else {
                            // Notify listener about executing tests
                            if (remainingTime > 0L) {
                                StringBuffer notification = new StringBuffer("Test '" + test.getId() + "'");

                                if (!testHandlers.isEmpty()) {
                                    
                                    notification.append(" has " + testHandlers.size());

                                    if (testHandlers.size() > 1) {
                                        notification.append(" test handlers under execution:");
                                    } else {
                                        notification.append(" test handler under execution:");
                                    }

                                    for (TestHandler handler : testHandlers) {
                                        notification.append(" '" + handler.getName() + "'");
                                    }

                                    notification.append(" The remaining timeout is " + Util.convert(remainingTime));
                                } else {
                                    // There are some tests to be restarted
                                    if (!testsToBeRestarted.isEmpty()) {
                                        // Compose a summary message about current test executions
                                        notification.append(" is trying to restart " + testsToBeRestarted.size());

                                        if (testsToBeRestarted.size() > 1) {
                                            notification.append(" sub-tests:");
                                        } else {
                                            notification.append(" sub-test:");
                                        }

                                        for (Test testToBeRestarted : testsToBeRestarted) {
                                            notification.append(" '" + testToBeRestarted.getRuntimeId() + "'");
                                        }

                                        notification.append(" The remaining timeout is " + Util.convert(remainingTime));
                                    } else {
                                        // There are no more handlers and there are no more tests to be restarted
                                        notification.append(" has no more test handlers under execution and no more tests to be restarted. Stopping work...");
                                        isRunning = false;
                                    }
                                }

                                notifyListener(notification.toString());
                            }
                        }
                    }
                }

                if (testHasFailed) {
                    p("Stopping monitor of the test '" + test.getId() + "' because: " + reasonOfTestFailure);
                    break;
                }

                sleep(Constant.MILLISECOND); // Wait for any updates

            } catch (Exception e) {
                p("Got troubles while trying to wait for test's '" + test.getId() + "' results");
                e.printStackTrace();
            }
        }

        notifyListener("Monitor of the test '" + test.getId() + "' is finishing its work. Ensuring the minimal duration for test executions ("
            + Util.convert(minimalExecutionTimeForTest) + ") in order to prevent any corrupted products in the test farm...");

        // Ensure the minimal execution times for test handlers
        try {
            if (!testHandlers.isEmpty()) {
                boolean mustWaitForTestsEnd = false;

                do {
                    mustWaitForTestsEnd = false;
                    long currentTime = System.currentTimeMillis();

                    for (TestHandler testHandler : testHandlers) {
                        long testExecutionStartTime = testHandler.getTestExecutionStartTime();

                        if (testExecutionStartTime <= 0L) {
                            // Test hasn't been started on a test node, use the time when the whole test handling was started
                            testExecutionStartTime = testHandlingStartedAt;
                            notifyListener("Test '" + testHandler.getName() + "' seems to be not able to start executions on its test node");
                        }

                        long testExecutionDuration = currentTime - testExecutionStartTime;

                        if (testExecutionDuration >= minimalExecutionTimeForTest) {
                            notifyListener("Test '" + testHandler.getName() + "' has been executing for more than "
                                + Util.convert(testExecutionDuration) + " and can be stopped...");
                            testHandler.stopTest(reasonOfTestFailure);
                        } else {
                            mustWaitForTestsEnd = true;

                            notifyListener("Test '" + testHandler.getName() + "' has been executing for "
                                + Util.convert(testExecutionDuration) + " and cannot be stopped immediately."
                                + " Will continue test executions for at least " + Util.convert(minimalExecutionTimeForTest)
                                + " in order to prevent any corrupted products in the test farm...");

                            // Don't stop here, since there might be other test handlers which could be stopped immediately
                        }
                    }

                    if (mustWaitForTestsEnd) {
                        try {
                            sleep(Constant.FIFTEEN_SECONDS);
                        } catch (Exception e) {
                            p("Got troubles while tried to ensure the minimal duration of test executions (" + Util.convert(minimalExecutionTimeForTest) + ")");
                            e.printStackTrace();
                        }
                    }
                } while (mustWaitForTestsEnd);
            } else {
                notifyListener("Has no running test handlers, checking for minimal duration of test executions will be skipped");
            }

        } catch (Exception e) {
            p("Got troubles while tried to ensure the minimal execution time for the test '" + test.getId() + "'");
            e.printStackTrace();
        }

        notifyListener("Stopping monitor of the test '" + test.getId() + "'. The whole test handling took " + Util.convert(System.currentTimeMillis() - testHandlingStartedAt));
        
        // Add a short summary about test execution
        if (testExecutionSummary != null && !testExecutionSummary.isEmpty()) {
            StringBuffer executionSummary = new StringBuffer("Test execution summary:\n");
            
            for (String summary : testExecutionSummary) {
                executionSummary.append(summary);
                executionSummary.append("\n");
            }

            notifyListener(executionSummary.toString());
        }

        if (testHasFailed) {
            if (reasonOfTestFailure == null || reasonOfTestFailure.isEmpty()) {
                reasonOfTestFailure = "Unknow reason of test failure";
                test.setStatus(Test.Status.FAILED, reasonOfTestFailure);
                p(reasonOfTestFailure);
            }

            // No need to notify test listener about used products, since all of them are already released
            notifyListenerAboutFailedTest(reasonOfTestFailure);

        } else {
            test.setStatus(Test.Status.FINISHED, "");
            
            // Also notify listener about the products that were used for this test executions
            // This notification is just of informative nature and there is no need to release them
            if (usedProducts != null && !usedProducts.isEmpty()) {
                test.setReservedProducts(usedProducts);

                StringBuffer notificationAboutUsedProducts = new StringBuffer();

                if (usedProducts.size() > 1) {
                    notificationAboutUsedProducts.append("The following " + usedProducts.size() + " products were used for the test executions:\n");
                } else {
                    notificationAboutUsedProducts.append("The following product was used for the test execution:\n");
                }

                for (Product current : usedProducts) {
                    notificationAboutUsedProducts.append("Product with IMEI ");
                    notificationAboutUsedProducts.append(current.getIMEI());
                    notificationAboutUsedProducts.append(" of type ");
                    notificationAboutUsedProducts.append(current.getRMCode());
                    notificationAboutUsedProducts.append(" and with FUSE connection name '");
                    notificationAboutUsedProducts.append(current.getFuseConnectionName());
                    notificationAboutUsedProducts.append("' on test node ");
                    notificationAboutUsedProducts.append(current.getHostname());
                    notificationAboutUsedProducts.append("\n");
                }

                notifyListener(notificationAboutUsedProducts.toString());
            }
            
            notifyListenerAboutFinishedTest();
        }

        testAutomationService.removeTestMonitor(test);
    }

    /**
     * Returns a list of environments required by the test and/or its test packages.
     *
     * @return A list of environments required by the test and/or its test packages
     */
    public synchronized List<String> getRequiredEnvironments() {
        return requiredEnvironments;
    }

    /**
     * Returns a list of reqular expression sets for each of the required environment.
     *
     * @return A list of reqular expression sets for each of the required environment
     */
    public synchronized List<List<String>> getRegexesForRequiredEnvironments() {
        return regexesForRequiredEnvironments;
    }

    /**
     * Returns a list of pattern sets for each of the required environment.
     *
     * @return A list of pattern sets for each of the required environment
     */
    public synchronized List<List<Pattern>> getPatternsForRequiredEnvironments() {
        return patternsForRequiredEnvironments;
    }

    /**
     * Returns instance of the test being monitored.
     *
     * @return Instance of the test being monitored
     */
    public synchronized Test getTest() {
        return test;
    }

    /**
     * Method for notifying monitor about successfully started test.
     *
     * @param startedTest Instance of the started test
     */
    public synchronized void notifyMonitorAboutStartedTest(Test startedTest) {
        p("Got a notification about STARTED test '" + startedTest.getRuntimeId() + "'");
        notifyListenerAboutStartedTest(startedTest);
    }

    /**
     * Method for notifying monitor about finished test.
     *
     * @param finishedTest Instance of the finished test
     * @param reason Reason behind test's failure
     */
    public synchronized void notifyMonitorAboutFinishedTest(Test finishedTest) {
        p("Got a notification about FINISHED test '" + finishedTest.getRuntimeId() + "'");
        testExecutionSummary.add("Test '" + finishedTest.getRuntimeId() + "' has successfully finished at "
                + timestampFormat.format(new Date(System.currentTimeMillis())));
        // Here we don't do anything, since test handler will remove itself in the removeTestHandler() method
    }

    /**
     * Method for notifying monitor about failed test.
     *
     * @param failedTest Instance of the failed test
     * @param reason Reason behind test's failure
     */
    public synchronized void notifyMonitorAboutFailedTest(Test failedTest, String reason) {
        // Don't fail any tests here, since its job of the test handlers and later monitor checks
        notifyListener("Got notification about a failure in the test '" + failedTest.getRuntimeId() + "': " + reason);
        // Create a test execution summary
        StringBuffer executionSummary = new StringBuffer();

        List<Product> products = failedTest.getReservedProducts();

        executionSummary.append("Test '");
        executionSummary.append(failedTest.getRuntimeId());
        executionSummary.append("' had a failure at " + timestampFormat.format(new Date(System.currentTimeMillis())));
        executionSummary.append(": " + reason);
        
        if (products != null && !products.isEmpty()) {
            executionSummary.append("\nTest '");
            executionSummary.append(failedTest.getRuntimeId());
            executionSummary.append("' has used the following ");
            
            if (products.size() > 1) {
                executionSummary.append(products.size() + " products:\n");
            } else {
                executionSummary.append("product:\n");
            }
            
            for (Product current : products) {
                executionSummary.append("Product with IMEI ");
                executionSummary.append(current.getIMEI());
                executionSummary.append(" of type ");
                executionSummary.append(current.getRMCode());
                executionSummary.append(" and with FUSE connection name '");
                executionSummary.append(current.getFuseConnectionName());
                executionSummary.append("' on test node ");
                executionSummary.append(current.getHostname());
                executionSummary.append("\n");
            }
        }

        testExecutionSummary.add(executionSummary.toString());
    }

    /**
     * Stops test monitor and performs all necessary cleanups.
     *
     * @param reason Reason behind test stop
     */
    public synchronized void stopTest(String reason) {
        p("Got a request to STOP monitor of the test '" + test.getId() + "' because: " + reason);

        isRunning = false;
        testHasFailed = true;
        reasonOfTestFailure = reason;
        test.setStatus(Test.Status.FAILED, reasonOfTestFailure);
        
        notify();
    }

    /**
     * Asynchronously handles specified message.
     *
     * @param message A message to be handled
     */
    public synchronized void handle(Message message) {
        if (messagePool.add(message)) {
            //p("Test monitor for '" + getName() + "' got a message to handle:\n" + message);
        } else {
            p("Error: Test monitor for '" + getName() + "' couldn't add a message for handling:\n" + message);
        }

        notify();
    }

    /**
     * Sets the list of name-value groups that should match all required products.
     *
     * @param matchingNameValueGroups A list of name-value groups that should match all required products
     */
    public synchronized void setNameValueGroupsForRequiredProducts(List<String> nameValueGroups) {
        nameValueGroupsForRequiredProducts = nameValueGroups;
    }

    /**
     * Returns the list of name-value groups that are matching all required products.
     *
     * @return The list of name-value groups that are matching all required products
     */
    public synchronized List<String> getNameValueGroupsForRequiredProducts() {
        return nameValueGroupsForRequiredProducts;
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
     * Sends to listener a message with specified text.
     *
     * @param text A text to be send in message to the listener
     */
    public synchronized void notifyListener(String text) {
        p(text);
        if (isRemoteListener) {
            RemoteClient client = (RemoteClient) listener;
            client.messageFromTestAutomationService(test, text);
        } else {
            listener.messageFromTestAutomationService(test, text);
        }
    }

    /**
     * Notifies listener about started test.
     *
     * @param startedTest Instance of the started test
     */
    public synchronized void notifyListenerAboutStartedTest(Test startedTest) {
        p("Notifying listener about STARTED test '" + startedTest.getRuntimeId() + "':\n" + startedTest);

        // Ensure that test status is consistent
        startedTest.setStatus(Test.Status.STARTED);

        if (isRemoteListener) {
            RemoteClient client = (RemoteClient) listener;
            client.testStarted(startedTest);
        } else {
            listener.testStarted(startedTest);
        }
    }

    /**
     * Notifies listener about failed test.
     *
     * @param reason Reason behind test's failure
     */
    public synchronized void notifyListenerAboutFailedTest(String reason) {
        p("Notifying listener about FAILED test '" + test.getId() + "' (" + reason + ")");

        if (reason != null && !reason.isEmpty()) {
            reasonOfTestFailure = reason;
        } else {
            reasonOfTestFailure = "Failing the test '" + test.getId() + "' because of unspecified reason";
        }

        // Ensure that test status and reason of failure are consistent
        test.setStatus(Test.Status.FAILED, reasonOfTestFailure);

        if (isRemoteListener) {
            RemoteClient client = (RemoteClient) listener;
            client.testFailed(test, reasonOfTestFailure);
        } else {
            listener.testFailed(test, reasonOfTestFailure);
        }
    }

    /**
     * Notifies listener about finished test.
     */
    public synchronized void notifyListenerAboutFinishedTest() {
        p("Notifying listener about FINISHED test '" + test.getId() + "'");

        // Ensure that test status is consistent
        test.setStatus(Test.Status.FINISHED);

        if (isRemoteListener) {
            RemoteClient client = (RemoteClient) listener;
            client.testFinished(test);
        } else {
            listener.testFinished(test);
        }
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
     * Callback method about a disconnected test node.
     */
    public synchronized void handleDisconnectedTestNode(TestNode testNode) {
        p("Got a notification about the DISCONNECTED test node " + testNode.getHostnameAndPort());

        // Notify all test handlers about disconnected test node
        String disconnectedHostnameAndPort = testNode.getHostnameAndPort();
        String disconnectedDescription = testNode.getDescription().getDescription();

        for (TestHandler testHandler : testHandlers) {
            TestNode reservedTestNode = testHandler.getReservedTestNode();

            if (reservedTestNode != null) {
                if (reservedTestNode.getHostnameAndPort().equals(disconnectedHostnameAndPort)) {
                    if (disconnectedDescription != null && !disconnectedDescription.isEmpty()) {
                        testHandler.stopTest("The reserved test node " + disconnectedHostnameAndPort
                                + " (\"" + disconnectedDescription  + "\") has disconnected");
                    } else {
                        testHandler.stopTest("The reserved test node " + disconnectedHostnameAndPort + " has disconnected");
                    }
                }
            }
        }
    }

    /**
     * Adds specified handlers to the list of running ones.
     *
     * @param testHandlers New test handlers ready to be started
     */
    public synchronized void addTestHandlers(List<TestHandler> testHandlers) {
        // Ensure that we don't have any dublicates
        List<TestHandler> addedHandlers = new ArrayList<TestHandler>(0);
        for (TestHandler testHandler : testHandlers) {
            if (!this.testHandlers.contains(testHandler)) {
                addedHandlers.add(testHandler);
                subIdIndex++;
            } else {
                p("Warning: A new test handler '" + testHandler.getName() + "' was already on the list of running handlers");
            }
        }

        // Add only those which were not already on the list
        this.testHandlers.addAll(addedHandlers);

        for (TestHandler testHandler : addedHandlers) {
            p("Launching test handler '" + testHandler.getName() + "'");
            testHandler.start();
        }

        notify();
    }

    /**
     * Returns current index for sub-test id.
     *
     * @return Current index for sub-test id
     */
    public synchronized int getSubIdIndex() {
        return subIdIndex;
    }

    /**
     * Sets current index for sub-test id.
     *
     * @param subIdIndex Current index for sub-test id
     */
    public synchronized void setSubIdIndex(int subIdIndex) {
        this.subIdIndex = subIdIndex;
    }

    /**
     * Updates monitored test to specified one.
     *
     * @param updatedTest Updated instance of the monitored test
     */
    public synchronized void updateMonitoredTest(Test update) {
        if (update != null) {
            test = update;
            extractEnvironmentRequirements();
        }

        notify();
    }

    /**
     * Returns instance of the test listener.
     *
     * @return Instance of the test listener
     */
    public synchronized TestAutomationServiceListener getListener() {
        return listener;
    }

    /**
     * Returns instance of the remote client.
     *
     * @return Instance of the remote client
     */
    public synchronized RemoteClient getRemoteClient() {
        return (RemoteClient) listener;
    }

    /**
     * Removes specified test handler from the list of running ones.
     *
     * @param testHandler A test handler to be removed
     * @param testHasFailed True if handled test has failed or false if it was successful
     */
    public synchronized void removeTestHandler(TestHandler testHandler, boolean testHasFailed) {
        p("Removing test handler '" + testHandler.getName() + "' from the list of running test handlers");

        if (testHandlers.contains(testHandler)) {
            if (testHandlers.remove(testHandler)) {
                p("Test handler '" + testHandler.getName() + "' was successfully removed from the list of running test handlers");
            } else {
                p("Error: Couldn't remove test handler '" + testHandler.getName() + "' from the list of running test handlers");
            }
        } else {
            p("Warning: Test handler '" + testHandler + "' wasn't on the list of running test handlers");
        }

        if (testHasFailed) {
            // Test was not successful
            Test failedTest = testHandler.getTest();
            p("Test '" + failedTest.getRuntimeId() + "' has failed");

            // Update total number of failures occured in the whole test
            totalNumberOfFailures++;
            notifyListener("Total number of occured failures right now is " + totalNumberOfFailures);

            boolean failedTestCanBeRestarted = true;

            // Check if the maximal number of test retries was already reached

            if (totalNumberOfFailures >= maximalNumberOfRetriesForFailedTest) {
                stopTest("Test '" + test.getId() + "' has got " + totalNumberOfFailures + " failures during its work and cannot be continued."
                    + " The maximal number of retries for failed tests was set to " + maximalNumberOfRetriesForFailedTest);
                failedTestCanBeRestarted = false;
            } else {
                p("Test '" + test.getId() + "' has got " + totalNumberOfFailures + " failures so far and still can be continued,"
                    + " since the maximal number of retries for a failed test is currently set to " + maximalNumberOfRetriesForFailedTest);

                // Check remained timeout for the whole test
                long currentTime = System.currentTimeMillis();
                long remainingTime = test.getTimeout() - (currentTime - testHandlingStartedAt);
                
                if (remainingTime >= minimalExecutionTimeForTest) {
                    p("Test '" + failedTest.getRuntimeId() + "' could be restarted, since the remaining time for test executions is " + Util.convert(remainingTime));

                    // The total number of occured failures and timeouts are OK, so let's issue a request for test restart
                    if (!testsToBeRestarted.isEmpty()) {
                        for (Test testToBeRestarted : testsToBeRestarted) {
                            if (testToBeRestarted.getRuntimeId().equalsIgnoreCase(failedTest.getRuntimeId())) {
                                p("A list of the tests to be restarted already contains test '" + failedTest.getRuntimeId() + "'");
                                failedTestCanBeRestarted = false;
                                break;
                            }
                        }
                    }
                } else {
                    p("Test '" + failedTest.getRuntimeId() + "' cannot be restarted, since the remaining timeout is less than exptected: " + Util.convert(minimalExecutionTimeForTest));
                    failedTestCanBeRestarted = false;
                }
            }

            if (failedTestCanBeRestarted) {
                testsToBeRestarted.add(failedTest);

                if (testAutomationService.issueRequestForTestRestart(this)) {
                    notifyListener("A request to restart the failed test '" + failedTest.getRuntimeId()
                            + "' has been successfully issued to the Test Automation Service");
                } else {
                    notifyListener("Couldn't issue a request to restart the failed test '" + failedTest.getRuntimeId() + "'");
                    // Don't stop the whole test here, since we could have some running valid sub-tests
                }
            }
        } else {
            // Test was successful
            Test successfulTest = testHandler.getTest();
            notifyListener("Test '" + successfulTest.getRuntimeId() + "' was successfull");
            
            // Update the list of used products, no matter what the product releasing mode was
            // In case of any failure these products are already released by the test handler
            List<Product> reservedProducts = successfulTest.getReservedProducts();
            
            if (reservedProducts != null && !reservedProducts.isEmpty()) {
                for (Product current : reservedProducts) {
                    usedProducts.add(current);
                }
            }

            // Check if there is anything else to do
            if (testHandlers.isEmpty() && !isReservingTestResources && testsToBeRestarted.isEmpty()) {
                notifyListener("All test handlers has finished their works. Test is not reserving any resources from the farm"
                        + " and there are no any sub-tests to be restarted. Stopping monitor of the test '" + test.getId() + "'...");
                isRunning = false;
            }
        }

        // Show what test handlers has left
        if (testHandlers.isEmpty() && testsToBeRestarted.isEmpty()) {
            if (isReservingTestResources) {
                notifyListener("Test '" + test.getId() + "' hasn't any handlers yet, but is still trying to reserve some test resources");
            } else {
                notifyListener("Test '" + test.getId() + "' hasn't any handlers and is not trying to reserve any test resources. Stopping monitor of the test '" + test.getId() + "'...");
                isRunning = false;
            }
        } else {
            // Compose a summary message about current test executions
            StringBuffer message = new StringBuffer("Test '" + test.getId() + "'");

            if (!testHandlers.isEmpty()) {
                message.append(" has " + testHandlers.size());
                if (testHandlers.size() > 1) {
                    message.append(" handlers:");
                } else {
                    message.append(" handler:");
                }
                for (TestHandler handler : testHandlers) {
                    message.append(" '" + handler.getName() + "'");
                }
            } else {
                message.append(" has no handlers");
            }

            if (!testsToBeRestarted.isEmpty()) {
                message.append(" Test '" + test.getId() + "' is trying to restart " + testsToBeRestarted.size());
                
                if (testsToBeRestarted.size() > 1) {
                    message.append(" sub-tests:");
                } else {
                    message.append(" sub-test:");
                }
            
                for (Test testToBeRestarted : testsToBeRestarted) {
                    message.append(" '" + testToBeRestarted.getRuntimeId() + "'");
                }
            }

            notifyListener(message.toString());
        }

        notify();
    }

    /**
     * Shutdowns this test monitor.
     */
    public synchronized void shutdown() {
        p("Got a request to shutdown...");
        isRunning = false;
        notify();
    }

    /**
     * Extracts environment requirements from the monitored test.
     */
    private synchronized void extractEnvironmentRequirements() {

        requiredEnvironments = new ArrayList<String>(0);
        regexesForRequiredEnvironments = new ArrayList<List<String>>(0);
        patternsForRequiredEnvironments = new ArrayList<List<Pattern>>(0);

        // Try to extract information about required environment from the test
        String requiredEnvironment = test.getRequiredEnvironment();

        if (requiredEnvironment != null && !requiredEnvironment.isEmpty()) {
            // Nothing else will be added, since test has declared what it needs
            requiredEnvironments.add(requiredEnvironment.toLowerCase());
        } else {
            // Otherwise try to go through requirements from the test packages
            List<TestPackage> testPackages = test.getTestPackages();

            if (testPackages != null && !testPackages.isEmpty()) {
                for (TestPackage testPackage : testPackages) {
                    requiredEnvironment = testPackage.getRequiredEnvironment();

                    if (requiredEnvironment != null && !requiredEnvironment.isEmpty()) {

                        // Automatically convert environment specification into lower case
                        requiredEnvironment = requiredEnvironment.toLowerCase();

                        // Add only when there are no dublicates
                        if (!requiredEnvironments.contains(requiredEnvironment)) {
                            requiredEnvironments.add(requiredEnvironment);
                        }
                    }
                }
            }
        }

        if (requiredEnvironments.isEmpty()) {
            // Try to extract required environments from the list of required products
            List<Product> requiredProducts = test.getRequiredProducts();
            String allEnvironmentRequirements = "";

            // Got a product after product and extract all name-value pairs
            if (requiredProducts != null && !requiredProducts.isEmpty()) {
                for (Product requiredProduct : requiredProducts) {
                    String nameValuePairs = requiredProduct.getNameValuePairs();

                    if (nameValuePairs != null && !nameValuePairs.isEmpty()) {
                        // Automatically convert required product specification into lower case
                        nameValuePairs = nameValuePairs.toLowerCase();

                        // Enclose each required product with pair of parentheses 
                        nameValuePairs = "(" + nameValuePairs + ")";

                        if (!allEnvironmentRequirements.contains(nameValuePairs)) {
                            // All required products are forming a single product set
                            allEnvironmentRequirements += " " + nameValuePairs;
                        }
                    }
                }
            }

            allEnvironmentRequirements = allEnvironmentRequirements.trim();

            if (!allEnvironmentRequirements.isEmpty()) {
                // Populate the list of required environments with a single product set specification
                if (!requiredEnvironments.contains(allEnvironmentRequirements)) {
                    requiredEnvironments.add(allEnvironmentRequirements);
                }

                // Since test used the old and deprecated way to specify required environments, we need to set it properly here
                if (test.getRequiredEnvironment().isEmpty()) {
                    test.setRequiredEnvironment(allEnvironmentRequirements);
                }
            }
        }

        // Resort the list of required environments:
        // put the longests (and probably the most exotic ones) into beginning of the list
        if (!requiredEnvironments.isEmpty()) {
            // Put the longests environment specifications at the beginning
            Collections.sort(requiredEnvironments,
                 new Comparator<String>() {
                     public int compare(String s1, String s2) {
                         if (s1.length() > s2.length()) {
                             return -1;
                         } else if (s1.length() < s2.length()) {
                             return 1;
                         } else {
                             return 0;
                         }
                     }
                 });

            // Create lists of regular expressions describing all required environments
            for (String environment : requiredEnvironments) {
                List<String> regularExpressions = Util.createRegularExpressions(environment);
                if (regularExpressions != null && !regularExpressions.isEmpty()) {

                    // Turn regular expressions into patterns
                    List<Pattern> patterns = new ArrayList<Pattern>(0);

                    for (String regularExpression : regularExpressions) {
                        patterns.add(Pattern.compile(regularExpression));
                    }

                    regexesForRequiredEnvironments.add(regularExpressions);
                    patternsForRequiredEnvironments.add(patterns);
                }
            }

            // Ensure that each environment has the corresponding set or regular expressions
            if (requiredEnvironments.size() != regexesForRequiredEnvironments.size()) {
                p("The number of required environments (" + requiredEnvironments.size()
                    + ") doesn't equal the number of extracted regular expressions (" + regexesForRequiredEnvironments.size()
                    + "). The test will be failed.");
                // Failing the test by reseting everything
                requiredEnvironments = new ArrayList<String>(0);
                regexesForRequiredEnvironments = new ArrayList<List<String>>(0);
                patternsForRequiredEnvironments = new ArrayList<List<Pattern>>(0);
            }
        }
    }

    /**
     * Sets a parameter indicating that this test monitor is trying to reserve some test resources from the farm.
     *
     * @param isReservingTestResources True if this test monitor is trying to reserve some test resources from the farm
     */
    public synchronized void setIsReservingTestResources(boolean isReservingTestResources) {
        this.isReservingTestResources = isReservingTestResources;
    }
    
    /**
     * Returns a list of tests to be restarted for this monitor.
     * 
     * @return A list of tests to be restarted for this monitor
     */
    public synchronized List<Test> getTestsToBeRestarted() {
        // Always return a copy, since this list may be populated soon by some other sub-tests
        return new ArrayList<Test>(testsToBeRestarted);
    }
    
    /**
     * Removes specified test from the list of tests to be restarted.
     * 
     * @param testToBeRestarted A test to be removed from the list of tests to be restarted
     */
    public synchronized void removeTestToBeRestarted(Test testToBeRestarted) {
        for (Test current : testsToBeRestarted) {
            if (current.getRuntimeId().equals(testToBeRestarted.getRuntimeId())) {
                testsToBeRestarted.remove(current);
                break;
            }
        }
    }

    /**
     * Prints specified text to output stream.
     *
     * @param text A text to be printed to output stream
     */
    protected synchronized void p(String text) {
        logger.log(Level.ALL, getName() + ": " + text);
    }
}
