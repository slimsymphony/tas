package com.nokia.ci.tas.commons;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.nokia.ci.tas.commons.message.Message;
import com.nokia.ci.tas.commons.message.FileOperation;
import com.nokia.ci.tas.commons.message.ProductOperation;
import com.nokia.ci.tas.commons.message.RegistryOperation;
import com.nokia.ci.tas.commons.message.TestOperation;
import com.nokia.ci.tas.commons.message.TextMessage;

/**
 * Converts all incoming XML messages into Message-based objects supported by the Test Automation Service.
 *
 * Although the functionality of this converter is very similar to the SAX or streaming parsers,
 * here we've been forced to use our own parsing approach, since the standard Java parsers are buffering too much or input data.
 *
 * While such XML data buffering is actually a nice feature in usual cases, it brings some troubles to our file transferring approach,
 * since file's binary data is immediately following the corresponding XML message.
 *
 * Of course, there is a standard way to specify some parameters of a SAX parser,
 * like the size of input buffer through the "http://apache.org/xml/properties/input-buffer-size" parameter for the Apache SAX parser.
 * But unfortunately the names of the same parameter may vary from implementation to implementation
 * and also may be simply ignored by the implementations.
 *
 * This leads us either to the hacking input streams forwarded to the standard SAX parsers (and keeping a copy of input buffer away from the SAX parser)
 * or to a more simple approach when input stream is simply read by Converter and parsed locally in a "line-after-line" manner.
 *
 * While the first approach may bring some advantages in letting the SAX parser handle the parsing of input data,
 * it also leads to a doubled parsing inside the emulated input stream, since we will have to track the end of XML messages.
 *
 * In contrast to this, the second approach forces us to read input stream in "byte-after-byte" manner and parse input lines,
 * but it let us to handle file transferries much more easier.
 *
 * And since the XML messages exchanged by the Test Automation Service components are relatively short,
 * that won't bring any big trade-offs in communication speeds or data processing performances.
 */
public class Converter {
    /**
     * Id indicating a state of parsing unknown XML element.
     */
    private final int PARSING_UNKNOWN = -1;

    /**
     * Id indicating a state of parsing message object.
     */
    private final int PARSING_MESSAGE = 0;

    /**
     * Id indicating a state of parsing test object.
     */
    private final int PARSING_TEST = 1;

    /**
     * Id indicating a state of parsing test package objects.
     */
    private final int PARSING_TEST_PACKAGES = 2;

    /**
     * Id indicating a state of parsing product object.
     */
    private final int PARSING_PRODUCT = 3;

    /**
     * Id indicating a state of parsing file description object.
     */
    private final int PARSING_FILE_DESCRIPTION = 4;

    /**
     * Id indicating a state of parsing test node description object.
     */
    private final int PARSING_TEST_NODE_DESCRIPTION = 5;

    /**
     * Id indicating a state of parsing registry object.
     */
    private final int PARSING_REGISTRY = 6;

    /**
     * Id indicating a state of parsing operation object.
     */
    private final int PARSING_OPERATION = 7;

    /**
     * Id indicating a state of parsing text message object.
     */
    private final int PARSING_TEXT = 8;

    /**
     * Id of the current parsing state.
     */
    private int parsing = PARSING_UNKNOWN;

    /**
     * A message object parsed from the current input stream.
     */
    private Message currentMessage;

    /**
     * A test object parsed from the current input stream.
     */
    private Test currentTest;

    /**
     * A test package object parsed from the current input stream.
     */
    private TestPackage currentTestPackage;

    /**
     * A product object parsed from the current input stream.
     */
    private Product currentProduct;

    /**
     * A SIM card object parsed from the current input stream.
     */
    private SimCard currentSimCard;

    /**
     * A file description object parsed from the current input stream.
     */
    private FileDescription currentFileDescription;

    /**
     * A test operation id parsed from the current input stream.
     */
    private TestOperation.Id currentTestOperationId;

    /**
     * A product operation id parsed from the current input stream.
     */
    private ProductOperation.Id currentProductOperationId;

    /**
     * A file operation id parsed from the current input stream.
     */
    private FileOperation.Id currentFileOperationId;

    /**
     * A registry operation id parsed from the current input stream.
     */
    private RegistryOperation.Id currentRegistryOperationId;

    /**
     * A registry operation category parsed from the current input stream.
     */
    private RegistryOperation.Remote currentRegistryCategory;

    /**
     * A test node description parsed from the current input stream.
     */
    private TestNodeDescription currentTestNodeDescription;

    /**
     * A list of products required by the test, parsed from the current input stream.
     */
    private List<Product> currentTestRequiredProducts;

    /**
     * A list of products reserved for the test, parsed from the current input stream.
     */
    private List<Product> currentTestReservedProducts;

    /**
     * A list of test packages associated with test parsed from the input stream.
     */
    private List<TestPackage> currentTestPackages;
    
