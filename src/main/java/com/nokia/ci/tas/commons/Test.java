package com.nokia.ci.tas.commons;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Incapsulates all the information required for describing a single test performed by the Testing Automation Service.
 *
 * The XML format used for representing tests:

    <!-- Base format for describing a test perfomed inside the Test Automation Service -->
    <!-- All test descriptions are always encoded in the UTF-8 format -->
    <?xml version="1.0" encoding="UTF-8"?>
    <!-- The "test" root element contains all data required to describe a test -->
    <test>
        <!-- Test id is always mandatory and it cannot contain any characters that are not accepted by Linux and Windows file systems and by the XML 1.0 standard -->
        <id>Test id</id>

        <!-- Sub id is indication of a splitted sub-test, being just a portion of its parent test.
             Sub ids must contain only the characters accepted by Linux and Windows file systems and by the XML 1.0 standard -->
        <sub-id>Sub id</sub-id>

        <!-- Path to the directory where the test artifacts were originally generated or stored on the side of SAIT plugin -->
        <workspace-path>/path/to/test/artifacts/</workspace-path>

        <!-- A URL optionally related to the test -->
		<url>http://test_build_machine_1.domain.com/test_workspace/</url>

        <!-- Name of the application which will be responsible for actual performing of the test on a test node -->
        <executor-application>ipy.exe</executor-application>

        <!-- Name of the script or other application which will be responsible for test's workflow after launching on a test node -->
        <executor-script>script.py</executor-script>

        <!-- Target is always mandatory and tell if reserved products will be flashed (in case of "flash" target) or emulated (in case of "nose" target) -->
        <target>flash | nose</target>

        <!-- Product releasing mode is mandatory and tells if reserved products will be released automatically by the Test Automation Service or manually by the test issuer -->
        <product-releasing-mode>automatic | manual</product-releasing-mode>

        <!-- Product disconnection timeout for the test in milliseconds -->
        <product-disconnection-timeout>1234567890</product-disconnection-timeout>

        <!-- Name of the file which will contain results of the test (logs, reports, dumps, etc.) that executor script should generate together with the executor application after the test is finished on the test node. Usually this is simply a ZIP archive file containing the files that SAIT should be aware of -->
        <results-filename>Filename1.xyz</results-filename>

        <!-- Current status of the test -->
        <status>unknown | pending | started | stopped | finished | failed</status>

        <!-- Some additional information related to the test's current status, if available -->
        <status-details>...</status-details>

        <!-- Timestamp of the moment (since Unix epoch) when test was started executing on the test node -->
        <start-time>1234567890123</start-time>

        <!-- Timeout of the test in milliseconds -->
        <timeout>1234567890</timeout>

        <!-- A list of all files involved in the test, called "test artifacts" in Test Automation Service terminology. Test artifacts also include the executor script, since it is also delivered to the test node. But the executor application is assumed to be a resident of the test node, like Iron Python or Perl. Otherwise it should be also delivered to the test node as one of the test's artifacts -->
        <artifacts>
            <!-- Each test artifact is identificated by the name of corresponding file existing in test's workspace directory -->
            <artifact>Filename1.abc</artifact>
            <artifact>Filename2.efg</artifact>
            <artifact>Filename2.hjk</artifact>
                          ...
            <artifact>FilenameN.zyx</artifact>
        </artifacts>
		<!-- Environment Parameters -->
		<envparams>
			<envparam><key>AOL_HOME</key><value>/home/aol</value></envparam>
		</envparams>
        <!-- A list of test sets that could be executed in parallel and independently from each other -->
        <parallel-test-sets>
            <!-- Each test set is identificated by the name of corresponding file existing in test's workspace directory -->
            <parallel-test-set>Test_set_1.zip</parallel-test-set>
            <parallel-test-set>Test_set_2.zip</parallel-test-set>
                                   ...
            <parallel-test-set>Test_set_N.zip</parallel-test-set>
        </parallel-test-sets>

        <!-- A list of test packages that could be executed in parallel and independently from each other -->
        <test-packages>
            <test-package>
                Description of test package 1...
            </test-package>
            <test-package>
                Description of test package 2...
            </test-package>
                ...
            <test-package>
                Description of test package N...
            </test-package>
        </test-packages>

        <!-- An expression describing the environment that test execution is requiring from the test farm -->
        <required-environment>(rm-code:RM-123;) and (rm-code:RM-321;hostname:TestNode1.Domain.com;)</required-environment>

        <!-- A list of all products required to perform this test (can be empty if test has specified the "nose" target) -->
        <required-products>
            <!-- The list of required products is populated by the test issuer and contents of each product element are heavily depending on the test configuration -->
            <product>
                Description of required product 1...
            </product>
            <product>
                Description of required product 2...
            </product>
               ...
            <product>
                Description of required product N...
            </product>
        </required-products>

        <!-- A list of all products reserved for this test (can be empty if test has specified the "nose" target) -->
        <reserved-products>
            <!-- The list of reserved products is populated by the Test Automation Service after the products were successfully reserved from the test farm -->
            <product>
                Description of reserved product 1...
            </product>
            <product>
                Description of reserved product 2...
            </product>
               ...
            <product>
                Description of reserved product N...
            </product>
        </reserved-products>
    </test>
 */
