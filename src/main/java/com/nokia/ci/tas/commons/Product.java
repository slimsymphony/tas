package com.nokia.ci.tas.commons;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Incapsulates all information about the product tested by the Testing Automation Service.
 *
 * The XML format used for representing supported products:

    <!-- Base format for describing a product available to the Test Automation Service inside its test farm -->
    <!-- All product descriptions are always encoded in the UTF-8 format -->
    <?xml version="1.0" encoding="UTF-8"?>
    <!-- The "product" root element contains all data required to describe a product -->
    <product>
        <!-- The FUSE connection name is used to identificate product connection on the test node -->
        <fuse-connection-name>NFPD USB_FPS21</fuse-connection-name>

        <!-- The FUSE connection id is used to identificate product connection on the test node -->
        <fuse-connection-id>Guid_0123456789</fuse-connection-id>
        
        <!-- The Trace connection id is used to identificate product Trace connection on the test node -->
        <trace-connection-id>GConnId_4977ce8366784f79</trace-connection-id>

        <!-- IMEI code of the product -->
        <imei>000000000000001</imei>

        <!-- RM code of the product -->
        <rm-code>RM-123</rm-code>

        <!-- Hardware type of the product -->
        <hardware-type>X1</hardware-type>

        <!-- Role assigned to the product by the test farm's maintainer -->
        <role>main | remote | reference</role>

        <!-- Current status of the product assigned by the Test Automation Service or Communicator -->
        <status>free | busy | disabled</status>

        <!-- Some additional information related to product's current status, if available -->
        <status-details>...</status-details>

        <!-- Timestamp of the moment (since Unix epoch) when product was reserved for some test or task -->
        <reservation-time>1234567890123</reservation-time>

        <!-- Timeout of the product's reservation in milliseconds -->
        <reservation-timeout>1234567890</reservation-timeout>

        <!-- Timestamp of the moment (since Unix epoch) when product has disconnected for the test node during some test or task -->
        <disconnection-time>1234567890123</disconnection-time>

        <!-- Hostname of the Test Automation Service that product belongs to -->
        <tas-hostname>some-test-automation-service.hostname.com</tas-hostname>

        <!-- Port number of the Test Automation Service that product belongs to -->
        <tas-port>12345</tas-port>

        <!-- Hostname assigned to the product by the Test Automation Communicator -->
        <hostname>hostname.domain.com</hostname>

        <!-- IP address assigned to the product by the Test Automation Communicator -->
        <ip>172.23.127.256</ip>

        <!-- TCP/IP port number assigned to the product by the Test Automation Communicator -->
        <port>15001</port>

        <!-- Description of the environment that product is providing
        <environment>Empty by default or with some description</environment>

        <!-- Description of the 1st (or only) SIM card available to the product -->
        <sim1>
            <!-- Phone number (or MSISDN) assigned to this SIM card. Empty phone number means that SIM card is not used -->
            <phone-number>+3585550000001</phone-number>

            <!-- The 1st and the 2nd Personal Identification Number codes -->
            <pin1>1234</pin1>
            <pin2>1234</pin2>

            <!-- The 1st and the 2nd PIN Unblocking Key codes -->
            <puk1>12345</puk1>
            <puk2>12345</puk2>

            <!-- A security code assigned to the SIM card -->
            <security-code>12345</security-code>

            <!-- International Mobile Subscription Identity number, which identificates this SIM card -->
            <imsi>244070103300372</imsi>

            <!-- Service dialling number associated with the SIM card -->
            <service-dialling-number>+3585550000010</service-dialling-number>

            <!-- Number of a voice mailbox -->
            <voice-mailbox-number>+3585550000011</voice-mailbox-number>
        </sim1>

        <!-- Description of the 2nd SIM card available to the dual-SIM product -->
        <sim2>
            <phone-number></phone-number>
            <pin1></pin1>
            <pin2></pin2>
            <puk1></puk1>
            <puk2></puk2>
            <security-code></security-code>
            <imsi></imsi>
            <service-dialling-number></service-dialling-number>
            <voice-mailbox-number></voice-mailbox-number>
        </sim2>
    </product>
 */
public class Product {

    /**
     * XML tag indicating the product description.
     */
    public static final String XML_ELEMENT_PRODUCT = "product";

    /**
     * XML tag indicating a FUSE connection name.
     */
    public static final String XML_ELEMENT_FUSE_CONNECTION_NAME = "fuse-connection-name";

    /**
     * XML tag indicating a FUSE connection id.
     */
    public static final String XML_ELEMENT_FUSE_CONNECTION_ID = "fuse-connection-id";
    
    /**
     * XML tag indicating a Trace connection id.
     */
    public static final String XML_ELEMENT_TRACE_CONNECTION_ID = "trace-connection-id";

    /**
     * XML tag indicating IMEI number of the product.
     */
    public static final String XML_ELEMENT_IMEI = "imei";

    /**
     * XML tag indicating RM code of the product.
     */
    public static final String XML_ELEMENT_RM_CODE = "rm-code";

    /**
     * XML tag indicating hardware type of the product.
     */
    public static final String XML_ELEMENT_HARDWARE_TYPE = "hardware-type";

    /**
     * XML tag indicating role of the product within the test farm.
     */
    public static final String XML_ELEMENT_ROLE = "role";

    /**
     * XML tag indicating the status of product.
     */
    public static final String XML_ELEMENT_STATUS = "status";

    /**
     * XML tag indicating details (if any) regarding the current status of the product.
     */
    public static final String XML_ELEMENT_STATUS_DETAILS = "status-details";

    /**
     * XML tag indicating timestamp of the moment when this product was reserved.
     */
    public static final String XML_ELEMENT_RESERVATION_TIME = "reservation-time";

    /**
     * XML tag indicating the timeout of product's reservation.
     */
    public static final String XML_ELEMENT_RESERVATION_TIMEOUT = "reservation-timeout";

    /**
     * XML tag indicating timestamp of the moment when this product was disconnected from the test node during some test.
     */
    public static final String XML_ELEMENT_DISCONNECTION_TIME = "disconnection-time";

    /**
     * XML tag indicating the hostname of Test Automation Communicator that is handling this product.
     */
    public static final String XML_ELEMENT_HOSTNAME = "hostname";

    /**
     * XML tag indicating the IP address of the product in the test farm.
     */
    public static final String XML_ELEMENT_IP_ADDRESS = "ip";

    /**
     * XML tag indicating the port number that was assigned to the product behind its IP address.
     */
    public static final String XML_ELEMENT_PORT_NUMBER = "port";

    /**
     * XML tag indicating the hostname of Test Automation Service this products belongs to.
     */
    public static final String XML_ELEMENT_TAS_HOSTNAME = "tas-hostname";

    /**
     * XML tag indicating the port number of Test Automation Service this products belongs to.
     */
    public static final String XML_ELEMENT_TAS_PORT_NUMBER = "tas-port";

    /**
     * XML tag indicating the environment that product is providing.
     */
    public static final String XML_ELEMENT_ENVIRONMENT = "environment";
    
    /**
     * XML tag indicating the serial number that product is providing.
     */
    public static final String XML_ELEMENT_SN = "sn";
    
