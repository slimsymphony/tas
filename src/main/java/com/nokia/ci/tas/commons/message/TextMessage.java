package com.nokia.ci.tas.commons.message;

import com.nokia.ci.tas.commons.Test;

/**
 * Incapsulates all information required to describe an operation on a test inside the Test Automation Services.
 *
 * The XML format used for representing a single Test Operation:

    <?xml version="1.0" encoding="UTF-8"?>
    <!-- Example of a Text Message encoded in XML -->
    <message>
        <!-- Text message contains a description of the related test and the text lines somehow related to that test -->
        <type>text-message</type>
        <sender>
            <hostname>sender.hostname.com</hostname>
            <port>12345</port>
        </sender>
        <receiver>
            <hostname>receiver.hostname.com</hostname>
            <port>23456</port>
        </receiver>
        <envelope>
            <!-- At least test ID should be presented to describe the possibly related test -->
            <test>
                <id>Id of the test related to the text message</id>
            </test>
            <!-- The text itself goes on a line-by-line manner -->
            <text>
                Text line 1
                Text line 2
                Text line 3
                    ...
                Text line N
            </text>
        </envelope>
    </message>
 */
public class TextMessage extends Message {

    /**
     * XML tag indicating the block of text this message is carrying on.
     */
    public static final String XML_ELEMENT_TEXT = "text";

    /**
     * Description of related test.
     */
    private Test test;

    /**
     * Related text message.
     */
    private String text = "";

    /**
     * Constructs Text message from the specified message.
     *
     * @param message Message to be used as data source
     */
    public TextMessage(Message message) {
        super(message);
        setType(Message.TYPE_TEXT_MESSAGE);
        test = new Test("Unknown ID");
        text = "";
    }

    /**
     * Parametrized constructor.
     *
     * @param test Description of related test
     * @param text Related text
     */
    public TextMessage(Test test, String text) {
        super(Message.TYPE_TEXT_MESSAGE);
        this.test = test;
        this.text = text;
    }

    /**
     * Sets the test related to this text message
     *
     * @param test Test related to this text message
     */
    public void setTest(Test test) {
        this.test = test;
    }

    /**
     * Returns test related to this text message
     *
     * @return Test related to this text message
     */
    public Test getTest() {
        return test;
    }

    /**
     * Sets the text for this message.
     *
     * @param text Text for this message
     */
    public void setText(String text) {
        if (text != null) {
            this.text = text;
        }
    }

    /**
     * Returns the text from this message.
     *
     * @return Text from this message
     */
    public String getText() {
        return text;
    }

    /**
     * Returns a textual representation of this message.
     *
     * @return A textual representation of this message
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("\n\n Text message:");
        string.append("\n\t Sender hostname:   " + senderHostname);
        string.append("\n\t Sender port:       " + senderPort);
        string.append("\n\t Receiver hostname: " + receiverHostname);
        string.append("\n\t Receiver port:     " + receiverPort);

        if (test != null) {
            string.append(test.toString());
        }

        if (text != null) {
            string.append("\n\t Text:              \n" + text);
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

        if (test != null) {
            xml.append(test.toXML(indentation));
        }

        if (text != null) {
            xml.append(indentation + "<" + XML_ELEMENT_TEXT + ">\n");
            xml.append(text);
            xml.append(indentation + "</" + XML_ELEMENT_TEXT + ">\n");
        }

        // Store created envelope
        setEnvelope(xml.toString());

        // Let the base class handle the rest of XML generation
        return super.toXML();
    }
}