public class Test {
    /**
     * XML tag indicating the test description.
     */
    public static final String XML_ELEMENT_TEST = "test";

    /**
     * XML tag indicating the test id.
     */
    public static final String XML_ELEMENT_ID = "id";

    /**
     * XML tag indicating the test sub-id.
     */
    public static final String XML_ELEMENT_SUB_ID = "sub-id";

    /**
     * XML tag indicating the path to the directory where test's artifacts are stored.
     */
    public static final String XML_ELEMENT_WORKSPACE_PATH = "workspace-path";

    /**
     * XML tag indicating the URL related to test.
     */
    public static final String XML_ELEMENT_URL = "url";

    /**
     * XML tag indicating the applicaiton which is responsible for launching test executions on a test node.
     */
    public static final String XML_ELEMENT_EXECUTOR_APPLICATION = "executor-application";

    /**
     * XML tag indicating the script which is responsible for the actual test execution on a test node.
     */
    public static final String XML_ELEMENT_EXECUTOR_SCRIPT = "executor-script";

    /**
     * XML tag indicating the target that test is aiming.
     */
    public static final String XML_ELEMENT_TARGET = "target";

    /**
     * Textual representation of the FLASH target.
     */
    public static final String TARGET_FLASH = "flash";

    /**
     * Textual representation of the NOSE target.
     */
    public static final String TARGET_NOSE = "nose";

    /**
     * XML tag indicating the product releasing mode used in a test.
     */
    public static final String XML_ELEMENT_PRODUCT_RELEASING_MODE = "product-releasing-mode";

    /**
     * Textual representation of the automatic product releasing mode.
     */
    public static final String PRODUCT_RELEASING_MODE_AUTOMATIC = "automatic";

    /**
     * Textual representation of the manual product releasing mode.
     */
    public static final String PRODUCT_RELEASING_MODE_MANUAL = "manual";

    /**
     * XML tag indicating the product disconnection timeout used in a test.
     */
    public static final String XML_ELEMENT_PRODUCT_DISCONNECTION_TIMEOUT = "product-disconnection-timeout";

    /**
     * XML tag indicating timestamp of the moment when this test was started.
     */
    public static final String XML_ELEMENT_START_TIME = "start-time";

    /**
     * XML tag indicating the timeout of test's execution.
     */
    public static final String XML_ELEMENT_TIMEOUT = "timeout";

    /**
     * XML tag indicating the name of file which should contain test results.
     */
    public static final String XML_ELEMENT_RESULTS_FILENAME = "results-filename";

    /**
     * XML tag indicating the status of test.
     */
    public static final String XML_ELEMENT_STATUS = "status";

    /**
     * Textual representation of the UNKNOWN status.
     */
    public static final String STATUS_UNKNOWN = "unknown";

    /**
     * Textual representation of the PENDING status.
     */
    public static final String STATUS_PENDING = "pending";

    /**
     * Textual representation of the STARTED status.
     */
    public static final String STATUS_STARTED = "started";

    /**
     * Textual representation of the STOPPED status.
     */
    public static final String STATUS_STOPPED = "stopped";

    /**
     * Textual representation of the FINISHED status.
     */
    public static final String STATUS_FINISHED = "finished";

    /**
     * Textual representation of the FAILED status.
     */
    public static final String STATUS_FAILED = "failed";

    /**
     * XML tag indicating details (if any) regarding the current status of the test.
     */
    public static final String XML_ELEMENT_STATUS_DETAILS = "status-details";

    /**
     * XML tag indicating the list of artifacts used in test.
     */
    public static final String XML_ELEMENT_ARTIFACTS = "artifacts";

    /**
     * XML tag indicating the a single artifact used in test.
     */
    public static final String XML_ELEMENT_ARTIFACT = "artifact";
    
    public static final String XML_ELEMENT_ENVPARAMS = "envparams";
    
    public static final String XML_ELEMENT_ENVPARAM = "envparam";
    
    public static final String XML_ELEMENT_ENVPARAM_KEY = "key";
    
