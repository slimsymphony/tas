package com.nokia.ci.tas.service;

import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.Socket;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.TestAutomationServiceListener;
import com.nokia.ci.tas.commons.Util;

import com.nokia.ci.tas.commons.message.Message;
import com.nokia.ci.tas.commons.message.TestOperation;
import com.nokia.ci.tas.commons.message.TextMessage;

/**
 * Represents an instance of remote Test Automation Client connected
 * to this instance of Test Automation Service.
 */
public class RemoteClient extends Thread implements TestAutomationServiceListener {

    /**
     * Hostname of remote client.
     */
    private String clientHostname;

    /**
     * Port number used by remote client for incoming messages.
     */
    private int clientPort;

    /**
     * Hostname of remote service.
     */
    private String serviceHostname;

    /**
     * Port number used by remote service for incoming messages.
     */
    private int servicePort;

    /**
     * A pool of messages to be send to remote client.
     */
    private ConcurrentLinkedQueue<Message> messagePool;

    /**
     * Variable which keeps this remote client working on the side of Test Automation Service.
     */
    private boolean isRunning = true;

    /**
     * Holder of all currently running tests associated with this instance of remote client.
     */
    private ConcurrentLinkedQueue<Test> tests;

    /**
     * Current instance of the Test Automation Service.
     */
    private TestAutomationService testAutomationService;

    /**
     * A moment of time when the last message was successfully send or received.
     */
    private long timeOfLastSuccessfulMessage = 0L;

    /**
     * Current configuration of the Test Automation Service.
     */
    private Configuration configuration;

    /**
     * Instance of the Test Automation Service's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationService.GLOBAL_LOGGER_NAME);

    /**
     * Default constructor.
     *
     * @param testAutomationService Instance of the Test Automation Service
     * @param hostname Hostname of remote Test Automation Client
     * @param port Port number used by remote Test Automation Client
     * @param serviceHostname Hostname of Test Automation Service
     * @param servicePort Port number used by Test Automation Service
     */
    public RemoteClient(TestAutomationService testAutomationService,
                        String hostname,
                        int port,
                        String serviceHostname,
                        int servicePort) {

        super("RemoteTestAutomationClient_" + hostname + port); // Use hostname and port as a name of the thread

        this.testAutomationService = testAutomationService;
        this.clientHostname = hostname;
        this.clientPort = port;
        this.serviceHostname = serviceHostname;
        this.servicePort = servicePort;
        this.configuration = testAutomationService.getConfiguration();

        messagePool = new ConcurrentLinkedQueue();
        tests = new ConcurrentLinkedQueue();

        // Creation of remote client means that it is online
        timeOfLastSuccessfulMessage = System.currentTimeMillis();

        setPriority(Thread.MIN_PRIORITY); // Always run with minimal priority
    }

    /**
     * Returns a hostname associated with remote client.
     *
     * @return A hostname associated with remote client
     */
    public synchronized String getClientHostname() {
        return clientHostname;
    }

    /**
     * Returns a port number associated with remote client.
     *
     * @return A port number associated with remote client
     */
    public synchronized int getClientPort() {
        return clientPort;
    }

    /**
     * Returns a hostname and port of remote client in canonical form "hostname:port".
     *
     * @return A hostname and port of remote client in canonical form "hostname:port"
     */
    public synchronized String getClientHostnameAndPort() {
        return clientHostname + ":" + clientPort;
    }

