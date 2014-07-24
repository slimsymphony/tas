package com.nokia.ci.tas.communicator;

import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.message.FileOperation;

/**
 * A single entry in file cache.
 */
public class FileCacheEntry {

    /**
     * Name of the file this entry is referring to.
     */
    private String fileName;

    /**
     * Test instance this entry is referring to.
     */
    private Test test;

    /**
     * Absolute path to the referred file.
     */
    private String absoluteFilePath;

    /**
     * The corresponding file transfer request operation.
     */
    private FileOperation fileTransferRequest;

    /**
     * True if file transfer request was successfully send.
     */
    private boolean isRequested = false;

    /**
     * Timestamp of the moment when file transfer request was send.
     */
    private long timeWhenRequested = 0L;

    /**
     * True if corresponding file transfer response was successfully received.
     */
    private boolean isTransferred = false;

    /**
     * Timestamp of the moment when the corresponding file transfer response was successfully received.
     */
    private long timeWhenTransferred = 0L;

    /**
     * Constructor.
     *
     * @param fileName Name of file this entry is referring to
     * @param test Test instance this entry is referring to
     * @param absoluteFilePath Absolute path to the corresponding file on local machine
     * @param fileTransferRequest File transfer request
     */
    public FileCacheEntry(String fileName, Test test, String absoluteFilePath, FileOperation fileTransferRequest) {
        this.fileName = fileName;
        this.test = test;
        this.absoluteFilePath = absoluteFilePath;
        this.fileTransferRequest = fileTransferRequest;
    }

    /**
     * Returns name of the file this entry is referring to.
     *
     * @return Name of the file this entry is referring to
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns test instance this entry is referring to.
     *
     * @return Test instance this entry is referring to
     */
    public Test getTest() {
        return test;
    }

    /**
     * Returns absolute path to referred file on local machine.
     *
     * @return Absolute path to referred file on local machine
     */
    public String getAbsoluteFilePath() {
        return absoluteFilePath;
    }

    /**
     * Returns the corresponding file transfer request.
     *
     * @return The corresponding file transfer request
     */
    public FileOperation getFileTransferRequest() {
        return fileTransferRequest;
    }

    /**
     * Returns true if the corresponding file transfer request was successfully send or false otherwise.
     *
     * @return True if the corresponding file transfer request was successfully send or false otherwise
     */
    public boolean isRequested() {
        return isRequested;
    }

    /**
     * Sets indication of a successful (true) or failed (false) send of corresponding file transfer request.
     *
     * @param isRequested Indication of a successful (true) or failed (false) send of corresponding file transfer request
     */
    public void setIsRequested(boolean isRequested) {
        this.isRequested = isRequested;
    }

    /**
     * Returns true if file was successfully transferred from remote part to local machine or false othewise.
     *
     * @return True if file was successfully transferred from remote part to local machine or false othewise
     */
    public boolean isTransferred() {
        return isTransferred;
    }

    /**
     * Sets indication of a successful (true) or failed (false) file transfer from remote part to local machine.
     *
     * @param isTransferred Indication of a successful (true) or failed (false) file transfer from remote part to local machine
     */
    public void setIsTransferred(boolean isTransferred) {
        this.isTransferred = isTransferred;
    }

    /**
     * Returns the timestamp of the moment when corresponding file transfer request was successfully send.
     *
     * @return The timestamp of the moment when corresponding file transfer request was successfully send
     */
    public long getTimeWhenRequested() {
        return timeWhenRequested;
    }

    /**
     * Sets the timestamp of the moment when corresponding file transfer request was successfully send.
     *
     * @param timeWhenRequested The timestamp of the moment when corresponding file transfer request was successfully send
     */
    public void setTimeWhenRequested(long timeWhenRequested) {
        this.timeWhenRequested = timeWhenRequested;
    }

    /**
     * Returns the timestamp of the moment when file was successfully transferred from remote part to local machine.
     *
     * @return The timestamp of the moment when file was successfully transferred from remote part to local machine
     */
    public long getTimeWhenTransferred() {
        return timeWhenTransferred;
    }

    /**
     * Sets the timestamp of the moment when file was successfully transferred from remote part to local machine.
     *
     * @param timeWhenTransferred The timestamp of the moment when file was successfully transferred from remote part to local machine
     */
    public void setTimeWhenTransferred(long timeWhenTransferred) {
        this.timeWhenTransferred = timeWhenTransferred;
    }
}