    /**
     * XML tag indicating the SW version that product is providing.
     */
    public static final String XML_ELEMENT_SW_VERSION = "sw-version";
    
    /**
     * XML tag indicating the finger print that product is providing.
     */
    public static final String XML_ELEMENT_FINGERPRINT = "fingerprint";
    
    /**
     * XML tag indicating the product code that product is providing.
     */
    public static final String XML_ELEMENT_PRODUCT_CODE= "productcode";

    /**
     * Textual representation of the main role.
     */
    public static final String ROLE_MAIN = "main";

    /**
     * Textual representation of the remote role.
     */
    public static final String ROLE_REMOTE = "remote";

    /**
     * Textual representation of the reference role.
     */
    public static final String ROLE_REFERENCE = "reference";

    /**
     * Textual representation of the free status.
     */
    public static final String STATUS_FREE = "free";

    /**
     * Textual representation of the busy status.
     * .
     */
    public static final String STATUS_BUSY = "busy";

    /**
     * Textual representation of the disabled status.
     */
    public static final String STATUS_DISABLED = "disabled";

    /**
     * JSON tag indicating a FUSE connection name.
     */
    public static final String JSON_ELEMENT_FUSE_CONNECTION_NAME = "ConnectionName";

    /**
     * JSON tag indicating a FUSE connection id.
     */
    public static final String JSON_ELEMENT_FUSE_CONNECTION_ID = "Connection";
    
    /**
     * JSON tag indicating a Trace Connection(Fuse connection) id.
     */
    public static final String JSON_ELEMENT_TRACE_CONNECTION_ID = "TraceConnection";
    
    
    /**
     * JSON tag indicating IMEI number of the product.
     */
    public static final String JSON_ELEMENT_IMEI = "Imei";

    /**
     * JSON tag indicating RM code of the product.
     */
    public static final String JSON_ELEMENT_RM_CODE = "RmCode";

    /**
     * JSON tag indicating hardware type of the product.
     */
    public static final String JSON_ELEMENT_HARDWARE_TYPE = "HardwareType";

    /**
     * JSON tag indicating role of the product within the test farm.
     */
    public static final String JSON_ELEMENT_ROLE = "Role";

    /**
     * JSON tag indicating the status of product.
     */
    public static final String JSON_ELEMENT_STATUS = "Status";

    /**
     * JSON tag indicating details (if any) regarding the current status of the product.
     */
    public static final String JSON_ELEMENT_STATUS_DETAILS = "StatusDetails";

    /**
     * JSON tag indicating timestamp of the moment when this product was reserved.
     */
    public static final String JSON_ELEMENT_RESERVATION_TIME = "ReservationTime";

    /**
     * JSON tag indicating the timeout of product's reservation.
     */
    public static final String JSON_ELEMENT_RESERVATION_TIMEOUT = "ReservationTimeout";

    /**
     * JSON tag indicating timestamp of the moment when this product was disconnected from the test node during some test.
     */
    public static final String JSON_ELEMENT_DISCONNECTION_TIME = "DisconnectionTime";

    /**
     * JSON tag indicating the hostname associated with the product in the test farm.
     */
    public static final String JSON_ELEMENT_HOSTNAME = "Hostname";

    /**
     * JSON tag indicating the IP address of the product in the test farm.
     */
    public static final String JSON_ELEMENT_IP_ADDRESS = "Ip";

    /**
     * JSON tag indicating the port number that was assigned to the product behind its IP address.
     */
    public static final String JSON_ELEMENT_PORT_NUMBER = "Port";

    /**
     * JSON tag indicating the hostname of Test Automation Service this products belongs to.
     */
    public static final String JSON_ELEMENT_TAS_HOSTNAME = "TasHostname";

    /**
     * JSON tag indicating the port number of Test Automation Service this products belongs to.
     */
    public static final String JSON_ELEMENT_TAS_PORT_NUMBER = "TasPort";

    /**
     * JSON tag indicating the environment that products is providing.
     */
    public static final String JSON_ELEMENT_ENVIRONMENT = "Environment";

    /**
     * JSON tag indicating the phone number for the reference products.
     */
    public static final String JSON_ELEMENT_REFERENCE_PRODUCT_PHONE_NUMBER = "PhoneNumber";
    
    /**
     * JSON tag indicating the serial number for the product.
     */
    public static final String JSON_ELEMENT_SN = "Sn";
    
    /**
     * JSON tag indicating the SW version for the product.
     */
    public static final String JSON_ELEMENT_SW_VERSION = "SwVer";
    
    /**
     * JSON tag indicating the finger print for the product.
     */
    public static final String JSON_ELEMENT_FINGERPRINT = "FingerPrint";
    
    /**
     * JSON tag indicating the product code for the product.
     */
    public static final String JSON_ELEMENT_PRODUCT_CODE = "ProductCode";

    // Deprecated JSON elements are here only for compatibility reasons

    /**
     * @deprecated
     * JSON tag indicating a FUSE connection name.
     */
    public static final String DEPRECATED_JSON_ELEMENT_FUSE_CONNECTION_NAME = "fuseConnectionName";

    /**
     * @deprecated
     * JSON tag indicating a FUSE connection id.
     */
    public static final String DEPRECATED_JSON_ELEMENT_FUSE_CONNECTION_ID = "fuseConnectionId";
    
    /**
     * @deprecated
     * JSON tag indicating a TRACE connection id.
     */
    public static final String DEPRECATED_JSON_ELEMENT_TRACE_CONNECTION_ID = "traceConnectionId";

    /**
     * @deprecated
     * JSON tag indicating IMEI number of the product.
     */
    public static final String DEPRECATED_JSON_ELEMENT_IMEI = "imei";

    /**
     * @deprecated
     * JSON tag indicating RM code of the product.
     */
    public static final String DEPRECATED_JSON_ELEMENT_RM_CODE = "rmCode";

    /**
     * @deprecated
     * JSON tag indicating hardware type of the product.
     */
    public static final String DEPRECATED_JSON_ELEMENT_HARDWARE_TYPE = "hardwareType";

    /**
     * @deprecated
     * JSON tag indicating role of the product within the test farm.
     */
    public static final String DEPRECATED_JSON_ELEMENT_ROLE = "role";

    /**
     * @deprecated
     * JSON tag indicating the status of product.
     */
    public static final String DEPRECATED_JSON_ELEMENT_STATUS = "status";

    /**
     * @deprecated
     * JSON tag indicating details (if any) regarding the current status of the product.
     */
    public static final String DEPRECATED_JSON_ELEMENT_STATUS_DETAILS = "statusDetails";

    /**
     * @deprecated
     * JSON tag indicating timestamp of the moment when this product was reserved.
     */
    public static final String DEPRECATED_JSON_ELEMENT_RESERVATION_TIME = "reservationTime";

    /**
     * @deprecated
     * JSON tag indicating the timeout of product's reservation.
     */
    public static final String DEPRECATED_JSON_ELEMENT_RESERVATION_TIMEOUT = "reservationTimeout";

    /**
     * @deprecated
     * JSON tag indicating timestamp of the moment when this product was disconnected from the test node during some test.
     */
    public static final String DEPRECATED_JSON_ELEMENT_DISCONNECTION_TIME = "disconnectionTime";

