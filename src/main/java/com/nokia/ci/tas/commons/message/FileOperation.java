package com.nokia.ci.tas.commons.message;

import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.FileDescription;

/**
 * Incapsulates all information required to describe an operation on a file inside the Test Automation Services.
 *
 * The XML format used for representing a single File Operation:

    <!-- Base format for describing operation on a file in the Test Automation Service -->
    <!-- All file operation messages are always encoded in the UTF-8 format -->
    <?xml version="1.0" encoding="UTF-8"?>
    <message>
        <!-- File operation messages always have type "file-operation" -->
        <type>file-operation</type>
        <sender>
            <hostname>sender.hostname.com</hostname>
            <port>12345</port>
        </sender>
        <receiver>
            <hostname>receiver.hostname.com</hostname>
            <port>23456</port>
        </receiver>
        <envelope>
            <!-- Code of the operation a sender would like the receiver to perform on the specified file -->
            <operation>get | put | abort</operation>
            <!-- File operation messages must contain description of related file and possibly related test -->
            <test>
                <!-- At least test id should be presented to describe the possibly related test -->
                <id>Id of the test related to the operation on a file</id>
            </test>
            <file>
                <!-- File description should always contain the file name -->
                <name>filename.ext</name>
                <!-- Paths to file are optional and could be useful when file transfer will be handled by some other techniques -->
                <path>/optional/path/to/file/directory/</path>
                <!-- Size of the file is always measured in bytes, and -1 is used if file size is unknown -->
                <size>80384</size>
            </file>
        </envelope>
    </message>
    <!-- If file operation was "put", then file's binary data goes here immediately at the next line after the message -->
 */
public class FileOperation extends Message {

    /**
     * XML tag indicating id of related file operation.
     */
    public static final String XML_ELEMENT_OPERATION = "operation";

    /**
     * Id for indicating a file retrieving operation.
     * Namely, when sender wants to "get" file from the receiver.
     */
    public static final String OPERATION_GET = "get";

    /**
     * Id for indicating a file storing operation.
     * Namely, when sender wants to "put" file onto the receiver.
     */
    public static final String OPERATION_PUT = "put";

    /**
     * Id for indicating invalid or failed file operation.
     * Namely, when sender wants to tell the receiver about "abortion" of the file operation.
     */
    public static final String OPERATION_ABORT = "abort";

    /**
     * Enumeration of supported file operation ids.
     *
     * GET - Used in cases when sender requests a copy of described file from the receiver
     * PUT - Used in cases when sender requests receiver to store a copy of specified file and its data
     * ABORT - Used in cases when file operation has failed or cannot be performed for some reasons
     *
     * Any part which has received a GET file operation must reply either with the PUT or ABORT file operations
     * Any part which has received a PUT file operation must either store delivered file data or reply back with the ABORT file operation
     */
    public enum Id {
        GET,
        PUT,
        ABORT
    };

    /**
     * Id of related file operation.
     */
    private Id id = Id.ABORT;

    /**
     * Description of a test which has ivolved file operation.
     */
    private Test test;

    /**
     * Description of a file involved in this file operation.
     */
    private FileDescription fileDescription;

    /**
     * Constructs File Operation from the specified message.
     *
     * @param message Message to be used as data source
     */
    public FileOperation(Message message) {
        super(message);
        setType(Message.TYPE_FILE_OPERATION);
        test = new Test("Unknown ID");
        fileDescription = new FileDescription();
    }

    /**
     * Parametrized constructor.
     *
     * @param test Description of a test which has ivolved file operation
     * @param id File operation id
     * @param fileName Name of the file
     * @param fileSize Size of the file in bytes
     */
    public FileOperation(Id id, Test test, FileDescription fileDescription) {
        super(Message.TYPE_FILE_OPERATION);
        this.id = id;
        this.test = test;
        this.fileDescription = fileDescription;
    }

    /**
     * Sets file operation id.
     *
     * @param id File operation id
     */
    public void setId(Id id) {
        this.id = id;
    }

    /**
     * Returns file operation id.
     *
     * @return File operation id
     */
    public Id getId() {
        return id;
    }

    /**
     * Sets test description involved a file operation
     *
     * @param test Test description involved a file operation
     */
    public void setTest(Test test) {
        this.test = test;
    }

    /**
     * Returns test description involved a file operation.
     *
     * @return Test description involved a file operation
     */
    public Test getTest() {
        return test;
    }

    /**
     * Sets file description associated with this file operation.
     *
     * @param fileDescription File description associated with this file operation
     */
    public void setFileDescription(FileDescription fileDescription) {
        this.fileDescription = fileDescription;
    }

    /**
     * Returns file description associated with this file operation.
     *
     * @return File description associated with this file operation
     */
    public FileDescription getFileDescription() {
        return fileDescription;
    }

    /**
     * Returns a textual representation of this message.
     *
     * @return A textual representation of this message
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("\n\n File operation:");
        string.append("\n\t Sender hostname:   " + senderHostname);
        string.append("\n\t Sender port:       " + senderPort);
        string.append("\n\t Receiver hostname: " + receiverHostname);
        string.append("\n\t Receiver port:     " + receiverPort);
        string.append("\n\t Test id:           " + test.getId());
        string.append("\n\t Operation id:      " + id.name());

        if (fileDescription != null) {
            string.append(fileDescription.toString());
        }

        if (test != null) {
            string.append("\n\t Related test:      " + test.getRuntimeId());
            //string.append(test.toString());
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
        if (id == Id.PUT) {
            xml.append(OPERATION_PUT);
        } else if (id == Id.GET) {
            xml.append(OPERATION_GET);
        } else if (id == Id.ABORT) {
            xml.append(OPERATION_ABORT);
        }
        xml.append("</" + XML_ELEMENT_OPERATION + ">\n");

        if (test != null) {
            xml.append(test.toXML(indentation));
        }

        if (fileDescription != null) {
            xml.append(fileDescription.toXML(indentation));
        }

        // Store created envelope
        setEnvelope(xml.toString());

        // Let the base class handle the rest of XML generation
        return super.toXML();
    }
}
