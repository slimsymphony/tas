package com.nokia.ci.tas.commons;

/**
 * Keeper of all constant values used in Test Automation Service components.
 */
public class Constant {

    /**
     * Time constant of 1 thousandth of a second.
     */
    public static final long MILLISECOND = 1L;

    /**
     * Time constant of 1 hundredth of a second in milliseconds.
     */
    public static final long CENTISECOND = 10L;

    /**
     * Time constant of 1 tenth of a second in milliseconds.
     */
    public static final long DECISECOND = 100L;

    /**
     * Time constant of 1 second in milliseconds.
     */
    public static final long ONE_SECOND = 1000L;

    /**
     * Time constant of 5 seconds in milliseconds.
     */
    public static final long FIVE_SECONDS = 5000L;

    /**
     * Time constant of 10 seconds in milliseconds.
     */
    public static final long TEN_SECONDS = 10000L;

    /**
     * Time constant of 15 seconds in milliseconds.
     */
    public static final long FIFTEEN_SECONDS = 15000L;

    /**
     * Time constant of 30 seconds in milliseconds.
     */
    public static final long THIRTY_SECONDS = 30000L;

    /**
     * Time constant of 1 minute in milliseconds.
     */
    public static final long ONE_MINUTE = 60000L;

    /**
     * Time constant of 2 minutes in milliseconds.
     */
    public static final long TWO_MINUTES = 120000L;

    /**
     * Time constant of 3 minutes in milliseconds.
     */
    public static final long THREE_MINUTES = 180000L;

    /**
     * Time constant of 5 minutes in milliseconds.
     */
    public static final long FIVE_MINUTES = 300000L;

    /**
     * Time constant of 10 minutes in milliseconds.
     */
    public static final long TEN_MINUTES = 600000L;

    /**
     * Time constant of 15 minutes in milliseconds.
     */
    public static final long FIFTEEN_MINUTES = 900000L;

    /**
     * Time constant of 1 hour in milliseconds.
     */
    public static final long ONE_HOUR = 3600000L;

    /**
     * Time constant of 1 day or 24 hours in milliseconds.
     */
    public static final long ONE_DAY = 86400000L;

    /**
     * Default buffer size to be used in networking and file handling.
     */
    public static final int DEFAULT_BUFFER_SIZE = 4096;

    /**
     * Number of tries to perform unsuccessful operations once again before issuing a failure.
     */
    public static final int NUMBER_OF_RETRIES = 3;

    /**
     * Maximal number of tests running on a single test node.
     */
    public static final int MAXIMAL_NUMBER_OF_RUNNING_TESTS_PER_TEST_NODE = 4;

    /**
     * Maximal number of retries for a single failed test.
     */
    public static final int MAXIMAL_NUMBER_OF_RETRIES_FOR_FAILED_TEST = 1;

    /**
     * Default timeout of a test processing in the Test Automation Service.
     * Currently 6 hours.
     */
    public static final long DEFAULT_TEST_TIMEOUT = 6L * ONE_HOUR;

    /**
     * A minimal timeout for actual execution of the test on a test node.
     */
    public static final long MINIMAL_TIMEOUT_FOR_TEST_EXECUTION = FIVE_MINUTES;

    /**
     * Name of command line parameter for specifying a hostname of the Test Automation Service.
     */
    public static final String TEST_AUTOMATION_SERVICE_HOSTNAME_ARGUMENT = "TestAutomationServiceHostname";

    /**
     * Name of command line parameter for specifying a port number used by Test Automation Service
     * for incoming connections.
     */
    public static final String TEST_AUTOMATION_SERVICE_PORT_NUMBER_ARGUMENT = "TestAutomationServicePortNumber";
    
    /**
     * Name of command line parameter for specifying a port number used by Test Automation Service monitor
     * for incoming connections.
     */
    public static final String TEST_AUTOMATION_SERVICE_MONITOR_PORT_NUMBER_ARGUMENT = "MonitorPortNumber";

