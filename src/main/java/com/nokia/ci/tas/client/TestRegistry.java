package com.nokia.ci.tas.client;

import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.TestAutomationServiceListener;

/**
 * Represents a single registry of the running test inside the Test Automation Client.
 */
public class TestRegistry {

    /**
     * Id of the test that registry holds.
     */
    private String testId;

    /**
     * Test that registry was created for.
     */
    private Test test;

    /**
     * Listener of all events related to test execution.
     */
    private TestAutomationServiceListener listener;

    /**
     * Hostname of remote service that organizes test executions.
     */
    private String remoteServiceHostname;

    /**
     * Port number of remote service that organizes test executions.
     */
    private int remoteServicePort;

    /**
     * Constrcutor.
     *
     * @param testId Id of the test that registry holds
     * @param test Test that registry will be created for
     * @param listener Listener of all events related to test execution
     * @param remoteServiceHostname Hostname of remote service that organizes test executions
     * @param remoteServicePort Port number of remote service that organizes test executions
     */
    public TestRegistry(String testId,
                        Test test,
                        TestAutomationServiceListener listener,
                        String remoteServiceHostname,
                        int remoteServicePort) {

        this.testId = testId;
        this.test = test;
        this.listener = listener;
        this.remoteServiceHostname = remoteServiceHostname;
        this.remoteServicePort = remoteServicePort;
    }

    /**
     * Gets test id that registry holds.
     *
     * @return Test id that registry holds
     */
    public String getTestId() {
        return testId;
    }

    /**
     * Gets test that registry was created for.
     *
     * @return Test that registry was created for
     */
    public Test getTest() {
        return test;
    }

    /**
     * Updates the test handled by this registry.
     *
     * @param test Updated test
     */
    public void setTest(Test test) {
        if (test != null) {
            this.test = test;
        }
    }

    /**
     * Gets a listener of all events related to test execution.
     *
     * @return A listener of all events related to test execution
     */
    public TestAutomationServiceListener getListener() {
        return listener;
    }

    /**
     * Gets hostname of remote service that organizes test executions.
     *
     * @return Hostname of remote service that organizes test executions
     */
    public String getRemoteServiceHostname() {
        return remoteServiceHostname;
    }

    /**
     * Gets port number of remote service that organizes test executions.
     *
     * @return Port number of remote service that organizes test executions
     */
    public int getRemoteServicePort() {
        return remoteServicePort;
    }

    /**
     * Generates a textual representation of the test registry object.
     *
     * @return A textual representation of the test registry object
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("\n Test Registry for test with id " + testId);
        string.append("\n\t Remote Service hostname: " + remoteServiceHostname);
        string.append("\n\t Remote Service port: " + remoteServicePort);
        string.append("\n\t Test: " + test);

        return string.toString();
    }
}