    /**
     * @deprecated
     * JSON tag indicating the hostname associated with the product in test farm.
     */
    public static final String DEPRECATED_JSON_ELEMENT_HOSTNAME = "hostname";

    /**
     * @deprecated
     * JSON tag indicating the IP address of the product in the test farm.
     */
    public static final String DEPRECATED_JSON_ELEMENT_IP_ADDRESS = "ip";

    /**
     * @deprecated
     * JSON tag indicating the port number that was assigned to the product behind its IP address.
     */
    public static final String DEPRECATED_JSON_ELEMENT_PORT_NUMBER = "port";

    /**
     * @deprecated
     * JSON tag indicating the hostname of Test Automation Service this products belongs to.
     */
    public static final String DEPRECATED_JSON_ELEMENT_TAS_HOSTNAME = "tasHostname";

    /**
     * @deprecated
     * JSON tag indicating the port number of Test Automation Service this products belongs to.
     */
    public static final String DEPRECATED_JSON_ELEMENT_TAS_PORT_NUMBER = "tasPort";
    
    /**
     * @deprecated
     * JSON tag indicating the serial number.
     */
    public static final String DEPRECATED_JSON_ELEMENT_SN = "sn";
    
    /**
     * @deprecated
     * JSON tag indicating the sw version.
     */
    public static final String DEPRECATED_JSON_ELEMENT_SW_VERSION = "swVer";
    
    /**
     * @deprecated
     * JSON tag indicating the finger print.
     */
    public static final String DEPRECATED_JSON_ELEMENT_FINGERPRINT = "fingerPrint";
    
    /**
     * @deprecated
     * JSON tag indicating the product code.
     */
    public static final String DEPRECATED_JSON_ELEMENT_PRODUCT_CODE = "productCode";
    
    /**
     * Supported product states:
     *
     * FREE - when product is available for testing
     * BUSY - when product is under testing or flashing
     * DISABLED - when product is not allowed to be used in testing for misconfigurations or other issues
     */
    public enum Status {
        FREE,
        BUSY,
        DISABLED
    };

    /**
     * Enumeration with all phone roles supported in Test Automation Service.
     *
     * MAIN - a product which tests its software and hardware
     * REMOTE - a product which plays a supportive role in a test
     * REFERENCE - a product which plays a supportive role for the main and remote phones, but which never gets flashed in the Test Automation Service
     */
    public enum Role {
        MAIN,
        REMOTE,
        REFERENCE
    };

    /**
     * @deprecated Please use ROLE_MAIN
     * Textual representation of the main role.
     */
    public static final String MAIN_ROLE_ID = "main";

    /**
     * @deprecated Please use ROLE_REMOTE
     * Textual representation of the remote role.
     */
    public static final String REMOTE_ROLE_ID = "remote";

    /**
     * @deprecated Please use ROLE_REFERENCE
     * Textual representation of the reference role.
     */
    public static final String REFERENCE_ROLE_ID = "reference";

    /**
     * IMEI code of the product.
     */
    private String imei = "";

    /**
     * RM code of the product.
     */
    private String rmCode = "";

    /**
     * Hardware type of the product
     */
    private String hardwareType = "";

    /**
     * FUSE connection name associated with the product on a testing node.
     */
    private String fuseConnectionName = "";

    /**
     * FUSE connection id associated with the product on a testing node.
     */
    private String fuseConnectionId = "";
    
    /**
     * Trace connection id associated with the product on a testing node.
     */
    private String traceConnectionId = "";

    /**
     * Hostname of the Test Automation Communicator that handles this product.
     */
    private String hostname = "";

    /**
     * IP address of the product within the test farm.
     * Tecnically this is the same IP address that is given to the corresponding test node,
     * regarless if it is IPv4 or IPv6.
     */
    private String ipAddress = "";

    /**
     * Port number associated with the product within the test farm.
     * Tecnically this is a port number that product was given by CI Proxy application running on the corresponding test node.
     */
    private String port = "";

    /**
     * Current role of the product.
     */
    private Role role = Role.MAIN;

    /**
     * Current status of the product.
     */
    private Status status = Status.FREE;

    /**
     * Some details related to the current status.
     */
    private String statusDetails = "";

    /**
     * Reservation timeout in milliseconds.
     * Maximal amount of time this product may be reserved for some test.
     * The product will be automatically set free after expiration of timeout.
     */
    private long reservationTimeout = Constant.DEFAULT_TEST_TIMEOUT;

    /**
     * A moment of time when this product was reserved for some test.
     */
    private long reservationTime = 0L;

    /**
     * A moment of time when product has disconnected from some test node during a test.
     */
    private long disconnectionTime = 0L;

    /**
     * Hostname of the Test Automation Service where to this product is registered.
     */
    private String testAutomationServiceHostname = "";

    /**
     * Port number of the Test Automation Service where to this product is registered.
     */
    private int testAutomationServicePort = 0;

    /**
     * Description of the environment that this product is providing.
     */
    private String environment = "";

    /**
     * The 1st or only SIM card in a dual-SIM product.
     */
    private SimCard sim1;

    /**
     * The 2nd SIM card in a dual-SIM product.
     */
    private SimCard sim2;

    /**
     * Serial Number, unique for each product.
     */
    private String sn;
    
    /**
     * Phone SW version.
     */
    private String swVer;
    
    /**
     * Finger-print for current build image. 
     */
    private String fingerprint;
    
    /**
     * Product Code(Variant)
     */
    private String productCode;

    /**
     * Constructor.
     */
    public Product() {
        sim1 = new SimCard(SimCard.XML_ELEMENT_SIM_CARD_1);
        sim2 = new SimCard(SimCard.XML_ELEMENT_SIM_CARD_2);
    }

    /**
     * Parametrised constructor.
     * One of the parameters may be null.
     *
     * @param rmCode RM code of the product
     * @param imei IMEI code of the product
     */
    public Product(String rmCode, String imei) {
        this();

        if (rmCode != null) {
            this.rmCode = rmCode;
        }

        if (imei != null) {
            this.imei = imei;
        }
    }

    /**
     * Sets IMEI code of the product.
     *
     * @param imei IMEI code of the product
     */
    public void setIMEI(String imei) {
        this.imei = imei;
    }

    /**
     * Returns IMEI code of the product.
     *
     * @return IMEI code of the product
     */
    public String getIMEI() {
        return imei;
    }

    
    public String getSn() {
    	return sn;
    }
    
    public void setSn( String sn ) {
    	this.sn = sn;
    }
    
    public String getSwVer() {
    	return swVer;
    }
    
    public void setSwVer(String sw) {
    	this.swVer = sw;
    }
    
    public String getFingerprint() {
    	return fingerprint;
    }
    
    public void setFingerprint(String f) {
    	this.fingerprint = f;
    }
    
    public String getProductCode() {
    	return productCode;
    }
    
    public void setProductCode( String pc ) {
    	this.productCode = pc;
    }
    
    
    /**
     * Sets RM code of the product.
     *
     * @param rmCode RM code of the product
     */
    public void setRMCode(String rmCode) {
        this.rmCode = rmCode;
    }