    /**
     * Current Env Param key value.
     */
    private String currentParamKey;  

    /**
     * A set of text message lines parsed from the current input stream.
     */
    private StringBuffer currentText;

    /**
     * Current XML element parsed from the input stream.
     */
    private String currentTag;

    /**
     * Current value of XML element parsed from the input stream.
     */
    private String currentData;

    /**
     * State variable indicating that sender's credentials are under parsing.
     */
    private boolean parsingMessageSender = false;

    /**
     * State variable indicating that receiver's credentials are under parsing.
     */
    private boolean parsingMessageReceiver = false;

    /**
     * State variable indicating that test's artifacts are under parsing.
     */
    private boolean parsingTestArtifacts = false;
    
    /**
     * State variable indicating that test's environment params are under parsing.
     */
    private boolean parsingTestEnvParams = false;

    /**
     * State variable indicating that files of a test package are under parsing.
     */
    private boolean parsingTestPackageFiles = false;

    /**
     * State variable indicating that a list of test's required products is under parsing.
     */
    private boolean parsingTestRequiredProducts = false;

    /**
     * State variable indicating that a list of test's reserved products is under parsing.
     */
    private boolean parsingTestReservedProducts = false;

    /**
     * Name of current XML element.
     */
    private String elementName;

    /**
     * Value of current XML element.
     */
    private String elementValue;

    /**
     * State variable indicating that current input XML line contains start name of the XML element.
     */
    private boolean hasElementStart;

    /**
     * State variable indicating that current input XML line contains value of the XML element.
     */
    private boolean hasElementValue;

    /**
     * State variable indicating that current input XML line contains end name of the XML element.
     */
    private boolean hasElementEnd;

    /**
     * Index of XML element name's start inside the input XML line.
     */
    private int nameStart;

    /**
     * Index of XML element name's end inside the input XML line.
     */
    private int nameEnd;

    /**
     * Index of XML element value's start inside the input XML line.
     */
    private int valueStart;

    /**
     * Index of XML element value's end inside the input XML line.
     */
    private int valueEnd;

    /**
     * Constructor.
     */
    public Converter() {
    }

    /**
     * Reads XML data from the specified input stream and returns parsed object.
     *
     * @param inputStream A stream with incoming XML data
     * @return Parsed object or null if parsing wasn't successful
     */
    public synchronized Object handle(InputStream inputStream) {
        Object parsedObject = null;
        try {
            currentMessage = null;
            currentTest = null;
            currentTestPackage = null;
            currentProduct = null;
            currentSimCard = null;
            currentFileDescription = null;
            currentText = null;
            currentTestNodeDescription = null;

            currentTestOperationId = null;
            currentProductOperationId = null;
            currentFileOperationId = null;
            currentRegistryOperationId = null;
            currentRegistryCategory = null;

            currentTag = null;
            currentData = null;

            parsingMessageSender = false;
            parsingMessageReceiver = false;
            parsingTestArtifacts = false;
            parsingTestEnvParams = false;
            parsingTestRequiredProducts = false;
            parsingTestReservedProducts = false;
            parsingTestPackageFiles = false;

            parsing = PARSING_UNKNOWN;

            int ch = -1;
            StringBuffer buffer = new StringBuffer();

            do {
                ch = inputStream.read();

                if (ch != '\n' && ch != -1) {
                    buffer.append(Character.toChars(ch));
                } else {
                    buffer.trimToSize();
                    parse(buffer.toString());
                    buffer = new StringBuffer();
                }

            } while (ch != -1);

        } catch (Exception e) {
            // Ignore
        }

        // Assign parsed object to extracted data elements
        if (currentMessage != null) {
            parsedObject = currentMessage;
            //p("XML version of the parsed input message:\n" + currentMessage.toXML());
        } else if (currentTest != null) {
            parsedObject = currentTest;
            //p("XML version of the parsed input test:\n" + currentTest.toXML());
        } else if (currentProduct != null) {
            parsedObject = currentProduct;
            //p("XML version of the parsed input product:\n" + currentProduct.toXML());
        }

        return parsedObject;
    }

