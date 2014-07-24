package com.nokia.ci.tas.commons.message;

import com.nokia.ci.tas.commons.Test;

/**
 * Incapsulates all information required to describe an operation on a test inside the Test Automation Services.
 *
 * The XML format used for representing a single Test Operation:

    <!-- All test operation messages must be encoded in UTF-8 and each line must end with the new-line symbol '\n' -->
    <?xml version="1.0" encoding="UTF-8"?>
    <message>
        <!-- Test operation messages always have the type "test-operation" -->
        <type>test-operation</type>
        <!-- Sender of the message must be always identified by its hostname and listening port number -->
        <sender>
            <hostname>sender.hostname.com</hostname>
            <port>12345</port>
        </sender>
        <!-- Receiver of the message must be always identified by its hostname and listening port number -->
        <receiver>
            <hostname>receiver.hostname.com</hostname>
            <port>23456</port>
        </receiver>
        <!-- Test operation message requires that envelope will contain a description about the test and operation that receiver should perform on it -->
        <envelope>
            <!-- Id of related test operation:
                 "start" operation means that receiver should start handling the test specified by the sender
                 "stop" operation means that receiver should stop handling the test specified by the sender
                 "update" operation means that receiver should update test information exactly to the one specified by the sender in this message
                 "check" operation means that receiver should check if specified test is still under execution or monitoring on the sender's side -->
            <operation>start | stop | update | check</operation>
            <!-- Information about related test -->
            <test>
                Description of related test...
            </test>
        </envelope>
    </message>
 */
public class TestOperation extends Message {

    /**
     * XML tag indicating id of related file operation.
     */
    public static final String XML_ELEMENT_OPERATION = "operation";

    /**
     * Id for indicating a test starting operation.
     */
    public static final String OPERATION_START = "start";

    /**
     * Id for indicating a test stopping operation.
     */
    public static final String OPERATION_STOP = "stop";

    /**
     * Id for indicating a test updating operation.
     */
    public static final String OPERATION_UPDATE = "update";

    /**
     * Id for indicating a test check operation.
     */
    public static final String OPERATION_CHECK = "check";

    /**
     * Enumeration of supported test operation ids.
     *
     * START  - Meaning that sender would like to start specified test on the receiver's side.
     *
     * A response to the START test operation message would be simply an UPDATE test operation message
     * with the test object in one of its supported statuses.
     *
     * STOP   - Meaning that sender would like to stop specified test on the receiver's side
     *
     * A response to the STOP test operation message would be simply an UPDATE test operation message
     * with the test object in STOPPED status, or one of its other statuses.
     *
     * UPDATE - Meaning that sender would like to update test information on the receiver's side according to provided data
     *
     * A response to the UPDATE test operation message is not required, since it is having an informative nature.
     * However, nothing could prevent you from putting a test in STOPPED or FAILED status inside the UPDATE test operation
     * to indicate about stopped or failed test respectively.
     *
     * CHECK  - Meaning that sender would like the receiver to check in what state the mentioned test is currently running
     *
     * A response to the CHECK test operation message would be simply an UPDATE test operation message
     * with the test object in its current status on the side of receiver.
     * If receiver is not handling or executing specified test, then receiver should send back a STOP test operation
     * with exactly the same test information but in the FAILED status.
     */
    public enum Id {
        START,
        STOP,
        UPDATE,
        CHECK
    };

    /**
     * Id of related test operation.
     */
    private Id id = Id.CHECK;

    /**
     * Description of related test.
     */
    private Test test;

    /**
     * Constructs Test Operation from the specified message.
     *
     * @param message Message to be used as data source
     */
    public TestOperation(Message message) {
        super(message);
        setType(Message.TYPE_TEST_OPERATION);
        test = new Test("Unknown ID");
    }

    /**
     * Parametrized constructor.
     *
     * @param test Description of related test
     * @param id Test operation id
     */
    public TestOperation(Id id, Test test) {
        super(Message.TYPE_TEST_OPERATION);
        this.id = id;
        this.test = test;
    }

    /**
     * Sets test operation id.
     *
     * @param id Test operation id
     */
    public void setId(Id id) {
        this.id = id;
    }

    /**
     * Returns test operation id.
     *
     * @return Test operation id
     */
    public Id getId() {
        return id;
    }

    /**
     * Sets the test which involved related operation
     *
     * @param test Test which involved related operation
     */
    public void setTest(Test test) {
        this.test = test;
    }

    /**
     * Returns test which has involved related operation
     *
     * @return Test which has involved related operation
     */
    public Test getTest() {
        return test;
    }

    /**
     * Returns a textual representation of this message.
     *
     * @return A textual representation of this message
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("\n\n Test operation:");
        string.append("\n\t Sender hostname:   " + senderHostname);
        string.append("\n\t Sender port:       " + senderPort);
        string.append("\n\t Receiver hostname: " + receiverHostname);
        string.append("\n\t Receiver port:     " + receiverPort);
        string.append("\n\t Operation id:      " + id.name());

		if (test != null) {
            string.append("\n" + test.toString());
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
        if (id == Id.UPDATE) {
            xml.append(OPERATION_UPDATE);
        } else if (id == Id.START) {
            xml.append(OPERATION_START);
        } else if (id == Id.STOP) {
            xml.append(OPERATION_STOP);
        } else if (id == Id.CHECK) {
            xml.append(OPERATION_CHECK);
        }
        xml.append("</" + XML_ELEMENT_OPERATION + ">\n");

        if (test != null) {
            xml.append(test.toXML(indentation));
        }

        // Store created envelope
        setEnvelope(xml.toString());

        // Let the base class handle the rest of XML generation
        return super.toXML();
    }
}