    /**
     * Returns RM code of the product.
     *
     * @return RM code of the product
     */
    public String getRMCode() {
        return rmCode;
    }

    /**
     * Sets hardware type of the product.
     *
     * @param hardwareType Hardware type of the product
     */
    public void setHardwareType(String hardwareType) {
        this.hardwareType = hardwareType;
    }

    /**
     * Returns hardware type of the product.
     *
     * @return Hardware type of the product
     */
    public String getHardwareType() {
        return hardwareType;
    }

    /**
     * Sets FUSE connection name associated with the product on test node.
     *
     * @param fuseConnectionName FUSE connection name associated with the product on test node
     */
    public void setFuseConnectionName(String fuseConnectionName) {
        this.fuseConnectionName = fuseConnectionName;
    }

    /**
     * Returns FUSE connection name associated with the product on test node.
     *
     * @return FUSE connection name associated with the product on test node
     */
    public String getFuseConnectionName() {
        return fuseConnectionName;
    }

    /**
     * Sets FUSE connection id associated with the product on test node.
     *
     * @param fuseConnectionId FUSE connection id associated with the product on test node
     */
    public void setFuseConnectionId(String fuseConnectionId) {
        this.fuseConnectionId = fuseConnectionId;
    }

    /**
     * Gets FUSE connection id associated with the product on test node.
     *
     * @return FUSE connection id associated with the product on test node
     */
    public String getFuseConnectionId() {
        return fuseConnectionId;
    }
    
    /**
     * Sets Trace connection id associated with the product on test node.
     *
     * @param traceConnectionId Trace connection id associated with the product on test node
     */
    public void setTraceConnectionId(String traceConnectionId) {
    	try {
    		System.err.println("Set trace Connection Id" + traceConnectionId);
    		throw new Exception("Check stack of setting trace connection id.");
    	}catch(Exception ex) {
    		ex.printStackTrace();
    	}
        this.traceConnectionId = traceConnectionId;
    }
    
    /**
     * Sets Trace connection id associated with the product on test node.
     *
     * @param traceConnectionId Trace connection id associated with the product on test node
     */
    public void setTraceConnectionId(String traceConnectionId, boolean stack) {
    	if(stack) {
	    	try {
	    		System.err.println("Set trace Connection Id" + traceConnectionId);
	    		throw new Exception("Check stack of setting trace connection id.");
	    	}catch(Exception ex) {
	    		ex.printStackTrace();
	    	}
    	}
        this.traceConnectionId = traceConnectionId;
    }

    /**
     * Gets Trace connection id associated with the product on test node.
     *
     * @return Trace connection id associated with the product on test node
     */
    public String getTraceConnectionId() {
        return traceConnectionId;
    }

    /**
     * Sets the phone number associated with the 1st (or only) SIM card.
     *
     * @param sim1PhoneNumber Phone number associated with the 1st (or only) SIM card
     */
    public void setSIM1PhoneNumber(String sim1PhoneNumber) {
        if (sim1PhoneNumber != null) {
            sim1.setPhoneNumber(sim1PhoneNumber);
        }
    }

    /**
     * Returns the phone number associated with the 1st (or only) SIM card.
     *
     * @return Phone number associated with the 1st (or only) SIM card
     */
    public String getSIM1PhoneNumber() {
        return sim1.getPhoneNumber();
    }

    /**
     * Sets the phone number associated with the 2nd SIM card,
     * optionally available in dual-sim products.
     *
     * @param sim1PhoneNumber Phone number associated with the 2nd SIM card
     */
    public void setSIM2PhoneNumber(String sim2PhoneNumber) {
        if (sim2PhoneNumber != null) {
            sim2.setPhoneNumber(sim2PhoneNumber);
        }
    }

    /**
     * Returns the phone number associated with the 2nd SIM card,
     * optionally available in dual-sim products.
     *
     * @return Phone number associated with the 2nd SIM card
     */
    public String getSIM2PhoneNumber() {
        return sim2.getPhoneNumber();
    }

    /**
     * Sets hostname of the Test Automation Communicator that is handling this product.
     *
     * @param hostname Hostname of the Test Automation Communicator that is handling this product
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Returns hostname of the Test Automation Communicator that is handling this product.
     *
     * @return Hostname of the Test Automation Communicator that is handling this product
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets IP address of the product.
     *
     * @param ipAddress IP address of the product
     */
    public void setIPAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Returns IP address of the product.
     *
     * @return IP address of the product
     */
    public String getIPAddress() {
        return ipAddress;
    }

    /**
     * Sets port number for the product on test node.
     *
     * @param port Port number for the product on test node
     */
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * Returns port number for the product on test node.
     *
     * @return Port number for the product on test node
     */
    public String getPort() {
        return port;
    }

    /**
     * Sets current role of the product.
     *
     * @param role Current role of the product
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * Returns current role of the product.
     *
     * @return Current role of the product
     */
    public Role getRole() {
        return role;
    }

    /**
     * Sets current status of the product.
     *
     * @param status Current status of the product
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Returns current status of the product.
     *
     * @return Current status of the product
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets details related to the current status of the product.
     *
     * @param statusDetails Some details related to the current status of the product
     */
    public void setStatusDetails(String statusDetails) {
        this.statusDetails = statusDetails;
    }

    /**
     * Returns details (if any) related to the current status of the product.
     *
     * @return Details (if any) related to the current status of the product
     */
    public String getStatusDetails() {
        return statusDetails;
    }

    /**
     * Sets current status of the product together with some status details.
     *
     * @param status Current status of the product
     * @param statusDetails Some details related to the current status of the product
     */
    public void setStatus(Status status, String statusDetails) {
        this.status = status;
        this.statusDetails = statusDetails;
    }

    /**
     * Sets reservation deadlines for this product.
     * Please note, that time of reservation is an absolute date value
     * while reservation timeout is a period of time.
     * Both values must be given in milliseconds.
     * Any negative values will be ignored.
     *
     * @param time A moment of time when this product was reserved for a test
     * @param timeout Maximal period of time this product may be reserved for a test
     */
    public void setReservation(long time, long timeout) {
        setReservationTime(time);
        setReservationTimeout(timeout);
    }

    /**
     * Sets time of reservation for this product.
     * Please note that any negative values will be ignored.
     *
     * @param time A moment of time when this product was reserved for the test
     */
    public void setReservationTime(long time) {
        if (time >= 0L) {
            reservationTime = time;
        }
    }

    /**
     * Returns time of reservation of this product.
     *
     * @return Time of reservation of this product
     */
    public long getReservationTime() {
        return reservationTime;
    }

    /**
     * Sets timeout of reservation for this product.
     * Please note that any negative or zero value will be ignored.
     *
     * @param timeout Timeout of reservation for this product
     */
    public void setReservationTimeout(long timeout) {
        if (timeout > 0L) {
            reservationTimeout = timeout;
        }
    }

    /**
     * Returns reservation timeout value associated with this product.
     *
     * @return Reservation timeout value associated with this product
     */
    public long getReservationTimeout() {
        return reservationTimeout;
    }

