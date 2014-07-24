package com.nokia.ci.tas.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Converter;
import com.nokia.ci.tas.commons.FileDescription;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.TestAutomationServiceListener;

import com.nokia.ci.tas.commons.message.FileOperation;
import com.nokia.ci.tas.commons.message.TestOperation;
import com.nokia.ci.tas.commons.message.TextMessage;

/**
 * Handles and dispatches all messages received by the instance of Testing Automation Client.
 */
public class Receiver extends Thread {

    /**
     * Pool of all received connections to be handled.
     */
    private ConcurrentLinkedQueue<Socket> socketPool;

    /**
     * Variable which keeps receiver running.
     */
    private boolean isRunning = true;

    /**
     * Current instance of the Test Automation Client.
     */
    private TestAutomationClient testAutomationClient;

    /**
     * Local copy of file sender.
     */
    private FileSender fileSender;

    /**
     * Local copy of client's hostname.
     */
    private String testAutomationClientHostname;

    /**
     * Local copy of client's port number.
     */
    private int testAutomationClientPort;

    /**
     * Converter of input XML streams.
     */
    private Converter converter;

    /**
     * Default constructor.
     */
    public Receiver(TestAutomationClient testAutomationClient) {

        super(); // Start as anonymous thread
        socketPool = new ConcurrentLinkedQueue();
        this.testAutomationClient = testAutomationClient;
        testAutomationClientHostname = testAutomationClient.getHostname();
        testAutomationClientPort = testAutomationClient.getPort();
        fileSender = testAutomationClient.getFileSender();
        converter = new Converter();

        // Always work with minimal priority
        setPriority(MIN_PRIORITY);
    }

