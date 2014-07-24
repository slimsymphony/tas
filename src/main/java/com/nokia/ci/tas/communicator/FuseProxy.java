package com.nokia.ci.tas.communicator;

import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.Socket;

import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.Util;

/**
 * A proxy involved in communication with FUSE subsystem through the CIProxy.exe applicaiton.
 * CIProxy.exe queries the FUSE subsystem installed on a test node and returns information
 * about all currently connected product in specially formatted string, like for example:
 *
 * 2@RM-763|USB1|GConnId_c2b2a6444f2446d4|004402136230483|15000$
 *
 * 2@RM-763|USB1|GConnId_c2b2a6444f2446d4|004402136230483|15000@RM-767|USB3|GConnId_407c14f5d0ba453e|004402138132034|15001$
 *
 * 0@RM-763|USB1|GConnId_c2b2a6444f2446d4|004402136230483|15000$
 *
 * 0@RM-767|USB3|GConnId_407c14f5d0ba453e|004402138132034|15001$
 *
 * 1@RM-767|USB3|GConnId_407c14f5d0ba453e|004402138132034|15001$
 *
 * or just
 *
 * 2$
 *
 * The first digit in returned string means event id that CI Proxy is telling us. The following event ids are supported:
 *
 *  0 - a product has disconnected
 *  1 - a product has connected
 *  2 - printing all currently available products
 *
 * Each product is represented by a set of data elements in the following order:
 *
 *  0. RM code
 *  1. FUSE connection name
 *  2. FUSE connection id
 *  3. IMEI number
 *  4. Port number
 *
 * Each product is separated from the others through a special symbol '@'.
 * Each element of product data is separated from each other through a special symbol '|'.
 * The end of message returned by CIProxy.exe application is always indicated by a special symbol '$'.
 */
public class FuseProxy extends Thread {

    /**
     * A command code used by the CI Proxy application for listing all available products.
     */
    private static final String LIST_ALL_PRODUCTS_COMMAND = "1";

    /**
     * Id of a product disconnected event issued by CI Proxy.
     */
    private static final int PRODUCT_DISCONNECTED = 0;

    /**
     * Id of a product connected event issued by CI Proxy.
     */
    private static final int PRODUCT_CONNECTED = 1;

    /**
     * Id of CI Proxy event indicating all currently available products.
     */
    private static final int ALL_AVAILABLE_PRODUCTS_LISTED = 2;

    /**
     * Hostname where CI Proxy is running by default.
     */
    private static final String PROXY_HOST = "localhost";

    /**
     * Port number that CI proxy is using by default.
     */
    private static final int PROXY_DEFAULT_PORT = 14999;

    /**
     * Character set that CI proxy is using by default.
     */
    private static final String PROXY_CHARSET = "ASCII";

    /**
     * A character used by CI proxy to identificate beginning of product's data.
     * Currently it is the '@' ASCII character.
     */
    private static final String PRODUCT_DATA_START_CHARACTER = "@";

    /**
     * A character used by CI Proxy to separate elements of product's data from each other.
     * Currently it is the '|' ASCII character.
     */
    private static final String PRODUCT_DATA_ELEMENTS_SEPARATOR = "|";

    /**
     * ASCII code of a character used by CI Proxy to identificate the end of a message.
     * Currently it is the '$' ASCII character.
     */
    private static final int MESSAGE_END_CHARACTER_CODE = 36;

    /**
     * Symbolic representation of a character used by CI Proxy to identificate the end of a message.
     * Currently it is the '$' ASCII character.
     */
    private static final String MESSAGE_END_CHARACTER = "$";

    /**
     * A socket interface to CI Proxy.
     */
    private Socket proxySocket;

    /**
     * Input stream to the CI Proxy.
     */
    private InputStream proxyInput;

    /**
     * Output stream from the CI Proxy.
     */
    private OutputStream proxyOutput;

    /**
     * Handler of all discovered products.
     */
    private ProductExplorer productExplorer;

    /**
     * Hostname of the host where CI Proxy is running.
     */
    private String hostname;

    /**
     * IP address of the host where CI Proxy is running.
     */
    private String hostIPAddress;

    /**
     * Variable which keeps this component running.
     */
    private boolean isRunning = true;

    /**
     * Instance of the Test Automation Communicator's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationCommunicator.GLOBAL_LOGGER_NAME);

    /**
     * Default constructor.
     *
     * @param productExplorer Instance fo a product exploring module
     */
    public FuseProxy(ProductExplorer productExplorer) {
        this.productExplorer = productExplorer;
        hostname = TestAutomationCommunicator.getInstance().getHostname();
    }