    /**
     * Sets moment of time when this product has disconnected from the test node.
     * Please note that any negative values will be ignored.
     *
     * @param time A moment of time when this product has disconnected from the test node
     */
    public void setDisconnectionTime(long time) {
        if (time >= 0L) {
            disconnectionTime = time;
        }
    }

    /**
     * Returns a moment of time when this product has disconnected from the test node
     *
     * @return A moment of time when this product has disconnected from the test node
     */
    public long getDisconnectionTime() {
        return disconnectionTime;
    }

    /**
     * Sets hostname and port number of the Test Automation Service where to this product is registered.
     *
     * @param hostname Hostname of the Test Automation Service where to this product is registered
     * @param port Port number of the Test Automation Service where to this product is registered
     */
    public void setTestAutomationService(String hostname, int port) {
        this.testAutomationServiceHostname = hostname;
        this.testAutomationServicePort = port;
    }

    /**
     * Sets hostname of the Test Automation Service where to this product is registered.
     *
     * @param testAutomationServiceHostname Hostname of the Test Automation Service where to this product is registered
     */
    public void setTestAutomationServiceHostname(String testAutomationServiceHostname) {
        this.testAutomationServiceHostname = testAutomationServiceHostname;
    }

    /**
     * Returns hostname of the Test Automation Service where to this product is registered.
     *
     * @return Hostname of the Test Automation Service where to this product is registered
     */
    public String getTestAutomationServiceHostname() {
        return testAutomationServiceHostname;
    }


    /**
     * Sets port number of the Test Automation Service where to this product is registered.
     *
     * @param testAutomationServicePort Port number of the Test Automation Service where to this product is registered
     */
    public void setTestAutomationServicePort(int testAutomationServicePort) {
        this.testAutomationServicePort = testAutomationServicePort;
    }

    /**
     * Returns port number of the Test Automation Service where to this product is registered.
     *
     * @return Port number of the Test Automation Service where to this product is registered
     */
    public int getTestAutomationServicePort() {
        return testAutomationServicePort;
    }

    /**
     * Sets description of the environment that product is providing.
     *
     * @param environment Description of the environment that product is providing
     */
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    /**
     * Returns description of the environment that product is providing.
     *
     * @return Description of the environment that product is providing
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * Sets the 1st (or only) SIM card for this product.
     *
     * @param sim1 The 1st (or only) SIM card for this product
     */
    public void setSim1(SimCard sim1) {
        this.sim1 = sim1;
    }

    /**
     * Returns the 1st (or only) SIM card for this product.
     *
     * @return The 1st (or only) SIM card for this product
     */
    public SimCard getSim1() {
        return sim1;
    }

    /**
     * Sets the 2nd SIM card for this product.
     *
     * @param sim2 The 2nd SIM card for this product
     */
    public void setSim2(SimCard sim2) {
        this.sim2 = sim2;
    }

    /**
     * Returns the 2nd SIM card for this product.
     *
     * @return The 2nd SIM card for this product
     */
    public SimCard getSim2() {
        return sim2;
    }

    /**
     * Returns true when product is free or false otherwise.
     *
     * @return True when product is free or false otherwise
     */
    public boolean isFree() {

        if (status == Status.FREE) {
            return true;
        }

        return false;
    }

    /**
     * Returns a textual representation of the object.
     *
     * @return A textual representation of the object
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("\n\n Product:");
        if (imei != null && !imei.isEmpty()) {
            string.append("\n\t IMEI:                  " + imei);
        }

        if (rmCode != null && !rmCode.isEmpty()) {
            string.append("\n\t RM code:               " + rmCode);
        }

        if (hardwareType != null && !hardwareType.isEmpty()) {
            string.append("\n\t Hardware type:         " + hardwareType);
        } else {
            string.append("\n\t Hardware type:         unknown");
        }
        
        if (sn != null && !sn.isEmpty()) {
            string.append("\n\t Serial Number:         " + sn);
        } else {
            string.append("\n\t Serial Number:         unknown");
        }
        
        if (swVer != null && !swVer.isEmpty()) {
            string.append("\n\t Software version:      " + swVer);
        } else {
            string.append("\n\t Software version:      unknown");
        }
        
        if (productCode != null && !productCode.isEmpty()) {
            string.append("\n\t Product Code:          " + productCode);
        } else {
            string.append("\n\t Product Code:      unknown");
        }

        if (fuseConnectionName != null && !fuseConnectionName.isEmpty()) {
            string.append("\n\t FUSE connection name:  " + fuseConnectionName);
        }

        if (fuseConnectionId != null && !fuseConnectionId.isEmpty()) {
            string.append("\n\t FUSE connection id:    " + fuseConnectionId);
        }
        
        if (traceConnectionId != null && !traceConnectionId.isEmpty()) {
            string.append("\n\t Trace connection id:    " + traceConnectionId);
        }

        if (hostname != null && !hostname.isEmpty()) {
            string.append("\n\t Hostname:              " + hostname);
        }

        if (ipAddress != null && !ipAddress.isEmpty()) {
            string.append("\n\t IP address:            " + ipAddress);
        }

        if (port != null && !port.isEmpty()) {
            string.append("\n\t TCP/IP port number:    " + port);
        }

        if (testAutomationServiceHostname != null && !testAutomationServiceHostname.isEmpty()) {
            string.append("\n\t TAS hostname:          " + testAutomationServiceHostname);
        }

        if (testAutomationServicePort > 0) {
            string.append("\n\t TAS port number:       " + testAutomationServicePort);
        }

        if (environment != null && !environment.isEmpty()) {
            string.append("\n\t Environment:           " + environment);
        }

        if (role == Role.REFERENCE) {
            string.append("\n\t Role:                  Reference product");
        } else if (role == Role.MAIN) {
            string.append("\n\t Role:                  Main product");
        } else if (role == Role.REMOTE) {
            string.append("\n\t Role:                  Remote product");
        } else {
            string.append("\n\t Role:                  Main or remote product");
        }

        if (status == Status.BUSY) {
            string.append("\n\t Status:                " + status.name() + " - " + statusDetails);

            if (reservationTime > 0L) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constant.TIMESTAMP_FORMAT);
                string.append("\n\t Reserved at:           " + simpleDateFormat.format(new Date(reservationTime)));
            } else {
                string.append("\n\t Reserved at:           unknown date and time!");
            }

            if (reservationTimeout > 0L) {
                string.append("\n\t Reservation timeout:   " + Util.convert(reservationTimeout));
            } else {
                string.append("\n\t Reservation timeout:   not specified");
            }
        } else if (status == Status.DISABLED) {
            string.append("\n\t Status:                " + status.name() + " - " + statusDetails);
        } else {
            string.append("\n\t Status:                " + status.name());
        }

        if (disconnectionTime > 0L) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constant.TIMESTAMP_FORMAT);
            string.append("\n\t Disconnected at:       " + simpleDateFormat.format(new Date(disconnectionTime)));
        }

        if (!sim1.getPhoneNumber().isEmpty()) {
            string.append("\n" + sim1.toString());
        }

        if (!sim2.getPhoneNumber().isEmpty()) {
            string.append("\n" + sim2.toString());
        }

        return string.toString();
    }

    /**
     * Returns XML representation of the product.
     *
     * @return XML representation of the product
     */
    public String toXML() {
        return toXML("");
    }