    public static final String XML_ELEMENT_ENVPARAM_VALUE = "value";

    /**
     * XML tag indicating the list of test packages associated with this test.
     */
    public static final String XML_ELEMENT_TEST_PACKAGES = "test-packages";

    /**
     * XML tag indicating an expression describing the environment required to execute this test.
     */
    public static final String XML_ELEMENT_REQUIRED_ENVIRONMENT = "required-environment";

    /**
     * XML tag indicating the list of products required to execute this test.
     */
    public static final String XML_ELEMENT_REQUIRED_PRODUCTS = "required-products";

    /**
     * XML tag indicating the list of products reserved for this test execution by the Test Automation Service.
     */
    public static final String XML_ELEMENT_RESERVED_PRODUCTS = "reserved-products";

    /**
     * Supported product releasing modes.
     *
     * AUTOMATICALLY_RELEASE_RESERVED_PRODUCTS mode means that reserved products should be automatically released
     * by the Test Automation Service after the test.
     *
     * MANUALLY_RELEASE_RESERVED_PRODUCTS mode means that reserved products should be manually released
     * by test issuers (or any other capable applications) after the test.
     */
    public enum ProductReleasingMode {
        AUTOMATICALLY_RELEASE_RESERVED_PRODUCTS,
        MANUALLY_RELEASE_RESERVED_PRODUCTS
    };

    /**
     * Supported test targets.
     *
     * FLASH target is used by tests which are actually flashing products.
     * NOSE target is used by test which are not using any physical products.
     */
    public enum Target {
        FLASH,
        NOSE
    };

    /**
     * Supported test states:
     *
     * UNKNOWN  - Meaning that test was just created or not yet initialized
     * PENDING  - Meaning that test was accepted by the Test Automation Service but not yet started on any of the test nodes
     * STARTED  - Meaning that test was successfully started on the assigned test node
     * STOPPED  - Meaning that test was stopped by some Test Automation Service component
     * FINISHED - Meaning that test has ended its execution on a test node and has returned file with test result back to the test issuer
     * FAILED   - Meaning that test was considered as failed for some reason
     */
    public enum Status {
        UNKNOWN,
        PENDING,
        STARTED,
        STOPPED,
        FINISHED,
        FAILED
    };

    /**
     * Unique identificator of the test.
     * Test id is used by Test Automation Service and Communicators to logically separate tests from each other.
     * A proper test id may contain any ASCII letters that Windows and Linux operating systems
     * are allowing to use in names of file or directory.
     */
    private String id = "";

    /**
     * Optional id for identifying a splitted sub-test.
     * Splitted ids are used when test is sub-divided into a few parts across its parallel test sets.
     * In that case each sub-test will be responsible for executing its own portion of parallel test sets.
     * A proper splitting id may contain only those ASCII letters that Windows and Linux operating systems
     * are allowing to use in names of file or directory.
     */
    private String subId = "";

    /**
     * Filepath to the directory where test's artifacts are located.
     */
    private String workspacePath = "";

    /**
     * URL related to the test.
     */
    private String url = "";

    /**
     * Name of the application which is installed to the testing node for execution of delivered tests.
     * Right now it is Iron Python by default, but might be something else in the future.
     */
    private String executorApplication = "";

    /**
     * Name of the script which actually executes test on a testing node.
     * If executor script will be specified, it will be automatically added to the list of test's artifacts.
     */
    private String executorScript = "";
    
    /**
     * Environment parameters for executor. Define env param here and executor will set those env paramters before execution.
     */
    private Map<String,String> executorEnvparams = new HashMap<String,String>();
    
    /**
     * Parameters for execute the main scripts. All the parameters should be stored in String form.
     */
    private List<String> executorParameters = new ArrayList<String>();

    /**
     * Name of the test results archive that should be generated by executor script
     * and then copied from testing node to Test Automation Service.
     */
    private String resultsFilename = "";

    /**
     * Current status of the test.
     */
    private Status status = Status.UNKNOWN;

    /**
     * Some details related to the current status.
     */
    private String statusDetails = "";

    /**
     * List of filenames representing all the artifacts required
     * to run the test on real phones.
     */
    private List<String> artifacts;

    /**
     * A list of test packages associated with this test.
     */
    private List<TestPackage> testPackages;

    /**
     * Product releasing mode of products reserved for needs of test.
     */
    private ProductReleasingMode productReleasingMode = ProductReleasingMode.AUTOMATICALLY_RELEASE_RESERVED_PRODUCTS;

    /**
     * Product disconnection timeout used in test.
     */
    private long productDisconnectionTimeout = Constant.FIVE_MINUTES;

    /**
     * Target of the test.
     */
    private Target target = Target.FLASH;

