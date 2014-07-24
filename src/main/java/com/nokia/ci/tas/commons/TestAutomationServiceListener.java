package com.nokia.ci.tas.commons;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This interface must be implemented by users of the Test Automation Client.
 */
public interface TestAutomationServiceListener {

    /**
     * Called when test is started.
     *
     * @param test Started test
     */
    public void testStarted(Test test);

    /**
     * Called when specified test has finished.
     * Please note, a "finished" test doesn't mean "successful" test results.
     * It just means that Test Automation Service has executed specified test
     * and test results are stored in the test issuer's workspace.
     *
     * @param test Finished test
     */
    public void testFinished(Test test);

    /**
     * Called when specified test has failed.
     *
     * @param test Failed test
     * @param reason Reason of test failure
     */
    public void testFailed(Test test, String reason);

    /**
     * Called when Test Automation Service has a message to the listener regarding specified test.
     *
     * @param test Test under execution
     * @param message A message from Test Automation Service regarding specified test
     */
    public void messageFromTestAutomationService(Test test, String message);

    /**
     * Resolves a file reading request from the Test Automation Client.
     * This method should simply open and return input stream to specified file
     * or return null if such file is not existing or not accessible by listener.
     *
     * @param directoryPath Path to a directory where file should be located
     * @param fileName Name of the file to be read
     * @return Input stream to specified file or null if file is not existing or cannot be accessed
     */
    public InputStream readFile(String directoryPath, String fileName);

    /**
     * Resolves a file creation request from the Test Automation Client.
     * This method should simply create specified file in the specified directory
     * and open output stream to that file.
     * If file cannot be created (or overwritten for some reasons),
     * this method should return null.
     *
     * @param directoryPath Path to a directory where file should be created
     * @param fileName Name of the file to be created
     * @return Output stream to created file or null if file cannot be created or overwritten for some reasons
     */
    public OutputStream createFile(String directoryPath, String fileName);
}