    /**
     * Returns JSON representation of the product.
     *
     * @return JSON representation of the product
     */
    public String toJSON() {
        return toJSON("");
    }

    /**
     * @deprecated
     * Returns JSON representation of the product.
     *
     * @return JSON representation of the product
     */
    public String toDeprecatedJSON() {
        return toDeprecatedJSON("");
    }

    /**
     * Returns XML representation of the product with specified indentation.
     *
     * @param indentation Indentation to be used in XML outputs
     * @return XML representation of the product with specified indentation
     */
    public String toXML(String indentation) {
        StringBuilder xml = new StringBuilder();

        xml.append(indentation + "<" + XML_ELEMENT_PRODUCT + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_SN + ">" + sn + "</" + XML_ELEMENT_SN + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_SW_VERSION + ">" + swVer + "</" + XML_ELEMENT_SW_VERSION + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_FINGERPRINT + ">" + fingerprint + "</" + XML_ELEMENT_FINGERPRINT + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_PRODUCT_CODE + ">" + productCode + "</" + XML_ELEMENT_PRODUCT_CODE + ">\n");
        //xml.append(indentation + "\t<" + XML_ELEMENT_FUSE_CONNECTION_NAME + ">" + fuseConnectionName + "</" + XML_ELEMENT_FUSE_CONNECTION_NAME + ">\n");
        //xml.append(indentation + "\t<" + XML_ELEMENT_FUSE_CONNECTION_ID + ">" + fuseConnectionId + "</" + XML_ELEMENT_FUSE_CONNECTION_ID + ">\n");
        //xml.append(indentation + "\t<" + XML_ELEMENT_TRACE_CONNECTION_ID + ">" + traceConnectionId + "</" + XML_ELEMENT_TRACE_CONNECTION_ID + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_IMEI + ">" + imei + "</" + XML_ELEMENT_IMEI + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_RM_CODE + ">" + rmCode + "</" + XML_ELEMENT_RM_CODE + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_HARDWARE_TYPE + ">" + hardwareType + "</" + XML_ELEMENT_HARDWARE_TYPE + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_ROLE + ">");
        if (role == Role.MAIN) {
            xml.append(ROLE_MAIN);
        } else if (role == Role.REMOTE) {
            xml.append(ROLE_REMOTE);
        } else if (role == Role.REFERENCE) {
            xml.append(ROLE_REFERENCE);
        } else {
            xml.append(ROLE_MAIN); // Default
        }
        xml.append("</" + XML_ELEMENT_ROLE + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_STATUS + ">");
        if (status == Status.FREE) {
            xml.append(STATUS_FREE);
        } else if (status == Status.BUSY) {
            xml.append(STATUS_BUSY);
        } else if (status == Status.DISABLED) {
            xml.append(STATUS_DISABLED);
        } else {
            xml.append(STATUS_DISABLED); // Default
        }
        xml.append("</" + XML_ELEMENT_STATUS + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_STATUS_DETAILS + ">" + statusDetails + "</" + XML_ELEMENT_STATUS_DETAILS + ">\n");

        // Don't show reservation time if it wasn't specified
        if (reservationTime > 0L) {
            xml.append(indentation + "\t<" + XML_ELEMENT_RESERVATION_TIME + ">" + reservationTime + "</" + XML_ELEMENT_RESERVATION_TIME + ">\n");
        }

        // Don't show reservation timeout if it isn't different from the default one
        if (reservationTimeout != Constant.DEFAULT_TEST_TIMEOUT) {
            xml.append(indentation + "\t<" + XML_ELEMENT_RESERVATION_TIMEOUT + ">" + reservationTimeout + "</" + XML_ELEMENT_RESERVATION_TIMEOUT + ">\n");
        }

        // Don't show disconnection time if it wasn't specified
        if (disconnectionTime > 0L) {
            xml.append(indentation + "\t<" + XML_ELEMENT_DISCONNECTION_TIME + ">" + disconnectionTime + "</" + XML_ELEMENT_DISCONNECTION_TIME + ">\n");
        }

        if (!testAutomationServiceHostname.isEmpty()) {
            xml.append(indentation + "\t<" + XML_ELEMENT_TAS_HOSTNAME + ">" + testAutomationServiceHostname + "</" + XML_ELEMENT_TAS_HOSTNAME + ">\n");
        }

        if (testAutomationServicePort > 0) {
            xml.append(indentation + "\t<" + XML_ELEMENT_TAS_PORT_NUMBER + ">" + testAutomationServicePort + "</" + XML_ELEMENT_TAS_PORT_NUMBER + ">\n");
        }

        xml.append(indentation + "\t<" + XML_ELEMENT_HOSTNAME + ">" + hostname + "</" + XML_ELEMENT_HOSTNAME + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_IP_ADDRESS + ">" + ipAddress + "</" + XML_ELEMENT_IP_ADDRESS + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_PORT_NUMBER + ">" + port + "</" + XML_ELEMENT_PORT_NUMBER + ">\n");

        xml.append(indentation + "\t<" + XML_ELEMENT_ENVIRONMENT + ">" + environment + "</" + XML_ELEMENT_ENVIRONMENT + ">\n");

        xml.append(sim1.toXML(indentation + "\t") + "\n");
        xml.append(sim2.toXML(indentation + "\t") + "\n");

        xml.append(indentation + "</" + XML_ELEMENT_PRODUCT + ">\n");

        return xml.toString();
    }