    /**
     * A list of products (real phones) required to execute this test.
     */
    private List<Product> requiredProducts;

    /**
     * An expression describing the environment required to execute this test.
     */
    private String requiredEnvironment = "";

    /**
     * A list of product (real phones) reserved by Test Automation Service for this test.
     */
    private List<Product> reservedProducts;

    /**
     * Timestamp of the moment when this test was actually started.
     */
    private long startTime = 0L;

    /**
     * Timeout setting for the test in milliseconds.
     * Any negative or zero value in timeout will set timeout to default value of 6 hours.
     */
    private long timeout = Constant.DEFAULT_TEST_TIMEOUT;

    /**
     * Default constructor.
     *
     * @param id Unique id for the test
     */
    public Test(String id) {
        this.id = id;
        artifacts = new ArrayList<String>(0);
        requiredProducts = new ArrayList<Product>(0);
        reservedProducts = new ArrayList<Product>(0);
        testPackages = new ArrayList<TestPackage>(0);
    }

    /**
     * Constructor with product releasing mode.
     *
     * @param id Unique id for the test
     * @param productReleasingMode Releazing mode to be used for all reserved products
     */
    public Test(String id, ProductReleasingMode productReleasingMode) {
        this(id);
        this.productReleasingMode = productReleasingMode;
    }

    /**
     * Constructor with test target.
     *
     * @param id Unique id for the test
     * @param target Target of the test
     */
    public Test(String id, Target target) {
        this(id, Test.ProductReleasingMode.AUTOMATICALLY_RELEASE_RESERVED_PRODUCTS);
        this.target = target;
    }

    /**
     * Constructor with product releasing mode and target.
     *
     * @param id Unique id for the test
     * @param productReleasingMode Releazing mode to be used for all reserved products
     * @param target Target of the test
     */
    public Test(String id, ProductReleasingMode productReleasingMode, Target target) {
        this(id, productReleasingMode);
        this.target = target;
    }

    /**
     * Copy constructor.
     *
     * @param other Instance of the test to be copied
     */
    public Test(Test other) {
        this(other.getId(), other.getProductReleasingMode(), other.getTarget());

        this.subId = other.getSubId();
        this.workspacePath = other.getWorkspacePath();
        this.url = other.getURL();
        this.executorApplication = other.getExecutorApplication();
        this.executorScript = other.getExecutorScript();
        this.resultsFilename = other.getResultsFilename();
        this.status = other.getStatus();
        this.statusDetails = other.getStatusDetails();
        this.productDisconnectionTimeout = other.getProductDisconnectionTimeout();
        this.requiredEnvironment = other.getRequiredEnvironment();
        this.startTime = other.getStartTime();
        this.timeout = other.getTimeout();

        this.artifacts = new ArrayList<String>(other.getArtifacts());
        this.requiredProducts = new ArrayList<Product>(other.getRequiredProducts());
        this.reservedProducts = new ArrayList<Product>(other.getReservedProducts());
    }

    /**
     * Sets id of the test.
     *
     * @param id Id of the test
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets id of the test
     *
     * @return Id of the test
     */
    public String getId() {
        return id;
    }

    /**
     * Sets sub-id of the test.
     *
     * @param subId Sub-id of the test
     */
    public void setSubId(String subId) {
        this.subId = subId;
    }

    /**
     * Gets sub-id of the test
     *
     * @return Sub-id of the test
     */
    public String getSubId() {
        return subId;
    }

    /**
     * Runtime id is a combination of initial id and possibly obtained sub-id.
     *
     * @return A combination of initial id and possibly obtained sub-id
     */
    public String getRuntimeId() {
        return id + subId;
    }

    /**
     * Sets the target of the test.
     *
     * @param target Target of the test
     */
    public void setTarget(Target target) {
        this.target = target;
    }

    /**
     * Returns a target assigned to the test.
     *
     * @return A target assigned to the test
     */
    public Target getTarget() {
        return target;
    }

    /**
     * Sets the list of products required to execute this test.
     *
     * @param products List of products required to execute this test
     */
    public void setRequiredProducts(List<Product> products) {
        requiredProducts = products;
    }

    /**
     * Returns the list of products required to execute this test.
     *
     * @return The list of products required to execute this test
     */
    public List<Product> getRequiredProducts() {
        return requiredProducts;
    }

    /**
     * Adds specified product to the list of required ones.
     *
     * @param product Product to be added to the list of required ones
     */
    public void addRequiredProduct(Product product) {
        if (requiredProducts == null) {
            requiredProducts = new ArrayList<Product>(0);
        }

        if (product != null) {
            if (!requiredProducts.contains(product)) {
                requiredProducts.add(product);
            }
        }
    }