    /**
     * Default port number for the Test Automation Service.
     * Value 31415 comes from the mathematical Pi constant.
     */
    public static final int TEST_AUTOMATION_SERVICE_DEFAULT_PORT_NUMBER = 31415;

    /**
     * Name of command line parameter for specifying a description for the Test Automation Service instance.
     */
    public static final String TEST_AUTOMATION_SERVICE_DESCRIPTION_ARGUMENT = "TestAutomationServiceDescription";

    /**
     * Name of command line parameter for specifying a port number used by Communicator for incoming connections.
     */
    public static final String TEST_AUTOMATION_COMMUNICATOR_PORT_NUMBER_ARGUMENT = "TestAutomationCommunicatorPortNumber";

    /**
     * Default port number used by Communicator.
     */
    public static final int TEST_AUTOMATION_COMMUNICATOR_DEFAULT_PORT_NUMBER = 27182;

    /**
     * Name of command line parameter for specifying a description for the Test Automation Communicator instance.
     */
    public static final String TEST_AUTOMATION_COMMUNICATOR_DESCRIPTION_ARGUMENT = "TestAutomationCommunicatorDescription";

    /**
     * Name of command line parameter for specifying a port number
     * used by Test Automation Client for all incoming connections.
     */
    public static final String TEST_AUTOMATION_CLIENT_PORT_NUMBER_ARGUMENT = "TestAutomationClientPortNumber";

    /**
     * Default port number for the Test Automation Client.
     * Value 16180 is taken in honor of the mathematical golden ratio number.
     */
    public static final int TEST_AUTOMATION_CLIENT_DEFAULT_PORT_NUMBER = 16180;

    /**
     * Default character used for separating parameter names from their values.
     */
    public static final String NAME_VALUE_SEPARATOR = ":";

    /**
     * Default character used for separating name-value pairs from each other.
     */
    public static final String NAME_VALUE_PAIR_SEPARATOR = ";";

    /**
     * The standard Java regular expression for any character sequence, including for no characters at all.
     */
    public static final String REGULAR_EXPRESSION_FOR_ANY_CHARACTER_SEQUENCE = "(.)*";

    /**
     * Symbol for defining the start of a test resource allocation expression.
     */
    public static final String TEST_RESOURCE_ALLOCATION_EXPRESSION_START_SYMBOL = "(";

    /**
     * Symbol for defining the end of a test resource allocation expression.
     */
    public static final String TEST_RESOURCE_ALLOCATION_EXPRESSION_END_SYMBOL = ")";

    /**
     * Operator for definition of "and" logical relationship between two groups of test resource allocation expressions.
     */
    public static final String TEST_RESOURCE_ALLOCATION_EXPRESSION_AND_OPERATOR = "and";

    /**
     * Operator for definition of "or" logical relationship between two groups of test resource allocation expressions.
     */
    public static final String TEST_RESOURCE_ALLOCATION_EXPRESSION_OR_OPERATOR = "or";

    /**
     * Operator for definition of "not" logical operation on a group of test resource allocation expressions.
     */
    public static final String TEST_RESOURCE_ALLOCATION_EXPRESSION_NOT_OPERATOR = "or";

    /**
     * A character which separates various configuration parameters during their passing to an executor script.
     */
    public static final String PRODUCT_PARAMETERS_SEPARATOR_FOR_EXECUTOR_SCRIPT = ":";

    /**
     * Format of the timestamp values.
     */
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * Format of the calendar dates.
     */
    public static final String DATE_FORMAT = "dd.MM.yyyy";

    /**
     * Format of the hour values.
     */
    public static final String HOUR_FORMAT = "HH:mm";
    
    /**
     * Textual description of unspecified reason for a failure.
     */
    public static final String UNSPECIFIED_REASON_OF_FAILURE = "The reason of failure is not specified";

    /**
     * The standard XML 1.0 declaration used in all XML-based messages supported by the Test Automation Service.
     */
    public static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /**
     * Current release version of the test automation components.
     */
    public static final String TEST_AUTOMATION_RELEASE_VERSION = "aol.alpha0.1";
}
