package com.nokia.ci.tas.service;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Util;

/**
 * Configuration handler for Test Automation Service.
 */
public class Configuration extends Thread {

    /**
     * Name of the configuration file.
     */
    private static final String NAME_OF_CONFIGURATION_FILE = "configuration.dat";

    /**
     * Parameter name for storing test default timeout in configuration file.
     */
    private static final String TEST_DEFAULT_TIMEOUT = "test-default-timeout";

    /**
     * Test default timeout in milliseconds.
     */
    private long testDefaultTimeout = Constant.DEFAULT_TEST_TIMEOUT;

    /**
     * Parameter name for storing minimal execution time for a test in configuration file.
     */
    private static final String TEST_MINIMAL_EXECUTION_TIME = "test-minimal-execution-time";

    /**
     * Minimal execution time for a test in milliseconds.
     */
    private long testMinimalExecutionTime = Constant.MINIMAL_TIMEOUT_FOR_TEST_EXECUTION;

    /**
     * Parameter name for storing maximal number of running tests per single test node in configuration file.
     */
    private static final String MAXIMAL_NUMBER_OF_TESTS_PER_NODE = "maximal-number-of-tests-per-node";

    /**
     * Maximal number of running tests per single test node.
     */
    private long maximalNumberOfTestsPerNode = Constant.MAXIMAL_NUMBER_OF_RUNNING_TESTS_PER_TEST_NODE;

    /**
     * Parameter name for storing maximal number of retries for a single failed test in configuration file.
     */
    private static final String MAXIMAL_NUMBER_OF_RETRIES_FOR_FAILED_TEST = "maximal-number-of-retries-for-failed-test";

    /**
     * Maximal number of retries for a single failed test.
     */
    private long maximalNumberOfRetriesForFailedTest = Constant.MAXIMAL_NUMBER_OF_RETRIES_FOR_FAILED_TEST;

    /**
     * Parameter name for storing test resources expectation timeout in configuration file.
     */
    private static final String TEST_RESOURCES_EXPECTATION_TIMEOUT = "test-resources-expectation-timeout";

    /**
     * Test resources expectation timeout in milliseconds.
     */
    private long testResourcesExpectationTimeout = 15L * Constant.ONE_MINUTE;

    /**
     * Parameter name for storing the threshold period for statistics in configuration file.
     */
    private static final String STATISTICS_THRESHOLD_PERIOD = "statistics-threshold-period";

    /**
     * Threshold period for statistics in milliseconds.
     */
    private long statisticsThresholdPeriod = 7L * 24L * Constant.ONE_HOUR;

    /**
     * Parameter name for storing the statistics update period for statistics in configuration file.
     */
    private static final String STATISTICS_UPDATE_PERIOD = "statistics-update-period";

    /**
     * Statistics update period in milliseconds with some additional seed.
     */
    private long statisticsUpdatePeriod = 15L * Constant.ONE_MINUTE + 23456L;

    /**
     * Parameter name for storing the remote client checking period in configuration file.
     */
    private static final String REMOTE_CLIENT_CHECKING_PERIOD = "remote-client-checking-period";

    /**
     * Remote client checking period in milliseconds.
     */
    private long remoteClientCheckingPeriod = Constant.TEN_MINUTES;

    /**
     * Parameter name for storing workload history period.
     */
    private static final String WORKLOAD_HISTORY_PERIOD = "workload-history-period";

    /**
     * Workload history period in days.
     */
    private long workloadHistoryPeriod = 7L;

    /**
     * Parameter name for storing duration of a single workload slice for daily statistics.
     */
    private static final String DAILY_WORKLOAD_HISTORY_SLICE_DURATION = "daily-workload-history-slice-duration";

    /**
     * Duration of a single workload slice for daily statistics in milliseconds.
     */
    private long dailyWorkloadHistorySliceDuration = Constant.FIVE_MINUTES;

    /**
     * Parameter name for putting Test Automation Service into maintenance mode.
     */
    private static final String MAINTENANCE_MODE = "maintenance-mode";

    /**
     * Maintenance mode indicator.
     */
    private long maintenanceMode = 0L;

    /**
     * Variable which keeps receiver running.
     */
    private boolean isRunning = true;

    /**
     * A reference to the configuration file.
     */
    private File configurationFile;

    /**
     * Instance of the Test Automation Service's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationService.GLOBAL_LOGGER_NAME);

    /**
     * Constructor.
     */
    public Configuration() {
        super(); // Start as anonymous thread
    }

