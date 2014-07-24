package com.nokia.ci.tas.client;

import java.io.OutputStream;

import java.net.InetAddress;
import java.net.Socket;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.Util;
import com.nokia.ci.tas.commons.message.Message;
import com.nokia.ci.tas.commons.message.TestOperation;

/**
 * Represents a remote Test Automation Service to a client.
 */
public class RemoteService extends Thread {

    /**
     * Hostname of the remote service.
     */
    private String serviceHostname;

    /**
     * Port number used by remote service for incoming messages.
     */
    private int servicePort;

    /**
     * A pool of messages to be send to remote service.
     */
    private ConcurrentLinkedQueue<Message> messagePool;

    /**
     * Variable which keeps this remote service instance running on the client side.
     */
    private boolean isRunning = true;

    /**
     * A moment of time when the last message was successfully send or received.
     */
    private long timeOfLastSuccessfulMessage = 0L;

    /**
     * Instance of currently running Test Automation Client.
     */
    private TestAutomationClient testAutomationClient;

    /**
     * Create a new instance of remote service.
     *
     * @param testAutomationClient Instance of the client of remote Test Automation Service
     * @param serviceHostname Hostname of remote Test Automation Service
     * @param servicePort Port number of remote Test Automation Service
     */
    public RemoteService(TestAutomationClient testAutomationClient,
                         String serviceHostname,
                         int servicePort) {

        super("RemoteTestAutomationService_" + serviceHostname + servicePort); // Use hostname and port as a name of the thread

        this.serviceHostname = serviceHostname;
        this.servicePort = servicePort;

        messagePool = new ConcurrentLinkedQueue();

        this.testAutomationClient = testAutomationClient;

        // Creation of remote service means that it is online
        timeOfLastSuccessfulMessage = System.currentTimeMillis();

        // Always work with minimal priority
        setPriority(MIN_PRIORITY);
    }

    /**
     * Returns a hostname associated with remote service.
     *
     * @return A hostname associated with remote service
     */
    public String getServiceHostname() {
        return serviceHostname;
    }

    /**
     * Returns a port number associated with remote service.
     *
     * @return A port number associated with remote service
     */
    public int getServicePort() {
        return servicePort;
    }

    /**
     * Returns a hostname and port of remote servie in canonical form "hostname:port".
     *
     * @return A hostname and port of remote service in canonical form
     */
    public String getServiceHostnameAndPort() {
        return serviceHostname + ":" + servicePort;
    }

