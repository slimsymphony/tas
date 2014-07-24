package com.nokia.ci.tas.communicator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.net.Socket;

import java.util.List;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Converter;
import com.nokia.ci.tas.commons.FileDescription;

import com.nokia.ci.tas.commons.message.FileOperation;
import com.nokia.ci.tas.commons.message.ProductOperation;
import com.nokia.ci.tas.commons.message.TestOperation;

/**
 * Asynchronous handler of a single incoming message
 */
public class Receiver extends Thread {

    /**
     * A pool of all incoming connections that needs to be handled.
     */
    private ConcurrentLinkedQueue<Socket> socketPool;

    /**
     * Variable that keeps Receiving running.
     */
    private boolean running = true;

    /**
     * Instance of the Test Automation Communicator.
     */
    private TestAutomationCommunicator testAutomationCommunicator;

    /**
     * Hostname of the Test Automation Communicator.
     */
    private String testAutomationCommunicatorHostname;

    /**
     * Port number of the Test Automation Communicator.
     */
    private int testAutomationCommunicatorPort;

    /**
     * Converter of input XML streams.
     */
    private Converter converter;

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

    /**
     * Constructor.
     *
     * @param fileCache Instance of the file caching module
     */
    public Receiver(FileCache fileCache) {
        super(); // Start as anonymous thread

        socketPool = new ConcurrentLinkedQueue();

        this.fileCache = fileCache;

        testAutomationCommunicator = TestAutomationCommunicator.getInstance();

        // Extract the following parameters to eliminate unnecessary method calls
        testAutomationCommunicatorHostname = testAutomationCommunicator.getHostname();
        testAutomationCommunicatorPort = testAutomationCommunicator.getPort();
        fileSeparator = testAutomationCommunicator.getFileSeparator();

        converter = new Converter();

        setPriority(MIN_PRIORITY); // Always work with minimal priority
    }

