package com.nokia.ci.tas.commons.message;

import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.Product;

/**
 * Incapsulates all information required to describe an operation on a product inside the Test Automation Services.
 *
 * The XML format used for representing a single Product Operation:

    <!-- Base format for describing operation on a product in the Test Automation Service -->
    <!-- All product operation messages are always encoded in the UTF-8 format -->
    <?xml version="1.0" encoding="UTF-8"?>
    <message>
        <!-- Product operation messages always have type "product-operation" -->
        <type>product-operation</type>
        <sender>
            <hostname>sender.hostname.com</hostname>
            <port>12345</port>
        </sender>
        <receiver>
            <hostname>receiver.hostname.com</hostname>
            <port>23456</port>
        </receiver>
        <envelope>
            <!-- Code of the operation a sender would like the receiver to perform on the specified product -->
            <operation>add | remove | update</operation>
            <!-- Product operation messages must contain description of related product and possibly related test -->
            <test>
                <!-- At least test id should be presented to describe the possibly related test -->
                <id>Test id</id>
            </test>
            <product>
                Description or related product...
            </product>
        </envelope>
    </message>
 */
public class ProductOperation extends Message {

    /**
     * XML tag indicating id of related file operation.
     */
    public static final String XML_ELEMENT_OPERATION = "operation";

    /**
     * Id for indicating product adding operation.
     * Add operation is used in cases when sender of product operation
     * requests receiver to add specifed product as available one.
     */
    public static final String OPERATION_ADD = "add";

    /**
     * Id for indicating product removal operation.
     * Remove operation is used in cases when sender of product operation
     * requests receiver to remove specified product from the list of available ones.
     */
    public static final String OPERATION_REMOVE = "remove";

    /**
     * Id for indicating product update operation.
     * Update operation is used in cases when sender of product operation
     * requests receiver to update local copy of product information
     * according to specified one.
     */
    public static final String OPERATION_UPDATE = "update";

    /**
     * Enumeration of supported product operation ids.
     *
     * ADD     - Used in cases when sender of product operation requests receiver to add specifed product as available one
     * REMOVE  - Used in cases when sender of product operation requests receiver to remove specifed product from the list of available ones
     * UPDATE  - Used in cases when sender of product operation requests receiver to update local copy of product information according to specified one
     */
    public enum Id {
        ADD,
        REMOVE,
        UPDATE
    };

    /**
     * Description of related test.
     */
    private Test test;

    /**
     * Description of related product.
     */
    private Product product;

    /**
     * Id of related product operation.
     */
    private Id id = Id.UPDATE;

    /**
     * Constructs Product Operation from the specified message.
     *
     * @param message Message to be used as data source
     */
    public ProductOperation(Message message) {
        super(message);
        setType(Message.TYPE_PRODUCT_OPERATION);
    }

    /**
     * Parametrized constructor.
     *
     * @param product Description of related product
     * @param id Product operation id
     */
    public ProductOperation(Id id, Product product) {
        super(Message.TYPE_PRODUCT_OPERATION);
        this.id = id;
        this.product = product;
    }

    /**
     * Sets product operation id.
     *
     * @param id Product operation id
     */
    public void setId(Id id) {
        this.id = id;
    }

    /**
     * Returns product operation id.
     *
     * @return Product operation id
     */
    public Id getId() {
        return id;
    }

    /**
     * Sets the test which is possibly involved into related operation
     *
     * @param test Test which is possibly involved into related operation
     */
    public void setTest(Test test) {
        this.test = test;
    }

    /**
     * Returns the test which is possibly involved into related operation
     *
     * @return Test which is possibly involved into related operation
     */
    public Test getTest() {
        return test;
    }

    /**
     * Sets the product involved into related operation.
     *
     * @param product Product involved into related operation
     */
    public void setProduct(Product product) {
        this.product = product;
    }

    /**
     * Returns the product involved into related operation.
     *
     * @return Product involved into related operation
     */
    public Product getProduct() {
        return product;
    }

    /**
     * Returns a textual representation of this message.
     *
     * @return A textual representation of this message
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("\n\n Product operation:");
        string.append("\n\t Sender hostname:   " + senderHostname);
        string.append("\n\t Sender port:       " + senderPort);
        string.append("\n\t Receiver hostname: " + receiverHostname);
        string.append("\n\t Receiver port:     " + receiverPort);
        string.append("\n\t Operation id:      " + id.name());

        if (test != null) {
            string.append(test.toString());
        }

        if (product != null) {
            string.append(product.toString());
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
        } else if (id == Id.ADD) {
            xml.append(OPERATION_ADD);
        } else if (id == Id.REMOVE) {
            xml.append(OPERATION_REMOVE);
        }
        xml.append("</" + XML_ELEMENT_OPERATION + ">\n");

        if (test != null) {
            xml.append(test.toXML(indentation));
        }

        if (product != null) {
            xml.append(product.toXML(indentation));
        }

        // Store created envelope
        setEnvelope(xml.toString());

        // Let the base class handle the rest of XML generation
        return super.toXML();
    }
}