    /**
     * Main routine of remote service.
     */
    @Override
    public void run() {
        p("Started working");

        while (isRunning) {
            try {
                if (!messagePool.isEmpty()) {
                    Message message = messagePool.poll(); // Always get the first message

                    if (message != null) {
                        p("Sending a message to Test Automation Service at " + serviceHostname + ":" + servicePort);

                        for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                            Socket socket = null;
                            OutputStream output = null;

                            try {
                                socket = new Socket(InetAddress.getByName(serviceHostname), servicePort);
                                output = socket.getOutputStream();

                                // Send message and close connection immediately
                                output.write(message.toXML().getBytes("UTF-8"));
                                output.flush();

                                output.close();
                                socket.close();

                                p("Message was successfully send");

                                timeOfLastSuccessfulMessage = System.currentTimeMillis();

                                // Message was successfully send
                                break; // Stop any other tries

                            } catch (Exception e) {
                                p("Got a problem during sending a message to Test Automation Service at " + serviceHostname + ":" + servicePort);

                                if (i >= (Constant.NUMBER_OF_RETRIES - 1)) {
                                    // This client has some serious network problems
                                    p("Has tried to send message for the maximal number of retries (" + Constant.NUMBER_OF_RETRIES + ")");
                                    p("The remote Test Automation Service at " + serviceHostname + ":" + servicePort + " most probably is not online anymore");

                                    testAutomationClient.handleRemoteServiceDisconnection(this);
                                    stopWorking();

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
                                        p("Got troubles while tried to close output stream to remote Test Automation Service at "
                                            + serviceHostname + ":" + servicePort
                                            + " - " + e.getClass() + " " + e.getMessage());
                                    }
                                }

                                // Always ensure that connection is closed
                                if (socket != null && !socket.isClosed()) {
                                    try {
                                        socket.close();
                                    } catch (Exception e) {
                                        p("Got troubles while tried to close a connection to remote Test Automation Service at "
                                            + serviceHostname + ":" + servicePort + " (" + socket.getInetAddress() + ") - "
                                            + e.getClass() + " - " + e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Check connection between remote client and Test Automation Service each 15 minutes
                    if ((System.currentTimeMillis() - timeOfLastSuccessfulMessage) > Constant.FIFTEEN_MINUTES) {
                        ConcurrentLinkedQueue<TestRegistry> executingTestRegistries = testAutomationClient.getTestRegistries();

                        if (!executingTestRegistries.isEmpty()) {
                            // Perform a simple check of connection
                            // If client is running a few simultaneous test on this Test Automation Service,
                            // a single connection check will be just enough
                            TestRegistry testRegistry = executingTestRegistries.peek(); // Get a copy of the first element

                            if (testRegistry != null) {
                                Test test = testRegistry.getTest();

                                for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                                    Socket socket = null;
                                    OutputStream output = null;

                                    try {
                                        TestOperation checkTestOperation = new TestOperation(TestOperation.Id.CHECK, test);
                                        checkTestOperation.setReceiver(serviceHostname, servicePort);
                                        checkTestOperation.setSender(testAutomationClient.getHostname(), testAutomationClient.getPort());

                                        // Send message
                                        socket = new Socket(InetAddress.getByName(checkTestOperation.getReceiverHostname()), checkTestOperation.getReceiverPort());
                                        output = socket.getOutputStream();

                                        output.write(checkTestOperation.toXML().getBytes("UTF-8"));
                                        output.flush();

                                        output.close();
                                        socket.close();

                                        // If we don't get any exceptions at this point, it means that Test Automation Service is still online
                                        p("Connection check between this Test Automation Client and remote Test Automation Service at " + serviceHostname + ":" + servicePort + " is OK");

                                        timeOfLastSuccessfulMessage = System.currentTimeMillis();

                                        break; // Stop any other retries

                                    } catch (Exception e) {
                                        p("Got a problem during checking connection between this client and remote Test Automation Service at "
                                            + serviceHostname + ":" + servicePort
                                            + " - " + e.getClass() + " " + e.getMessage());

                                        if (i >= (Constant.NUMBER_OF_RETRIES - 1)) {
                                            // This client has some serious network problems
                                            p("Has tried to check connection between this client and Test Automation Service at " + serviceHostname + ":" + servicePort
                                                + " for the maximal number of retries (" + Constant.NUMBER_OF_RETRIES + ")");
                                            p("The remote Test Automation Service at " + serviceHostname + ":" + servicePort + " most probably is not online anymore");

                                            testAutomationClient.handleRemoteServiceDisconnection(this);
                                            stopWorking();

                                            break;
                                        } else {
                                            p("Will try to check connection between this client and Test Automation Service at " + serviceHostname + ":" + servicePort
                                                + " once again after " + Util.convert(Constant.THIRTY_SECONDS));
                                            sleep(Constant.THIRTY_SECONDS);
                                        }
                                    } finally {
                                        // Always ensure that output stream is closed
                                        if (output != null) {
                                            try {
                                                output.close();
                                            } catch (Exception e) {
                                                p("Got troubles while tried to close output stream to remote Test Automation Service at "
                                                    + serviceHostname + ":" + servicePort
                                                    + " - " + e.getClass() + " " + e.getMessage());
                                            }
                                        }

                                        // Always ensure that connection is closed
                                        if (socket != null && !socket.isClosed()) {
                                            try {
                                                socket.close();
                                            } catch (Exception e) {
                                                p("Got troubles while tried to close a connection to remote Test Automation Service at "
                                                    + serviceHostname + ":" + servicePort + " (" + socket.getInetAddress() + "): "
                                                    + e.getClass() + " - " + e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            p("Remote client doesn't has any tests anymore. Stop working");
                            testAutomationClient.handleRemoteServiceDisconnection(this);
                            stopWorking();
                        }
                    }
                }

                sleep(Constant.MILLISECOND); // Wait for any updates
            }
            catch (InterruptedException e) {
                p("Having an interruption problem in remote Test Automation Service at " + serviceHostname + ":" + servicePort);
                testAutomationClient.handleRemoteServiceDisconnection(this);
                stopWorking();
            }
        }
    }

    /**
     * Will put specified message into the messaging pool
     * and will send it to remote Test Automation Service as soon as possible.
     *
     * @param message Message to be send
     */
    public synchronized void handle(Message message) {
        if (messagePool.add(message)) {
            //p("Will handle a message:\n" + message);
        } else {
            p("Error: Couldn't add a message for handling:\n" + message);
        }

        notify();
    }

    /**
     * Stops this instance of Remote Service.
     */
    public synchronized void stopWorking() {
        messagePool.clear();
        isRunning = false;

        notify();
    }

    /**
     * Print specified text on debugging output stream.
     *
     * @param text A text to be printed on debugging output stream
     */
    private void p(String text) {
        testAutomationClient.p(text);
    }
}