    /**
     * Receiver's main routine.
     */
    @Override
    public void run() {
        p("Started working");

        while (running) {
            try {
                if (!socketPool.isEmpty()) {
                    Socket connection = socketPool.poll(); // Always get the first connection

                    // Always ensure that connection is not already closed
                    if (connection != null && !connection.isClosed()) {
                        InputStream inputStream = null;

                        try {
                            inputStream = connection.getInputStream();
                            Object message = converter.handle(inputStream);

                            if (message != null) {
                                p("Handling incoming message of type " + message.getClass().getSimpleName() + " from " + connection.getInetAddress().getCanonicalHostName());

                                if (message instanceof FileOperation) {
                                    FileOperation fileTransfer = (FileOperation) message;
                                    p("Processing incoming file transfer:\n" + fileTransfer);

                                    // Test Automation Communicator may receive file transfer messages only from remote Test Automation Clients
                                    TestExecutor testExecutor = testAutomationCommunicator.getTestExecutor(fileTransfer.getTest().getRuntimeId());

                                    if (testExecutor != null) {
                                        if (fileTransfer.getId() == FileOperation.Id.GET) {
                                            // Remote client has requested a file from this Test Automation Communicator
                                            p("Should now send file " + fileTransfer.getFileDescription().getFileName() + " back to " + fileTransfer.getSenderHostname() + ":" + fileTransfer.getSenderPort());

                                            File file = new File(testExecutor.getTestWorkspace().getAbsolutePath() + fileSeparator + fileTransfer.getFileDescription().getFileName());

                                            if (file.exists() && file.canRead()) {
                                                p("File " + file.getAbsolutePath() + " exists and can be send back to " + fileTransfer.getSenderHostname() + ":" + fileTransfer.getSenderPort());

                                                FileDescription fileDescription = fileTransfer.getFileDescription();
                                                fileDescription.setFileSize(file.length());

                                                FileOperation fileTransferReply = new FileOperation(FileOperation.Id.PUT, fileTransfer.getTest(), fileDescription);
                                                fileTransferReply.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);
                                                fileTransferReply.setReceiver(fileTransfer.getSenderHostname(), fileTransfer.getSenderPort());

                                                testAutomationCommunicator.getSender().handle(fileTransferReply);

                                            } else {
                                                p("Couldn't access file " + file.getAbsolutePath() + " due to its unexistence or not sufficient access rights");

                                                FileOperation fileTransferReply = fileTransfer;
                                                fileTransferReply.setId(FileOperation.Id.ABORT);
                                                fileTransferReply.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);
                                                fileTransferReply.setReceiver(fileTransfer.getSenderHostname(), fileTransfer.getSenderPort());

                                                testAutomationCommunicator.getSender().handle(fileTransferReply);
                                                p("A reply about this issue is send back to " + fileTransfer.getSenderHostname() + ":" + fileTransfer.getSenderPort());
                                            }

                                        } else if (fileTransfer.getId() == FileOperation.Id.PUT) {
                                            // Remote client is sending a copy of specified file to this Test Automation Communicator
                                            boolean isSuccess = false;
                                            FileDescription fileDescription = fileTransfer.getFileDescription();
                                            p("Should now receive file " + fileDescription.getFileName() + " from " + fileTransfer.getSenderHostname() + ":" + fileTransfer.getSenderPort());

                                            // Try to create file in the workspace of mentioned test
                                            File file = new File(testExecutor.getTestWorkspace().getAbsolutePath() + fileSeparator + fileDescription.getFileName());

                                            if (file.exists()) {
                                                if (file.delete()) {
                                                    p("File was already existed at " + file.getAbsolutePath() + " but was deleted up on a new file transfer");
                                                } else {
                                                    p("Warning: couldn't delete old version of file at " + file.getAbsolutePath() + " probably due to insuficient access rights!");
                                                }
                                            }

                                            if (!file.exists()) {
                                                try {
                                                    file.createNewFile();
                                                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                                                    p("File " + file.getAbsolutePath() + " was successfully created");

                                                    int numberOfBytesInBuffer = 0;
                                                    long numberOfReceivedBytes = 0L;
                                                    byte[] buffer = new byte[Constant.DEFAULT_BUFFER_SIZE];

                                                    long fileTransferStartedAt = System.currentTimeMillis();

                                                    do {
                                                        numberOfBytesInBuffer = inputStream.read(buffer);

                                                        if (numberOfBytesInBuffer > 0) {
                                                            fileOutputStream.write(buffer, 0, numberOfBytesInBuffer);
                                                            fileOutputStream.flush();
                                                            numberOfReceivedBytes += numberOfBytesInBuffer;
                                                        } else {
                                                            // Safety against possible deadlocks in read()
                                                            break;
                                                        }
                                                    } while (numberOfBytesInBuffer > 0);

                                                    fileOutputStream.flush();
                                                    fileOutputStream.close();

                                                    // Make transferred file readable by anyone on this system
                                                    file.setReadable(true, false);

                                                    // Just show a nice message about bytes and time of transfer
                                                    long time = System.currentTimeMillis() - fileTransferStartedAt;
                                                    time /= 1000L; // Turn milliseconds into seconds

                                                    String size = "";

                                                    if (numberOfReceivedBytes > 1048576) { // Turn bytes into megabytes
                                                        size = "" + (numberOfReceivedBytes / 1048576L ) + " MB";
                                                    } else if (numberOfReceivedBytes > 1024) { // Turn bytes into kilobytes
                                                        size = "" + (numberOfReceivedBytes / 1024L ) + " KB";
                                                    } else {
                                                        size = "" + numberOfReceivedBytes + " bytes";
                                                    }

                                                    p("Has received " + numberOfReceivedBytes + " bytes (" + size + ") of file's " + file.getName()
                                                        + " data over network in about " + time + " seconds");

                                                    if (fileDescription.getFileSize() != FileDescription.UNKNOWN_FILE_SIZE) {
                                                        // Check if number of send bytes equals the number of bytes mentioned in the file transfer message
                                                        if (file.length() == fileDescription.getFileSize()) {
                                                            isSuccess = true;
                                                            p("Successfully received file over the network");
                                                        } else {
                                                            p("Failed to receive file over the network: "
                                                                + fileDescription.getFileSize() + " bytes were supposed to be transferred, but has managed to send only "
                                                                + numberOfReceivedBytes + " bytes. And file itself actually contained " + file.length() + " bytes");
                                                        }
                                                    } else {
                                                        // Assume that everything was fine
                                                        isSuccess = true;
                                                        p("Sender of the file " + fileDescription.getFileName() + " hasn't specified file size."
                                                            + " Assuming that file transfer was successful, since no problems has occured during file transfer");
                                                    }

                                                    // Notify test executor about file transfer success or failure
                                                    if (isSuccess) {
                                                        p("Notifying executor of the test '" + testExecutor.getName() + "' about successful receive of file " + file.getName() + "'...");
                                                        testExecutor.fileReceived(file.getName(), isSuccess);
                                                    } else {
                                                        p("Notifying executor of the test '" + testExecutor.getName() + "' about unsuccessful receive of file " + file.getName() + "'...");
                                                    }

                                                    // Update file cache entry
                                                    if (isSuccess) {
                                                        p("Updating the corresponding file cache entry for " + file.getAbsolutePath());

                                                        FileCacheEntry fileCacheEntry = fileCache.getEntry(file.getAbsolutePath());

                                                        if (fileCacheEntry != null) {
                                                            fileCacheEntry.setIsTransferred(true);
                                                            fileCacheEntry.setTimeWhenTransferred(System.currentTimeMillis());

                                                            fileCache.update(fileCacheEntry);

                                                            p("Checking file cache against similar file requests...");

                                                            List<FileCacheEntry> untransferredSimilarEntries = fileCache.getUntransferredEntries(testExecutor.getTest(), file.getName());

                                                            p("Got " + untransferredSimilarEntries.size() + " similar untransferred entries");

                                                            for (FileCacheEntry entry : untransferredSimilarEntries) {
                                                                p("Processing entry for " + entry.getAbsoluteFilePath());
                                                                p("Copying file from " + file.getAbsolutePath() + " to " + entry.getAbsoluteFilePath());

                                                                try {
                                                                    if (fileCache.copyFile(file.getAbsolutePath(), entry.getAbsoluteFilePath())) {
                                                                        p("Got a successful copy of file " + file.getAbsolutePath() + " into file " + entry.getAbsoluteFilePath());

                                                                        entry.setIsRequested(true);
                                                                        entry.setTimeWhenRequested(System.currentTimeMillis());
                                                                        entry.setIsTransferred(true);
                                                                        entry.setTimeWhenTransferred(System.currentTimeMillis());

                                                                        fileCache.update(entry);

                                                                        // Notify test executor about successful file transfer
                                                                        TestExecutor executor =
                                                                            testAutomationCommunicator.getTestExecutor(entry.getTest().getRuntimeId());

                                                                        if (executor != null) {
                                                                            p("Notifying test executor '" + executor.getName() + "' about successful file copy");
                                                                            executor.fileReceived(file.getName(), true);
                                                                        }

                                                                    } else {
                                                                        p("Error: Couldn't get a successful copy of file " + file.getAbsolutePath() + " into file " + entry.getAbsoluteFilePath());
                                                                    }

                                                                } catch (Exception e) {
                                                                    e.printStackTrace();
                                                                }
                                                            }
                                                        } else {
                                                            p("Got NULL instead of file cache entry for " + file.getAbsolutePath());
                                                        }
                                                    }

                                                    p("Enabling file transferes in sender once again");
                                                    testAutomationCommunicator.getSender().enableFileTransfers();

                                                } catch (Exception e) {
                                                    p("Got troubles during receiving a file " + file.getAbsolutePath());
                                                    e.printStackTrace();

                                                    p("Notifying executor of the test '" + testExecutor.getName() + "' about failed file receive...");
                                                    testExecutor.fileReceived(file.getName(), false);
                                                }
                                            } else {
                                                p("Couldn't re-create file at " + file.getAbsolutePath() + " probably due to insufficient access rights");
                                                p("Notifying executor of the test '" + testExecutor.getName() + "' about troubles with file access during the file receive...");
                                                testExecutor.fileReceived(file.getName(), false);
                                            }
                                        } else {
                                            p("Notifying executor of the test '" + testExecutor.getName() + "' about file transfer of type " + fileTransfer.getType()
                                                + " regarding the file " + fileTransfer.getFileDescription().getFileName());

                                            if (fileTransfer.getId() == FileOperation.Id.ABORT) {
                                                testExecutor.fileReceived(fileTransfer.getFileDescription().getFileName(), false);
                                                testExecutor.fileSend(fileTransfer.getFileDescription().getFileName(), false);
                                            }
                                        }
                                    } else {
                                        p("Test '" + fileTransfer.getTest().getRuntimeId() + "' is not handled by this Test Automation Communicator."
                                            + " The following file transfer message will be ignored:\n" + fileTransfer);
                                    }
                                } else if (message instanceof TestOperation) {

                                    testAutomationCommunicator.handleTestOperation((TestOperation) message);

                                } else if (message instanceof ProductOperation) {

                                    testAutomationCommunicator.handleProductOperation((ProductOperation) message);

                                } else {
                                    p("Message is of type " + message.getClass().getName() + " are not supported and will be ignored");
                                }
                            }

                            inputStream.close();
                            connection.close();

                        } catch (Exception e) {
                            p("Got troubles during processing incoming connection from "
                                + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                                + " - " + e.getClass() + " - " + e.getMessage());
                            e.printStackTrace();
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
                                    p("Got troubles during closing incoming connection from "
                                        + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                                        + " - " + e.getClass() + " - " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

                sleep(Constant.MILLISECOND); // Wait for any updates

            } catch (Exception e) {
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
            }
        }
    }

    /**
     * Handles specified connection.
     *
     * @param socket Connection to be handled
     */
    public synchronized void handle(Socket socket) {
        if (socketPool.add(socket)) {
            //p("Got a connection to handle: " + socket.getInetAddress());
        } else {
            p("Error: Couldn't add a connection for handling: " + socket.getInetAddress());
        }
        notify();
    }

    /**
     * Prints specified text to output stream.
     *
     * @param text A text to be printed to output stream
     */
    private void p(String text) {
        logger.log(Level.ALL, "Receiver: " + text);
    }
}
