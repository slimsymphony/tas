package com.nokia.ci.tas.communicator;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.Socket;

import java.util.List;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.FileDescription;
import com.nokia.ci.tas.commons.TestNodeDescription;

import com.nokia.ci.tas.commons.Util;
import com.nokia.ci.tas.commons.message.FileOperation;
import com.nokia.ci.tas.commons.message.Message;
import com.nokia.ci.tas.commons.message.RegistryOperation;
import com.nokia.ci.tas.commons.message.TextMessage;

/**
 * Senders takes care about all outgoing messages send to remote Test Automation Service.
 * In addition to its message sending functionality the sender
 * also automatically performs checks of connection between this Test Automation Client
 * and remote Test Automation Service. If sender haven't send any messags during some time,
 * it will try to send a registration and product status messages to remote Test Automation Service
 * just to check that connection is still fine.
 */
public class Sender extends Thread {
    /**
     * A pool containing all the messages that sender will send.
     */
    private ConcurrentLinkedQueue<Message> messagePool;

    /**
     * Variable what keeps sender running.
     */
    private boolean isRunning = true;

    /**
     * The last moment of time when this Communicator send a registration message to Test Automation Service.
     */
    private long timeOfLastRegistration = 0L;

    /**
     * Instance of the Test Automation Communicator.
     */
    private TestAutomationCommunicator testAutomationCommunicator;

    /**
     * Hostname of the Test Automation Service.
     */
    private String testAutomationServiceHostname;

    /**
     * Port number of the Test Automation Service.
     */
    private int testAutomationServicePort;

    /**
     * Hostname of the Test Automation Communicator.
     */
    private String testAutomationCommunicatorHostname;

    /**
     * Port number of the Test Automation Communicator.
     */
    private int testAutomationCommunicatorPort;

    /**
     * Instance of the file caching utility.
     */
    private FileCache fileCache;

    /**
     * File separator used by hosting system.
     */
    private String fileSeparator;

