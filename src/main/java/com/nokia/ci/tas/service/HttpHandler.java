package com.nokia.ci.tas.service;

import java.io.OutputStream;
import java.io.PrintWriter;

import java.net.Socket;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.TestNodeDescription;

/**
 * Handles and dispatches all the HTTP messages received by the instance of Testing Automation Service.
 */
public class HttpHandler extends Thread {

    /**
     * Token for the test node's hostname.
     */
    public static final String TEST_NODE_HOSTNAME_TOKEN = TestNodeDescription.XML_ELEMENT_TEST_NODE + "-" + TestNodeDescription.XML_ELEMENT_HOSTNAME;

    /**
     * Token for the test node's port number.
     */
    public static final String TEST_NODE_PORT_NUMBER_TOKEN = TestNodeDescription.XML_ELEMENT_TEST_NODE + "-" + TestNodeDescription.XML_ELEMENT_PORT_NUMBER;

    /**
     * Token for the product's IMEI number.
     */
    public static final String PRODUCT_IMEI_TOKEN = Product.XML_ELEMENT_PRODUCT + "-" + Product.XML_ELEMENT_IMEI;

    /**
     * Key token for getting day's statistics.
     */
    public static final String DATE_STATISTICS_TOKEN = "statistics-date";
    
    /**
     * Key token for getting test farm's products.
     */
    public static final String PRODUCTS_TOKEN = "products";

    /**
     * Key token for indicating an action.
     */
    public static final String ACTION_TOKEN = "action";

    /**
     * Action for turning maintenance mode on.
     */
    public static final String ACTION_TURN_MAINTENANCE_MODE_ON = "turn-maintenance-mode-on";

    /**
     * Action for turning maintenance mode off.
     */
    public static final String ACTION_TURN_MAINTENANCE_MODE_OFF = "turn-maintenance-mode-off";

    /**
     * A pool of requests issued to this HTTP handler.
     */
    private ConcurrentLinkedQueue<HttpRequest> requests;

    /**
     * Variable which keeps handler running.
     */
    private boolean isRunning = true;

    /**
     * Current instance of the Test Automation Service.
     */
    private TestAutomationService testAutomationService;

    /**
     * Current status of the Test Automation Service instance.
     */
    private String status = "";
    
    /**
     * Description of all products handled by the Test Automation Service.
     */
    private String productDescriptions = "";

    /**
     * The last time status was updated.
     */
    private long lastTimeStatusChecked = 0L;

    /**
     * The standard sequence of characters to end lines in HTTP headers.
     */
    private static final String CRLF = "\r\n";

    /**
     * Instance of the Test Automation Service's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationService.GLOBAL_LOGGER_NAME);

    /**
     * Default constructor.
     *
     * @param testAutomationService Instance of running Test Automation Service
     */
    public HttpHandler(TestAutomationService testAutomationService) {

        super(); // Start as anonymous thread

        this.testAutomationService = testAutomationService;

        requests = new ConcurrentLinkedQueue();

        try {
            status = testAutomationService.getCurrentStatus();
            productDescriptions = testAutomationService.getProductDescriptions();
            lastTimeStatusChecked = System.currentTimeMillis();
        } catch (Exception e) {
            // Ignore
        }

        lastTimeStatusChecked = System.currentTimeMillis();

        setPriority(Thread.MAX_PRIORITY);
    }