    /**
     * Sets an expression describing the environment required to execute this test.
     *
     * @param expression An expression describing the environment required to execute this test
     */
    public void setRequiredEnvironment(String requiredEnvironment) {
        if (requiredEnvironment != null) {
            this.requiredEnvironment = requiredEnvironment;
        }
    }

    /**
     * Returns an expression describing the environment required to execute this test.
     *
     * @return An expression describing the environment required to execute this test
     */
    public String getRequiredEnvironment() {
        return requiredEnvironment;
    }

    /**
     * Sets the list of products reserved for this test by Test Automation Service.
     *
     * @param product The list of products reserved for this test by Test Automation Service
     */
    public void setReservedProducts(List<Product> products) {
        reservedProducts = products;
    }

    /**
     * Returns the list of products reserved for this test by Test Automation Service.
     *
     * @return The list of products reserved for this test by Test Automation Service
     */
    public List<Product> getReservedProducts() {
        return reservedProducts;
    }

    /**
     * Sets start time of the test in milliseconds from the Unix epoch.
     * Any negative or zero value will be ignored.
     *
     * @param time Start time of the test in milliseconds from the Unix epoch
     */
    public void setStartTime(long time) {
        if (time > 0L) {
            this.startTime = time;
        }
    }

    /**
     * Returns start time of the test in milliseconds since the Unix epoch.
     *
     * @return Start time of the test in milliseconds since the Unix epoch
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Sets timeout of the test in milliseconds.
     * Any negative or zero value will be ignored.
     *
     * @param timeout Test timeout in milliseconds
     */
    public void setTimeout(long timeout) {
        if (timeout > 0L) {
            this.timeout = timeout;
        }
    }

    /**
     * Returns timeout of the test in milliseconds.
     *
     * @return Test timeout in milliseconds
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Utility method for checking if test has expired or not.
     *
     * @param testStartedAt Moment of time when test processing has started
     * @return Returns true if a difference between the moment of test's start
     * and current moment is larger than timeout value, or false otherwise
     */
    public boolean isExpired(long testStartedAt) {

        if ((System.currentTimeMillis() - testStartedAt) > timeout) {
            return true;
        }

        return false;
    }

    /**
     * Sets path to the test artifacts on the side of test's issuer.
     *
     * @param workspacePath Path to the test artifacts on the side of test's issuer
     */
    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    /**
     * Gets path to the test artifacts on the side of test's issuer.
     *
     * @return Path to the test artifacts on the side of test's issuer
     */
    public String getWorkspacePath() {
        return workspacePath;
    }

    /**
     * Sets URL related to the test.
     *
     * @param url URL related to the test
     */
    public void setURL(String url) {
        this.url = url;
    }

    /**
     * Gets URL related to the test.
     *
     * @return URL related to the test
     */
    public String getURL() {
        return url;
    }

    /**
     * Sets name of the applicaiton which is installed on the test node for actual execution of delivered tests.
     * Right now it is Iron Python by default, but might be something else in the future.
     *
     * @param executorApplication Name of the applicaiton which is installed on the test node for actual execution of delivered tests
     */
    public void setExecutorApplication(String executorApplication) {
        this.executorApplication = executorApplication;
    }

    /**
     * Returns name of the application which is installed on the test node for actual execution of delivered tests.
     * Right now it is Iron Python by default, but might be something else in the future.
     *
     * @return Name of the application which is installed on the test node for actual execution of delivered tests
     */
    public String getExecutorApplication() {
        return executorApplication;
    }

    /**
     * Sets name of the script which will actually execute test on the test node.
     *
     * @param executorScript Name of the script which will actually execute test on the test node
     */
    public void setExecutorScript(String executorScript) {
        this.executorScript = executorScript;

        // Executor script should be automatically added to the artifacts
        addArtifact(executorScript);
    }

    /**
     * Returns name of the script which will actually execute test on the test node.
     *
     * @return Name of the script which will actually execute test on the test node
     */
    public String getExecutorScript() {
        return executorScript;
    }

    public Map<String, String> getExecutorEnvparams() {
		return executorEnvparams;
	}

	public void setExecutorEnvparams( Map<String, String> executorEnvparams ) {
		this.executorEnvparams = executorEnvparams;
	}
	
	public void addexecutorEnvparam( String key, String value ) {
		this.executorEnvparams.put( key, value );
	}

	public List<String> getExecutorParameters() {
		return executorParameters;
	}

	public void setExecutorParameters( List<String> executorParameters ) {
		this.executorParameters = executorParameters;
	}