    /**
     * Returns JSON representation of the product with specified indentation.
     *
     * @param indentation Indentation to be used in JSON outputs
     * @return JSON representation of the product with specified indentation
     */
    public String toJSON(String indentation) {
        StringBuilder json = new StringBuilder();

        json.append(indentation + "{\n");

        json.append(indentation + "\t\"" + JSON_ELEMENT_SN + "\":\"" + sn + "\",\n");
        json.append(indentation + "\t\"" + JSON_ELEMENT_SW_VERSION + "\":\"" + swVer + "\",\n");
        json.append(indentation + "\t\"" + JSON_ELEMENT_FINGERPRINT + "\":\"" + fingerprint + "\",\n");
        json.append(indentation + "\t\"" + JSON_ELEMENT_PRODUCT_CODE + "\":\"" + productCode + "\",\n");
        //json.append(indentation + "\t\"" + JSON_ELEMENT_FUSE_CONNECTION_NAME + "\":\"" + fuseConnectionName + "\",\n");
        //json.append(indentation + "\t\"" + JSON_ELEMENT_FUSE_CONNECTION_ID + "\":\"" + fuseConnectionId + "\",\n");
        //json.append(indentation + "\t\"" + JSON_ELEMENT_TRACE_CONNECTION_ID + "\":\"" + traceConnectionId + "\",\n");
        json.append(indentation + "\t\"" + JSON_ELEMENT_IMEI + "\":\"" + imei + "\",\n");
        json.append(indentation + "\t\"" + JSON_ELEMENT_RM_CODE + "\":\"" + rmCode + "\",\n");
        json.append(indentation + "\t\"" + JSON_ELEMENT_HARDWARE_TYPE + "\":\"" + hardwareType + "\",\n");

        // Role
        json.append(indentation + "\t\"" + JSON_ELEMENT_ROLE + "\":\"");
        if (role == Role.MAIN) {
            json.append(ROLE_MAIN);
        } else if (role == Role.REMOTE) {
            json.append(ROLE_REMOTE);
        } else if (role == Role.REFERENCE) {
            json.append(ROLE_REFERENCE);
        } else {
            json.append(ROLE_MAIN); // Default
        }
        json.append("\",\n");

        // Status
        json.append(indentation + "\t\"" + JSON_ELEMENT_STATUS + "\":\"");
        if (status == Status.FREE) {
            json.append(STATUS_FREE);
        } else if (status == Status.BUSY) {
            json.append(STATUS_BUSY);
        } else if (status == Status.DISABLED) {
            json.append(STATUS_DISABLED);
        } else {
            json.append(STATUS_DISABLED); // Default
        }
        json.append("\",\n");

        json.append(indentation + "\t\"" + JSON_ELEMENT_STATUS_DETAILS + "\":\"" + statusDetails + "\",\n");

        // Don't show reservation time if it wasn't specified
        if (reservationTime > 0L) {
            json.append(indentation + "\t\"" + JSON_ELEMENT_RESERVATION_TIME + "\":\"" + reservationTime + "\",\n");
        }

        // Don't show reservation timeout if it isn't different from the default one
        if (reservationTimeout != Constant.DEFAULT_TEST_TIMEOUT) {
            json.append(indentation + "\t\"" + JSON_ELEMENT_RESERVATION_TIMEOUT + "\":\"" + reservationTimeout + "\",\n");
        }

        // Don't show disconnection time if it wasn't specified
        if (disconnectionTime > 0L) {
            json.append(indentation + "\t\"" + JSON_ELEMENT_DISCONNECTION_TIME + "\":\"" + disconnectionTime + "\",\n");
        }

        if (!testAutomationServiceHostname.isEmpty()) {
            json.append(indentation + "\t\"" + JSON_ELEMENT_TAS_HOSTNAME + "\":\"" + testAutomationServiceHostname + "\",\n");
        }

        if (testAutomationServicePort > 0) {
            json.append(indentation + "\t\"" + JSON_ELEMENT_TAS_PORT_NUMBER + "\":\"" + testAutomationServicePort + "\",\n");
        }

        json.append(indentation + "\t\"" + JSON_ELEMENT_HOSTNAME + "\":\"" + hostname + "\",\n");
        json.append(indentation + "\t\"" + JSON_ELEMENT_IP_ADDRESS + "\":\"" + ipAddress + "\",\n");
        json.append(indentation + "\t\"" + JSON_ELEMENT_PORT_NUMBER + "\":\"" + port + "\",\n");

        json.append(indentation + "\t\"" + JSON_ELEMENT_ENVIRONMENT + "\":\"" + environment + "\",\n");

        if (role == Role.REFERENCE) {
            // Reference products should add the "PhoneNumber" element in their JSON files,
            // since so far all reference products were exclusively single SIM-card products

            String phoneNumber = sim1.getPhoneNumber();

            if (phoneNumber == null || phoneNumber.isEmpty()) {
                // Try to get phone number from the second SIM-card
                phoneNumber = sim2.getPhoneNumber();
            }

            if (phoneNumber == null) {
                phoneNumber = "";
            }

            // Note, that SIM-cards specifiec data will be added in parallel to this phone number
            json.append(indentation + "\t\"" + JSON_ELEMENT_REFERENCE_PRODUCT_PHONE_NUMBER + "\":\"" + phoneNumber + "\",\n");
        }

        json.append(sim1.toJSON(indentation) + ",\n");
        json.append(sim2.toJSON(indentation) + "\n");

        json.append(indentation + "}\n");

        return json.toString();
    }

    /**
     * Returns a list of parameter names used to build name-value pairs.
     * Each parameter name will have the Constant.NAME_VALUE_SEPARATOR as a suffix
     * for sufficient separation of similar look a like parameter names,
     * like "status" and "status details", "reservation-time" or "reservation-timeout", etc.
     *
     * @return A list of parameter names used to build name-value pairs
     */
    public List<String> getParameterNames() {
        List<String> nameTokens = new ArrayList<String>(0);

        nameTokens.add(XML_ELEMENT_SN + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_SW_VERSION + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_FINGERPRINT + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_PRODUCT_CODE + Constant.NAME_VALUE_SEPARATOR);
        //nameTokens.add(XML_ELEMENT_FUSE_CONNECTION_NAME + Constant.NAME_VALUE_SEPARATOR);
        //nameTokens.add(XML_ELEMENT_FUSE_CONNECTION_ID + Constant.NAME_VALUE_SEPARATOR);
        //nameTokens.add(XML_ELEMENT_TRACE_CONNECTION_ID + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_IMEI + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_RM_CODE + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_HARDWARE_TYPE + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_ROLE + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_STATUS + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_STATUS_DETAILS + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_RESERVATION_TIME + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_RESERVATION_TIMEOUT + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_DISCONNECTION_TIME + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_TAS_HOSTNAME + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_TAS_PORT_NUMBER + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_HOSTNAME + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_IP_ADDRESS + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_PORT_NUMBER + Constant.NAME_VALUE_SEPARATOR);
        nameTokens.add(XML_ELEMENT_ENVIRONMENT + Constant.NAME_VALUE_SEPARATOR);

        nameTokens.addAll(sim1.getParameterNames());
        nameTokens.addAll(sim2.getParameterNames());

        return nameTokens;
    }