    /**
     * Returns default timeout for a test in milliseconds.
     *
     * @return Default timeout for a test in milliseconds
     */
    public long getTestDefaultTimeout() {
        return testDefaultTimeout;
    }

    /**
     * Returns the minimal execution time for a test in milliseconds.
     *
     * @return Minimal execution time for a test in milliseconds
     */
    public long getTestMinimalExecutionTime() {
        return testMinimalExecutionTime;
    }

    /**
     * Returns the maximal number of running tests per single test node.
     *
     * @return Maximal number of running tests per single test node
     */
    public long getMaximalNumberOfTestsPerNode() {
        return maximalNumberOfTestsPerNode;
    }

    /**
     * Returns the maximal number of retries for a single failed test.
     *
     * @return Maximal number of retries for a single failed test
     */
    public long getMaximalNumberOfRetriesForFailedTest() {
        return maximalNumberOfRetriesForFailedTest;
    }

    /**
     * Returns test resources expectation timeout in milliseconds.
     *
     * @return Test resources expectation timeout in milliseconds
     */
    public long getTestResourcesExpectationTimeout() {
        return testResourcesExpectationTimeout;
    }

    /**
     * Returns a threshold period for statistics in milliseconds.
     *
     * @return Threshold period for statistics in milliseconds
     */
    public long getStatisticsThresholdPeriod() {
        return statisticsThresholdPeriod;
    }

    /**
     * Returns statistics update period in milliseconds.
     *
     * @return Statistics update period in milliseconds
     */
    public long getStatisticsUpdatePeriod() {
        return statisticsUpdatePeriod;
    }

    /**
     * Returns remote client checking period in milliseconds.
     *
     * @return Remote client checking period in milliseconds
     */
    public long getRemoteClientCheckingPeriod() {
        return remoteClientCheckingPeriod;
    }

    /**
     * Returns workload history period in days.
     *
     * @return Workload history period in days
     */
    public long getWorkloadHistoryPeriod() {
        return workloadHistoryPeriod;
    }

    /**
     * Returns daily workload history slice duration in milliseconds.
     *
     * @return Daily workload history slice duration in milliseconds
     */
    public long getDailyWorkloadHistorySliceDuration() {
        return dailyWorkloadHistorySliceDuration;
    }

    /**
     * Returns code of the current maintenance mode.
     *
     * @return Code of the current maintenance mode
     */
    public long getMaintenanceMode() {
        return maintenanceMode;
    }

    /**
     * Return true if maintenance mode is switched on, or false otherwise.
     *
     * @return True if maintenance mode is switched on, or false otherwise
     */
    public boolean isMaintenanceMode() {
        if (maintenanceMode <= 0L) {
            // Zero or negative values are interpreted as maintenance is off
            return false;
        } else {
            // Positive values are interpreted as maintenance is on
            return true;
        }
    }