	/**
     * Sets name of the archive file which will contain test results or reports.
     *
     * @param resultsFilename Name of the archive file which will contain test results or reports
     */
    public void setResultsFilename(String resultsFilename) {
        this.resultsFilename = resultsFilename;
    }

    /**
     * Returns name of the archive file which will contain test results or reports.
     *
     * @return Name of the archive file which will contain test results or reports
     */
    public String getResultsFilename() {
        return resultsFilename;
    }

    /**
     * Sets current status of the test.
     *
     * @param status Current status of the test
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Returns current status of the test.
     *
     * @return Current status of the test
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets details related to the current status of the test.
     *
     * @param statusDetails Some details related to the current status of the test
     */
    public void setStatusDetails(String statusDetails) {
        this.statusDetails = statusDetails;
    }

    /**
     * Returns details (if any) related to the current status of the test.
     *
     * @return Details (if any) related to the current status of the test
     */
    public String getStatusDetails() {
        return statusDetails;
    }

    /**
     * Sets current status of the test together with some status details.
     *
     * @param status Current status of the test
     * @param statusDetails Some details related to the current status of the test
     */
    public void setStatus(Status status, String statusDetails) {
        this.status = status;
        this.statusDetails = statusDetails;
    }

    /**
     * Sets a list of artifacts (files) to be used in this test.
     *
     * @param artifacts A list of artifacts (files) to be used in this test
     */
    public void setArtifacts(List<String> artifacts) {
        this.artifacts = artifacts;
    }

    /**
     * Returns a list of artifacts (files) to be used in this test.
     *
     * @return A list of artifacts (files) to be used in this test
     */
    public List<String> getArtifacts() {
        return artifacts;
    }

    /**
     * Adds specified artifact (file) into the list of artifacts to be used in this test.
     *
     * @param artifact Artifact (file) to be added into the list of artifacts used in this test
     */
    public void addArtifact(String artifact) {

        if (!artifacts.contains(artifact)) {
            artifacts.add(artifact);
        }
    }

    /**
     * Sets a list of test packages that could be executed in parallel or independently from each other.
     *
     * @param testPackages A list of test packages that could be executed in parallel or independently from each other
     */
    public void setTestPackages(List<TestPackage> testPackages) {
        if (testPackages != null) {
            this.testPackages = testPackages;
        }
    }

    /**
     * Returns a list of test packages that could be executed in parallel or independently from each other.
     *
     * @return A list of test packages that could be executed in parallel or independently from each other
     */
    public List<TestPackage> getTestPackages() {
        return testPackages;
    }

    /**
     * Adds a test package that could be executed in parallel or independently from the others.
     *
     * @param parallelTestSet A test package that could be executed in parallel or independently from the others
     */
    public void addTestPackage(TestPackage testPackage) {

        if (testPackage != null) {
            if (testPackages == null) {
                testPackages = new ArrayList<TestPackage>(0);
            }

            testPackages.add(testPackage);
        }
    }

    /**
     * Returns a number of test packages available to this test.
     *
     * @return A number of test packages available to this test
     */
    public int getNumberOfTestPackages() {
        return testPackages.size();
    }

    /**
     * Sets a releasing mode of reserved products.
     *
     * @param productReleasingMode Releasing mode of reserved products
     */
    public void setProductReleasingMode(ProductReleasingMode productReleasingMode) {
        this.productReleasingMode = productReleasingMode;
    }

    /**
     * Returns a releasing mode of reserved products.
     *
     * @return A releasing mode of reserved products
     */
    public ProductReleasingMode getProductReleasingMode() {
        return productReleasingMode;
    }

    /**
     * Sets a product disconnection timeout for this test in milliseconds.
     * Please note that any negative or zero value will be ignored.
     *
     * @param timeout Product disconnection timeout in milliseconds
     */
    public void setProductDisconnectionTimeout(long timeout) {
        if (timeout > 0L) {
            productDisconnectionTimeout = timeout;
        }
    }

    /**
     * Returns a product disconnection timeout specified for this test.
     *
     * @return Product disconnection timeout specified for this test
     */
    public long getProductDisconnectionTimeout() {
        return productDisconnectionTimeout;
    }

