package com.nokia.ci.tas.commons.message;

import com.nokia.ci.tas.commons.TestNodeDescription;

/**
 * Incapsulates all information required to describe a registry operation inside the Test Automation Services.
 * Registry operation may be used by a Test Automation Service in adding or removing registries for a remote part.
 *
 * The XML format used for representing a single Registry Operation:

    <!-- All registry operation messages must be encoded in UTF-8 and each line must end with the new-line symbol '\n' -->
    <?xml version="1.0" encoding="UTF-8"?>
    <message>
        <!-- Type of the registry message must be specified as "registry-operation" -->
        <type>registry-operation</type>
        <!-- "Sender" here is either Test Automation Client or Test Automation Communicator -->
        <sender>
            <!-- Hostname of the Test Automation Client or Test Automation Communicator -->
            <hostname>sender.hostname.com</hostname>
            <!-- Listening port of the Test Automation Client or Test Automation Communicator -->
            <port>12345</port>
        </sender>
        <!-- "Receiver" here is Test Automation Service -->
        <receiver>
            <!-- Hostname of the Test Automation Service -->
            <hostname>receiver.hostname.com</hostname>
            <!-- Listening port of the Test Automation Service -->
            <port>23456</port>
        </receiver>
        <!-- Registry operation message requires that envelope will contain a description about the remote part: either Test Automation Client or Test Automation Communicator -->
        <envelope>
            <!-- Id of related registry operation:
                 "register" operation means a new Test Automation Client or Test Automation Communicator will be working with this Test Automation Service
                 "deregister" operation means that Test Automation Client or Test Automation Communicator will be not anymore working with this Test Automation Service
                 "update" operation means that Test Automation Client or Test Automation Communicator are updating their contacts to the ones mentioned in envelope -->
            <operation>register | deregister | update</operation>
            <!-- The "remote" element describes the category of a remote part
                 "test-node" must be used by the Test Automation Communicators
                 "client" must be used by the Test Automation Client -->
            <remote>test-node | client</remote>
            <!-- Test node can put more details about itself inside this block -->
            <test-node>
                Description of related test node...
            </test-node>
        </envelope>
    </message>
 */
public class RegistryOperation extends Message {

    /**
     * XML tag indicating id of related file operation.
     */
    public static final String XML_ELEMENT_OPERATION = "operation";

    /**
     * Id for indicating registration operation.
     */
    public static final String OPERATION_REGISTER = "register";

    /**
     * Id for indicating deregistration operation.
     */
    public static final String OPERATION_DEREGISTER = "deregister";

    /**
     * Id for indicating registry update operation.
     */
    public static final String OPERATION_UPDATE = "update";

    /**
     * XML tag indicating a category of the remote part.
     */
    public static final String XML_ELEMENT_REMOTE = "remote";

    /**
     * Id for indicating a remote test node.
     */
    public static final String REMOTE_TEST_NODE = "test-node";

    /**
     * Id for indicating a remote client.
     */
    public static final String REMOTE_CLIENT = "client";

    /**
     * Enumeration of supported registry operation ids.
     *
     * REGISTER - Used in cases when remote part wants to register to the Test Automation Service
     * DEREGISTER - Used in cases when remote part wants to deregister itself from the Test Automation Service
     * UPDATE - Used in cases when remote part wants to update its registry information on the side of Test Automation Service
     */
    public enum Id {
        REGISTER,
        DEREGISTER,
        UPDATE
    };

    /**
     * Enumeration of supported remote categories.
     *
     * TEST_NODE - Used in cases when remote part is a test node
     * CLIENT - Used in cases when remote part is a client
     * UNKNOWN - Used in cases when remote part is not identificated
     */
    public enum Remote {
        TEST_NODE,
        CLIENT,
        UNKNOWN
    }

    /**
     * Id of related registry operation.
     */
    private Id id = Id.UPDATE;

    /**
     * Category of remote part.
     */
    private Remote remote = Remote.UNKNOWN;

    /**
     * Description of a test node.
     */
    private TestNodeDescription testNodeDescription;

    /**
     * Constructs Registry Operation from the specified message.
     *
     * @param message Message to be used as data source
     */
    public RegistryOperation(Message message) {
        super(message);
        setType(Message.TYPE_REGISTRY_OPERATION);
    }

    /**
     * Parametrized constructor.
     *
     * @param id Registry operation id
     * @param remote Category of a remote part
     */
    public RegistryOperation(Id id, Remote remote) {
        super(Message.TYPE_REGISTRY_OPERATION);
        this.id = id;
        this.remote = remote;
    }

    /**
     * Sets registry operation id.
     *
     * @param id Registry operation id
     */
    public void setId(Id id) {
        this.id = id;
    }

    /**
     * Returns registry operation id.
     *
     * @return Registry operation id
     */
    public Id getId() {
        return id;
    }

    /**
     * Sets the category of a remote part.
     *
     * @param remote Category of a remote part
     */
    public void setRemote(Remote remote) {
        this.remote = remote;
    }

    /**
     * Returns the category of a remote part.
     *
     * @return The category of a remote part
     */
    public Remote getRemote() {
        return remote;
    }

    /**
     * Sets the test node description.
     *
     * @param testNodeDescription Test node description
     */
    public void setTestNodeDescription(TestNodeDescription testNodeDescription) {
        if (testNodeDescription != null) {
            this.testNodeDescription = testNodeDescription;
        }
    }

    /**
     * Returns the test node description.
     *
     * @return Test node description
     */
    public TestNodeDescription getTestNodeDescription() {
        return testNodeDescription;
    }

    /**
     * Returns a textual representation of this message.
     *
     * @return A textual representation of this message
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("\n\n Registry operation:");
        string.append("\n\t Sender hostname:      " + senderHostname);
        string.append("\n\t Sender port:          " + senderPort);
        string.append("\n\t Receiver hostname:    " + receiverHostname);
        string.append("\n\t Receiver port:        " + receiverPort);
        string.append("\n\t Operation id:         " + id.name());
        string.append("\n\t Remote part category: " + remote.name());

        if (testNodeDescription != null) {
            string.append(testNodeDescription.toString());
        }

        return string.toString();
    }

    /**
     * Returns XML representation of this message
     *
     * @return XML representation of this message
     */
    @Override
    public String toXML() {
        StringBuilder xml = new StringBuilder();
        String indentation = "\t\t"; // Just for a nicer printouts

        xml.append(indentation + "<" + XML_ELEMENT_OPERATION + ">");
        if (id == Id.REGISTER) {
            xml.append(OPERATION_REGISTER);
        } else if (id == Id.DEREGISTER) {
            xml.append(OPERATION_DEREGISTER);
        } else if (id == Id.UPDATE) {
            xml.append(OPERATION_UPDATE);
        }
        xml.append("</" + XML_ELEMENT_OPERATION + ">\n");

        xml.append(indentation + "<" + XML_ELEMENT_REMOTE + ">");
        if (remote == Remote.CLIENT) {
            xml.append(REMOTE_CLIENT);
        } else if (remote == Remote.TEST_NODE) {
            xml.append(REMOTE_TEST_NODE);
        } else {
            xml.append(Remote.UNKNOWN);
        }
        xml.append("</" + XML_ELEMENT_REMOTE + ">\n");

        if (testNodeDescription != null) {
            xml.append(testNodeDescription.toXML("\t\t")); // With identation for a nicer printouts
        }

        // Store created envelope
        setEnvelope(xml.toString());

        // Let the base class handle the rest of XML generation
        return super.toXML();
    }
}