    /**
     * Parsing a single line of XML data.
     *
     * @param xmlLine Single line of XML data from the input stream
     */
    private void parse(String xmlLine) throws Exception {
        //p("Parsing XML line: " + xmlLine);
        xmlLine = xmlLine.trim();

        if (!xmlLine.isEmpty()) {
            // Try to extract element name
            elementName = null;
            elementValue = null;
            hasElementStart = false;
            hasElementValue = false;
            hasElementEnd = false;
            nameStart = xmlLine.indexOf("<");
            nameEnd = xmlLine.indexOf(">");

            if (nameStart != -1 && nameEnd != -1) {
                try {
                    elementName = xmlLine.substring(nameStart + 1, nameEnd);

                    if (!elementName.isEmpty()) {
                        if (elementName.startsWith("/")) {
                            // We have an end of element
                            elementName = elementName.substring(1);
                            hasElementEnd = true;
                        } else {
                            hasElementStart = true;

                            // Check what we have at the end of xml line
                            if (xmlLine.indexOf("</" + elementName + ">") != -1) {
                                hasElementEnd = true;
                            }
                        }
                    } else {
                        hasElementStart = true;
                    }
                } catch (Exception e) {
                    p("Got troubles with parsing the name of XML element:");
                    e.printStackTrace();
                }
            }

            valueStart = xmlLine.indexOf(">");
            valueEnd = xmlLine.lastIndexOf("<");

            if (valueStart != -1 && valueEnd != -1) {
                if (valueStart < valueEnd) {
                    try {
                        elementValue = xmlLine.substring(valueStart + 1, valueEnd);
                        hasElementValue = true;
                    } catch (Exception e) {
                        p("Got troubles with parsing the value of XML element:");
                        e.printStackTrace();
                    }
                }
            }

            if (hasElementStart) {
                startElement(elementName);
            }

            if (hasElementValue) {
                elementValue(elementValue);
            }

            if (hasElementEnd) {
                endElement(elementName);
            }

            // If its neither of above and there is some data, interpret it as a text line
            if (!hasElementStart && !hasElementValue && !hasElementEnd) {
                parseTextData(xmlLine);
            }
        }
    }

