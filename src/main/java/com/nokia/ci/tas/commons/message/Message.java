package com.nokia.ci.tas.commons.message;

import com.nokia.ci.tas.commons.Constant;

/**
 * Base class for all messages used in the Testing Automation Service components.
 *
 * The XML format used for representing a message:

    <!-- Example of a Message encoded in XML -->
    <!-- All messages are always started with the standard XML declaration and encoded in UTF-8 -->
    <?xml version="1.0" encoding="UTF-8"?>
    <message>
        <type>Type of the message tells what kind of information this message should contain in envelope</type>
        <!--
            All messages always specify their sender and receiver
            This helps with message forwarding and also lets to store messages as XML files or XML data records in the databases
        -->
        <sender>
            <!-- Hostnames are preferred over IP addresses, since IP addresses may change over short periods of time, while hostnames usually don't -->
            <hostname>sender.hostname.com</hostname>
            <port>12345</port>
        </sender>
        <receiver>
            <hostname>receiver.hostname.com</hostname>
            <port>34567</port>
        </receiver>
        <envelope>
            <!-- Contents of the envelope are fully depending on the type of message -->
        </envelope>
    </message>
    <!-- Any binary data send with the XML message goes immediately at the next line after the message -->
 */
public class Message {

    /**
     * XML root tag indicating a message.
     */
    public static final String XML_ELEMENT_MESSAGE = "message";

    /**
     * XML tag indicating a type of the message.
     */
    public static final String XML_ELEMENT_TYPE = "type";

    /**
     * XML tag indicating a sender of message.
     */
    public static final String XML_ELEMENT_SENDER = "sender";

    /**
     * XML tag indicating a receiver of message.
     */
    public static final String XML_ELEMENT_RECEIVER = "receiver";

    /**
     * XML tag indicating a hostname of receiver or sender of the message.
     */
    public static final String XML_ELEMENT_HOSTNAME = "hostname";

    /**
     * XML tag indicating a port number of receiver or sender of the message.
     */
    public static final String XML_ELEMENT_PORT = "port";

    /**
     * XML tag indicating an envelope of the message.
     */
    public static final String XML_ELEMENT_ENVELOPE = "envelope";

    /**
     * Id for indicating an unknown or unsupported message.
     */
    public static final String TYPE_UNKNOWN = "unknown";

    /**
     * Id for indicating a Test Operation message.
     * Test operations are used for handling tests inside the Test Automation Service,
     * i.e. for starting and stopping tests.
     */
    public static final String TYPE_TEST_OPERATION = "test-operation";

    /**
     * Id for indicating a Product Operation message.
     * Product operations are used for handling products inside the Test Automation Service farms,
     * i.e. for reserving, setting free, updating or disabling products.
     */
    public static final String TYPE_PRODUCT_OPERATION = "product-operation";

    /**
     * Id for indicating a File Operation message.
     * File operations are used for handling files inside the Test Automation Service,
     * i.e. for retrieving and storing files.
     */
    public static final String TYPE_FILE_OPERATION = "file-operation";

    /**
     * Id for indicating a Registry Operation message.
     * Registry operations are used for notifying Test Automation Service about new remote clients and test nodes,
     * or about their de-registration or update in contact information.
     */
    public static final String TYPE_REGISTRY_OPERATION = "registry-operation";

    /**
     * Id for indicating a text message.
     * Text messages are used for informal purposes related to some activities in the Test Automation Service components.
     */
    public static final String TYPE_TEXT_MESSAGE = "text-message";

    /**
     * Type of the message.
     */
    private String type = TYPE_UNKNOWN;

    /**
     * Hostname of the sender.
     */
    public String senderHostname = "";

    /**
     * Port number of the sender.
     */
    public int senderPort = 0;

    /**
     * Hostname of the receiver.
     */
    public String receiverHostname = "";

    /**
     * Port number of the receiver.
     */
    public int receiverPort = 0;

    /**
     * Envelope of the message.
     */
    public String envelope = "";

    /**
     * Constructor.
     */
    public Message() {
        type = TYPE_UNKNOWN;
        senderHostname = "";
        senderPort = 0;
        receiverHostname = "";
        receiverPort = 0;
        envelope = "";
    }

    /**
     * Parametrized constructor.
     *
     * @param type Type of the message
     */
    public Message(String type) {
        this();
        this.type = type;
    }

    /**
     * Emulation of a copy constructor.
     *
     * @param message Message containing initial data
     */
    public Message(Message message) {
        this.type = message.getType();
        this.senderHostname = message.getSenderHostname();
        this.senderPort = message.getSenderPort();
        this.receiverHostname = message.getReceiverHostname();
        this.receiverPort = message.getReceiverPort();
        this.envelope = message.getEnvelope();
    }

