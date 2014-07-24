package com.nokia.ci.tas.communicator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.List;
import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.Util;

/**
 * File cache is a list of all file requests issued by test executors.
 * The primary task of file cache is to help with eliminating unnecessary duplicates of file request send outside.
 */
public class FileCache {

    /**
     * A list of all file requests issued from this Test Automation Communicator.
     */
    private ConcurrentLinkedQueue<FileCacheEntry> cache;

    /**
     * Instance of the Test Automation Communicator's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationCommunicator.GLOBAL_LOGGER_NAME);

    /**
     * Constructor.
     */
    public FileCache() {
        cache = new ConcurrentLinkedQueue();
    }

    /**
     * Adds specified entry to the cache.
     *
     * @param entry Entry to be added to the cache
     */
    public void add(FileCacheEntry entry) {
        if (cache.add(entry)) {
            p("Added entry for " + entry.getAbsoluteFilePath() + " (Requested:" + entry.isRequested() + ", Transferred:" + entry.isTransferred() + ")");
        }
    }

    /**
     * Updates the corresponding entry in cache.
     *
     * @param entry Entry to be updated
     */
    public void update(FileCacheEntry entry) {
        for (FileCacheEntry existing : cache) {
            if (existing.getAbsoluteFilePath().equals(entry.getAbsoluteFilePath())) {
                p("Updating entry for " + entry.getAbsoluteFilePath() + " (Requested:" + entry.isRequested() + ", Transferred:" + entry.isTransferred() + ")");
                existing = entry;
                break;
            }
        }
    }