    /**
     * Creates a textual representation of the object.
     *
     * @return A textual representation of the object
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("Test:");
        string.append("\n\t Id:                     " + id);
        if (subId != null && !subId.isEmpty()) {
            string.append("\n\t Sub-id:                 " + subId);
        }
        string.append("\n\t Workspace path:         " + workspacePath);

        if (url != null && !url.isEmpty()) {
            string.append("\n\t URL:                    " + url);
        }

        string.append("\n\t Executor application:   " + executorApplication);
        string.append("\n\t Executor script:        " + executorScript);
        string.append("\n\t Results filename:       " + resultsFilename);

        if (statusDetails != null && !statusDetails.isEmpty()) {
            string.append("\n\t Status:                 " + status.name() + " - " + statusDetails);
        } else {
            string.append("\n\t Status:                 " + status.name());
        }

        string.append("\n\t Product releasing mode: " + productReleasingMode.name());
        string.append("\n\t Target:                 " + target.name());
        string.append("\n\t Product disconnection timeout: " + Util.convert(productDisconnectionTimeout));

        if (startTime > 0L) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constant.TIMESTAMP_FORMAT);
            string.append("\n\t Start time:             " + simpleDateFormat.format(new Date(startTime)));
        }

        if (timeout > 0L) {
            string.append("\n\t Timeout:                " + Util.convert(timeout));
        } else {
            string.append("\n\t Timeout:                not specified");
        }

        if (artifacts != null && !artifacts.isEmpty()) {
            string.append("\n\t Has " + artifacts.size() + " artifacts:");

            for (String artifact : artifacts) {
                string.append("\n\t\t " + artifact);
            }
        }
        
        if (this.executorEnvparams != null && !executorEnvparams.isEmpty()) {
            string.append("\n\t Has " + executorEnvparams.size() + " Environment params:");

            for (String key : executorEnvparams.keySet()) {
                string.append("\n\t\t " + key+":"+executorEnvparams.get( key ));
            }
        }

        if (testPackages != null) {
            string.append("\n\t Has " + testPackages.size() + " test packages:");

            for (TestPackage testPackage : testPackages) {
                string.append("\n" + testPackage.toString());
            }
        }

        if (requiredEnvironment != null && !requiredEnvironment.isEmpty()) {
            string.append("\n\t Required environment:   " + requiredEnvironment);
        }

        if (requiredProducts != null && !requiredProducts.isEmpty()) {
            if (requiredProducts.size() == 1) {
                string.append("\n\n\t Test has 1 required product:");
            } else {
                string.append("\n\n\t Test has " + requiredProducts.size() + " required products:");
            }

            for (Product requiredProduct : requiredProducts) {
                string.append(requiredProduct.toString());
            }
        }

        if (reservedProducts != null && !reservedProducts.isEmpty()) {
            if (reservedProducts.size() == 1) {
                string.append("\n\n\t Test has 1 reserved product:");
            } else {
                string.append("\n\n\t Test has " + reservedProducts.size() + " reserved products:");
            }

            for (Product reservedProduct : reservedProducts) {
                string.append(reservedProduct.toString());
            }
        }

        return string.toString();
    }

    /**
     * Returns XML representation of the test.
     *
     * @return XML representation of the test
     */
    public String toXML() {
        return toXML("");
    }