    /**
     * Sets the type of message.
     *
     * @param type Type of message
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the type of message
     *
     * @return Type of messge
     */
    public String getType() {
        return type;
    }

    /**
     * Sets sender information
     *
     * @param hostname Hostname of the sender
     * @param port Port number of the sender
     */
    public void setSender(String hostname, int port) {
        this.senderHostname = hostname;
        this.senderPort = port;
    }

    /**
     * Sets hostname of the sender
     *
     * @param senderHostname Hostname of the sender
     */
    public void setSenderHostname(String senderHostname) {
        this.senderHostname = senderHostname;
    }

    /**
     * Returns hostname of the sender
     *
     * @return Hostname of the sender
     */
    public String getSenderHostname() {
        return senderHostname;
    }

    /**
     * Sets port of the sender
     *
     * @param senderPort Port of the sender
     */
    public void setSenderPort(int senderPort) {
        this.senderPort = senderPort;
    }

    /**
     * Returns port of the sender
     *
     * @return Port of the sender
     */
    public int getSenderPort() {
        return senderPort;
    }

    /**
     * Sets receiver information
     *
     * @param hostname Hostname of the receiver
     * @param port Port number of the receiver
     */
    public void setReceiver(String hostname, int port) {
        this.receiverHostname = hostname;
        this.receiverPort = port;
    }

    /**
     * Sets hostname of the receiver
     *
     * @param receiverHostname Hostname of the receiver
     */
    public void setReceiverHostname(String receiverHostname) {
        this.receiverHostname = receiverHostname;
    }

    /**
     * Returns hostname of the receiver
     *
     * @return Hostname of the receiver
     */
    public String getReceiverHostname() {
        return receiverHostname;
    }

    /**
     * Sets port of the receiver
     *
     * @param senderPort Port of the receiver
     */
    public void setReceiverPort(int receiverPort) {
        this.receiverPort = receiverPort;
    }

    /**
     * Returns port of the receiver
     *
     * @return Port of the receiver
     */
    public int getReceiverPort() {
        return receiverPort;
    }

    /**
     * Sets envelope of the message
     *
     * @param envelope Envelope of the message
     */
    public void setEnvelope(String envelope) {
        if (envelope != null) {
            this.envelope = envelope;
        }
    }

    /**
     * Returns envelope of the message
     *
     * @return Envelope of the message
     */
    public String getEnvelope() {
        return envelope;
    }

    /**
     * Returns a textual representation of the object.
     *
     * @return A textual representation of the object
     */
    @Override
    public String toString() {

        StringBuilder string = new StringBuilder();

        string.append("\n\n Message:");
        string.append("\n\t Type:              " + type);
        string.append("\n\t Sender hostname:   " + senderHostname);
        string.append("\n\t Sender port:       " + senderPort);
        string.append("\n\t Receiver hostname: " + receiverHostname);
        string.append("\n\t Receiver port:     " + receiverPort);

        if (envelope != null && !envelope.isEmpty()) {
            string.append("\n\t Envelope:          " + envelope);
        } else {
            string.append("\n\t Envelope:          Not available");
        }

        return string.toString();
    }

    /**
     * Returns XML representation of the message.
     *
     * @return XML representation of the message
     */
    public String toXML() {
        StringBuilder xml = new StringBuilder();

        xml.append(Constant.XML_DECLARATION + "\n");
        xml.append("<" + XML_ELEMENT_MESSAGE + ">\n");
        xml.append("\t<" + XML_ELEMENT_TYPE + ">" + type + "</" + XML_ELEMENT_TYPE + ">\n");
        xml.append("\t<" + XML_ELEMENT_SENDER + ">\n");
        xml.append("\t\t<" + XML_ELEMENT_HOSTNAME + ">" + senderHostname + "</" + XML_ELEMENT_HOSTNAME + ">\n");
        xml.append("\t\t<" + XML_ELEMENT_PORT + ">" + senderPort + "</" + XML_ELEMENT_PORT + ">\n");
        xml.append("\t</" + XML_ELEMENT_SENDER + ">\n");
        xml.append("\t<" + XML_ELEMENT_RECEIVER + ">\n");
        xml.append("\t\t<" + XML_ELEMENT_HOSTNAME + ">" + receiverHostname + "</" + XML_ELEMENT_HOSTNAME + ">\n");
        xml.append("\t\t<" + XML_ELEMENT_PORT + ">" + receiverPort + "</" + XML_ELEMENT_PORT + ">\n");
        xml.append("\t</" + XML_ELEMENT_RECEIVER + ">\n");

        xml.append("\t<" + XML_ELEMENT_ENVELOPE + ">\n");
        xml.append(envelope);
        xml.append("\t</" + XML_ELEMENT_ENVELOPE + ">\n");
        xml.append("</" + XML_ELEMENT_MESSAGE + ">\n");

        return xml.toString();
    }
}