    /**
     * Configurator's main routine.
     */
    @Override
    public void run() {
        p("Started working");

        // Try to discover configuration file
        try {
            configurationFile = new File(NAME_OF_CONFIGURATION_FILE);

            if (!configurationFile.exists()) {
                if (configurationFile.createNewFile()) {
                    p("A new configuraiton file was created at " + configurationFile.getAbsolutePath());
                    // Store current configurations
                    try {
                        PrintWriter productConfiguration = new PrintWriter(configurationFile);
                        productConfiguration.append("");

                        // Add default parameters
                        productConfiguration.append("# All configuration settings supported by the Test Automation Service\n\n");

                        productConfiguration.append("# Test default timeout in milliseconds (" + Util.convert(testDefaultTimeout) + " by default, or "
                                + testDefaultTimeout + ")\n");
                        productConfiguration.append(TEST_DEFAULT_TIMEOUT + "=" + testDefaultTimeout + "\n\n");

                        productConfiguration.append("# Minimal execution time for a test in milliseconds (" + Util.convert(testMinimalExecutionTime) + " by default, or "
                                + testMinimalExecutionTime + ")\n");
                        productConfiguration.append(TEST_MINIMAL_EXECUTION_TIME + "=" + testMinimalExecutionTime +"\n\n");

                        productConfiguration.append("# Maximal number of running tests per test node (" + Constant.MAXIMAL_NUMBER_OF_RUNNING_TESTS_PER_TEST_NODE + " tests by default)\n");
                        productConfiguration.append(MAXIMAL_NUMBER_OF_TESTS_PER_NODE + "=" + maximalNumberOfTestsPerNode + "\n\n");

                        productConfiguration.append("# Maximal number of retries for a single failed test (" + Constant.MAXIMAL_NUMBER_OF_RETRIES_FOR_FAILED_TEST + " by default)\n");
                        productConfiguration.append(MAXIMAL_NUMBER_OF_RETRIES_FOR_FAILED_TEST + "=" + maximalNumberOfRetriesForFailedTest + "\n\n");

                        productConfiguration.append("# Test resources expectation timeout in milliseconds (" + Util.convert(testResourcesExpectationTimeout) + " by default, or "
                                + testResourcesExpectationTimeout + ")\n");
                        productConfiguration.append(TEST_RESOURCES_EXPECTATION_TIMEOUT + "=" + testResourcesExpectationTimeout + "\n\n");

                        productConfiguration.append("# Statistics threshold period in milliseconds (" + Util.convert(statisticsThresholdPeriod) + " by default, or "
                                + statisticsThresholdPeriod + ")\n");
                        productConfiguration.append(STATISTICS_THRESHOLD_PERIOD + "=" + statisticsThresholdPeriod + "\n\n");

                        productConfiguration.append("# Statistics update period in milliseconds (" + Util.convert(statisticsUpdatePeriod) + " by default, or "
                                + statisticsUpdatePeriod + ")\n");
                        productConfiguration.append(STATISTICS_UPDATE_PERIOD + "=" + statisticsUpdatePeriod + "\n\n");

                        productConfiguration.append("# Remote client checking period in milliseconds (" + Util.convert(remoteClientCheckingPeriod) + " by default, or "
                                + remoteClientCheckingPeriod + ")\n");
                        productConfiguration.append(REMOTE_CLIENT_CHECKING_PERIOD + "=" + remoteClientCheckingPeriod + "\n\n");

                        productConfiguration.append("# Default workload history period in days (" + workloadHistoryPeriod + " days by default)\n");
                        productConfiguration.append(WORKLOAD_HISTORY_PERIOD + "=" + workloadHistoryPeriod + "\n\n");

                        productConfiguration.append("# Duration of a single workload slice for daily statistics in milliseconds ("
                                + Util.convert(dailyWorkloadHistorySliceDuration) + " by default, or "
                                + dailyWorkloadHistorySliceDuration + ")\n");
                        productConfiguration.append(DAILY_WORKLOAD_HISTORY_SLICE_DURATION + "=" + dailyWorkloadHistorySliceDuration + "\n\n");

                        productConfiguration.append("# Enabling (1) and disabling (0) maintenance mode on this Test Automation Service\n");
                        productConfiguration.append(MAINTENANCE_MODE + "=" + maintenanceMode + "\n");

                        productConfiguration.flush();
                        productConfiguration.close();

                        p("Configuration file is updated at " + configurationFile.getAbsolutePath());

                    } catch (Exception e) {
                        p("Warning: Got some troubles while tried to init configuration file at " + configurationFile.getAbsolutePath());
                        e.printStackTrace();
                    }
                } else {
                    p("Warning: Couldn't init configuration file at " + configurationFile.getAbsolutePath());
                }
            } else {
                p("Configuraiton file already exists at " + configurationFile.getAbsolutePath());
            }
        } catch (Exception e) {
            p("Got troubles while tried to init configuration file: " + e.getClass() + " - " + e.getMessage());
            e.printStackTrace();
        }

        while (isRunning) {
            BufferedReader reader = null;

            try {
                p("Checking configuration file...");

                reader = new BufferedReader(new FileReader(configurationFile));

                String line = null;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (!line.isEmpty() && !line.startsWith("#")) {
                        if (line.startsWith(TEST_DEFAULT_TIMEOUT)) {

                            testDefaultTimeout = parse(line, testDefaultTimeout);
                            p("Test default timeout is " + Util.convert(testDefaultTimeout));

                        } else if (line.startsWith(TEST_MINIMAL_EXECUTION_TIME)) {

                            testMinimalExecutionTime = parse(line, testMinimalExecutionTime);
                            p("Test minimal execution time is " + Util.convert(testMinimalExecutionTime));

                        } else if (line.startsWith(MAXIMAL_NUMBER_OF_TESTS_PER_NODE)) {

                            maximalNumberOfTestsPerNode = parse(line, maximalNumberOfTestsPerNode);
                            p("Maximal number of running tests per test node is " + maximalNumberOfTestsPerNode);

                        } else if (line.startsWith(MAXIMAL_NUMBER_OF_RETRIES_FOR_FAILED_TEST)) {

                            maximalNumberOfRetriesForFailedTest = parse(line, maximalNumberOfRetriesForFailedTest);
                            p("Maximal number of retries for a single failed test is " + maximalNumberOfRetriesForFailedTest);

                        } else if (line.startsWith(TEST_RESOURCES_EXPECTATION_TIMEOUT)) {

                            testResourcesExpectationTimeout = parse(line, testResourcesExpectationTimeout);
                            p("Test resources expectation timeout is " + Util.convert(testResourcesExpectationTimeout));

                        } else if (line.startsWith(STATISTICS_THRESHOLD_PERIOD)) {

                            statisticsThresholdPeriod = parse(line, statisticsThresholdPeriod);
                            p("Statistics threshold period is " + Util.convert(statisticsThresholdPeriod));

                        } else if (line.startsWith(STATISTICS_UPDATE_PERIOD)) {

                            statisticsUpdatePeriod = parse(line, statisticsUpdatePeriod);
                            p("Statistics update period is " + Util.convert(statisticsUpdatePeriod));

                        } else if (line.startsWith(REMOTE_CLIENT_CHECKING_PERIOD)) {

                            remoteClientCheckingPeriod = parse(line, remoteClientCheckingPeriod);
                            p("Remote client checking period is " + Util.convert(remoteClientCheckingPeriod));

                        } else if (line.startsWith(WORKLOAD_HISTORY_PERIOD)) {

                            workloadHistoryPeriod = parse(line, workloadHistoryPeriod);
                            p("Workload history period is " + workloadHistoryPeriod + " days");

                        } else if (line.startsWith(DAILY_WORKLOAD_HISTORY_SLICE_DURATION)) {

                            dailyWorkloadHistorySliceDuration = parse(line, dailyWorkloadHistorySliceDuration);
                            p("Daily workload history slice duration is " + Util.convert(dailyWorkloadHistorySliceDuration));

                        } else if (line.startsWith(MAINTENANCE_MODE)) {
                            maintenanceMode = parse(line, maintenanceMode);
                            p("Maintenance mode is " + maintenanceMode);
                        } else {
                            p("Got unsupported parameter line: " + line);
                        }
                    }
                }

                reader.close();

            } catch (Exception e) {
                p("Got troubles during its work: " + e.getClass() + " - " + e.getMessage() + " - " + e.toString());
                e.printStackTrace();
            } finally {
                // Try to close reader by all means
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (Exception e) {
                    p("Got troubles while tried to close configuration file reader: " + e.getClass() + " - " + e.getMessage() + " - " + e.toString());
                    e.printStackTrace();
                }
            }

            // No matter what has happened, go to sleep for about five minutes
            try {
                sleep(Constant.FIVE_MINUTES + 23456L);
            } catch (Exception e) {
                p("Got interrupted during sleep time: " + e.getClass() + " - " + e.getMessage() + " - " + e.toString());
                e.printStackTrace();
            }
        }
    }

    /**
     * Studies provided input line for a long value and extracts it.
     * Returns new parsed value if it was different from the specified old one, or old value otherwise.
     *
     * @param line Input text
     * @param old Old long value
     * @return New parsed value if it was different from the specified old one, or old value otherwise
     */
    private long parse(String line, long old) {
        long result = old;

        try {
            // Parameter name is always separated from its value with the equality sign
            if (line.indexOf("=") != -1) {
                String parsedValue = line.substring(line.indexOf("=") + 1).trim();

                if (!parsedValue.isEmpty()) {
                    long parsedLong = Long.parseLong(parsedValue);

                    if (parsedLong >= 0L) {
                        if (parsedLong != old) {
                            result = parsedLong;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return result;
    }

    /**
     * Shutdowns configuration handler.
     */
    protected synchronized void shutdown() {
        p("Got a request to shutdown. Stop working...");
        isRunning = false;
        interrupt();
    }

    /**
     * Print specified text on debugging output stream.
     *
     * @param text A text to be printed on debugging output stream
     */
    private synchronized void p(String text) {
        logger.log(Level.ALL, "Configuration: " + text);
    }
}