    /**
     * Returns a string containing representation of this product in form of "parameter name"-"parameter value" pairs
     * suitable for search of matching products out of testing farm.
     * Each "parameter name" is separated from the "parameter value" with the Constant.NAME_VALUE_SEPARATOR symbol,
     * while "parameter name"-"parameter value" pairs are separated from each other with the Constant.NAME_VALUE_PAIR_SEPARATOR symbol.
     *
     * @return A string containing representation of this product in form of "parameter name"-"parameter value" pairs
     * suitable for search of matching products out of testing farm
     */
    public String getNameValuePairs() {
        StringBuilder representation = new StringBuilder();

        if (sn != null && !sn.isEmpty()) {
            representation.append(XML_ELEMENT_SN + Constant.NAME_VALUE_SEPARATOR + sn + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }
        
        if (swVer != null && !swVer.isEmpty()) {
            representation.append(XML_ELEMENT_SW_VERSION + Constant.NAME_VALUE_SEPARATOR + swVer + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }
        
        if (fingerprint != null && !fingerprint.isEmpty()) {
            representation.append(XML_ELEMENT_FINGERPRINT + Constant.NAME_VALUE_SEPARATOR + fingerprint + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }
        
        if (productCode != null && !productCode.isEmpty()) {
            representation.append(XML_ELEMENT_PRODUCT_CODE + Constant.NAME_VALUE_SEPARATOR + productCode + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }
        /*if (fuseConnectionName != null && !fuseConnectionName.isEmpty()) {
            representation.append(XML_ELEMENT_FUSE_CONNECTION_NAME + Constant.NAME_VALUE_SEPARATOR + fuseConnectionName + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        if (fuseConnectionId != null && !fuseConnectionId.isEmpty()) {
            representation.append(XML_ELEMENT_FUSE_CONNECTION_ID + Constant.NAME_VALUE_SEPARATOR + fuseConnectionId + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }
        
        if (traceConnectionId != null && !traceConnectionId.isEmpty()) {
            representation.append(XML_ELEMENT_TRACE_CONNECTION_ID + Constant.NAME_VALUE_SEPARATOR + traceConnectionId + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }*/

        if (imei != null && !imei.isEmpty()) {
            representation.append(XML_ELEMENT_IMEI + Constant.NAME_VALUE_SEPARATOR + imei + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        if (rmCode != null && !rmCode.isEmpty()) {
            representation.append(XML_ELEMENT_RM_CODE + Constant.NAME_VALUE_SEPARATOR + rmCode + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        if (hardwareType != null && !hardwareType.isEmpty()) {
            representation.append(XML_ELEMENT_HARDWARE_TYPE + Constant.NAME_VALUE_SEPARATOR + hardwareType + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        // Role is always set
        representation.append(XML_ELEMENT_ROLE + Constant.NAME_VALUE_SEPARATOR);
        if (role == Role.MAIN) {
            representation.append(ROLE_MAIN);
        } else if (role == Role.REMOTE) {
            representation.append(ROLE_REMOTE);
        } else if (role == Role.REFERENCE) {
            representation.append(ROLE_REFERENCE);
        } else {
            representation.append(ROLE_MAIN); // Default
        }
        representation.append(Constant.NAME_VALUE_PAIR_SEPARATOR);

        // Status is always set
        representation.append(XML_ELEMENT_STATUS + Constant.NAME_VALUE_SEPARATOR);
        if (status == Status.FREE) {
            representation.append(STATUS_FREE);
        } else if (status == Status.BUSY) {
            representation.append(STATUS_BUSY);
        } else if (status == Status.DISABLED) {
            representation.append(STATUS_DISABLED);
        } else {
            representation.append(STATUS_DISABLED); // Default
        }
        representation.append(Constant.NAME_VALUE_PAIR_SEPARATOR);

        // Status details are not used
        // Reservation time is not used
        // Reservation timeout is not used
        // Disconnection time is not used

        if (testAutomationServiceHostname != null && !testAutomationServiceHostname.isEmpty()) {
            representation.append(XML_ELEMENT_TAS_HOSTNAME + Constant.NAME_VALUE_SEPARATOR + testAutomationServiceHostname + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        if (testAutomationServicePort > 0) {
            representation.append(XML_ELEMENT_TAS_PORT_NUMBER + Constant.NAME_VALUE_SEPARATOR + testAutomationServicePort + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        if (hostname != null && !hostname.isEmpty()) {
            representation.append(XML_ELEMENT_HOSTNAME + Constant.NAME_VALUE_SEPARATOR + hostname + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        if (ipAddress != null && !ipAddress.isEmpty()) {
            representation.append(XML_ELEMENT_IP_ADDRESS + Constant.NAME_VALUE_SEPARATOR + ipAddress + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        if (port != null && !port.isEmpty()) {
            representation.append(XML_ELEMENT_PORT_NUMBER + Constant.NAME_VALUE_SEPARATOR + port + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        if (environment != null && !environment.isEmpty()) {
            representation.append(XML_ELEMENT_ENVIRONMENT + Constant.NAME_VALUE_SEPARATOR + environment + Constant.NAME_VALUE_PAIR_SEPARATOR);
        }

        representation.append(sim1.getNameValuePairs());
        representation.append(sim2.getNameValuePairs());

        // Automatically turn everything into lower case
        return representation.toString().toLowerCase();
    }

    /**
     * @deprecated
     * Returns JSON representation of the product with specified indentation.
     *
     * @param indentation Indentation to be used in JSON outputs
     * @return JSON representation of the product with specified indentation
     */
    public String toDeprecatedJSON(String indentation) {
        StringBuilder json = new StringBuilder();

        json.append(indentation + "{\n");

        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_SN + "\":\"" + sn + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_SW_VERSION + "\":\"" + swVer + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_FINGERPRINT + "\":\"" + fingerprint + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_PRODUCT_CODE + "\":\"" + productCode + "\",\n");
        //json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_FUSE_CONNECTION_NAME + "\":\"" + fuseConnectionName + "\",\n");
        //json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_FUSE_CONNECTION_ID + "\":\"" + fuseConnectionId + "\",\n");
        //json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_TRACE_CONNECTION_ID + "\":\"" + traceConnectionId + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_IMEI + "\":\"" + imei + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_RM_CODE + "\":\"" + rmCode + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_HARDWARE_TYPE + "\":\"" + hardwareType + "\",\n");

        // Role
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_ROLE + "\":\"");
        if (role == Role.MAIN) {
            json.append(ROLE_MAIN);
        } else if (role == Role.REMOTE) {
            json.append(ROLE_REMOTE);
        } else if (role == Role.REFERENCE) {
            json.append(ROLE_REFERENCE);
        } else {
            json.append(ROLE_MAIN); // Default
        }
        json.append("\",\n");

        // Status
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_STATUS + "\":\"");
        if (status == Status.FREE) {
            json.append(STATUS_FREE);
        } else if (status == Status.BUSY) {
            json.append(STATUS_BUSY);
        } else if (status == Status.DISABLED) {
            json.append(STATUS_DISABLED);
        } else {
            json.append(STATUS_DISABLED); // Default
        }
        json.append("\",\n");

        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_STATUS_DETAILS + "\":\"" + statusDetails + "\",\n");

        // Don't show reservation time if it wasn't specified
        if (reservationTime > 0L) {
            json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_RESERVATION_TIME + "\":\"" + reservationTime + "\",\n");
        }

        // Don't show reservation timeout if it isn't different from the default one
        if (reservationTimeout != Constant.DEFAULT_TEST_TIMEOUT) {
            json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_RESERVATION_TIMEOUT + "\":\"" + reservationTimeout + "\",\n");
        }

        // Don't show disconnection time if it wasn't specified
        if (disconnectionTime > 0L) {
            json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_DISCONNECTION_TIME + "\":\"" + disconnectionTime + "\",\n");
        }

        if (!testAutomationServiceHostname.isEmpty()) {
            json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_TAS_HOSTNAME + "\":\"" + testAutomationServiceHostname + "\",\n");
        }

        if (testAutomationServicePort > 0) {
            json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_TAS_PORT_NUMBER + "\":\"" + testAutomationServicePort + "\",\n");
        }

        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_HOSTNAME + "\":\"" + hostname + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_IP_ADDRESS + "\":\"" + ipAddress + "\",\n");
        json.append(indentation + "\t\"" + DEPRECATED_JSON_ELEMENT_PORT_NUMBER + "\":\"" + port + "\",\n");

        json.append(sim1.toDeprecatedJSON(indentation + "\t") + ",\n");
        json.append(sim2.toDeprecatedJSON(indentation + "\t") + "\n");

        json.append(indentation + "}\n");

        return json.toString();
    }
}