    /**
     * Parsing some data related to a test object.
     *
     * @param data Data related to a test object
     */
    private void parseTestData(String data) {
        //p("parseTestData(): currentTag = " + currentTag + ", data = " + data);

        if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_ID)) {
            currentTest.setId(data);
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_SUB_ID)) {
            currentTest.setSubId(data);
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_WORKSPACE_PATH)) {
            currentTest.setWorkspacePath(data);
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_URL)) {
            currentTest.setURL(data);
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_EXECUTOR_APPLICATION)) {
            currentTest.setExecutorApplication(data);
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_EXECUTOR_SCRIPT)) {
            currentTest.setExecutorScript(data);
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_TARGET)) {
            Test.Target target = Test.Target.FLASH; // By default

            if (data.equalsIgnoreCase(Test.TARGET_FLASH)) {
                target = Test.Target.FLASH;
            } else if (data.equalsIgnoreCase(Test.TARGET_NOSE)) {
                target = Test.Target.NOSE;
            }

            currentTest.setTarget(target);

        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_PRODUCT_RELEASING_MODE)) {
            Test.ProductReleasingMode productReleasingMode = Test.ProductReleasingMode.AUTOMATICALLY_RELEASE_RESERVED_PRODUCTS; // Default

            if (data.equalsIgnoreCase(Test.PRODUCT_RELEASING_MODE_AUTOMATIC)) {
                productReleasingMode = Test.ProductReleasingMode.AUTOMATICALLY_RELEASE_RESERVED_PRODUCTS;
            } else if (data.equalsIgnoreCase(Test.PRODUCT_RELEASING_MODE_MANUAL)) {
                productReleasingMode = Test.ProductReleasingMode.MANUALLY_RELEASE_RESERVED_PRODUCTS;
            }

            currentTest.setProductReleasingMode(productReleasingMode);

        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_REQUIRED_ENVIRONMENT)) {
            currentTest.setRequiredEnvironment(data);
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_PRODUCT_DISCONNECTION_TIMEOUT)) {
            try {
                Long timeout = Long.valueOf(data);
                currentTest.setProductDisconnectionTimeout(timeout.longValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_STATUS)) {
            Test.Status status = Test.Status.UNKNOWN; // Default
            try {
                if (data.equalsIgnoreCase(Test.STATUS_UNKNOWN)) {
                    status = Test.Status.UNKNOWN;
                } else if (data.equalsIgnoreCase(Test.STATUS_PENDING)) {
                    status = Test.Status.PENDING;
                } else if (data.equalsIgnoreCase(Test.STATUS_STARTED)) {
                    status = Test.Status.STARTED;
                } else if (data.equalsIgnoreCase(Test.STATUS_STOPPED)) {
                    status = Test.Status.STOPPED;
                } else if (data.equalsIgnoreCase(Test.STATUS_FINISHED)) {
                    status = Test.Status.FINISHED;
                } else if (data.equalsIgnoreCase(Test.STATUS_FAILED)) {
                    status = Test.Status.FAILED;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentTest.setStatus(status);
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_STATUS_DETAILS)) {
            currentTest.setStatusDetails(data);
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_START_TIME)) {
            try {
                Long startTime = Long.valueOf(data);
                currentTest.setStartTime(startTime.longValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_TIMEOUT)) {
            try {
                Long timeout = Long.valueOf(data);
                currentTest.setTimeout(timeout.longValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_RESULTS_FILENAME)) {
            currentTest.setResultsFilename(data);
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_ARTIFACT)) {
            if (parsingTestArtifacts) {
                currentTest.addArtifact(data);
            }
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_TEST_PACKAGES)) {
            parsing = PARSING_TEST_PACKAGES;
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_ENVPARAM_KEY)) {
            if (parsingTestEnvParams) {
            	currentParamKey = data;
            }
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_ENVPARAM_VALUE)) {
            if (parsingTestEnvParams) {
            	currentTest.addexecutorEnvparam( currentParamKey, data );
            }
        }
    }

    /**
     * Parsing some data related to a test package.
     *
     * @param data Data related to a test package
     */
    private void parseTestPackageData(String data) {
        //p("parseTestPackageData(): currentTag = " + currentTag + ", data = " + data);
        if (currentTag.equalsIgnoreCase(TestPackage.XML_ELEMENT_ID)) {
            currentTestPackage.setId(data);
        } else if (currentTag.equalsIgnoreCase(TestPackage.XML_ELEMENT_REQUIRED_ENVIRONMENT)) {
            currentTestPackage.setRequiredEnvironment(data);
        } else if (currentTag.equalsIgnoreCase(TestPackage.XML_ELEMENT_FILE)) {
            currentTestPackage.addFile(data);
        }
    }

    /**
     * Parsing some data related to a product object.
     *
     * @param data Data related to a product object
     */
    private void parseProductData(String data) {
        //p("parseProductData(): " + data);

        if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_FUSE_CONNECTION_NAME)) {
            currentProduct.setFuseConnectionName(data);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_FUSE_CONNECTION_ID)) {
            currentProduct.setFuseConnectionId(data);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_TRACE_CONNECTION_ID)) {
            currentProduct.setTraceConnectionId(data,false);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_SN)) {
            currentProduct.setSn( data );
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_SW_VERSION)) {
            currentProduct.setSwVer( data );
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_FINGERPRINT)) {
            currentProduct.setFingerprint( data );
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_PRODUCT_CODE)) {
            currentProduct.setProductCode( data );
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_IMEI)) {
            currentProduct.setIMEI(data);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_RM_CODE)) {
            currentProduct.setRMCode(data);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_HARDWARE_TYPE)) {
            currentProduct.setHardwareType(data);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_ROLE)) {
            Product.Role role = Product.Role.MAIN; // Default
            try {
                if (data.equalsIgnoreCase(Product.ROLE_MAIN)) {
                    role = Product.Role.MAIN;
                } else if (data.equalsIgnoreCase(Product.ROLE_REMOTE)) {
                    role = Product.Role.REMOTE;
                } else if (data.equalsIgnoreCase(Product.ROLE_REFERENCE)) {
                    role = Product.Role.REFERENCE;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentProduct.setRole(role);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_STATUS)) {
            Product.Status status = Product.Status.FREE; // Default
            try {
                if (data.equalsIgnoreCase(Product.STATUS_FREE)) {
                    status = Product.Status.FREE;
                } else if (data.equalsIgnoreCase(Product.STATUS_BUSY)) {
                    status = Product.Status.BUSY;
                } else if (data.equalsIgnoreCase(Product.STATUS_DISABLED)) {
                    status = Product.Status.DISABLED;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentProduct.setStatus(status);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_STATUS_DETAILS)) {
            currentProduct.setStatusDetails(data);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_RESERVATION_TIME)) {
            try {
                Long reservationTime = Long.valueOf(data);
                currentProduct.setReservationTime(reservationTime.longValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_RESERVATION_TIMEOUT)) {
            try {
                Long reservationTimeout = Long.valueOf(data);
                currentProduct.setReservationTimeout(reservationTimeout.longValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_DISCONNECTION_TIME)) {
            try {
                Long disconnectionTime = Long.valueOf(data);
                currentProduct.setDisconnectionTime(disconnectionTime.longValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_HOSTNAME)) {
            currentProduct.setHostname(data);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_IP_ADDRESS)) {
            currentProduct.setIPAddress(data);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_PORT_NUMBER)) {
            currentProduct.setPort(data);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_TAS_HOSTNAME)) {
            currentProduct.setTestAutomationServiceHostname(data);
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_TAS_PORT_NUMBER)) {
            try {
                Integer port = Integer.valueOf(data);
                currentProduct.setTestAutomationServicePort(port.intValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_ENVIRONMENT)) {
            currentProduct.setEnvironment(data);
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_PHONE_NUMBER)) {
            currentSimCard.setPhoneNumber(data);
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_PIN_1_CODE)) {
            currentSimCard.setPin1Code(data);
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_PIN_2_CODE)) {
            currentSimCard.setPin2Code(data);
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_PUK_1_CODE)) {
            currentSimCard.setPuk1Code(data);
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_PUK_2_CODE)) {
            currentSimCard.setPuk2Code(data);
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_SECURITY_CODE)) {
            currentSimCard.setSecurityCode(data);
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_IMSI)) {
            currentSimCard.setIMSI(data);
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_SERVICE_DIALLING_NUMBER)) {
            currentSimCard.setServiceDiallingNumber(data);
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_VOICE_MAILBOX_NUMBER)) {
            currentSimCard.setVoiceMailboxNumber(data);
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_OPERATOR)) {
            currentSimCard.setOperator( data );
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_OPERATOR_CODE)) {
            currentSimCard.setOperatorCode( data );
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_OPERATOR_COUNTRY)) {
            currentSimCard.setOperatorCountry( data );
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_SIGNAL)) {
            currentSimCard.setSignal( data );
        }
    }

    /**
     * Parsing some data related to a file description object.
     *
     * @param data Data related to a file description object
     */
    private void parseFileDescription(String data) {
        //p("parseFileDescription(" + data + ")");

        if (currentTag.equalsIgnoreCase(FileDescription.XML_ELEMENT_FILENAME)) {
            currentFileDescription.setFileName(data);
        } else if (currentTag.equalsIgnoreCase(FileDescription.XML_ELEMENT_FILEPATH)) {
            currentFileDescription.setFilePath(data);
        } else if (currentTag.equalsIgnoreCase(FileDescription.XML_ELEMENT_FILESIZE)) {
            try {
                Long fileSize = Long.valueOf(data);
                currentFileDescription.setFileSize(fileSize.longValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Parsing some data related to a test node description object.
     *
     * @param data Data related to a test node description object
     */
    private void parseTestNodeDescription(String data) {
        //p("parseTestNodeDescription(" + data + ")");

        if (currentTag.equalsIgnoreCase(TestNodeDescription.XML_ELEMENT_HOSTNAME)) {
            currentTestNodeDescription.setHostname(data);
        } else if (currentTag.equalsIgnoreCase(TestNodeDescription.XML_ELEMENT_PORT_NUMBER)) {
            try {
                Integer port = Integer.valueOf(data);
                currentTestNodeDescription.setPort(port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (currentTag.equalsIgnoreCase(TestNodeDescription.XML_ELEMENT_DESCRIPTION)) {
            currentTestNodeDescription.setDescription(data);
        } else if (currentTag.equalsIgnoreCase(TestNodeDescription.XML_ELEMENT_TEST_AUTOMATION_SOFTWARE_VERSION)) {
            currentTestNodeDescription.setTestAutomationSoftwareVersion(data);
        }
    }

    /**
     * Parsing some data related to an operation object.
     *
     * @param data Data related to an operation object
     */
    private void parseOperation(String data) {
        //p("parseOperation(" + data + ")");

        if (data.equalsIgnoreCase(FileOperation.OPERATION_PUT)) {
            currentFileOperationId = FileOperation.Id.PUT;
        } else if (data.equalsIgnoreCase(FileOperation.OPERATION_GET)) {
            currentFileOperationId = FileOperation.Id.GET;
        } else if (data.equalsIgnoreCase(FileOperation.OPERATION_ABORT)) {
            currentFileOperationId = FileOperation.Id.ABORT;
        } // Try to check if it is a test related operation
        else if (data.equalsIgnoreCase(TestOperation.OPERATION_UPDATE)) {
            currentTestOperationId = TestOperation.Id.UPDATE;
        } else if (data.equalsIgnoreCase(TestOperation.OPERATION_START)) {
            currentTestOperationId = TestOperation.Id.START;
        } else if (data.equalsIgnoreCase(TestOperation.OPERATION_STOP)) {
            currentTestOperationId = TestOperation.Id.STOP;
        } else if (data.equalsIgnoreCase(TestOperation.OPERATION_CHECK)) {
            currentTestOperationId = TestOperation.Id.CHECK;
        } // Try to check if it is a product related operation
        else if (data.equalsIgnoreCase(ProductOperation.OPERATION_UPDATE)) {
            currentProductOperationId = ProductOperation.Id.UPDATE;
        } else if (data.equalsIgnoreCase(ProductOperation.OPERATION_ADD)) {
            currentProductOperationId = ProductOperation.Id.ADD;
        } else if (data.equalsIgnoreCase(ProductOperation.OPERATION_REMOVE)) {
            currentProductOperationId = ProductOperation.Id.REMOVE;
        } // Try to check if it is a registry related operation
        else if (data.equalsIgnoreCase(RegistryOperation.OPERATION_REGISTER)) {
            currentRegistryOperationId = RegistryOperation.Id.REGISTER;
        } else if (data.equalsIgnoreCase(RegistryOperation.OPERATION_DEREGISTER)) {
            currentRegistryOperationId = RegistryOperation.Id.DEREGISTER;
        } else if (data.equalsIgnoreCase(RegistryOperation.OPERATION_UPDATE)) {
            currentRegistryOperationId = RegistryOperation.Id.UPDATE;
        }
    }

    /**
     * Parsing some data related to a registry object.
     *
     * @param data Data related to a registry object
     */
    private void parseRegistry(String data) {
        //p("parseRegistry(" + data + ")");

        if (currentTag.equalsIgnoreCase(RegistryOperation.XML_ELEMENT_REMOTE)) {
            if (data.equalsIgnoreCase(RegistryOperation.REMOTE_CLIENT)) {
                currentRegistryCategory = RegistryOperation.Remote.CLIENT;
            } else if (data.equalsIgnoreCase(RegistryOperation.REMOTE_TEST_NODE)) {
                currentRegistryCategory = RegistryOperation.Remote.TEST_NODE;

                // Create an new test node description object
                currentTestNodeDescription = new TestNodeDescription();
            } else {
                currentRegistryCategory = RegistryOperation.Remote.UNKNOWN;
            }
        }
    }

    /**
     * Parsing some data related to a text message object.
     *
     * @param data Data related to a text message object
     */
    private void parseTextData(String data) {
        //p("parseTextData(" + data + ")");
        // Always add data as a single line
        currentText.append(data);
        currentText.append("\n");
    }

    /**
     * Parsing some data related to a message object.
     *
     * @param data Data related to a message object
     */
    private void parseMessageData(String data) {
        //p("parseMessageData(" + data + ")");

        if (currentTag.equalsIgnoreCase(Message.XML_ELEMENT_TYPE)) {
            currentMessage.setType(data);
        } else if (currentTag.equalsIgnoreCase(Message.XML_ELEMENT_HOSTNAME)) {
            if (parsingMessageSender) {
                currentMessage.setSenderHostname(data);
            } else if (parsingMessageReceiver) {
                currentMessage.setReceiverHostname(data);
            }
        } else if (currentTag.equalsIgnoreCase(Message.XML_ELEMENT_PORT)) {
            try {
                Integer port = Integer.valueOf(data);

                if (parsingMessageSender) {
                    currentMessage.setSenderPort(port.intValue());
                } else if (parsingMessageReceiver) {
                    currentMessage.setReceiverPort(port.intValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Parsing some unsupported or unrecognized element.
     *
     * @param data Unsupported or unrecognized element
     */
    private void parseUnknownData(String data) {
        //p("parseUnknownData(): " + data);
    }

    /**
     * Called when some XML element's start was recognized in the input stream.
     *
     * @param qName XML element's start
     */
    private void startElement(String qName) {
        currentTag = qName;
        //p("startElement(): currentTag = " + currentTag);

        if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_TEST)) {
            parsing = PARSING_TEST;
            currentTest = new Test("");
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_TEST_PACKAGES)) {
            parsing = PARSING_TEST_PACKAGES;
            currentTestPackages = new ArrayList<TestPackage>(0);
        } else if (currentTag.equalsIgnoreCase(TestPackage.XML_ELEMENT_TEST_PACKAGE)) {
            currentTestPackage = new TestPackage("");
        } else if (currentTag.equalsIgnoreCase(TestPackage.XML_ELEMENT_FILES)) {
            parsingTestPackageFiles = true;
        } else if (currentTag.equalsIgnoreCase(Product.XML_ELEMENT_PRODUCT)) {
            parsing = PARSING_PRODUCT;
            currentProduct = new Product();
        } else if (currentTag.equalsIgnoreCase(Message.XML_ELEMENT_MESSAGE)) {
            parsing = PARSING_MESSAGE;
            currentMessage = new Message();
        } else if (currentTag.equalsIgnoreCase(FileOperation.XML_ELEMENT_OPERATION)) {
            parsing = PARSING_OPERATION;
        } else if (currentTag.equalsIgnoreCase(TextMessage.XML_ELEMENT_TEXT)) {
            parsing = PARSING_TEXT;
            currentText = new StringBuffer();
        } else if (currentTag.equalsIgnoreCase(RegistryOperation.XML_ELEMENT_REMOTE)) {
            parsing = PARSING_REGISTRY;
        } else if (currentTag.equalsIgnoreCase(TestNodeDescription.XML_ELEMENT_TEST_NODE)) {
            parsing = PARSING_TEST_NODE_DESCRIPTION;
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_ARTIFACTS)) {
            parsingTestArtifacts = true;
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_ENVPARAMS)) {
            parsingTestEnvParams = true;
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_REQUIRED_PRODUCTS)) {
            parsingTestRequiredProducts = true;
            currentTestRequiredProducts = new ArrayList<Product>(0);
        } else if (currentTag.equalsIgnoreCase(Test.XML_ELEMENT_RESERVED_PRODUCTS)) {
            parsingTestReservedProducts = true;
            currentTestReservedProducts = new ArrayList<Product>(0);
        } else if (currentTag.equalsIgnoreCase(Message.XML_ELEMENT_SENDER)) {
            parsingMessageSender = true;
        } else if (currentTag.equalsIgnoreCase(Message.XML_ELEMENT_RECEIVER)) {
            parsingMessageReceiver = true;
        } else if (currentTag.equalsIgnoreCase(FileDescription.XML_ELEMENT_FILE)) {
            if (!parsingTestPackageFiles) {
                // If not test package files are in question, then it's all about usual files descriptions
                parsing = PARSING_FILE_DESCRIPTION;
                currentFileDescription = new FileDescription();
            }
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_SIM_CARD_1)) {
            currentSimCard = new SimCard(SimCard.XML_ELEMENT_SIM_CARD_1);
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_SIM_CARD_2)) {
            currentSimCard = new SimCard(SimCard.XML_ELEMENT_SIM_CARD_2);
        }
    }

    /**
     * Called when some XML element's end was recognized in the input stream.
     *
     * @param qName XML element's end
     */
    private void endElement(String qName) throws Exception {
        currentTag = qName;
        //p("endElement(): currentTag = " + currentTag + ", parsing = " + parsing);

        // Process data according to the current state

        switch (parsing) {
            case PARSING_MESSAGE: {
                parseMessageData(currentData);
            } break;

            case PARSING_TEST: {
                parseTestData(currentData);
            } break;

            case PARSING_TEST_PACKAGES: {
                parseTestPackageData(currentData);
            }

            case PARSING_PRODUCT: {
                parseProductData(currentData);
            } break;

            case PARSING_FILE_DESCRIPTION: {
                parseFileDescription(currentData);
            } break;

            case PARSING_REGISTRY: {
                parseRegistry(currentData);
            } break;

            case PARSING_TEST_NODE_DESCRIPTION: {
                parseTestNodeDescription(currentData);
            } break;

            case PARSING_OPERATION: {
                parseOperation(currentData);
            } break;

            case PARSING_TEXT: {
                // Text lines has ended
                parsing = PARSING_UNKNOWN;
            } break;

            default: {
                parseUnknownData(currentData);
            } break;
        }

        if (qName.equalsIgnoreCase(Test.XML_ELEMENT_TEST)) {
            // Parsing of a test object is over
            parsing = PARSING_UNKNOWN;
        } else if (qName.equalsIgnoreCase(TestPackage.XML_ELEMENT_TEST_PACKAGE)) {

            if (currentTestPackage != null) {
                currentTestPackages.add(currentTestPackage);
            }

            parsingTestPackageFiles = false;

        } else if (qName.equalsIgnoreCase(Test.XML_ELEMENT_TEST_PACKAGES)) {

            if (currentTest != null) {
                if (currentTestPackages != null) {
                    currentTest.setTestPackages(currentTestPackages);
                }
            }

            parsing = PARSING_TEST; // Continue parsing the test

        } else if (qName.equalsIgnoreCase(Product.XML_ELEMENT_PRODUCT)) {
            // Parsing of a product object is over

            if (parsingTestRequiredProducts) {
                currentTestRequiredProducts.add(currentProduct);
            }

            if (parsingTestReservedProducts) {
                currentTestReservedProducts.add(currentProduct);
            }

            parsing = PARSING_UNKNOWN;

        } else if (qName.equalsIgnoreCase(FileDescription.XML_ELEMENT_FILE)) {

            if (!parsingTestPackageFiles) {
                // Parsing of a file description object is over
                parsing = PARSING_UNKNOWN;
            }

        } else if (qName.equalsIgnoreCase(TestNodeDescription.XML_ELEMENT_TEST_NODE)) {
            // Parsing of a test node description is over

            // Store parsed test node description
            if (currentMessage != null && currentMessage instanceof RegistryOperation) {
                RegistryOperation registryOperation = new RegistryOperation(currentMessage);

                if (currentTestNodeDescription != null) {
                    registryOperation.setTestNodeDescription(currentTestNodeDescription);

                    // Store changes
                    currentMessage = registryOperation;
                }
            }

            parsing = PARSING_UNKNOWN;

        } else if (qName.equalsIgnoreCase(Message.XML_ELEMENT_MESSAGE)) {
            // Parsing of a message object is over

            // This is the place where we should create a specific message
            String messageType = currentMessage.getType();

            if (messageType.equalsIgnoreCase(Message.TYPE_TEST_OPERATION)) {
                // Create a test operation message
                TestOperation testOperation = new TestOperation(currentMessage);

                if (currentTest != null) {
                    testOperation.setTest(currentTest);
                }

                if (currentTestOperationId != null) {
                    testOperation.setId(currentTestOperationId);
                }

                // Store changes
                currentMessage = testOperation;

            } else if (messageType.equalsIgnoreCase(Message.TYPE_PRODUCT_OPERATION)) {
                // Create a product operation message
                ProductOperation productOperation = new ProductOperation(currentMessage);

                if (currentTest != null) {
                    productOperation.setTest(currentTest);
                }

                if (currentProduct != null) {
                    productOperation.setProduct(currentProduct);
                }

                if (currentProductOperationId != null) {
                    productOperation.setId(currentProductOperationId);
                }

                // Store changes
                currentMessage = productOperation;

            } else if (messageType.equalsIgnoreCase(Message.TYPE_FILE_OPERATION)) {
                // Create a file operation message
                FileOperation fileOperation = new FileOperation(currentMessage);

                if (currentTest != null) {
                    fileOperation.setTest(currentTest);
                }

                if (currentFileDescription != null) {
                    fileOperation.setFileDescription(currentFileDescription);
                }

                if (currentFileOperationId != null) {
                    fileOperation.setId(currentFileOperationId);
                }

                // Store changes
                currentMessage = fileOperation;

            } else if (messageType.equalsIgnoreCase(Message.TYPE_REGISTRY_OPERATION)) {
                // Create a registry operation message
                RegistryOperation registryOperation = new RegistryOperation(currentMessage);

                if (currentRegistryOperationId != null) {
                    registryOperation.setId(currentRegistryOperationId);
                }

                if (currentRegistryCategory != null) {
                    registryOperation.setRemote(currentRegistryCategory);
                }

                if (currentTestNodeDescription != null) {
                    registryOperation.setTestNodeDescription(currentTestNodeDescription);
                }

                // Store changes
                currentMessage = registryOperation;

            } else if (messageType.equalsIgnoreCase(Message.TYPE_TEXT_MESSAGE)) {
                // Create a text message
                TextMessage textMessage = new TextMessage(currentMessage);

                if (currentTest != null) {
                    textMessage.setTest(currentTest);
                }

                if (currentText != null) {
                    textMessage.setText(currentText.toString());
                }

                // Store changes
                currentMessage = textMessage;
            }

            parsing = PARSING_UNKNOWN;

            // Stop handling of current message by throwing exception catched in the handle() method
            throw new Exception("Message parsing is over");

        } else if (qName.equalsIgnoreCase(Test.XML_ELEMENT_ARTIFACTS)) {
            // A list of test artifacts is over
            parsingTestArtifacts = false;
        } else if (qName.equalsIgnoreCase(Test.XML_ELEMENT_ENVPARAMS)) {
            // A list of test ENVPARAMS is over
            parsingTestEnvParams = false;
        } else if (qName.equalsIgnoreCase(Test.XML_ELEMENT_REQUIRED_PRODUCTS)) {
            // Parsing of the list of products required by a test is over
            // Store parsed required products
            if (currentTest != null) {
                currentTest.setRequiredProducts(currentTestRequiredProducts);
            }
            parsingTestRequiredProducts = false;
        } else if (qName.equalsIgnoreCase(Test.XML_ELEMENT_RESERVED_PRODUCTS)) {
            // Parsing of the list of products reserved by a test is over
            // Store parsed reserved products
            if (currentTest != null) {
                currentTest.setReservedProducts(currentTestReservedProducts);
            }
            parsingTestReservedProducts = false;
        } else if (currentTag.equalsIgnoreCase(Message.XML_ELEMENT_SENDER)) {
            // Parsing of the message sender's credentials is over
            parsingMessageSender = false;
        } else if (currentTag.equalsIgnoreCase(Message.XML_ELEMENT_RECEIVER)) {
            // Parsing of the message receiver's credentials is over
            parsingMessageReceiver = false;
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_SIM_CARD_1)) {
            // Parsing of the 1st SIM card is over
            // Store parsed SIM card
            if (currentProduct != null) {
                currentProduct.setSim1(currentSimCard);
            }
        } else if (currentTag.equalsIgnoreCase(SimCard.XML_ELEMENT_SIM_CARD_2)) {
            // Parsing of the 2nd SIM card is over
            // Store parsed SIM card
            if (currentProduct != null) {
                currentProduct.setSim2(currentSimCard);
            }
        } else if (currentTag.equalsIgnoreCase(FileOperation.XML_ELEMENT_OPERATION)) {
            // Parsing of the operation is over
            parsing = PARSING_UNKNOWN;
        } else if (currentTag.equalsIgnoreCase(RegistryOperation.XML_ELEMENT_REMOTE)) {
            // Parsing of the remote object is over
            parsing = PARSING_UNKNOWN;
        } else if (currentTag.equalsIgnoreCase(TextMessage.XML_ELEMENT_TEXT)) {
            // Parsing of the text message data is over
            parsing = PARSING_UNKNOWN;
        }
    }

    /**
     * Called when some XML element's value was recognized in the input stream.
     *
     * @param value XML element's value
     */
    private void elementValue(String value) {
        //p("elementValue():\t " + value);
        currentData = value;
    }

    /**
     * Prints specified text to some debugging stream.
     *
     * @param text A text to be printed to some debugging stream
     */
    private void p(String text) {
        System.out.println("Converter: " + text);
    }
}