    /**
     * Returns true if cache contains at least one unrequested entry and false otherwise.
     * The "unrequested" entry means that its corresponding file transfer request wasn't yet send.
     *
     * @return True if cache contains at least one unrequested entry and false otherwise
     */
    public boolean hasUnrequestedEntries() {
        for (FileCacheEntry entry : cache) {
            if (!entry.isRequested()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the first unrequested entry in cache.
     * The "unrequested" entry means that its corresponding file transfer request wasn't yet send.
     *
     * @return The first unrequested entry in cache
     */
    public FileCacheEntry getUnrequestedEntry() {
        for (FileCacheEntry entry : cache) {
            if (!entry.isRequested()) {
                p("Returning unrequested entry for " + entry.getAbsoluteFilePath() + " (Requested:" + entry.isRequested() + ", Transferred:" + entry.isTransferred() + ")");
                return entry;
            }
        }

        //p("Returning NULL instead of unrequested entry");
        return null;
    }

    /**
     * Returns a list of unrequested file cache entries.
     * 
     * @return A list of unrequested file cache entries
     */
    public List<FileCacheEntry> getUnrequestedEntries() {
        List<FileCacheEntry> entries = new ArrayList<FileCacheEntry>(0);

        for (FileCacheEntry entry : cache) {
            if (!entry.isRequested()) {
                entries.add(entry);
            }
        }

        return entries;
    }

    /**
     * Removes all entries from the cache that are refering to the same absolute file path.
     *
     * @param absoluteFilePath Absolute file path to be examined
     */
    public void removeEntryFor(String absoluteFilePath) {
        p("Removing entries with file path " + absoluteFilePath);
        for (FileCacheEntry entry : cache) {
            if (entry.getAbsoluteFilePath().equals(absoluteFilePath)) {
                if (cache.remove(entry)) {
                    p("Removed an entry with file path " + absoluteFilePath + " (Requested:" + entry.isRequested() + ", Transferred:" + entry.isTransferred() + ")");
                }
            }
        }
    }

    /**
     * Returns an entry refering to specified absolute file path or null if such entry is not existing.
     *
     * @param absoluteFilePath Absolute file path to be examined
     * @return An entry refering to specified absolute file path or null if such entry is not existing
     */
    public FileCacheEntry getEntry(String absoluteFilePath) {
        for (FileCacheEntry entry : cache) {
            if (entry.getAbsoluteFilePath().equals(absoluteFilePath)) {
                p("Returning entry for " + entry.getAbsoluteFilePath() + " (Requested:" + entry.isRequested() + ", Transferred:" + entry.isTransferred() + ")");
                return entry;
            }
        }

        return null;
    }

    /**
     * Returns a list of all untransferred entries that are corresponding to specified test and file name.
     * The "unrequested" entry means that its corresponding file transfer request wasn't yet send.
     *
     * @param test Test that entries should be referring to
     * @param fileName File name that entries should referring to
     * @return A list of all untransferred entries that are corresponding to specified test and file name
     */
    public List<FileCacheEntry> getUntransferredEntries(Test test, String fileName) {
        List<FileCacheEntry> result = new ArrayList<FileCacheEntry>(0);

        for (FileCacheEntry entry : cache) {
            if (!entry.isTransferred()) {
                if (entry.getFileName().equals(fileName)) {
                    Test existingTest = entry.getTest();
                    if (existingTest.getId().equals(test.getId())) {
                        // File name and test id are the same
                        result.add(entry);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Performs a file copy from one specified absolute path to another.
     * Returns true if file copying was successful or false otherwise.
     *
     * @param sourceAbsolutePath Absolute path of the file to be copied
     * @param destinationAbsolutePath Absolute path of the file to be created
     * @return True if file copying was successful or false otherwise
     */
    public synchronized boolean copyFile(String sourceAbsolutePath, String destinationAbsolutePath) {

        boolean isSuccess = false;

        try {
            p("Trying to copy a file from " + sourceAbsolutePath + " to " + destinationAbsolutePath);

            // Try to create file in the destination
            File sourceFile = new File(sourceAbsolutePath);

            if (!sourceFile.exists()) {
                p("Error: The source file wasn't existing at specified path: " + sourceFile.getAbsolutePath());
                return false;
            }

            // At this point source file is verified to be existing
            // Don't check any reading permissions, since Java isn't always correct about that
            FileInputStream sourceStream = new FileInputStream(sourceFile);

            // Create and open destination file
            File destinationFile = new File(destinationAbsolutePath);

            if (destinationFile.exists()) {
                if (destinationFile.delete()) {
                    p("Destination file was already existed, but was deleted up on a requested file copy to path: " + destinationFile.getAbsolutePath());
                } else {
                    p("Error: Couldn't delete the old version of a file due to insuficient access rights at the specified path: " + destinationFile.getAbsolutePath());
                    return false;
                }
            }

            if (!destinationFile.exists()) {
                try {
                    destinationFile.createNewFile();
                    FileOutputStream destinationStream = new FileOutputStream(destinationFile);
                    p("Empty destination file was successfully created at " + destinationFile.getAbsolutePath());

                    int numberOfBytesInBuffer = 0;
                    long numberOfCopiedBytes = 0L;
                    byte[] buffer = new byte[Constant.DEFAULT_BUFFER_SIZE];

                    long fileCopyingStartedAt = System.currentTimeMillis();

                    do {
                        numberOfBytesInBuffer = sourceStream.read(buffer);

                        if (numberOfBytesInBuffer > 0) {
                            destinationStream.write(buffer, 0, numberOfBytesInBuffer);
                            destinationStream.flush();
                            numberOfCopiedBytes += numberOfBytesInBuffer;
                        } else {
                            // Safety against possible deadlocks in read()
                            break;
                        }
                    } while (numberOfBytesInBuffer > 0);

                    destinationStream.flush();
                    destinationStream.close();
                    sourceStream.close();

                    // Make copied file readable by anyone on this system
                    destinationFile.setReadable(true, false);

                    // Just show a nice message about bytes and time of transfer
                    long time = System.currentTimeMillis() - fileCopyingStartedAt;

                    String size = "";

                    if (numberOfCopiedBytes > 1048576) { // Turn bytes into megabytes
                        size = "" + (numberOfCopiedBytes / 1048576L ) + " MB";
                    } else if (numberOfCopiedBytes > 1024) { // Turn bytes into kilobytes
                        size = "" + (numberOfCopiedBytes / 1024L ) + " KB";
                    } else {
                        size = "" + numberOfCopiedBytes + " bytes";
                    }

                    p("Has copied " + numberOfCopiedBytes + " bytes (" + size + ") from file " + sourceFile.getAbsolutePath()
                        + " to file " + destinationFile.getAbsolutePath() + " in about " + Util.convert(time));

                    // Ensure that the number of copied bytes equals the number of available ones
                    long sourceLength = sourceFile.length();
                    long destinationLength = destinationFile.length();

                    if (sourceLength == destinationLength) {
                        p("The source and destination files are both of equal size: " + destinationLength + " bytes. File copy was successful");
                        isSuccess = true;
                    } else {
                        p("The source and destination files are of inequal sizes: " + sourceLength
                            + " bytes in source file and " + destinationLength + " bytes in destination file. File copy was unsuccessful");
                        isSuccess = false;
                    }

                } catch (Exception e) {
                    p("Got troubles during performing a copy of a file from " + sourceAbsolutePath + " to " + destinationAbsolutePath + " due to: "
                        + e.getClass() + " - " + e.getMessage());
                    e.printStackTrace();
                    isSuccess = false;
                }
            } else {
                p("Error: Couldn't re-create a destination file due to insufficient access rights at specified path: " + destinationFile.getAbsolutePath());
                isSuccess = false;
            }
        } catch (Exception e) {
            p("Got troubles during performing a copy of a file from " + sourceAbsolutePath + " to " + destinationAbsolutePath + " due to: "
                + e.getClass() + " - " + e.getMessage());
            e.printStackTrace();
            isSuccess = false;
        }

        return isSuccess;
    }

    /**
     * Prints specified text to output stream.
     *
     * @param text A text to be printed to output stream
     */
    private void p(String text) {
        logger.log(Level.ALL, "File Cache: " + text);
    }
}