    /**
     * Returns XML representation of the test with specified indentation.
     *
     * @param indentation Indentation to be used in XML outputs
     * @return XML representation of the test with specified indentation
     */
    public String toXML(String indentation) {
        StringBuilder xml = new StringBuilder();

        xml.append(indentation + "<" + XML_ELEMENT_TEST + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_ID + ">" + id + "</" + XML_ELEMENT_ID + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_SUB_ID + ">" + subId + "</" + XML_ELEMENT_SUB_ID + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_WORKSPACE_PATH + ">" + workspacePath + "</" + XML_ELEMENT_WORKSPACE_PATH + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_URL + ">" + url + "</" + XML_ELEMENT_URL + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_TARGET + ">");
        if (target == Target.FLASH) {
            xml.append(TARGET_FLASH);
        } else if (target == Target.NOSE) {
            xml.append(TARGET_NOSE);
        } else {
            xml.append(TARGET_FLASH); // Default
        }
        xml.append("</" + XML_ELEMENT_TARGET + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_PRODUCT_RELEASING_MODE + ">");
        if (productReleasingMode == ProductReleasingMode.AUTOMATICALLY_RELEASE_RESERVED_PRODUCTS) {
            xml.append(PRODUCT_RELEASING_MODE_AUTOMATIC);
        } else if (productReleasingMode == ProductReleasingMode.MANUALLY_RELEASE_RESERVED_PRODUCTS) {
            xml.append(PRODUCT_RELEASING_MODE_MANUAL);
        } else {
            xml.append(PRODUCT_RELEASING_MODE_AUTOMATIC); // Default
        }
        xml.append("</" + XML_ELEMENT_PRODUCT_RELEASING_MODE + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_PRODUCT_DISCONNECTION_TIMEOUT + ">" + productDisconnectionTimeout + "</" + XML_ELEMENT_PRODUCT_DISCONNECTION_TIMEOUT + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_STATUS + ">");
        if (status == Status.UNKNOWN) {
            xml.append(STATUS_UNKNOWN);
        } else if (status == Status.PENDING) {
            xml.append(STATUS_PENDING);
        } else if (status == Status.STARTED) {
            xml.append(STATUS_STARTED);
        } else if (status == Status.STOPPED) {
            xml.append(STATUS_STOPPED);
        } else if (status == Status.FINISHED) {
            xml.append(STATUS_FINISHED);
        } else if (status == Status.FAILED) {
            xml.append(STATUS_FAILED);
        } else {
            xml.append(STATUS_UNKNOWN); // Default
        }
        xml.append("</" + XML_ELEMENT_STATUS + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_STATUS_DETAILS + ">" + statusDetails + "</" + XML_ELEMENT_STATUS_DETAILS + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_START_TIME + ">" + startTime + "</" + XML_ELEMENT_START_TIME + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_TIMEOUT + ">" + timeout + "</" + XML_ELEMENT_TIMEOUT + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_EXECUTOR_APPLICATION + ">" + executorApplication + "</" + XML_ELEMENT_EXECUTOR_APPLICATION + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_EXECUTOR_SCRIPT + ">" + executorScript + "</" + XML_ELEMENT_EXECUTOR_SCRIPT + ">\n");

        if (artifacts != null && !artifacts.isEmpty()) {
            xml.append(indentation + "\t<" + XML_ELEMENT_ARTIFACTS + ">\n");
            for (String currentArtifact : artifacts) {
                xml.append(indentation + "\t\t<" + XML_ELEMENT_ARTIFACT + ">" + currentArtifact + "</" + XML_ELEMENT_ARTIFACT + ">\n");
            }
            xml.append(indentation + "\t</" + XML_ELEMENT_ARTIFACTS + ">\n");
        }
        
        if(this.executorEnvparams!=null && !this.executorEnvparams.isEmpty()) {
        	xml.append(indentation + "\t</" + XML_ELEMENT_ENVPARAMS + ">\n");
        	for( String key : this.executorEnvparams.keySet() ) {
        		xml.append(indentation + "\t\t<" + XML_ELEMENT_ENVPARAM + ">");
        		xml.append( "<" ).append( XML_ELEMENT_ENVPARAM_KEY ).append( ">" ).append( key ).append( "</" ).append( XML_ELEMENT_ENVPARAM_KEY ).append( ">" );
        		xml.append( "<" ).append( XML_ELEMENT_ENVPARAM_VALUE ).append( ">" ).append( this.executorEnvparams.get( key ) ).append( "</" ).append( XML_ELEMENT_ENVPARAM_VALUE ).append( ">" );
        		xml.append("</" + XML_ELEMENT_ENVPARAM + ">\n");
        	}
        	xml.append(indentation + "\t</" + XML_ELEMENT_ENVPARAMS + ">\n");
        }
        
        if (testPackages != null && !testPackages.isEmpty()) {
            xml.append(indentation + "\t<" + XML_ELEMENT_TEST_PACKAGES + ">\n");
            for (TestPackage testPackage : testPackages) {
                xml.append(testPackage.toXML(indentation + "\t") + "\n");
            }
            xml.append(indentation + "\t</" + XML_ELEMENT_TEST_PACKAGES + ">\n");
        }

        xml.append(indentation + "\t<" + XML_ELEMENT_RESULTS_FILENAME + ">" + resultsFilename + "</" + XML_ELEMENT_RESULTS_FILENAME + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_REQUIRED_ENVIRONMENT + ">" + requiredEnvironment + "</" + XML_ELEMENT_REQUIRED_ENVIRONMENT + ">\n");

        if (requiredProducts != null && !requiredProducts.isEmpty()) {
            xml.append(indentation + "\t<" + XML_ELEMENT_REQUIRED_PRODUCTS + ">\n");
            for (Product requiredProduct : requiredProducts) {
                xml.append(requiredProduct.toXML(indentation + "\t\t"));
            }
            xml.append(indentation + "\t</" + XML_ELEMENT_REQUIRED_PRODUCTS + ">\n");
        }

        if (reservedProducts != null && !reservedProducts.isEmpty()) {
            xml.append(indentation + "\t<" + XML_ELEMENT_RESERVED_PRODUCTS + ">\n");
            for (Product reservedProduct : reservedProducts) {
                xml.append(reservedProduct.toXML(indentation + "\t\t"));
            }
            xml.append(indentation + "\t</" + XML_ELEMENT_RESERVED_PRODUCTS + ">\n");
        }

        xml.append(indentation + "</" + XML_ELEMENT_TEST + ">\n");

        return xml.toString();
    }
}
