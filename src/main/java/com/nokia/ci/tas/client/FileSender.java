package com.nokia.ci.tas.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.Socket;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.FileDescription;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.TestAutomationServiceListener;

import com.nokia.ci.tas.commons.message.FileOperation;

/**
 * Senders takes care about all outgoing file transfer messages.
 */
public class FileSender extends Thread {

    /**
     * A pool containing all the messages that sender will send.
     */
    private ConcurrentLinkedQueue<FileOperation> messagePool;

    /**
     * Variable what keeps sender running.
     */
    private boolean isRunning = true;

    /**
     * Instance of the Test Automation Client.
     */
    private TestAutomationClient testAutomationClient;

    /**
     * Constrcutor.
     */
    public FileSender(TestAutomationClient testAutomationClient) {

        super(); // Start as anonymous thread

        this.testAutomationClient = testAutomationClient;

        messagePool = new ConcurrentLinkedQueue();

        // Always work with minimal priority
        setPriority(MIN_PRIORITY);
    }

    /**
     * Sender's main routine.
     */
    @Override
    public void run() {
        p("Started working");

        while (isRunning) {
            try {
                if (!messagePool.isEmpty()) {
                    FileOperation message = messagePool.poll(); // Always get the first message

                    if (message != null) {
                        FileOperation fileTransfer = (FileOperation) message;
                        p("Processing outcoming file transfer:" + fileTransfer);

                        // Test Automation Clients may send file transfer messages only to remote Test Automation Communicators
                        TestRegistry testRegistry = testAutomationClient.getTestRegistry(fileTransfer.getTest().getId());

                        if (testRegistry != null) {
                            if (fileTransfer.getId() == FileOperation.Id.PUT) {
                                // Test Automation Client is sending a copy of specified file to the remote part
                                try {
                                    Test test = testRegistry.getTest();
                                    String testWorkspacePath = test.getWorkspacePath();
                                    FileDescription fileDescription = fileTransfer.getFileDescription();
                                    File file = new File(testWorkspacePath + System.getProperty("file.separator") + fileDescription.getFileName());

                                    boolean hadSuccessfulFileReply = false;
                                    BufferedInputStream fileData = null;
                                    InputStream inputStreamFromListener = null;
                                    long fileSize = FileDescription.UNKNOWN_FILE_SIZE;

                                    // Always try to get input stream to specified file from the test listener,
                                    // and use file objects only if listener didn't helped with file access

                                    TestAutomationServiceListener testListener = testRegistry.getListener();

                                    if (testListener != null) {
                                        p("Trying to read file " + fileDescription.getFileName() + " with help from the listener...");
                                        inputStreamFromListener = testListener.readFile(testWorkspacePath, fileDescription.getFileName());
                                    }

                                    if (inputStreamFromListener != null) {
                                        fileData = new BufferedInputStream(inputStreamFromListener);
                                        p("Listener has helped with resolving a path to file " + fileDescription.getFileName());
                                    } else {
                                        p("Listener couldn't help with resolving a path to file " + fileDescription.getFileName()
                                                + " Trying to solve this problem with plain file streams");

                                        if (file.exists() && file.canRead()) {
                                            try {
                                                fileData = new BufferedInputStream(new FileInputStream(file));
                                                fileSize = file.length();
                                                if (fileDescription.getFileSize() != fileSize) {
                                                    fileDescription.setFileSize(fileSize);
                                                }
                                                p("File " + file.getName() + " does exists and can be send back as REPLY, file is " + fileSize + " bytes in length");
                                            } catch (Exception e) {
                                                p("Test Automation Client couldn't resolve access issues to file " + fileDescription.getFileName());
                                                fileData = null;
                                            }
                                        }
                                    }

                                    if (fileData != null) {
                                        // Send file transfer reply together with file data
                                        for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                                            p("Trying to send file " + fileDescription.getFileName() + " to remote part at "
                                                    + fileTransfer.getReceiverHostname() + ":" + fileTransfer.getReceiverPort()
                                                    + " regarding the test '" + fileTransfer.getTest().getRuntimeId() + "'");

                                            Socket socket = null;
                                            OutputStream output = null;

                                            try {
                                                socket = new Socket(InetAddress.getByName(fileTransfer.getReceiverHostname()), fileTransfer.getReceiverPort());
                                                output = socket.getOutputStream();

                                                // Send file transfer message first
                                                output.write(fileTransfer.toXML().getBytes("UTF-8"));
                                                output.flush();

                                                // Send file data after
                                                p("File Transfer message was successfully send, now should also send file's " + fileDescription.getFileName() + " data...");

                                                // The send all bytes that file has
                                                int numberOfBytesInBuffer = 0;
                                                long numberOfActuallySendBytes = 0L;
                                                byte[] buffer = new byte[Constant.DEFAULT_BUFFER_SIZE];

                                                long fileTransferStartedAt = System.currentTimeMillis();

                                                do {
                                                    numberOfBytesInBuffer = fileData.read(buffer);

                                                    if (numberOfBytesInBuffer > 0) {
                                                        output.write(buffer, 0, numberOfBytesInBuffer);
                                                        output.flush();
                                                        numberOfActuallySendBytes += numberOfBytesInBuffer;
                                                    } else {
                                                        // Safety against possible deadlocks in read()
                                                        break;
                                                    }
                                                } while (numberOfBytesInBuffer > 0);

                                                output.flush();

                                                output.close();
                                                socket.close();

                                                fileData.close();

                                                // Just show a nice message about bytes and time of transfer
                                                long time = System.currentTimeMillis() - fileTransferStartedAt;
                                                time /= 1000L; // Turn milliseconds into seconds

                                                String size = "";

                                                if (numberOfActuallySendBytes > 1048576) { // Turn bytes into megabytes
                                                    size = "" + (numberOfActuallySendBytes / 1048576L ) + " MB";
                                                } else if (numberOfActuallySendBytes > 1024) { // Turn bytes into kilobytes
                                                    size = "" + (numberOfActuallySendBytes / 1024L ) + " KB";
                                                } else {
                                                    size = "" + numberOfActuallySendBytes + " bytes";
                                                }

                                                p("Has send " + numberOfActuallySendBytes + " bytes (" + size + ") of file's " + fileDescription.getFileName()
                                                    + " data in about " + time + " seconds");

                                                hadSuccessfulFileReply = true;

                                                break; // Stop any other trials

                                            } catch (Exception e) {
                                                p("Got troubles while tried to send a file " + fileDescription.getFileName()
                                                        + " regarding the test '" + test.getId() + "': "
                                                        + e.getClass() + " " + e.getMessage());

                                                p("Will attempt to re-send file transfer reply for " + (Constant.NUMBER_OF_RETRIES - i) + " more times...");
                                            } finally {
                                                // Always ensure that output stream is closed
                                                if (output != null) {
                                                    try {
                                                        output.close();
                                                    } catch (Exception e) {
                                                        p("Got troubles while tried to close output stream to "
                                                            + fileTransfer.getReceiverHostname() + ":" + fileTransfer.getReceiverPort()
                                                            + " regarding the test '" + test.getId() + "': "
                                                            + e.getClass() + " " + e.getMessage());
                                                    }
                                                }

                                                // Always ensure that socket is closed
                                                if (socket != null && !socket.isClosed()) {
                                                    try {
                                                        socket.close();
                                                    } catch (Exception e) {
                                                        p("Got troubles while tried to close a connection to "
                                                            + socket.getInetAddress().getHostName() + ":" + socket.getPort()
                                                            + " regarding the test '" + test.getId() + "': "
                                                            + e.getClass() + " " + e.getMessage());
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (!hadSuccessfulFileReply) {
                                        // Send a notification reply about unsuccessful file transfer
                                        FileOperation fileTransferReply = new FileOperation(FileOperation.Id.ABORT, fileTransfer.getTest(), fileDescription);
                                        fileTransferReply.setSender(testAutomationClient.getHostname(), testAutomationClient.getPort());
                                        fileTransferReply.setReceiver(fileTransfer.getReceiverHostname(), fileTransfer.getReceiverPort());

                                        for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                                            p("Trying to send notification about unresolvable file transfer request back to "
                                                    + fileTransfer.getReceiverHostname() + ":" + fileTransfer.getReceiverPort()
                                                    + " regarding the test '" + test.getId() + "'");

                                            Socket socket = null;
                                            OutputStream output = null;

                                            try {
                                                socket = new Socket(InetAddress.getByName(fileTransfer.getReceiverHostname()), fileTransfer.getReceiverPort());
                                                output = socket.getOutputStream();

                                                // Send file transfer reply
                                                output.write(fileTransferReply.toXML().getBytes("UTF-8"));
                                                output.flush();

                                                output.close();
                                                socket.close();

                                                // Send file data
                                                p("File Transfer message was successfully send");

                                                hadSuccessfulFileReply = true;

                                                break; // Stop any other trials

                                            } catch (Exception e) {
                                                p("Got troubles while tried to send a notification about unresolvable send of file " + fileDescription.getFileName()
                                                        + " regarding the test '" + test.getId() + "': "
                                                        + e.getClass() + " " + e.getMessage());

                                                p("Will attempt to re-send notification for " + (Constant.NUMBER_OF_RETRIES - i) + " more times...");
                                            } finally {
                                                // Always ensure that output stream is closed
                                                if (output != null) {
                                                    try {
                                                        output.close();
                                                    } catch (Exception e) {
                                                        p("Got troubles while tried to close output stream to "
                                                            + fileTransfer.getReceiverHostname() + ":" + fileTransfer.getReceiverPort()
                                                            + " regarding the test '" + test.getId() + "': "
                                                            + e.getClass() + " " + e.getMessage());
                                                    }
                                                }

                                                // Always ensure that socket is closed
                                                if (socket != null && !socket.isClosed()) {
                                                    try {
                                                        socket.close();
                                                    } catch (Exception e) {
                                                        p("Got troubles while tried to close a connection to "
                                                            + socket.getInetAddress().getHostName() + ":" + socket.getPort()
                                                            + " regarding the test '" + test.getId() + "': "
                                                            + e.getClass() + " " + e.getMessage());
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Perform all possible cleanups

                                    if (fileData != null) {
                                        try {
                                            fileData.close();
                                        } catch (Exception e) {
                                            // Ignore
                                        }
                                    }

                                    if (inputStreamFromListener != null) {
                                        try {
                                            inputStreamFromListener.close();
                                        } catch (Exception e) {
                                            // Ignore
                                        }
                                    }

                                    // Make a final decision about networking failures
                                    if (!hadSuccessfulFileReply) {
                                        // Something went terribly wrong, so send a Test Stop message to the Test Automation Service
                                        for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                                            p("Got networking issues and cannot continue the test '" + test.getId() + "'. Test will be stopped");

                                            try {
                                                testAutomationClient.stopTest(testRegistry.getTest(),
                                                                              testRegistry.getRemoteServiceHostname(),
                                                                              testRegistry.getRemoteServicePort(),
                                                                              testRegistry.getListener());
                                                p("Successfully issued a request to stop the test '" + test.getId() + "'");
                                                break; // Stop any other trials

                                            } catch (Exception e) {
                                                p("Got troubles while tried to issue a request to stop the test '" + test.getId() + "': "
                                                        + e.getClass() + " " + e.getMessage());
                                                p("Will attempt to issue request for " + (Constant.NUMBER_OF_RETRIES - i) + " more times...");
                                            }
                                        }
                                    }

                                } catch (Exception e) {
                                    p("Got troubles while tried to process outcoming file transfer message for file " + fileTransfer.getFileDescription().getFileName()
                                            + " regarding the test '" + fileTransfer.getTest().getId() + "': "
                                            + e.getClass() + " " + e.getMessage());
                                }
                           } else {
                                // All other file transfers messages are simply forwarded to remote part
                                for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {

                                    Socket socket = null;
                                    OutputStream output = null;

                                    try {
                                        socket = new Socket(InetAddress.getByName(fileTransfer.getReceiverHostname()), fileTransfer.getReceiverPort());
                                        output = socket.getOutputStream();

                                        // Send message and close connection immediately
                                        output.write(fileTransfer.toXML().getBytes("UTF-8"));
                                        output.flush();

                                        output.close();
                                        socket.close();

                                        p("File Transfer message was successfully send");
                                        break; // Stop any other trials

                                    } catch (Exception e) {
                                        p("Got troubles while tried to process outcoming file transfer message to remote part at "
                                                + fileTransfer.getReceiverHostname() + ":" + fileTransfer.getReceiverPort()
                                                + " regarding the test '" + fileTransfer.getTest().getId() + "' "
                                                + " and file " + fileTransfer.getFileDescription().getFileName()
                                                + ": " + e.getClass() + " " + e.getMessage());
                                        p("Will attempt to re-send notification for " + (Constant.NUMBER_OF_RETRIES - i) + " more times...");
                                    } finally {
                                        // Always ensure that output stream is closed
                                        if (output != null) {
                                            try {
                                                output.close();
                                            } catch (Exception e) {
                                                p("Got troubles while tried to close output stream to "
                                                    + fileTransfer.getReceiverHostname() + ":" + fileTransfer.getReceiverPort()
                                                    + " regarding the test '" + fileTransfer.getTest().getId() + "': "
                                                    + e.getClass() + " " + e.getMessage());
                                            }
                                        }

                                        // Always ensure that socket is closed
                                        if (socket != null && !socket.isClosed()) {
                                            try {
                                                socket.close();
                                            } catch (Exception e) {
                                                p("Got troubles while tried to close a connection to "
                                                    + socket.getInetAddress().getHostName() + ":" + socket.getPort()
                                                    + " regarding the test '" + fileTransfer.getTest().getId() + "': "
                                                    + e.getClass() + " " + e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Reply with a notification about invalid file transfer

                            // Prevent sending replies to itself
                            if (!fileTransfer.getSenderHostname().equalsIgnoreCase(testAutomationClient.getHostname())
                                && fileTransfer.getSenderPort() != testAutomationClient.getPort()) {

                                p("Test " + fileTransfer.getTest().getId() + " is not handled by this Test Automation Client");

                                FileOperation fileTransferReply = fileTransfer;

                                fileTransferReply.setId(FileOperation.Id.ABORT);
                                fileTransferReply.setSender(testAutomationClient.getHostname(), testAutomationClient.getPort());
                                fileTransferReply.setReceiver(fileTransfer.getSenderHostname(), fileTransfer.getSenderPort());

                                handle(fileTransferReply);

                                p("A reply about this issue is send back to remote part at " + fileTransfer.getSenderHostname() + ":" + fileTransfer.getSenderPort());
                            }
                        }
                    }
                }

                sleep(Constant.MILLISECOND); // Wait for any updates
            }
            catch (InterruptedException e) {
                p("Sender was interrupted. Stop working");
                isRunning = false;
            }
        }
    }

    /**
     * Handles specified message.
     *
     * @param message Message to be handled
     */
    public synchronized void handle(FileOperation message) {
        if (messagePool.add(message)) {
            //p("Will handle a message:\n" + message);
        } else {
            p("Error: Couldn't add a message for handling:\n" + message);
        }
        notify();
    }

    /**
     * Stops sender running.
     */
    public synchronized void stopWorking() {
        isRunning = false;
    }

    /**
     * Prints specified text to output stream.
     *
     * @param text A text to be printed to output stream
     */
    private void p(String text) {
        if (testAutomationClient != null) {
            testAutomationClient.p("File sender: " + text);
        }
    }
}