    /**
     * Instance of the Test Automation Communicator's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationCommunicator.GLOBAL_LOGGER_NAME);

    private boolean canSendFileRequests = true;
    private long timeOfLastFileRequest = 0L;

    /**
     * Constrcutor.
     *
     * @param fileCache Instance of the file caching module
     */
    public Sender(FileCache fileCache) {
        super(); // Start as anonymous thread

        this.fileCache = fileCache;

        messagePool = new ConcurrentLinkedQueue();

        testAutomationCommunicator = TestAutomationCommunicator.getInstance();

        // Extract the following parameters to eliminate unnecessary method calls
        testAutomationServiceHostname = testAutomationCommunicator.getTestAutomationServiceHostname();
        testAutomationServicePort = testAutomationCommunicator.getTestAutomationServicePort();
        testAutomationCommunicatorHostname = testAutomationCommunicator.getHostname();
        testAutomationCommunicatorPort = testAutomationCommunicator.getPort();
        fileSeparator = testAutomationCommunicator.getFileSeparator();

        timeOfLastFileRequest = System.currentTimeMillis();

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
                    Message message = messagePool.poll(); // Always get the first message

                    if (message != null) {
                        p("Sending a message of type " + message.getType() + " to " + message.getReceiverHostname() + ":" + message.getReceiverPort());

                        if (message instanceof FileOperation) {
                            FileOperation fileOperation = (FileOperation) message;
                            TestExecutor testExecutor = testAutomationCommunicator.getTestExecutor(fileOperation.getTest().getRuntimeId());

                            p("Processing outcoming file operation:\n" + fileOperation);

                            if (testExecutor != null) {
                                if (fileOperation.getId() == FileOperation.Id.PUT) {
                                    // Test Automation Communicator is trying to send a copy of specified file to the remote part
                                    FileDescription fileDescription = fileOperation.getFileDescription();

                                    boolean isSuccess = false;
                                    String reasonOfFailure = Constant.UNSPECIFIED_REASON_OF_FAILURE;

                                    for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                                        Socket socket = null;
                                        OutputStream output = null;
                                        FileInputStream fileInputStream = null;

                                        try {
                                            File file = new File(testExecutor.getTestWorkspace().getAbsolutePath() + fileSeparator + fileDescription.getFileName());

                                            if (file.exists() && file.canRead()) {
                                                p("File " + file.getAbsolutePath() + " does exists and can be send");

                                                fileInputStream = new FileInputStream(file);

                                                socket = new Socket(InetAddress.getByName(fileOperation.getReceiverHostname()), fileOperation.getReceiverPort());
                                                output = socket.getOutputStream();

                                                // First send the file transfer message itself
                                                output.write(fileOperation.toXML().getBytes("UTF-8"));
                                                output.flush();
                                                p("File Transfer message was successfully send, now should also send " + fileDescription.getFileSize() + " bytes of file data...");

                                                // The send all bytes that file has
                                                long numberOfBytesToSend = fileDescription.getFileSize();
                                                int numberOfBytesInBuffer = 0;
                                                long numberOfActuallySendBytes = 0L;
                                                byte[] buffer = new byte[Constant.DEFAULT_BUFFER_SIZE];

                                                long fileTransferStartedAt = System.currentTimeMillis();

                                                do {
                                                    numberOfBytesInBuffer = fileInputStream.read(buffer);

                                                    if (numberOfBytesInBuffer > 0) {
                                                        output.write(buffer, 0, numberOfBytesInBuffer);
                                                        output.flush();

                                                        numberOfBytesToSend -= numberOfBytesInBuffer;
                                                        numberOfActuallySendBytes += numberOfBytesInBuffer;
                                                    } else {
                                                        // Safety against possible deadlocks in read()
                                                        break;
                                                    }
                                                } while (numberOfBytesToSend > 0);

                                                output.flush();

                                                output.close();
                                                socket.close();
                                                fileInputStream.close();

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

                                                //p("Has send " + size + " of file's " + fileDescription.getFileName() + " data over network in about " + time + " seconds");
                                                p("Has send " + size + " of file's " + fileDescription.getFileName() + " data over network in about " + time + " seconds");

                                                // Check if number of send bytes equals the number of bytes mentioned in the file transfer message
                                                if (file.length() == numberOfActuallySendBytes) {
                                                    p("File " + fileDescription.getFileName() + " was successfully transferred over network");
                                                    isSuccess = true;
                                                } else {
                                                    reasonOfFailure = "Failed to send file over the network: "
                                                            + fileDescription.getFileSize() + " bytes were supposed to be transferred, but has managed to send only "
                                                            + numberOfActuallySendBytes + " bytes. The file itself actually contained " + file.length() + " bytes";
                                                }
                                            } else {
                                                reasonOfFailure = "Specified file " + file.getAbsolutePath() + " either is not existing or cannot be accessed";
                                            }
                                        } catch (Exception e) {
                                            reasonOfFailure = "Got troubles while tried to send a file to remote part at "
                                                + fileOperation.getReceiverHostname() + ":" + fileOperation.getReceiverPort()
                                                + " - " + e.getClass() + ": " + e.getMessage();
                                        } finally {
                                            // Always ensure that file input stream is closed
                                            if (fileInputStream != null) {
                                                try {
                                                    fileInputStream.close();
                                                } catch (Exception e) {
                                                    p("Got troubles while tried to close file input stream: " + e.getClass() + " " + e.getMessage());
                                                }
                                            }

                                            // Always ensure that output stream is closed
                                            if (output != null) {
                                                try {
                                                    output.close();
                                                } catch (Exception e) {
                                                    p("Got troubles while tried to close output stream to "
                                                        + fileOperation.getReceiverHostname() + ":" + fileOperation.getReceiverPort()
                                                        + " - " + e.getClass() + " " + e.getMessage());
                                                }
                                            }

                                            // Always ensure that connection is closed
                                            if (socket != null && !socket.isClosed()) {
                                                try {
                                                    socket.close();
                                                } catch (Exception e) {
                                                    p("Got troubles during closing outcoming connection to "
                                                        + socket.getInetAddress().getHostName() + ":" + socket.getPort()
                                                        + " - " + e.getClass() + " - " + e.getMessage());
                                                    e.printStackTrace();
                                                }
                                            }
                                        }

                                        if (isSuccess) {
                                            break; // Stop any other tries
                                        } else {
                                            p("Got a failure on attempt #" + (i + 1) + ": " + reasonOfFailure);
                                            continue; // For preventing any chances of a deadlock
                                        }
                                    }

                                    // Notify test executor about successful or failed file transfer
                                    if (isSuccess) {
                                        testExecutor.fileSend(fileDescription.getFileName(), true);
                                    } else {
                                        // Notify test executor about failed file transfer
                                        testExecutor.fileSend(fileDescription.getFileName(), false);
                                    }
                                } else {
                                    // All the other file transfers messages are simply forwarded to remote part
                                    boolean isSuccess = false;
                                    String reasonOfFailure = Constant.UNSPECIFIED_REASON_OF_FAILURE;

                                    for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                                        Socket socket = null;
                                        OutputStream output = null;

                                        try {
                                            p("Trying to send a file transfer message to remote part at " + fileOperation.getReceiverHostname() + ":" + fileOperation.getReceiverPort());

                                            socket = new Socket(InetAddress.getByName(fileOperation.getReceiverHostname()), fileOperation.getReceiverPort());
                                            output = socket.getOutputStream();

                                            p("Connection to remote part at " + fileOperation.getReceiverHostname() + ":" + fileOperation.getReceiverPort() + " is OK");

                                            output.write(fileOperation.toXML().getBytes("UTF-8"));
                                            output.flush();

                                            p("File transfer message was successfully send");

                                            output.close();
                                            socket.close();

                                            p("File transfer message handling is over");

                                            isSuccess = true;
                                            canSendFileRequests = false; // Will be de-blocked after reply
                                            timeOfLastFileRequest = System.currentTimeMillis();

                                        } catch (Exception e) {
                                            reasonOfFailure = "Got troubles while tried to send a file transfer message to remote part at "
                                                + fileOperation.getReceiverHostname() + ":" + fileOperation.getReceiverPort()
                                                + " - " + e.getClass() + ": " + e.getMessage();
                                        } finally {
                                            // Always ensure that output stream is closed
                                            if (output != null) {
                                                try {
                                                    output.close();
                                                } catch (Exception e) {
                                                    p("Got troubles while tried to close output stream to "
                                                        + fileOperation.getReceiverHostname() + ":" + fileOperation.getReceiverPort()
                                                        + " - " + e.getClass() + " " + e.getMessage());
                                                }
                                            }

                                            // Always ensure that connection is closed
                                            if (socket != null && !socket.isClosed()) {
                                                try {
                                                    socket.close();
                                                } catch (Exception e) {
                                                    p("Got troubles during closing outcoming connection to "
                                                        + socket.getInetAddress().getHostName() + ":" + socket.getPort()
                                                        + " - " + e.getClass() + " - " + e.getMessage());
                                                    e.printStackTrace();
                                                }
                                            }
                                        }

                                        if (isSuccess) {
                                            break; // Stop any other tries
                                        } else {
                                            p("Got a failure on attempt #" + (i + 1) + ": " + reasonOfFailure);
                                            continue; // For preventing any chances of a deadlock
                                        }
                                    }
                                }
                            } else {
                                p("Couln't get executor of test '" + fileOperation.getTest().getRuntimeId() + "' The message handling will be skipped");
                            }
                        } else if (message instanceof TextMessage) {
                            // Text messages are only of informative nature, so there is no need to resend them if sending has failed
                            TextMessage textMessage = (TextMessage) message;

                            Socket socket = null;
                            OutputStream outputStream = null;

                            try {
                                socket = new Socket(textMessage.getReceiverHostname(), textMessage.getReceiverPort());
                                outputStream = socket.getOutputStream();

                                // Send message
                                outputStream.write(textMessage.toXML().getBytes("UTF-8"));
                                outputStream.flush();

                                outputStream.close();
                                socket.close();

                            } catch (Exception e) {
                                p("Got troubles during processing a text message: " + message, e);
                            } finally {
                                // Always ensure that output stream is closed
                                if (outputStream != null) {
                                    try {
                                        outputStream.close();
                                    } catch (Exception e) {
                                        p("Got troubles while tried to close output stream to "
                                            + textMessage.getReceiverHostname() + ":" + textMessage.getReceiverPort()
                                            + " - " + e.getClass() + " " + e.getMessage());
                                    }
                                }

                                // Always ensure that connection is closed
                                if (socket != null && !socket.isClosed()) {
                                    try {
                                        socket.close();
                                    } catch (Exception e) {
                                        p("Got troubles during closing outcoming connection to "
                                            + socket.getInetAddress().getHostName() + ":" + socket.getPort()
                                            + " - " + e.getClass() + " - " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } else {
                            // Send other types of messages
                            boolean isSuccess = false;
                            String reasonOfFailure = Constant.UNSPECIFIED_REASON_OF_FAILURE;

                            for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                                Socket socket = null;
                                OutputStream output = null;

                                try {
                                    socket = new Socket(InetAddress.getByName(message.getReceiverHostname()), message.getReceiverPort());
                                    output = socket.getOutputStream();

                                    // Send message
                                    output.write(message.toXML().getBytes("UTF-8"));
                                    output.flush();

                                    output.close();
                                    socket.close();

                                    isSuccess = true;

                                } catch (Exception e) {
                                    p("Having troubles during processing a message:" + message, e);
                                    reasonOfFailure = "Got troubles while tried to send a message to remote part at "
                                        + message.getReceiverHostname() + ":" + message.getReceiverPort()
                                        + " - " + e.getClass() + ": " + e.getMessage();
                                } finally {
                                    // Always ensure that output stream is closed
                                    if (output != null) {
                                        try {
                                            output.close();
                                        } catch (Exception e) {
                                            p("Got troubles while tried to close output stream to "
                                                + message.getReceiverHostname() + ":" + message.getReceiverPort()
                                                + " - " + e.getClass() + " " + e.getMessage());
                                        }
                                    }

                                    // Always ensure that connection is closed
                                    if (socket != null && !socket.isClosed()) {
                                        try {
                                            socket.close();
                                        } catch (Exception e) {
                                            p("Got troubles during closing outcoming connection to "
                                                + socket.getInetAddress().getHostName() + ":" + socket.getPort()
                                                + " - " + e.getClass() + " - " + e.getMessage());
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                if (isSuccess) {
                                    break; // Stop any other tries
                                } else {
                                    p("Got a failure on attempt #" + (i + 1) + ": " + reasonOfFailure);
                                    continue; // For preventing any chances of a deadlock
                                }
                            }
                        }
                    }
                }

                // Process outcoming file requests
                if (canSendFileRequests && fileCache.hasUnrequestedEntries()) {
                    List<FileCacheEntry> fileCacheEntries = fileCache.getUnrequestedEntries();

                    for (FileCacheEntry fileCacheEntry : fileCacheEntries) {
                        p("Processing unrequested entry from the file cache...");

                        FileOperation fileTransferRequest = fileCacheEntry.getFileTransferRequest();

                        p("Processing a file transfer request:\n" + fileTransferRequest);

                        // All the other file transfers messages are simply forwarded to remote part
                        boolean isSuccess = false;
                        String reasonOfFailure = Constant.UNSPECIFIED_REASON_OF_FAILURE;

                        for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                            Socket socket = null;
                            OutputStream output = null;

                            try {
                                p("Trying to send a file transfer message to remote part at " + fileTransferRequest.getReceiverHostname() + ":" + fileTransferRequest.getReceiverPort());

                                socket = new Socket(InetAddress.getByName(fileTransferRequest.getReceiverHostname()), fileTransferRequest.getReceiverPort());
                                output = socket.getOutputStream();

                                p("Connection to remote part at " + fileTransferRequest.getReceiverHostname() + ":" + fileTransferRequest.getReceiverPort() + " is OK");

                                output.write(fileTransferRequest.toXML().getBytes("UTF-8"));
                                output.flush();

                                p("File transfer message was successfully send");

                                output.close();
                                socket.close();

                                p("File transfer message handling is over");

                                // Update file cache entry
                                fileCacheEntry.setIsRequested(true);
                                fileCacheEntry.setTimeWhenRequested(System.currentTimeMillis());

                                fileCache.update(fileCacheEntry);

                                isSuccess = true;
                                canSendFileRequests = false;
                                timeOfLastFileRequest = System.currentTimeMillis();

                            } catch (Exception e) {
                                reasonOfFailure = "Got troubles while tried to send a file transfer message to remote part at "
                                    + fileTransferRequest.getReceiverHostname() + ":" + fileTransferRequest.getReceiverPort()
                                    + " - " + e.getClass() + ": " + e.getMessage();
                            } finally {
                                // Always ensure that output stream is closed
                                if (output != null) {
                                    try {
                                        output.close();
                                    } catch (Exception e) {
                                        p("Got troubles while tried to close output stream to "
                                            + fileTransferRequest.getReceiverHostname() + ":" + fileTransferRequest.getReceiverPort()
                                            + " - " + e.getClass() + " " + e.getMessage());
                                    }
                                }

                                // Always ensure that connection is closed
                                if (socket != null && !socket.isClosed()) {
                                    try {
                                        socket.close();
                                    } catch (Exception e) {
                                        p("Got troubles during closing outcoming connection to "
                                            + socket.getInetAddress().getHostName() + ":" + socket.getPort()
                                            + " - " + e.getClass() + " - " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            }

                            if (isSuccess) {
                                break; // Stop any other tries
                            } else {
                                p("Got a failure on attempt #" + (i + 1) + ": " + reasonOfFailure);
                                continue; // For preventing any chances of a deadlock
                            }
                        }

                        // At least one file request was successfully send
                        if (!canSendFileRequests) {
                            p("Stop processing unrequested entries from the file cache...");
                            break;
                        }
                    }

                    // If we have some unrequested file requests and none of them has been successfully send, postpond new file request retries for some time
                    if (canSendFileRequests && fileCache.hasUnrequestedEntries()) {
                        p("None of the file requests was proceeded successfully. Postponding new file requests for at least " + Util.convert(Constant.FIVE_MINUTES));
                        canSendFileRequests = false;
                        timeOfLastFileRequest = System.currentTimeMillis();
                    }
                }

                // Ensure that we don't have any deadlocks with file transfer requests
                if (!canSendFileRequests && ((System.currentTimeMillis() - timeOfLastFileRequest) > Constant.FIVE_MINUTES)) {
                    canSendFileRequests = true;
                    timeOfLastFileRequest = System.currentTimeMillis();
                    p("Allowing file transfer requests again, since the last file transfer was more than " + Util.convert(Constant.FIVE_MINUTES) + " ago");
                }

                // Send test node registry update each minute, no matter what the communication state is
                if ((System.currentTimeMillis() - timeOfLastRegistration) > Constant.ONE_MINUTE) {
                    p("Adding a registry operation into the messaging pool...");

                    timeOfLastRegistration = System.currentTimeMillis();

                    TestNodeDescription testNodeDescription = testAutomationCommunicator.getTestNodeDescription();

                    RegistryOperation registryOperation = new RegistryOperation(RegistryOperation.Id.UPDATE, RegistryOperation.Remote.TEST_NODE);
                    registryOperation.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);
                    registryOperation.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
                    registryOperation.setTestNodeDescription(testNodeDescription);

                    handle(registryOperation);
                }

                sleep(Constant.MILLISECOND); // Wait for any updates

            } catch (Exception e) {
                isRunning = false;
                p("Having troubles during work", e);
            }
        }

        p("Ended work");
    }

    /**
     * Allows sender be send outcoming file transfer requests once again.
     */
    public void enableFileTransfers() {
        canSendFileRequests = true;
        timeOfLastFileRequest = System.currentTimeMillis();
        p("File transfers are enabled again");
    }

    /**
     * Handles specified message.
     *
     * @param message Message to be handled
     */
    public synchronized void handle(Message message) {
        if (messagePool.add(message)) {
            //p("Got a message to handle:\n" + message);
        } else {
            p("Error: Couldn't add a message for handling:\n" + message);
        }

        notify();
    }

    /**
     * Performs all necessary cleanups and shutdowns this Sender.
     */
    protected synchronized void shutdown() {
        p("Got a request to shutdown...");
        isRunning = false;
    }

    /**
     * Prints specified text to output stream.
     *
     * @param text A text to be printed to output stream
     */
    private void p(String text) {
        logger.log(Level.ALL, "Sender: " + text);
    }

    /**
     * Prints specified text and occured exception to output stream.
     *
     * @param text A text to be printed to output stream
     * @param exception An occured exception
     */
    private void p(String text, Exception exception) {
        logger.log(Level.ALL, "Sender: " + text + " - " + exception.getClass() + ": " + exception.getMessage(), exception);
    }
}