    /**
     * Main routine.
     */
    @Override
    public void run() {
        p("Started working");

        while (isRunning) {
            try {
                if (!messagePool.isEmpty()) {
                    Message message = messagePool.poll(); // Always get the first message

                    if (message != null) {
                        for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                            Socket socket = null;
                            OutputStream output = null;

                            try {
                                p("Trying to send a message to remote client at " + clientHostname + ":" + clientPort);

                                socket = new Socket(InetAddress.getByName(clientHostname), clientPort);
                                output = socket.getOutputStream();

                                // Send message and close connection immediately
                                output.write(message.toXML().getBytes("UTF-8"));
                                output.flush();

                                output.close();
                                socket.close();

                                // Message was successfully send
                                timeOfLastSuccessfulMessage = System.currentTimeMillis();

                                break; // Stop tries

                            } catch (Exception e) {
                                p("Got a problem during sending a message to remote client at "
                                        + clientHostname + ":" + clientPort + " because: " + e.getClass() + " " + e.getMessage());

                                if (i >= (Constant.NUMBER_OF_RETRIES - 1)) {
                                    // This remote client has some serious network problems
                                    p("Has tried to send message for the maximal number of retries (" + Constant.NUMBER_OF_RETRIES + ")");
                                    p("Stopping remote client at " + clientHostname + ":" + clientPort);
                                    p("Remote client at " + clientHostname + ":" + clientPort + " has " + tests.size() + " issued tests");

                                    for (Test test : tests) {
                                        p("Trying to stop test '" + test.getId() + "'");
                                        // Test monitors are always associated with test ids
                                        TestMonitor testMonitor = testAutomationService.getTestMonitor(test.getId());

                                        if (testMonitor != null) {
                                            p("Notifying monitor of the test '" + test.getId() + "' about network failures");
                                            testMonitor.stopTest("Remote client at " + message.getReceiverHostname() + ":" + message.getReceiverPort() + " is not accessible anymore");
                                        }
                                    }

                                    if (!tests.isEmpty()) {
                                        // Notify service about occured failure
                                        testAutomationService.handleDisconnectedRemoteClient(clientHostname, clientPort, true);
                                    }

                                    isRunning = false;
                                    break;
                                } else {
                                    p("Will try to re-send message after " + Util.convert(Constant.THIRTY_SECONDS));
                                    sleep(Constant.THIRTY_SECONDS);
                                }
                            } finally {
                                // Always ensure that output stream is closed
                                if (output != null) {
                                    try {
                                        output.close();
                                    } catch (Exception e) {
                                        p("Got troubles while tried to close output stream to remote Test Automation Client at "
                                            + clientHostname + ":" + clientPort
                                            + " - " + e.getClass() + " " + e.getMessage());
                                    }
                                }

                                // Always ensure that connection is closed
                                if (socket != null && !socket.isClosed()) {
                                    try {
                                        socket.close();
                                    } catch (Exception e) {
                                        p("Got troubles while tried to close a connection to remote Test Automation Client at "
                                            + clientHostname + ":" + clientHostname + " (" + socket.getInetAddress() + ") - "
                                            + e.getClass() + " - " + e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if ((System.currentTimeMillis() - timeOfLastSuccessfulMessage) > configuration.getRemoteClientCheckingPeriod()) {
                        // Check what tests are still running and what are not

                        // Try to check a connection between Test Automation Service and remote Client
                        String messageText = "Connection check between Test Automation Service at " + serviceHostname + ":" + servicePort + " and this Test Automation Client is OK";

                        for (Test runningTest : tests) {
                            TextMessage textMessage = new TextMessage(runningTest, messageText);
                            textMessage.setSender(serviceHostname, servicePort);
                            textMessage.setReceiver(clientHostname, clientPort);
                            handle(textMessage);
                        }
                    }
                }

                sleep(Constant.MILLISECOND); // Wait for any updates

            } catch (InterruptedException e) {
                p("Remote client was interrupted");
                e.printStackTrace();

                isRunning = false;

                for (Test test : tests) {
                    p("Trying to stop test '" + test.getId() + "'");
                    // Test monitors are always associated with test ids
                    TestMonitor testMonitor = testAutomationService.getTestMonitor(test.getId());

                    if (testMonitor != null) {
                        p("Notifying monitor of the test '" + test.getId() + "' about network failures");
                        testMonitor.stopTest("Remote client got a failure on the side of Test Automation Service");
                    }
                }

                if (!tests.isEmpty()) {
                    // Notify service about occured failure
                    testAutomationService.handleDisconnectedRemoteClient(clientHostname, clientPort, true);
                }
            }
        }

        p("Successfully ended its work on the side of Test Automation Service");
    }

    /**
     * Will put specified message into the messaging pool
     * and will send it to remote Test Automation Service as soon as possible.
     *
     * @param message Message to be send
     */
    public synchronized void handle(Message message) {
        if (messagePool.add(message)) {
            //p("Got a message to handle: " + message);
        } else {
            p("Error: Couldn't add a message for handling: " + message);
        }
        notify();
    }

    /**
     * Checks if this remote client has a running test with specified id.
     *
     * @param testId Id of the running test
     * @return True if this remote client has a running test with specified id or false otherwise
     */
    public synchronized boolean hasRunningTestWithId(String testId) {

        boolean hasRunningTestWithId = false;

        for (Test current : tests) {
            if (current.getId().equals(testId)) {
                hasRunningTestWithId = true;
                break;
            }
        }

        return hasRunningTestWithId;
    }

    /**
     * Adds specified test as one issued by this remote client.
     *
     * @param test Test issued by this remote client
     */
    public synchronized void addTest(Test test) {
        if (!hasRunningTestWithId(test.getId())) {
            if (tests.add(test)) {
                p("Successfully added test '" + test.getId() + "' to the list of tests issued by this remote client");
            } else {
                p("Error: Couldn't add the test '" + test.getId() + "' to the list of tests issued by this remote client");
            }
        } else {
            p("Warning: Couldn't add the test '" + test.getId() + "' to the list of tests issued by this remote client, since it is already on the list");
        }
    }

    /**
     * Removes specified test from the list of tests issued by this remote client.
     *
     * @param test Test to be removed
     */
    public synchronized void removeTest(Test test) {
        for (Test current : tests) {
            if (current.getId().equals(test.getId())) {
                if (tests.remove(current)) {
                    p("Successfully removed test '" + test.getId() + "' from the list of tests issued by this remote client");
                } else {
                    p("Error: Couldn't remove the test '" + test.getId() + "' from the list of tests issued by this remote client");
                }
                break;
            }
        }
    }

    /**
     * From TestAutomationServiceListener.
     *
     * Called when test is started.
     *
     * @param test Started test
     */
    @Override
    public void testStarted(Test test) {
        p("Notifying remote client about STARTED test '" + test.getRuntimeId() + "'");

        test.setStatus(Test.Status.STARTED);

        TestOperation testOperation = new TestOperation(TestOperation.Id.UPDATE, test);
        testOperation.setReceiver(clientHostname, clientPort);
        testOperation.setSender(serviceHostname, servicePort);

        handle(testOperation);
    }

    /**
     * From TestAutomationServiceListener.
     *
     * Called when specified test has finished.
     * Please note, a "finished" test doesn't mean "successful" test results.
     * It just means that Test Automation Service has executed specified test
     * and test results are stored in the test issuer's workspace.
     *
     * @param test Finished test
     */
    @Override
    public void testFinished(Test test) {
        p("Notifying remote client about FINISHED test '" + test.getRuntimeId() + "'");

        test.setStatus(Test.Status.FINISHED, "");

        TestOperation testOperation = new TestOperation(TestOperation.Id.UPDATE, test);
        testOperation.setReceiver(clientHostname, clientPort);
        testOperation.setSender(serviceHostname, servicePort);

        handle(testOperation);

        removeTest(test);
    }

    /**
     * From TestAutomationServiceListener.
     *
     * Called when specified test has failed.
     *
     * @param test Failed test
     * @param reason Reason of test failure
     */
    @Override
    public void testFailed(Test test, String reason) {
        p("Notifying remote client about FAILED test '" + test.getRuntimeId() + "'");

        test.setStatus(Test.Status.FAILED, reason);

        TestOperation testOperation = new TestOperation(TestOperation.Id.UPDATE, test);
        testOperation.setReceiver(clientHostname, clientPort);
        testOperation.setSender(serviceHostname, servicePort);

        handle(testOperation);

        removeTest(test);
    }

    /**
     * From TestAutomationServiceListener.
     *
     * Called when Test Automation Service has a message to the listener regarding specified test.
     *
     * @param test Test under execution
     * @param message A message from Test Automation Service regarding specified test
     */
    @Override
    public void messageFromTestAutomationService(Test test, String message) {

        // Ensure a proper end of each message
        if (message != null && !message.isEmpty()) {

            // Always ensure that text is ending with the new line character
            if (!message.endsWith("\n")) {
                message += "\n";
            }

            // Inform remote client about the message from Test Automation Service
            TextMessage textMessage = new TextMessage(test, message);
            textMessage.setSender(serviceHostname, servicePort);
            textMessage.setReceiver(clientHostname, clientPort);

            handle(textMessage);
        }
    }

    /**
     * Resolves a file reading issue despites of network or computing environment configurations.
     *
     * @param directoryPath Path to a directory where file should be located
     * @param fileName Name of the file to be read
     * @return Input stream or null if file cannot be accessed
     */
    @Override
    public InputStream readFile(String directoryPath, String fileName) {
        p("readFile(" + directoryPath + ", " + fileName + ") is called, but not used on the side of TAS");
        return null;
    }

    /**
     * Resolves a file writing issues between Test Automation Client and Service.
     *
     * @param directoryPath Path to a directory where file should be located
     * @param fileName Name of the file to be written
     * @return Output stream to specified file or null if file cannot be accessed
     */
    @Override
    public OutputStream createFile(String directoryPath, String fileName) {
        p("createFile(" + directoryPath + ", " + fileName + ") is called, but not used on the side of TAS");
        return null;
    }

    /**
     * Print specified text on debugging output stream.
     *
     * @param text A text to be printed on debugging output stream
     */
    private void p(String text) {
        logger.log(Level.ALL, "Remote client at " + getClientHostnameAndPort() + ": " + text);
    }
}