    /**
     * Receiver's main routine.
     */
    @Override
    public void run() {
        p("Started working");

        while (isRunning) {
            try {
                if (!requests.isEmpty()) {
                    HttpRequest httpRequest = requests.poll();
                    
                    Socket connection = null;
                    String request = null;

                    if (httpRequest != null) {
                        connection = httpRequest.getConnection();
                        request = httpRequest.getRequest();
                    }
                    
                    OutputStream outputStream = null;

                    if (connection != null) {
                        String response = "";

                        if (request != null) {
                            p("Handling request " + request + " from " + connection.getInetAddress().getHostName() + ":" + connection.getPort());

                            if (request.contains(ACTION_TOKEN)) {
                                try {
                                    // So far all actions are enabled only on test nodes
                                    String action = request.substring(request.indexOf(Constant.NAME_VALUE_SEPARATOR) + 1, request.indexOf(Constant.NAME_VALUE_PAIR_SEPARATOR));
                                    request = request.substring(request.indexOf(action) + action.length() + 1);

                                    String hostname = request.substring(request.indexOf(Constant.NAME_VALUE_SEPARATOR) + 1, request.indexOf(Constant.NAME_VALUE_PAIR_SEPARATOR));
                                    request = request.substring(request.indexOf(hostname) + hostname.length() + 1);

                                    String portNumber = request.substring(request.indexOf(Constant.NAME_VALUE_SEPARATOR) + 1, request.indexOf(Constant.NAME_VALUE_PAIR_SEPARATOR));
                                    int port = Integer.parseInt(portNumber);

                                    // Get test node mentioned in the action
                                    TestNode testNode = testAutomationService.getTestNode(hostname, port);

                                    if (testNode != null) {
                                        // Perform an action
                                        if (action.equalsIgnoreCase(ACTION_TURN_MAINTENANCE_MODE_ON)) {
                                            // Turn on the maintenance mode
                                            testNode.setMaintenanceMode(true);
                                            p("Got a request to turn maintenance mode ON for the test node " + hostname + ":" + portNumber);
                                        } else if (action.equalsIgnoreCase(ACTION_TURN_MAINTENANCE_MODE_OFF)) {
                                            // Turn off the maintenance mode
                                            testNode.setMaintenanceMode(false);
                                            p("Got a request to turn maintenance mode OFF for the test node " + hostname + ":" + portNumber);
                                        } else {
                                            // Do nothing
                                            p("Got a request for unsupported action " + action + " on test node " + hostname + ":" + portNumber);
                                        }

                                        // Return user to test node's page
                                        response = testNode.getDetailedStatus().toString();

                                    } else {
                                        // Return user to the main page
                                        response = status;
                                    }
                                } catch (Exception e) {
                                    p("Got troubles during processing incoming connection from " + connection.getInetAddress() + ": " + e.getClass() + " - " + e.getMessage());
                                    e.printStackTrace();
                                }
                            } else if (request.contains(PRODUCT_IMEI_TOKEN)) {
                                try {
                                    String hostname = request.substring(request.indexOf(Constant.NAME_VALUE_SEPARATOR) + 1, request.indexOf(Constant.NAME_VALUE_PAIR_SEPARATOR));
                                    request = request.substring(request.indexOf(hostname) + hostname.length() + 1);

                                    String portNumber = request.substring(request.indexOf(Constant.NAME_VALUE_SEPARATOR) + 1, request.indexOf(Constant.NAME_VALUE_PAIR_SEPARATOR));
                                    int port = Integer.parseInt(portNumber);
                                    request = request.substring(request.indexOf(portNumber) + portNumber.length() + 1);

                                    String productIMEI = request.substring(request.indexOf(Constant.NAME_VALUE_SEPARATOR) + 1, request.indexOf(Constant.NAME_VALUE_PAIR_SEPARATOR));

                                    TestNode testNode = testAutomationService.getTestNode(hostname, port);

                                    if (testNode != null) {

                                        if (productIMEI != null && !productIMEI.isEmpty()) {
                                            response = testNode.getDetailedProductStatus(productIMEI).toString();
                                        } else {
                                            response = testNode.getDetailedStatus().toString();
                                        }

                                    } else {
                                        response = status;
                                    }
                                } catch (Exception e) {
                                    p("Got troubles during processing incoming connection from " + connection.getInetAddress() + ": " + e.getClass() + " - " + e.getMessage());
                                    e.printStackTrace();
                                }
                            } else if (request.contains(TEST_NODE_HOSTNAME_TOKEN)) {
                                try {
                                    String hostname = request.substring(request.indexOf(Constant.NAME_VALUE_SEPARATOR) + 1, request.indexOf(Constant.NAME_VALUE_PAIR_SEPARATOR));
                                    request = request.substring(request.indexOf(hostname) + hostname.length() + 1);

                                    String portNumber = request.substring(request.indexOf(Constant.NAME_VALUE_SEPARATOR) + 1, request.indexOf(Constant.NAME_VALUE_PAIR_SEPARATOR));
                                    int port = Integer.parseInt(portNumber);

                                    TestNode testNode = testAutomationService.getTestNode(hostname, port);

                                    if (testNode != null) {
                                        response = testNode.getDetailedStatus().toString();
                                    } else {
                                        response = status;
                                    }
                                } catch (Exception e) {
                                    p("Got troubles during processing incoming connection from " + connection.getInetAddress() + ": " + e.getClass() + " - " + e.getMessage() + " " + e.getStackTrace());
                                    e.printStackTrace();
                                }
                            } else if (request.contains(DATE_STATISTICS_TOKEN)) {
                                try {
                                    String parsedDate = request.substring(request.indexOf(Constant.NAME_VALUE_SEPARATOR) + 1, request.indexOf(Constant.NAME_VALUE_PAIR_SEPARATOR));

                                    if (parsedDate != null) {
                                        response = testAutomationService.getDateStatus(parsedDate).toString();
                                    } else {
                                        response = status;
                                    }
                                } catch (Exception e) {
                                    p("Got troubles during processing incoming connection from " + connection.getInetAddress() + ": " + e.getClass() + " - " + e.getMessage());
                                    e.printStackTrace();
                                }
                            } else if (request.contains(PRODUCTS_TOKEN)) {
                                try {
                                    response = productDescriptions;
                                } catch (Exception e) {
                                    p("Got troubles during processing incoming connection from " + connection.getInetAddress() + ": " + e.getClass() + " - " + e.getMessage());
                                    e.printStackTrace();
                                }
                            } else {
                                response = status;
                            }
                        } else {
                            p("Handling invalid request " + request + " from " + connection.getInetAddress());
                            // Simply put a default status
                            response = status;
                        }

                        // Send response back and close connection
                        try {
                            outputStream = connection.getOutputStream();
                            PrintWriter output = new PrintWriter(outputStream);

                            output.print("HTTP/1.1 200 OK" + CRLF);
                            output.print("Content-Type: text/html; charset=UTF-8" + CRLF);
                            output.print("Content-Length: " + response.getBytes("UTF-8").length + CRLF);
                            output.print(CRLF);
                            output.print(response + CRLF + CRLF);
                            output.flush();

                            // Don't close connections, input or output streams here immediately, since HTTP 1.1 is using pervasive connections
                            sleep(Constant.DECISECOND);

                        } catch (Exception e) {
                            p("Got troubles during processing incoming connection from " + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                                + " (" + connection.getInetAddress().getHostAddress() + ":" + connection.getPort() + ") - "
                                + e.getClass() + " - " + e.getMessage());
                            e.printStackTrace();
                        } finally {
                            // Always ensure that output stream is closed
                            if (outputStream != null) {
                                try {
                                    outputStream.close();
                                } catch (Exception e) {
                                    p("Got troubles while tried to close output stream from "
                                        + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                                        + " - " + e.getClass() + " " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }

                            // Always ensure that connection is closed
                            if (connection != null) {
                                try {
                                    connection.close();
                                } catch (Exception e) {
                                    p("Got troubles while tried to close a connection from "
                                        + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                                        + " - " + e.getClass() + " - " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    // Always perform cleanups, no matter what has happened before

                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Exception e) {
                            p("Got troubles during closing output stream from connection " + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                                + " (" + connection.getInetAddress().getHostAddress() + ":" + connection.getPort() + ") - "
                                + e.getClass() + " - " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    if (connection != null) {
                        try {
                            connection.close();
                            p("Successfully closed connection from " + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                                + " (" + connection.getInetAddress().getHostAddress() + ":" + connection.getPort() + ")");
                        } catch (Exception e) {
                            p("Got troubles during closing connection from " + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                                + " (" + connection.getInetAddress().getHostAddress() + ":" + connection.getPort() + ") - "
                                + e.getClass() + " - " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }

                // Update main status each 15 seconds
                if ((System.currentTimeMillis() - lastTimeStatusChecked) > Constant.FIFTEEN_SECONDS) {
                    try {
                        status = testAutomationService.getCurrentStatus();
                        productDescriptions = testAutomationService.getProductDescriptions();
                    } catch (Exception e) {
                        p("Got troubles during extracting current status of the Test Automation Service: " + e.getClass() + " - " + e.getMessage());
                        e.printStackTrace();
                    }

                    // Don't overload Test Automation Service in any case
                    lastTimeStatusChecked = System.currentTimeMillis();
                }

                sleep(Constant.MILLISECOND); // Wait for any updates
            }
            catch (InterruptedException e) {
                p("HTTP handler was interrupted, stop working");
                p("Closing all available incoming connections");

                for (HttpRequest request : requests) {
                    Socket connection = request.getConnection();

                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (Exception ioe) {
                            // Ignore
                        }
                    }
                }

                requests.clear();

                isRunning = false;
            }
        }
    }

    /**
     * Puts specified socket into connection pool.
     *
     * @param socket Connection to be processed
     * @param request HTTP request to be processed
     */
    public synchronized void handle(Socket socket, String request) {
        requests.add(new HttpRequest(socket, request));
        notify();
    }

    /**
     * Generates Uniform Resource Identifier (URI) for specified test node.
     *
     * @param hostname Hostname of the test node
     * @param port Port number of the test node
     * @return URI for specified test node
     */
    public static String getTestNodeURI(String hostname, int port) {
        StringBuffer link = new StringBuffer("/");

        link.append(TEST_NODE_HOSTNAME_TOKEN + Constant.NAME_VALUE_SEPARATOR + hostname + Constant.NAME_VALUE_PAIR_SEPARATOR);
        link.append(TEST_NODE_PORT_NUMBER_TOKEN + Constant.NAME_VALUE_SEPARATOR + port + Constant.NAME_VALUE_PAIR_SEPARATOR);

        return link.toString();
    }
    
    /**
     * Generates Uniform Resource Identifier (URI) for specified action on specified test node.
     *
     * @param action Name of the action to be perfomed
     * @param hostname Hostname of the test node
     * @param port Port number of the test node
     * @return URI for specified test node
     */
    public static String getTestNodeActionURI(String action, String hostname, int port) {
        StringBuffer link = new StringBuffer("/");

        link.append(ACTION_TOKEN + Constant.NAME_VALUE_SEPARATOR + action + Constant.NAME_VALUE_PAIR_SEPARATOR);
        link.append(TEST_NODE_HOSTNAME_TOKEN + Constant.NAME_VALUE_SEPARATOR + hostname + Constant.NAME_VALUE_PAIR_SEPARATOR);
        link.append(TEST_NODE_PORT_NUMBER_TOKEN + Constant.NAME_VALUE_SEPARATOR + port + Constant.NAME_VALUE_PAIR_SEPARATOR);

        return link.toString();
    }

    /**
     * Generates Uniform Resource Identifier (URI) for specified product.
     *
     * @param testNodeHotname Hostname of the test node where product is available
     * @param testNodePort Port number of the test node where product is available
     * @param imei IMEI number of the product
     * @return URI for specified product
     */
    public static String getProductURI(String testNodeHotname, int testNodePort, String imei) {
        StringBuffer link = new StringBuffer(getTestNodeURI(testNodeHotname, testNodePort));

        link.append(PRODUCT_IMEI_TOKEN + Constant.NAME_VALUE_SEPARATOR + imei + Constant.NAME_VALUE_PAIR_SEPARATOR);

        return link.toString();
    }

    /**
     * Generates Uniform Resource Identifier (URI) for specified date statistics.
     *
     * @param date Date to be used
     * @return URI for specified date statistics
     */
    public static String getDateStatisticsURI(String date) {
        StringBuffer link = new StringBuffer("/");

        link.append(DATE_STATISTICS_TOKEN + Constant.NAME_VALUE_SEPARATOR + date + Constant.NAME_VALUE_PAIR_SEPARATOR);

        return link.toString();
    }

    /**
     * Stops handler running.
     */
    public synchronized void stopWorking() {
        isRunning = false;
    }

    /**
     * Print specified text on debugging output stream.
     *
     * @param text A text to be printed on debugging output stream
     */
    private synchronized void p(String text) {
        logger.log(Level.ALL, "HTTP Handler: " + text);
    }
}
