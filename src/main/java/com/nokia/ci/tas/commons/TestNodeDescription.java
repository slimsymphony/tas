package com.nokia.ci.tas.commons;

/**
 * Incapsulates all information related to a test node inside the Test Automation farm.
 *
 * The XML format used for representing a description about test node:

    <test-node>
        <!-- Hostname of the test node within the Test Automation Service farm -->
        <hostname>test.node.hostname.com</hostname>
        <!-- Port number used by the test node for communications -->
        <port>12345</port>
        <!-- Description about this test node -->
        <description>Description, if any</description>
    </test-node>
 */
public class TestNodeDescription {

    /**
     * XML tag indicating a description about test node.
     */
    public static final String XML_ELEMENT_TEST_NODE = "test-node";

    /**
     * XML tag indicating hostname of the test node.
     */
    public static final String XML_ELEMENT_HOSTNAME = "hostname";

    /**
     * XML tag indicating a port number used by the test node for communications.
     */
    public static final String XML_ELEMENT_PORT_NUMBER = "port";

    /**
     * XML tag indicating description of the test node.
     */
    public static final String XML_ELEMENT_DESCRIPTION = "description";

    /**
     * XML tag indicating version of the Test Automation software running on the test node.
     */
    public static final String XML_ELEMENT_TEST_AUTOMATION_SOFTWARE_VERSION = "test-automation-software-version";

    /**
     * Hostname of the test node.
     */
    private String hostname = "";

    /**
     * Port number used by the test node for communications.
     */
    private int port = 0;

    /**
     * Description about this test node.
     */
    private String description = "";

    /**
     * Version of the Test Automation software running on the test node.
     */
    private String testAutomationSoftwareVersion = "";

    /**
     * Constructor.
     */
    public TestNodeDescription() {
    }

    /**
     * Sets hostname and port of the test node.
     *
     * @param hostname Hostname of the test node
     * @param port Port number of the test node
     */
    public void setHostnameAndPort(String hostname, int port) {
        setHostname(hostname);
        setPort(port);
    }

    /**
     * Sets hostname of the test node.
     *
     * @param hostname Hostname of the test node
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Returns hostname of the test node.
     *
     * @return Hostname of the test node
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets port number of the test node.
     *
     * @param port Port number of the test node
     */
    public void setPort(int port) {
        if (port > 0) {
            this.port = port;
        }
    }

    /**
     * Returns port number of the test node.
     *
     * @return Port number of the test node
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets description of the test node.
     *
     * @param description Description of the test node
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns description of the test node.
     *
     * @return Description of the test node
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets version of the Test Automation software running on the test node.
     *
     * @param testAutomationSoftwareVersion Version of the Test Automation software running on the test node
     */
    public void setTestAutomationSoftwareVersion(String testAutomationSoftwareVersion) {
        this.testAutomationSoftwareVersion = testAutomationSoftwareVersion;
    }

    /**
     * Returns version of the Test Automation software running on the test node.
     *
     * @return Version of the Test Automation software running on the test node
     */
    public String getTestAutomationSoftwareVersion() {
        return testAutomationSoftwareVersion;
    }

    /**
     * Returns a textual representation of the test node description.
     *
     * @return A textual representation of the test node description
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("\n\n Test node description:");

        if (hostname != null && !hostname.isEmpty()) {
            string.append("\n\t Hostname:         " + hostname);
        }

        if (port > 0) {
            string.append("\n\t Port:             " + port);
        }

        if (description != null && !description.isEmpty()) {
            string.append("\n\t Description:      " + description);
        }

        if (testAutomationSoftwareVersion != null && !testAutomationSoftwareVersion.isEmpty()) {
            string.append("\n\t Software version: " + testAutomationSoftwareVersion);
        }

        return string.toString();
    }

    /**
     * Returns XML representation of the test node description.
     *
     * @return XML representation of the test node description
     */
    public String toXML() {
        return toXML("");
    }

    /**
     * Returns XML representation of the test node description with specified indentation.
     *
     * @param indentation Indentation to be used in XML outputs
     * @return XML representation of the test node description with specified indentation
     */
    public String toXML(String indentation) {
        StringBuilder xml = new StringBuilder();

        xml.append(indentation + "<" + XML_ELEMENT_TEST_NODE + ">\n");

        if (hostname != null) {
            xml.append(indentation + "\t<" + XML_ELEMENT_HOSTNAME + ">" + hostname + "</" + XML_ELEMENT_HOSTNAME + ">\n");
        }

        if (port > 0) {
            xml.append(indentation + "\t<" + XML_ELEMENT_PORT_NUMBER + ">" + port + "</" + XML_ELEMENT_PORT_NUMBER + ">\n");
        }

        if (description != null) {
            xml.append(indentation + "\t<" + XML_ELEMENT_DESCRIPTION + ">" + description + "</" + XML_ELEMENT_DESCRIPTION + ">\n");
        }

        if (testAutomationSoftwareVersion != null) {
            xml.append(indentation + "\t<" + XML_ELEMENT_TEST_AUTOMATION_SOFTWARE_VERSION + ">" + testAutomationSoftwareVersion + "</" + XML_ELEMENT_TEST_AUTOMATION_SOFTWARE_VERSION + ">\n");
        }

        xml.append(indentation + "</" + XML_ELEMENT_TEST_NODE + ">\n");

        return xml.toString();
    }
}