    /**
     * Main routine.
     */
    @Override
    public void run() {
        try {
            if (hostname == null || hostname.isEmpty()) {
                // Try to get the hostname
                hostname = InetAddress.getLocalHost().getCanonicalHostName();
            }

            if (hostIPAddress == null || hostIPAddress.isEmpty()) {
                // Try to get the IP address
                hostIPAddress = InetAddress.getLocalHost().getHostAddress();
            }

            proxySocket = new Socket(PROXY_HOST, PROXY_DEFAULT_PORT);

            proxyInput = proxySocket.getInputStream();
            proxyOutput = proxySocket.getOutputStream();

            List<Product> discoveredProducts = new ArrayList(0);
            StringBuffer proxyMessage = new StringBuffer();
            int ch = -1;

            // Reset the list of discovered products
            discoveredProducts.clear();

            // Ask CI Proxy to list all currently available products
            proxyOutput.write(LIST_ALL_PRODUCTS_COMMAND.getBytes(PROXY_CHARSET));
            proxyOutput.flush();

            while (isRunning) {
                // Read next symbol
                ch = proxyInput.read();

                if (ch != -1) { // If we have some data
                    proxyMessage.append(Character.toChars(ch));

                    if (ch == MESSAGE_END_CHARACTER_CODE) {
                        p("Processing CI Proxy message: " + proxyMessage.toString());

                        String message = proxyMessage.toString().trim();

                        if (!message.isEmpty() && message.endsWith(MESSAGE_END_CHARACTER)) {
                            try {
                                // Don't take the '$' symbol (end of message) into parsing
                                message = message.substring(0, message.length() - 1);

                                // Parse operation code
                                String operationCode = message.substring(0, 1);

                                int code = Integer.parseInt(operationCode);

                                message = message.substring(1); // Don't take event code in parsing

                                // Parse block of data for each product
                                int startIndex = 0;
                                int endIndex = -1;

                                do {
                                    // Each product's data block starts with symbol '@'
                                    startIndex = message.indexOf(PRODUCT_DATA_START_CHARACTER, startIndex);
                                    endIndex = message.indexOf(PRODUCT_DATA_START_CHARACTER, startIndex + 1);

                                    String productBlock = "";

                                    if (endIndex != -1) {
                                        productBlock = message.substring(startIndex + 1, endIndex);
                                    } else {
                                        productBlock = message.substring(startIndex + 1);
                                    }

                                    if (!productBlock.isEmpty()) {
                                        switch (code) {
                                            case PRODUCT_DISCONNECTED: {
                                                Product product = parseProduct(productBlock);
                                                if (product != null) {
                                                    productExplorer.removeProduct(product);
                                                }
                                            } break;

                                            case PRODUCT_CONNECTED: {
                                                Product product = parseProduct(productBlock);
                                                if (product != null) {
                                                    productExplorer.addProduct(product);
                                                }
                                            } break;

                                            case ALL_AVAILABLE_PRODUCTS_LISTED: {
                                                Product product = parseProduct(productBlock);
                                                if (product != null) {
                                                    productExplorer.updateProduct(product);
                                                }
                                            } break;

                                            default: {
                                                p("Unsupported CI Proxy event: " + operationCode);
                                            } break;
                                        }
                                    }

                                    startIndex = endIndex;

                                } while (endIndex != -1);

                            } catch (Exception e) {
                                p("Got troubles while tried to parse CI Proxy message: " + message);
                                e.printStackTrace();
                            }
                        }

                        // Get ready for the next message
                        proxyMessage = new StringBuffer();
                    }
                }

                sleep(Constant.MILLISECOND); // Wait for any updates
            }
        } catch (Exception e) {
            p("FUSE Proxy got a trouble and stops working: " + e.getClass() + " - " + e.getMessage());
            isRunning = false;
        }

        try {
            if (proxyInput != null) {
                proxyInput.close();
            }
        } catch (Exception e) {
            // Ignore
        }

        try {
            if (proxyOutput != null) {
                proxyOutput.close();
            }
        } catch (Exception e) {
            // Ignore
        }

        try {
            if (proxySocket != null) {
                proxySocket.close();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Parses product from the CI Proxy message
     * and returns object of type Product containing parsed data or null if parsing wasn't successful.
     * The product messages from CI Proxy must have the following format:
     *
     * RM code|FUSE connection name|FUSE connection id|IMEI number|Port number
     *
     * where character "|" separates product data elements from each other.
     *
     * @param proxyMessage A message from CI Proxy containing product data
     * @return Object of type Product containing parsed data or null if parsing wasn't successful
     */
    private Product parseProduct(String proxyMessage) {

        Product product = null;
        try {
            // 1. Parse RM code
            int index = 0;
            String rmCode = proxyMessage.substring(index, proxyMessage.indexOf(PRODUCT_DATA_ELEMENTS_SEPARATOR, index));

            // 2. Parse FUSE connection name
            index += rmCode.length() + 1;
            String fuseConnectionName = proxyMessage.substring(index, proxyMessage.indexOf(PRODUCT_DATA_ELEMENTS_SEPARATOR, index));

            // 3. Parse FUSE connection id
            index += fuseConnectionName.length() + 1;
            String fuseConnectionId = proxyMessage.substring(index, proxyMessage.indexOf(PRODUCT_DATA_ELEMENTS_SEPARATOR, index));

            // 4. Parse IMEI number
            index += fuseConnectionId.length() + 1;
            String imei = proxyMessage.substring(index, proxyMessage.indexOf(PRODUCT_DATA_ELEMENTS_SEPARATOR, index));

            // 5. Parse port number
            index += imei.length() + 1;
            String port = proxyMessage.substring(index);

            product = new Product();

            product.setRMCode(rmCode);
            product.setFuseConnectionName(fuseConnectionName);
            product.setFuseConnectionId(fuseConnectionId);
            //TODO: parse traceConnection
            String traceConnectionId = fuseConnectionId;
            product.setTraceConnectionId( fuseConnectionId );
            product.setIMEI(imei);
            product.setPort(port);

            // Plasse note, that the hostname is not supplied in CI Proxy messages
            product.setHostname(hostname);

            // Please note, that the host IP address is not supplied in CI Proxy messages
            product.setIPAddress(hostIPAddress);

        } catch (Exception e) {
            p("Got troubles while tried to parse proxy message: " + proxyMessage + " - " + e.getClass() + " - " + e.getMessage());
            e.printStackTrace();
        }

        return product;
    }

    /**
     * Prints specified text to output stream.
     *
     * @param text Message to print out
     */
    private void p(String text) {
        logger.log(Level.ALL, "FUSE Proxy: " + text);
    }
}