    /**
     * Receiver's main routine.
     */
    @Override
    public void run() {
        p("Started working");

        while (isRunning) {
            try {
                if (!socketPool.isEmpty()) {
                    Socket connection = socketPool.poll(); // Always get the first connection

                    if (connection != null) {
                        InputStream inputStream = null;

                        try {
                            inputStream = connection.getInputStream();
                            Object message = converter.handle(inputStream);

                            if (message != null) {
                                if (message instanceof FileOperation) {
                                    FileOperation fileTransfer = (FileOperation) message;
                                    p("Processing incoming file transfer:" + fileTransfer);

                                    // Test Automation Clients may receive file transfer messages only from remote Test Automation Communicators
                                    TestRegistry testRegistry = testAutomationClient.getTestRegistry(fileTransfer.getTest().getId());

                                    if (testRegistry != null) {
                                        String senderHostname = fileTransfer.getSenderHostname();
                                        int senderPort = fileTransfer.getSenderPort();

                                        if (senderHostname != null && !senderHostname.isEmpty() && senderPort > 0) {
                                            if (fileTransfer.getId() == FileOperation.Id.GET) {
                                                // Remote part requests a copy of specified file from this Test Automation Client
                                                p("Should now send file " + fileTransfer.getFileDescription().getFileName() + " back to " + fileTransfer.getSenderHostname() + ":" + fileTransfer.getSenderPort());

                                                FileDescription fileDescription = new FileDescription();
                                                fileDescription.setFileName(fileTransfer.getFileDescription().getFileName());
                                                fileDescription.setFileSize(FileDescription.UNKNOWN_FILE_SIZE);

                                                FileOperation fileTransferReply = new FileOperation(FileOperation.Id.PUT, fileTransfer.getTest(), fileDescription);
                                                fileTransferReply.setSender(testAutomationClientHostname, testAutomationClientPort);
                                                fileTransferReply.setReceiver(senderHostname, senderPort);

                                                fileSender.handle(fileTransferReply);

                                                p("File sender is notified about a request to transfer file " + fileTransferReply.getFileDescription().getFileName()
                                                    + " back to " + fileTransferReply.getReceiverHostname() + ":" + fileTransferReply.getReceiverPort());

                                            } else if (fileTransfer.getId() == FileOperation.Id.PUT) {
                                                // Remote part sends a copy of specified file to this Test Automation Client
                                                p("Should now receive file " + fileTransfer.getFileDescription().getFileName() + " from " + fileTransfer.getSenderHostname() + ":" + fileTransfer.getSenderPort());

                                                try {
                                                    Test test = testRegistry.getTest();
                                                    String testWorkspacePath = test.getWorkspacePath();
                                                    FileDescription fileDescription = fileTransfer.getFileDescription();
                                                    File file = new File(testWorkspacePath + System.getProperty("file.separator") + fileDescription.getFileName());

                                                    boolean isSuccess = false;
                                                    OutputStream fileData = null;
                                                    OutputStream outputStreamFromListener = null;

                                                    // Always try to get output stream to file from the test listener,
                                                    // and use file objects only if listener didn't helped with file access

                                                    TestAutomationServiceListener testListener = testRegistry.getListener();

                                                    if (testListener != null) {
                                                        p("Trying to create file " + fileDescription.getFileName() + " with help from the listener...");
                                                        outputStreamFromListener = testListener.createFile(testWorkspacePath, fileDescription.getFileName());
                                                    }

                                                    if (outputStreamFromListener != null) {
                                                        fileData = outputStreamFromListener;
                                                        p("Listener has helped with resolving a path to file " + fileDescription.getFileName());
                                                    } else {
                                                        p("Listener couldn't help with resolving a path to file " + fileDescription.getFileName()
                                                                + " Trying to solve this problem with plain file streams");
                                                        // Try to use file streams
                                                        if (file.exists()) {
                                                            if (file.delete()) {
                                                                p("File was already existed at " + file.getAbsolutePath() + " but was deleted up on a new file send");
                                                            } else {
                                                                p("Warning: couldn't delete old version of file at " + file.getAbsolutePath() + " probably due to insuficient access rights!");
                                                            }
                                                        }

                                                        p("Trying to create an empty file at " + file.getAbsolutePath());

                                                        try {
                                                            file.createNewFile();
                                                            p("An empty file was successfully created at " + file.getAbsolutePath());
                                                        } catch (Exception e) {
                                                            p("Got troubles during creation of empty file at "
                                                              + file.getAbsolutePath() + " - " + e.getClass() + " " + e.getMessage());
                                                        }

                                                        fileData = new FileOutputStream(file);
                                                    }

                                                    if (fileData != null) {
                                                        try {
                                                            p("Trying to receive file data from " + senderHostname + ":" + senderPort + " regarding the test '" + fileTransfer.getTest().getId() + "':");

                                                            int numberOfBytesInBuffer = 0;
                                                            long numberOfActuallyReceivedBytes = 0L;
                                                            byte[] buffer = new byte[Constant.DEFAULT_BUFFER_SIZE];

                                                            long fileTransferStartedAt = System.currentTimeMillis();

                                                            do {
                                                                numberOfBytesInBuffer = inputStream.read(buffer);

                                                                if (numberOfBytesInBuffer > 0) {
                                                                    fileData.write(buffer, 0, numberOfBytesInBuffer);
                                                                    fileData.flush();
                                                                    numberOfActuallyReceivedBytes += numberOfBytesInBuffer;
                                                                } else {
                                                                    // Safety against possible deadlocks in read()
                                                                    break;
                                                                }
                                                            } while (numberOfBytesInBuffer > 0);

                                                            // Just show a nice message about bytes and time of transfer
                                                            long time = System.currentTimeMillis() - fileTransferStartedAt;
                                                            time /= 1000L; // Turn milliseconds into seconds

                                                            String size = "";

                                                            if (numberOfActuallyReceivedBytes > 1048576) { // Turn bytes into megabytes
                                                                size = "" + (numberOfActuallyReceivedBytes / 1048576L ) + " MB";
                                                            } else if (numberOfActuallyReceivedBytes > 1024) { // Turn bytes into kilobytes
                                                                size = "" + (numberOfActuallyReceivedBytes / 1024L ) + " KB";
                                                            } else {
                                                                size = "" + numberOfActuallyReceivedBytes + " bytes";
                                                            }

                                                            p("Has received " + numberOfActuallyReceivedBytes + " bytes (" + size + ") out of " + fileDescription.getFileSize()
                                                                + " specified bytes. File's " + fileDescription.getFileName()
                                                                + " transfer took about " + time + " seconds");

                                                            if (numberOfActuallyReceivedBytes == fileDescription.getFileSize()) {
                                                                isSuccess = true;
                                                                p("File " + fileDescription.getFileName() + " was successfully received over network");
                                                            } else {
                                                                p("Failed to receive file over the network: "
                                                                    + fileDescription.getFileSize() + " bytes were supposed to be transferred, but has managed to send only "
                                                                    + numberOfActuallyReceivedBytes + " bytes. The file itself actually contains " + file.length() + " bytes");
                                                            }

                                                            // Perform all possible cleanups

                                                            if (fileData != null) {
                                                                try {
                                                                    fileData.flush();
                                                                    fileData.close();
                                                                    p("File data stream was successfully closed");
                                                                } catch (Exception e) {
                                                                    p("Got troubles while tried to close file data stream: " + e.getClass() + " " + e.getMessage());
                                                                }
                                                            }

                                                            if (outputStreamFromListener != null) {
                                                                try {
                                                                    outputStreamFromListener.flush();
                                                                    outputStreamFromListener.close();
                                                                    p("Output stream from listener was successfully closed");
                                                                } catch (Exception e) {
                                                                    p("Got troubles while tried to close output stream from listener: " + e.getClass() + " " + e.getMessage());
                                                                }
                                                            }

                                                        } catch (Exception e) {
                                                            p("Got troubles while tried to process incoming file transfer for file " + fileDescription.getFileName()
                                                                    + " regarding the test '" + fileTransfer.getTest().getId() + "': "
                                                                    + e.getClass() + " " + e.getMessage());
                                                        }
                                                    }

                                                    // Perform all possible cleanups

                                                    if (outputStreamFromListener != null) {
                                                        try {
                                                            outputStreamFromListener.close();
                                                        } catch (Exception e) {
                                                            // Ignore
                                                        }
                                                    }

                                                    if (fileData != null) {
                                                        try {
                                                            fileData.close();
                                                        } catch (Exception e) {
                                                            // Ignore
                                                        }
                                                    }

                                                    if (!isSuccess) {
                                                        p("Issuing a request to re-transfer file " + fileDescription.getFileName() + " from remote part at "
                                                            + senderHostname + ":" + senderPort);
                                                        FileOperation fileTransferRequest = new FileOperation(FileOperation.Id.GET, fileTransfer.getTest(), fileTransfer.getFileDescription());
                                                        fileTransferRequest.setSender(testAutomationClientHostname, testAutomationClientPort);
                                                        fileTransferRequest.setReceiver(senderHostname, senderPort);

                                                        fileSender.handle(fileTransferRequest);
                                                    }
                                                } catch (Exception e) {
                                                    p("Got troubles while tried to process incoming file transfer for file " + fileTransfer.getFileDescription().getFileName()
                                                            + " regarding the test '" + fileTransfer.getTest().getId() + "': "
                                                            + e.getClass() + " " + e.getMessage());
                                                }
                                            }
                                        }
                                    } else {
                                        // This client hasn't test registries for mentioned test id
                                        p("This clietn hasn't a test registy for mentioned test '" + fileTransfer.getTest().getId() + "'");

                                        FileOperation fileTransferReply = fileTransfer;
                                        fileTransferReply.setId(FileOperation.Id.ABORT);

                                        if (fileTransferReply != null) {
                                            // Send reply about unsuccessful file transfer
                                            fileTransferReply.setSender(testAutomationClientHostname, testAutomationClientPort);
                                            fileTransferReply.setReceiver(fileTransfer.getSenderHostname(), fileTransfer.getSenderPort());

                                            fileSender.handle(fileTransferReply);

                                            p("File sender will send a reply with ABORT for requested file operation");
                                        }
                                    }
                                } else if (message instanceof TestOperation) {

                                    testAutomationClient.handleTestOperation((TestOperation) message);

                                } else if (message instanceof TextMessage) {

                                    testAutomationClient.handleTextMessage((TextMessage) message);

                                } else {
                                    p("The received message is of unsupported type and will be ignored:\n" + message + "\n");
                                }
                            }

                            inputStream.close();
                            connection.close();

                        } catch (Exception e) {
                            p("Got troubles during processing incoming connection from "
                                + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                                + " - " + e.getClass() + " " + e.getMessage());
                            e.printStackTrace();

                            // Don't tell anything to Test Automation Client,
                            // since a broken connection may come from any remote part
                        } finally {
                            // Always ensure that input stream is closed
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (Exception e) {
                                    p("Got troubles while tried to close input stream from "
                                        + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                                        + " - " + e.getClass() + " " + e.getMessage());
                                }
                            }

                            // Always ensure that connection is closed
                            if (connection != null && !connection.isClosed()) {
                                try {
                                    connection.close();
                                } catch (Exception e) {
                                    p("Got troubles while tried to close a connection from "
                                        + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                                        + " - " + e.getClass() + " - " + e.getMessage());
                                }
                            }
                        }
                    }
                }

                sleep(Constant.MILLISECOND); // Wait for any updates
            }
            catch (InterruptedException e) {
                p("Receiver was interrupted, stop working");
                p("Closing all available incoming connections");

                for (Socket connection : socketPool) {
                    try {
                        connection.close();
                    } catch (Exception ioe) {
                        // Ignore
                    }
                }

                socketPool.clear();
                isRunning = false;

                p("Receiver has stopped working");
            }
        }
    }

    /**
     * Puts specified socket to be handled by receiver.
     *
     * @param socket A socket to be handled by receiver
     */
    public synchronized void handle(Socket socket) {
        if (socketPool.add(socket)) {
            //p("Will handle a socket from: " + socket.getInetAddress());
        } else {
            p("Error: Couldn't handle a socket from: " + socket.getInetAddress());
        }

        notify();
    }

    /**
     * Stops receiver running.
     */
    public synchronized void stopWorking() {
        isRunning = false;
    }

    /**
     * Print specified text on debugging output stream.
     *
     * @param text A text to be printed on debugging output stream
     */
    private synchronized void p(String text) {
        if (testAutomationClient != null) {
            testAutomationClient.p("Receiver: " + text);
        }
    }
}
