package com.nokia.ci.tas.service;

import java.io.OutputStream;

import java.net.InetAddress;
import java.net.Socket;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Calendar;
import java.util.GregorianCalendar;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.MonitorUtils;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.TestNodeDescription;
import com.nokia.ci.tas.commons.Util;

import com.nokia.ci.tas.commons.message.Message;
import com.nokia.ci.tas.commons.message.ProductOperation;

import com.nokia.ci.tas.commons.statistics.ProductEntry;
import com.nokia.ci.tas.commons.statistics.Statistics;
import com.nokia.ci.tas.commons.statistics.TestEntry;

/**
 * Represents a single Test Node in the Testing Automation Service.
 */
public class TestNode extends Thread implements Comparable<TestNode> {

    /**
     * Constant value indicating that some test cannot be executed by this test node.
     */
    public static final int CANNOT_EXECUTE_TEST = 0;

    /**
     * Constant value indicating that some test can be executed by this test node in the future,
     * but not just right now.
     */
    public static final int CAN_EXECUTE_TEST_IN_THE_FUTURE = 1;

    /**
     * Constant value indicating that some test can be executed by this test node right now.
     */
    public static final int CAN_EXECUTE_TEST_RIGHT_NOW = 2;

    /**
     * Hostname of the test node.
     */
    private String hostname;

    /**
     * Port number used by test node for incoming messages.
     */
    private int port;

    /**
     * Description of the test node.
     */
    private TestNodeDescription description;

    /**
     * A pool of messages to be handled by this node.
     */
    private ConcurrentLinkedQueue<Message> messagePool;

    /**
     * A list of products available on this node.
     */
    private CopyOnWriteArrayList<Product> products;

    /**
     * A list of temporarly disconnected products.
     */
    private CopyOnWriteArrayList<Product> temporarlyDisconnectedProducts;

    /**
     * A list of permanently disconnected products.
     */
    private CopyOnWriteArrayList<Product> permanentlyDisconnectedProducts;

    /**
     * A list of products reserved in manual mode.
     */
    private CopyOnWriteArrayList<Product> manuallyReservedProducts;

    /**
     * Variable which keeps this test node running on the side of Test Automation Service.
     */
    private boolean isRunning = true;

    /**
     * Variable which tells if test node has disconnected.
     */
    private boolean isDisconnected = false;

    /**
     * A moment of time when the test node product list was updated.
     */
    private long lastNotificationTime = 0L;

    /**
     * Local reference to the current Test Automation Service instance.
     */
    private TestAutomationService testAutomationService;

    /**
     * Keeps copies of tests being currently under execution.
     */
    private CopyOnWriteArrayList<Test> runningTests;

    /**
     * Keeps total number of executed tests.
     */
    private long totalNumberOfExecutedTests = 0L;

    /**
     * Keeps total number of failed tests.
     */
    private long totalNumberOfFailedTests = 0L;

    /**
     * Hostname of the Test Automation Service.
     */
    private String testAutomationServiceHostname;

    /**
     * Port number of the Test Automation Service.
     */
    private int testAutomationServicePort;

    /**
     * Date and time format used for timestamps in logging prints.
     */
    private SimpleDateFormat timestampFormat;

    /**
     * Current configuration of the Test Automation Service.
     */
    private Configuration configuration;

    /**
     * Current statistics from the Test Automation Service.
     */
    private Statistics statistics;

    /**
     * Keeps a copy of web-page representing current status of this test node.
     */
    private String currentStatus = "";
    
    /**
     * Keeps a copy of JSON descriptions for all products handled by this test node.
     */
    private String productDescriptions = "";

    /**
     * Keeps the moment of the last status update.
     */
    private long timeOfLastStatusUpdate = 0L;

    /**
     * Tells whenever this test node is in maintenance mode or not.
     */
    private boolean isMaintenanceMode = false;

    /**
     * Instance of the Test Automation Service's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationService.GLOBAL_LOGGER_NAME);

    /**
     * Creates a test node object
     *
     * @param testAutomationService Instance of running Test Automation Service
     * @param hostname Hostname of the test node
     * @param port Port number used by the test node
     */
    public TestNode(TestAutomationService testAutomationService, String hostname, int port) {

        super(hostname + port); // Use hostname and port as a name of the thread

        this.testAutomationService = testAutomationService;
        this.hostname = hostname;
        this.port = port;
        this.testAutomationServiceHostname = testAutomationService.getHostname();
        this.testAutomationServicePort = testAutomationService.getPort();
        this.configuration = testAutomationService.getConfiguration();
        this.statistics = testAutomationService.getStatistics();

        messagePool = new ConcurrentLinkedQueue();

        products = new CopyOnWriteArrayList();
        temporarlyDisconnectedProducts = new CopyOnWriteArrayList();
        permanentlyDisconnectedProducts = new CopyOnWriteArrayList();
        manuallyReservedProducts = new CopyOnWriteArrayList();
        runningTests = new CopyOnWriteArrayList();

        timestampFormat = new SimpleDateFormat(Constant.TIMESTAMP_FORMAT);

        // Creation of a test node means that it is online
        lastNotificationTime = System.currentTimeMillis();

        setPriority(Thread.MIN_PRIORITY); // Always run with minimal priority
    }

    /**
     * Returns a hostname associated with this test node.
     *
     * @return A hostname associated with this test node
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Returns a port number associated with this test node.
     *
     * @return A port number associated with this test node
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets a description about this test node.
     *
     * @param description Description about this test node
     */
    public void setDescription(TestNodeDescription description) {
        if (description != null) {
            this.description = description;
        }
    }

    /**
     * Returns a description about this test node.
     *
     * @return Description about this test node
     */
    public TestNodeDescription getDescription() {
        return description;
    }
    /**
     * Returns a hostname and port associated with this test node
     * in canonical form "hostname:port".
     *
     * @return A hostname and port in canonical form
     */
    public String getHostnameAndPort() {
        return hostname + ":" + port;
    }

    /**
     * Returns a list of products available to this test node.
     *
     * @return A list of products available to this test node
     */
    public synchronized List<Product> getProducts() {
        return products;
    }

    /**
     * Returns current number of products.
     *
     * @return Current number of products.
     */
    public synchronized int getNumberOfProducts() {
        return products.size();
    }

    /**
     * Return a list of temporarly disconnected products associated with this test node.
     *
     * @return A list of temporarly disconnected products associated with this test node
     */
    public synchronized List<Product> getTemporarlyDisconnectedProducts() {
        return temporarlyDisconnectedProducts;
    }

    /**
     * Return a list of permanently disconnected products associated fomr the beginning with this test node.
     *
     * @return A list of permanently disconnected products associated fomr the beginning with this test node
     */
    public synchronized List<Product> getPermanentlyDisconnectedProducts() {
        return permanentlyDisconnectedProducts;
    }

    /**
     * Test node's main routine.
     */
    @Override
    public void run() {
        p("Started working");

        while (isRunning) {
            try {
                if (!messagePool.isEmpty()) {
                    // Try to handle messaging
                    Message message = messagePool.poll(); // Always get the first message

                    if (message != null) {
                        for (int i = 0; i < Constant.NUMBER_OF_RETRIES; i++) {
                            p("Trying to send a message to the test node " + hostname + ":" + port);

                            Socket socket = null;
                            OutputStream output = null;

                            try {
                                socket = new Socket(InetAddress.getByName(hostname), port);
                                output = socket.getOutputStream();

                                // Send a message and close connection immediately
                                output.write(message.toXML().getBytes("UTF-8"));
                                output.flush();

                                output.close();
                                socket.close();

                                // Message was successfully send
                                break; // Stop any other tries

                            } catch (Exception e) {
                                p("Got a problem during sending a message to the test node " + hostname + ":" + port + " - " + e.getClass() + " " + e.getMessage());

                                if (i >= (Constant.NUMBER_OF_RETRIES - 1)) {
                                    // This test node has some serious network problems
                                    p("Has tried to send message for the maximal number of retries (" + Constant.NUMBER_OF_RETRIES + ")");
                                    p("Stopping the test node " + hostname + ":" + port);

                                    isRunning = false;
                                    isDisconnected = true;

                                    break;
                                } else {
                                    p("Will try to re-send message after " + Util.convert(Constant.THIRTY_SECONDS));
                                    sleep(Constant.THIRTY_SECONDS);
                                }
                            } finally {
                                // Always ensure that output stream is closed
                                if (output != null) {
                                    try {
                                        output.close();
                                    } catch (Exception e) {
                                        p("Got troubles during closing outcoming connection to the test node " + hostname + ":" + port
                                            + " - " + e.getClass() + " " + e.getMessage());
                                    }
                                }

                                // Always ensure that connection is closed
                                if (socket != null && !socket.isClosed()) {
                                    try {
                                        socket.close();
                                    } catch (Exception e) {
                                        p("Got troubles during closing outcoming connection to the test node " + hostname + ":" + port
                                            + " - " + e.getClass() + " - " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }

                // Update a list of products on remote side
                if ((System.currentTimeMillis() - lastNotificationTime) > Constant.ONE_MINUTE) {
                    if (!products.isEmpty()) {
                        // Send product updates to the test node
                        for (Product product : products) {
                            Socket socket = null;
                            OutputStream output = null;

                            try {
                                ProductOperation updateProductOperation = new ProductOperation(ProductOperation.Id.UPDATE, product);
                                updateProductOperation.setSender(testAutomationServiceHostname, testAutomationServicePort);
                                updateProductOperation.setReceiver(hostname, port);

                                socket = new Socket(InetAddress.getByName(hostname), port);
                                output = socket.getOutputStream();

                                // Send a message and close connection immediately
                                output.write(updateProductOperation.toXML().getBytes("UTF-8"));
                                output.flush();

                                output.close();
                                socket.close();

                                // If we don't get any exceptions at this point, it means that test node is still online

                            } catch (Exception e) {
                                p("Got a problem during connection check between Test Automation Service and test node "
                                        + getHostnameAndPort() + ": " + e.getClass() + " " + e.getMessage()+",stack="+MonitorUtils.getStack( e ));
                                isDisconnected = true;
                                isRunning = false;

                                break;
                            } finally {
                                // Always ensure that output stream is closed
                                if (output != null) {
                                    try {
                                        output.close();
                                    } catch (Exception e) {
                                        p("Got troubles during closing outcoming connection to the test node " + hostname + ":" + port
                                            + " - " + e.getClass() + " " + e.getMessage());
                                    }
                                }

                                // Always ensure that connection is closed
                                if (socket != null && !socket.isClosed()) {
                                    try {
                                        socket.close();
                                    } catch (Exception e) {
                                        p("Got troubles during closing outcoming connection to the test node " + hostname + ":" + port
                                            + " - " + e.getClass() + " - " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    } else {
                        // Ensure that test node is still alive
                        Socket socket = null;
                        OutputStream output = null;

                        try {
                            socket = new Socket(InetAddress.getByName(hostname), port);
                            output = socket.getOutputStream();

                            // Close connection immediately
                            output.close();
                            socket.close();

                            // If we don't get any exceptions at this point, it means that test node is still online

                        } catch (Exception e) {
                            p("Got a problem during connection check between Test Automation Service and test node "
                                    + getHostnameAndPort() + ": " + e.getClass() + " " + e.getMessage() +",stack="+MonitorUtils.getStack( e ));

                            isDisconnected = true;
                            isRunning = false;
                            break;
                        } finally {
                            // Always ensure that output stream is closed
                            if (output != null) {
                                try {
                                    output.close();
                                } catch (Exception e) {
                                    p("Got troubles during closing outcoming connection to the test node " + hostname + ":" + port
                                        + " - " + e.getClass() + " " + e.getMessage());
                                }
                            }

                            // Always ensure that connection is closed
                            if (socket != null && !socket.isClosed()) {
                                try {
                                    socket.close();
                                } catch (Exception e) {
                                    p("Got troubles during closing outcoming connection to the test node " + hostname + ":" + port
                                        + " - " + e.getClass() + " - " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    // Print all currently available products
                    StringBuffer currentStatus = new StringBuffer();

                    if (products.isEmpty()) {
                        currentStatus.append("Currently hasn't any connected products");
                    } else {
                        currentStatus.append("Currently has " + products.size() + " connected products:");

                        for (Product product : products) {
                            currentStatus.append("\n Has a product of type " + product.getRMCode()
                                + " and with IMEI " + product.getIMEI()
                                + " (SN:'" + product.getSn() + "')"
                                + " in status " + product.getStatus());

                            if (product.getStatus() == Product.Status.BUSY) {
                                currentStatus.append(" - " + product.getStatusDetails());
                                currentStatus.append(", reserved at " + timestampFormat.format(new Date(product.getReservationTime())));
                                currentStatus.append(", for at least " + Util.convert(product.getReservationTimeout()));
                            }
                        }
                    }

                    p(currentStatus.toString());

                    // Check all temporarly disconnected products
                    List<Product> expiredProducts = new ArrayList<Product>(0);

                    currentStatus = new StringBuffer();

                    if (temporarlyDisconnectedProducts.isEmpty()) {
                        currentStatus.append("Currently hasn't any temporarly disconnected products");
                    } else {
                        currentStatus.append("Current temporarly disconnected products:");

                        for (int i = 0; i < temporarlyDisconnectedProducts.size(); i++) {
                            Product product = temporarlyDisconnectedProducts.get(i);

                            currentStatus.append("\n Has a temporarly disconnected product of type " + product.getRMCode()
                                + " and with IMEI " + product.getIMEI()
                                + " (SN:'" + product.getSn() + "')"
                                + " in status " + product.getStatus());

                            if (product.getStatus() == Product.Status.BUSY) {
                                currentStatus.append(" - " + product.getStatusDetails());
                                currentStatus.append(", disconnected at " + timestampFormat.format(new Date(product.getDisconnectionTime())));

                                TestMonitor testMonitor = null;
                                Test productReservedTest = null;

                                for (Test runningTest : runningTests) {
                                    if (runningTest.getRuntimeId().equals(product.getStatusDetails())) {
                                        // Got a running test which is responsible for this product
                                        // Note, test monitor is always associated with test's original id
                                        testMonitor = testAutomationService.getTestMonitor(runningTest.getId());
                                        productReservedTest = runningTest;
                                        break;
                                    }
                                }

                                if (testMonitor != null) {
                                    long productDisconnectionTimeout = productReservedTest.getProductDisconnectionTimeout();
                                    currentStatus.append(", for maximal timeout of " + Util.convert(productDisconnectionTimeout));

                                    if ((System.currentTimeMillis() - product.getDisconnectionTime()) > productDisconnectionTimeout) {
                                        // Got expiration of timeout for a disconnected product
                                        p("Got an expiration of product disconnection timeout during the test '" + productReservedTest.getRuntimeId() + "'");

                                        // Remove this product from the list of temporarly removed devices
                                        expiredProducts.add(product);
                                        String reasonOfTestFailure = "Product of type " + product.getRMCode() + " and with IMEI " + product.getIMEI();
                                        reasonOfTestFailure += " (SN:'" + product.getSn() + "')";
                                        reasonOfTestFailure += " has got expiration of disconnection timeout (" + Util.convert(productDisconnectionTimeout) + ")";

                                        testMonitor.notifyMonitorAboutFailedTest(productReservedTest, reasonOfTestFailure);

                                        // Consider this product as a permanently disconnected one
                                        addToPermanentlyDisconnectedProducts(product);
                                    }
                                } else {
                                    // No monitor for specified test. The product should be set as free immediately
                                    // But right now we are skipping such releasing, just to see who has misbehaved on this test node
                                }
                            }
                        }
                    }

                    p(currentStatus.toString());

                    if (!expiredProducts.isEmpty()) {
                        for (Product expiredProduct : expiredProducts) {
                            int index = hasTemporarlyDisconnectedProductWithIMEI(expiredProduct.getIMEI());

                            if (index != -1) {
                                temporarlyDisconnectedProducts.remove(index);
                                p("A product of type " + expiredProduct.getRMCode()
                                    + " and with IMEI " + expiredProduct.getIMEI()
                                    + " (FUSE connection name '" + expiredProduct.getFuseConnectionName() + "')"
                                    + " was removed from the list of temporarly disconnected products due to disconnection timeout expiration");
                            }
                        }
                    }

                    lastNotificationTime = System.currentTimeMillis();
                }

                // Update status page each 15 seconds
                if ((System.currentTimeMillis() - timeOfLastStatusUpdate) > Constant.FIFTEEN_SECONDS) {
                    timeOfLastStatusUpdate = System.currentTimeMillis();
                    updateCurrentStatus();
                }

                sleep(Constant.MILLISECOND); // Wait for updates
            }
            catch (InterruptedException e) {
                p("Test node was interrupted internally on the side of Test Automation Service. Stop working");
                //e.printStackTrace();

                isDisconnected = true;
                isRunning = false;
                break;
            }
        }

        if (isDisconnected) {
            p("The test node " + getHostnameAndPort() + " is most probably disconnected or has problems with the network. Removing it from the Test Automation Service...");
            // Notify Test Automation Servie about disconnected test node
            testAutomationService.handleDisconnectedTestNode(hostname, port);
        }

        p("Ended work");
    }

    /**
     * Handles specified message.
     *
     * @param message Message to be send
     */
    public synchronized void handle(Message message) {
        if (messagePool.add(message)) {
            //p("Got a message to handle:\n" + message);
        } else {
            p("Error: Couldn't add a message for handling:\n" + message);
        }

        notify();
    }

    /**
     * Returns a list of currently free products, available to this test node.
     * If there will be no free products at all, an empty list will be returned.
     *
     * @return A list of currently free products, available to this test node
     */
    public synchronized List<Product> getFreeProducts() {
        List<Product> freeProducts = new ArrayList<Product>(0);

        for (Product product : products) {
            if (product.isFree()) {
                freeProducts.add(product);
            }
        }

        return freeProducts;
    }

    /**
     * Reserves specified list of products for specified test.
     * All necessary checks will be performed during this reservations,
     * like checking if specified products are truly free and available to this test node.
     * In case of any failure an empty list will be returned.
     * Otherwise a list of successfully reserved products will be returned back.
     *
     * @param test Product reserving test
     * @param selectedProducts A list of products to be reserved
     * @param reservationTimeout Duration of products reservation in milliseconds
     * @return A list of successfully reserved products, or empty list in case of any reservation failure
     */
    public synchronized List<Product> reserveProducts(Test test, List<Product> selectedProducts, long reservationTimeout) {
        boolean success = true;
        long currentTime = System.currentTimeMillis();
        List<Product> reservedProducts = new ArrayList<Product>(0);

        // Reservation timeout couldn't be negative, zero or longer than test's timeout
        if (reservationTimeout <= 0L || reservationTimeout > test.getTimeout()) {
            reservationTimeout = test.getTimeout();
        }

        for (Product selectedProduct : selectedProducts) {
            int index = hasProductWithIMEI(selectedProduct.getIMEI());

            if (index != -1) {
                Product product = products.get(index);

                if (product.isFree()) {
                    product.setStatus(Product.Status.BUSY, test.getRuntimeId());
                    product.setReservation(currentTime, reservationTimeout);

                    products.set(index, product);

                    reservedProducts.add(product);

                    if (test.getProductReleasingMode() == Test.ProductReleasingMode.MANUALLY_RELEASE_RESERVED_PRODUCTS) {
                        // Check if we need to add this product to the list of manually reserved products
                        if (!manuallyReservedProducts.contains(product)) {
                            manuallyReservedProducts.add(product);
                            p("Product with IMEI " + product.getIMEI()
                                + " and of type " + product.getRMCode()
                                + " (SN:'" + product.getSn() + "')"
                                + " was added to the list of manually reserved products up on request from the test '" + test.getRuntimeId() + "'");
                        }
                    }

                    p("Product with IMEI " + product.getIMEI()
                        + " and of type " + product.getRMCode()
                        + " (SN:'" + product.getSn() + "')"
                        + " is now reserved for the test '" + product.getStatusDetails()
                        + "' as " + product.getRole() + " product");

                    // Notify test node about product reservation
                    ProductOperation updateProductOperation = new ProductOperation(ProductOperation.Id.UPDATE, product);
                    updateProductOperation.setSender(product.getTestAutomationServiceHostname(), product.getTestAutomationServicePort());
                    updateProductOperation.setReceiver(hostname, port); // Test node

                    handle(updateProductOperation);
                } else {
                    p("Product with IMEI " + product.getIMEI()
                        + " and of type " + product.getRMCode()
                        + " (SN:'" + product.getSn() + "')"
                        + " wasn't free at reservation. Stoping product reservations");
                    success = false;
                    break;
                }
            }
        }

        if (!success) {
            p("Automatically releasing already reserved products due to occured status mismatch error...");
            // Automatically release reserved products
            freeProducts(reservedProducts);
            // Return an empty list as a failure indication
            return new ArrayList<Product>(0);
        }

        return reservedProducts;
    }

    /**
     * Handles specified operation on a product.
     *
     * @param productOperation Operation on a product
     */
    public synchronized void handleProductOperation(ProductOperation productOperation) {
        Product product = productOperation.getProduct();
        if (product != null) {
            switch (productOperation.getId()) {
                case UPDATE: {
                    updateProduct(product);
                } break;

                case ADD: {
                    addProduct(product);
                } break;

                case REMOVE: {
                    removeProduct(product);
                } break;

                default: {
                    p("Got UNSUPPORTED product operation " + productOperation.getId() + " for product: " + product);
                } break;
            }
        }
    }

    /**
     * Handles update of a product.
     *
     * @param product Product to update
     */
    private synchronized void updateProduct(Product product) {
        //p("Updating a product of type " + product.getRMCode() + " and with IMEI " + product.getIMEI());

        String imei = product.getIMEI();

        if (imei != null && !imei.isEmpty()) {
            int index = hasProductWithIMEI(product.getIMEI());

            if (index != -1) {
                // Update specified product
                Product originalProduct = products.get(index);

                if (originalProduct.getStatus() == Product.Status.BUSY) {
                    // Product is busy, check reservation timeouts
                    if ((System.currentTimeMillis() - originalProduct.getReservationTime()) > originalProduct.getReservationTimeout()) {

                        TestMonitor testMonitor = null;
                        Test productReservingTest = null;

                        for (Test runningTest : runningTests) {
                            if (runningTest.getRuntimeId().equals(product.getStatusDetails())) {
                                // Got a running test which is responsible for this product
                                // Note, test monitor is always associated with test's original id
                                testMonitor = testAutomationService.getTestMonitor(runningTest.getId());
                                productReservingTest = runningTest;
                                break;
                            }
                        }

                        if (testMonitor != null) {
                            String reasonOfTestFailure = "Reserved product with IMEI " + originalProduct.getIMEI()
                                                             + " and of type " + originalProduct.getRMCode()
                                                             + " (SN:'" + product.getSn() + "')"
                                                             + " has got expiration of reservation timeout: "
                                                             + Util.convert(originalProduct.getReservationTimeout());

                            testMonitor.notifyMonitorAboutFailedTest(productReservingTest, reasonOfTestFailure);

                            // Test monitor will release corresponding products
                        } else {
                            originalProduct.setStatus(Product.Status.FREE, "");
                            originalProduct.setReservation(0, configuration.getTestDefaultTimeout());
                            originalProduct.setDisconnectionTime(0L);

                            p("Product with IMEI " + originalProduct.getIMEI()
                                        + " and of type " + originalProduct.getRMCode()
                                        + " (SN:'" + product.getSn() + "')"
                                        + " is set as " + originalProduct.getStatus()
                                        + " due to expiration of reservation timeout");
                        }
                    }
                }

                // Store any changes
                product.setStatus(originalProduct.getStatus());
                product.setStatusDetails(originalProduct.getStatusDetails());
                product.setReservationTime(originalProduct.getReservationTime());
                product.setReservationTimeout(originalProduct.getReservationTimeout());
                product.setDisconnectionTime(originalProduct.getDisconnectionTime());

                products.set(index, product);

                //p("Product of type " + product.getRMCode() + " and with IMEI " + product.getIMEI() + " was successfully updated on this test node");
            } else {
                // Add this product as a new one
                addProduct(product);
            }
        }
    }

    /**
     * Handles addition of a new product.
     *
     * @param product A product to be added
     */
    private synchronized void addProduct(Product product) {
        p("Adding a new product of type " + product.getRMCode()
            + " and with IMEI " + product.getIMEI()
            + " (SN:'" + product.getSn() + "')");

        String imei = product.getIMEI();

        if (imei != null && !imei.isEmpty()) {
            int index = hasProductWithIMEI(product.getIMEI());

            if (index == -1) {
                // Check if specified product was on the list of temporarly disconnected devices
                int disconnectedProductIndex = hasTemporarlyDisconnectedProductWithIMEI(imei);

                if (disconnectedProductIndex != -1) {
                    // We have a re-connected product
                    Product temporarlyDisconnectedProduct = temporarlyDisconnectedProducts.get(disconnectedProductIndex);

                    // Check reservation timeouts
                    if ((System.currentTimeMillis() - temporarlyDisconnectedProduct.getReservationTime()) > temporarlyDisconnectedProduct.getReservationTimeout()) {
                        // Notify responsible test handler about occured timeout expiration
                        TestMonitor testMonitor = null;
                        Test productReservedTest = null;

                        for (Test runningTest : runningTests) {
                            if (runningTest.getRuntimeId().equals(product.getStatusDetails())) {
                                // Got a running test which is responsible for this product
                                // Note, test monitor is always associated with test's original id
                                testMonitor = testAutomationService.getTestMonitor(runningTest.getId());
                                productReservedTest = runningTest;
                                break;
                            }
                        }

                        if (testMonitor != null) {
                            testMonitor.notifyMonitorAboutFailedTest(productReservedTest,
                                                                     "Reserved product with IMEI " + temporarlyDisconnectedProduct.getIMEI()
                                                                         + " and of type " + temporarlyDisconnectedProduct.getRMCode()
                                                                         + " has got expiration of reservation timeout: "
                                                                         + Util.convert(temporarlyDisconnectedProduct.getReservationTimeout()));
                        }

                        // Make this product immediately free to be available for other tests
                        temporarlyDisconnectedProduct.setStatus(Product.Status.FREE, "");
                        temporarlyDisconnectedProduct.setReservation(0, configuration.getTestDefaultTimeout());
                        temporarlyDisconnectedProduct.setDisconnectionTime(0L);

                        p("Product with IMEI " + temporarlyDisconnectedProduct.getIMEI()
                                    + " and of type " + temporarlyDisconnectedProduct.getRMCode()
                                    + " (SN:'" + product.getSn() + "')"
                                    + " is set as " + temporarlyDisconnectedProduct.getStatus()
                                    + " due to expiration of reservation timeout");
                    }

                    // Add as updated and re-connected product
                    temporarlyDisconnectedProduct.setDisconnectionTime(0L);

                    products.add(temporarlyDisconnectedProduct);
                    p("Product of type " + product.getRMCode()
                        + " and with IMEI " + product.getIMEI()
                        + " (SN:'" + product.getSn() + "')"
                        + " was successfully added to this test node");

                    // Remove this product from the list of temporarly disconnected devices
                    temporarlyDisconnectedProducts.remove(disconnectedProductIndex);

                    p("A product of type " + product.getRMCode()
                        + " and with IMEI " + product.getIMEI()
                        + " (SN:'" + product.getSn() + "')"
                        + " was removed from the list of temporarly disconnected products due to its successful re-connection");

                    // Set re-connected product up to date on the side of Test Automation Communicator
                    ProductOperation updateProductOperation = new ProductOperation(ProductOperation.Id.UPDATE, temporarlyDisconnectedProduct);
                    updateProductOperation.setSender(testAutomationServiceHostname, testAutomationServicePort);
                    updateProductOperation.setReceiver(hostname, port); // Test node

                    handle(updateProductOperation);

                } else {
                    // Product wasn't on the list of temporarly disconnected products, add it as a new one
                    product.setStatus(Product.Status.FREE, "");
                    product.setReservation(0L, configuration.getTestDefaultTimeout());
                    product.setDisconnectionTime(0L);

                    products.add(product);
                    p("Product of type " + product.getRMCode()
                        + " and with IMEI " + product.getIMEI()
                        + " (SN:'" + product.getSn() + "')"
                        + " was successfully added to this test node");
                }
            } else {
                p("Product of type " + product.getRMCode()
                    + " and with IMEI " + product.getIMEI()
                    + " (SN:'" + product.getSn() + "')"
                    + " is already added to this test node");
                updateProduct(product);
            }

            // Check also against permanently disconnected products
            removeFromPermanentlyDisconnectedProducts(product);
        }
    }

    /**
     * Handles removal of a product.
     *
     * @param product A product to remove
     */
    private synchronized void removeProduct(Product product) {
        p("Removing a product of type " + product.getRMCode()
            + " and with IMEI " + product.getIMEI()
            + " (SN:'" + product.getSn() + "')");
        String imei = product.getIMEI();

        if (imei != null && !imei.isEmpty()) {
            int index = hasProductWithIMEI(product.getIMEI());

            if (index != -1) {
                // Check if product is busy or not
                Product originalProduct = products.get(index);

                if (originalProduct.getStatus() == Product.Status.BUSY) {
                    // We have a reserved product
                    // The reserved products are allowed to be disconnected only for some time
                    // If a reserved product will not get back online after some time, the test will be failed

                    // Check if specified product was on the list of temporarly disconnected devices
                    int disconnectedProductIndex = hasTemporarlyDisconnectedProductWithIMEI(imei);

                    if (disconnectedProductIndex == -1) {
                        // Check if disconnection time was specified
                        if (product.getDisconnectionTime() > 0L) {
                            originalProduct.setDisconnectionTime(product.getDisconnectionTime());
                        } else {
                            originalProduct.setDisconnectionTime(System.currentTimeMillis());
                        }

                        // Add this product to the list of temporarly disconnected devices
                        temporarlyDisconnectedProducts.add(originalProduct);

                        p("A product of type " + product.getRMCode()
                            + " and with IMEI " + product.getIMEI()
                            + " (SN:'" + product.getSn() + "')"
                            + " was added to the list of temporarly disconnected products due to its disconnection being busy");
                    }
                } else {
                    // Check against permanently disconnected products
                    addToPermanentlyDisconnectedProducts(product);
                }

                // Remove specified product
                products.remove(index);

                p("Product of type " + product.getRMCode()
                    + " and with IMEI " + product.getIMEI()
                    + " (SN:'" + product.getSn() + "')"
                    + " was successfully removed from this test node");
            } else {
                p("Product of type " + product.getRMCode()
                    + " and with IMEI " + product.getIMEI()
                    + " (SN:'" + product.getSn() + "')"
                    + " is already not presented on this test node");
            }
        }
    }

    /**
     * Returns an index of the product with specified IMEI code or -1 if such product is not presented.
     *
     * @param imei IMEI code to be searched for
     * @return An index of the product with specified IMEI code or -1 if such product is not presented
     */
    protected int hasProductWithIMEI(String imei) {
        int index = -1;

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            if (product.getIMEI().equals(imei)) {
                index = i;
                break;
            }
        }

        return index;
    }

    /**
     * Returns an index of temporarly disconnected product with specified IMEI code or -1 if such product is not existing.
     *
     * @param imei IMEI code to be searched for
     * @return An index of the temporarly disconnected product with specified IMEI code or -1 if such product is not existing
     */
    protected int hasTemporarlyDisconnectedProductWithIMEI(String imei) {
        int index = -1;

        for (int i = 0; i < temporarlyDisconnectedProducts.size(); i++) {
            Product product = temporarlyDisconnectedProducts.get(i);

            if (product.getIMEI().equals(imei)) {
                index = i;
                break;
            }
        }

        return index;
    }

    /**
     * Returns true if a product with specified IMEI number is on the lists of currently available,
     * temporarly or permanently disconnected products or false otherwise.
     *
     * @param imei IMEI number to be checked
     * @return True if a product with specified IMEI number is on the lists of currently available,
     * temporarly or permanently disconnected products or false otherwise
     */
    protected boolean holdsProduct(String imei) {

        if (imei != null && !imei.isEmpty()) {
            int index = hasProductWithIMEI(imei);

            if (index != -1) {
                return true;
            }

            index = hasTemporarlyDisconnectedProductWithIMEI(imei);

            if (index != -1) {
                return true;
            }

            // Try to search among permanently disconnected devices
            for (int i = 0; i < permanentlyDisconnectedProducts.size(); i++) {
                Product product = permanentlyDisconnectedProducts.get(i);

                if (product.getIMEI().equals(imei)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns a number of tests that node is executing.
     *
     * @return A number of tests that node is executing
     */
    public synchronized int getNumberOfRunningTests() {
        return runningTests.size();
    }

    /**
     * Increases number of currently running tests.
     */
    public synchronized void increaseNumberOfRunningTests(Test test) {
        boolean canAdd = true;

        for (Test runningTest : runningTests) {
            if (runningTest.getRuntimeId().equals(test.getRuntimeId())) {
                canAdd = false;
                break;
            }
        }

        if (canAdd) {
            p("Adding test '" + test.getRuntimeId() + "' to the list of running tests");
            runningTests.add(test);
        } else {
            p("Cannot add test '" + test.getRuntimeId() + "' to the list of running tests");
        }
    }

    /**
     * Decreases number of currently running tests.
     *
     * @param testHasFailed True when a test on this node has failed due to some technical issues
     */
    public synchronized void decreaseNumberOfRunningTests(Test test, boolean testHasFailed) {

        // One more test is over
        totalNumberOfExecutedTests++;

        if (testHasFailed) {
            totalNumberOfFailedTests++;
        }

        for (Test runningTest : runningTests) {
            if (runningTest.getRuntimeId().equals(test.getRuntimeId())) {
                if (runningTests.remove(runningTest)) {
                    p("Removed test '" + test.getRuntimeId() + "' from the list of running tests");
                } else {
                    p("Couldn't remove test '" + test.getRuntimeId() + "' from the list of running tests");
                }
                break;
            }
        }
    }

    /**
     * Returns total number of tests executed on this test node.
     *
     * @return Total number of tests executed on this test node
     */
    public synchronized long getTotalNumberOfExecutedTests() {
        return totalNumberOfExecutedTests;
    }

    /**
     * Returns total number of tests failed on this test node.
     *
     * @return Total number of tests failed on this test node
     */
    public synchronized long getTotalNumberOfFailedTests() {
        return totalNumberOfFailedTests;
    }

    /**
     * Set specified products as a freely available ones.
     *
     * @param releasedProducts A list of products to be set as a freely available ones
     */
    public void freeProducts(List<Product> releasedProducts) {
        if (releasedProducts != null && !releasedProducts.isEmpty()) {
            for (Product releasedProduct : releasedProducts) {
                freeProduct(releasedProduct);
            }
        }
    }

    /**
     * Releases specified product.
     *
     * @param product A product to be released
     */
    public void freeProduct(Product product) {
        String imei = product.getIMEI();

        if (imei != null && !imei.isEmpty()) {
            // Try to find this product from the list of available devices
            int index = hasProductWithIMEI(imei);

            if (index != -1) {
                Product originalProduct = products.get(index);

                originalProduct.setStatus(Product.Status.FREE, "");
                originalProduct.setReservation(0, configuration.getTestDefaultTimeout());
                originalProduct.setDisconnectionTime(0L);

                products.set(index, originalProduct);

                p("Product with IMEI " + originalProduct.getIMEI()
                            + " and of type " + originalProduct.getRMCode()
                            + " (SN:'" + product.getSn() + "')"
                            + " is set as " + originalProduct.getStatus());

                // Notify test node about changes
                ProductOperation freeProductOperation = new ProductOperation(ProductOperation.Id.UPDATE, originalProduct);
                freeProductOperation.setSender(originalProduct.getTestAutomationServiceHostname(), originalProduct.getTestAutomationServicePort());
                freeProductOperation.setReceiver(hostname, port); // Test node
                handle(freeProductOperation);

            } else {
                // Otherwise try to find this product from the list of temporarly disconnected devices
                int temporarlyDisconnectedProductIndex = hasTemporarlyDisconnectedProductWithIMEI(imei);

                if (temporarlyDisconnectedProductIndex != -1) {
                    // Simply remove this product from the list of temporarly disconnected devices,
                    // since no one is interested in tracing this product anymore

                    temporarlyDisconnectedProducts.remove(temporarlyDisconnectedProductIndex);

                    p("Product with IMEI " + product.getIMEI()
                            + " and of type " + product.getRMCode()
                            + " (SN:'" + product.getSn() + "')"
                            + " was on the list of temporarly disconnected products and was successfully removed from it due to releasing request");
                }
            }

            // Try to free also the manually reserved products

            for (Product manuallyReservedProduct : manuallyReservedProducts) {
                if (manuallyReservedProduct.getIMEI().equals(imei)) {
                    if (manuallyReservedProducts.remove(manuallyReservedProduct)) {
                        p("Product with IMEI " + manuallyReservedProduct.getIMEI()
                            + " and of type " + manuallyReservedProduct.getRMCode()
                            + " (SN:'" + product.getSn() + "')"
                            + " is removed from the list of manually reserved products up on request to set free this product");
                    } else {
                        p("Error: Couldn't remove a product with IMEI " + manuallyReservedProduct.getIMEI()
                            + " and of type " + manuallyReservedProduct.getRMCode()
                            + " (SN:'" + product.getSn() + "')"
                            + " from the list of manually reserved products up on request to set free this product");
                    }
                    break;
                }
            }
        }
    }

    /**
     * Adds specified product to the list of permanently disconnected products.
     *
     * @param product A permanently disconnected product
     */
    private synchronized void addToPermanentlyDisconnectedProducts(Product product) {
        if (product != null && !product.getIMEI().isEmpty()) {
            // Check if this product was earlier permanently disconnected
            boolean canAdd = true;

            for (Product permanentlyDisconnectedProduct : permanentlyDisconnectedProducts) {
                if (permanentlyDisconnectedProduct.getIMEI().equals(product.getIMEI())) {
                    canAdd = false;
                    break;
                }
            }

            if (canAdd) {
                permanentlyDisconnectedProducts.add(product);
                
                StringBuffer message = new StringBuffer();

                message.append("Warning! Product with IMEI ");
                message.append(product.getIMEI());
                message.append(" and of type ");
                message.append(product.getRMCode());
                message.append(" (FUSE connection name '");
                message.append(product.getFuseConnectionName());
                message.append("') in status ");
                
                Product.Status productStatus = product.getStatus();

                if (product.isFree()) {
                    message.append(productStatus);
                } else if (productStatus == Product.Status.BUSY) {
                    message.append(productStatus);
                    message.append(" - ");
                    message.append(product.getStatusDetails());
                } else if (productStatus == Product.Status.DISABLED) {
                    message.append(productStatus);
                    message.append(" - ");
                    message.append(product.getStatusDetails());
                } else {
                    message.append(productStatus);
                }
                
                message.append(" has permanently disconnected from the test node " + hostname + ":" + port);
                
                if (description != null) {
                    String testNodeDescription = description.getDescription();

                    if (testNodeDescription != null && !testNodeDescription.isEmpty()) {
                        message.append(" (\"" + testNodeDescription  + "\")");
                    }
                }
                
                message.append(" and Test Automation Service " + testAutomationServiceHostname + ":" + testAutomationServicePort);

                message.append(" Disconnection occured at ");
                
                if (product.getDisconnectionTime() > 0L) {
                    message.append(timestampFormat.format(new Date(product.getDisconnectionTime())));
                } else {
                    message.append("about ");
                    message.append(timestampFormat.format(new Date(System.currentTimeMillis())));
                }

                p(message.toString());
                testAutomationService.createMessage("Product_Disconn_"+product.getFuseConnectionName(),message.toString());

                // Notify Test Automation Service supervisers about a test node without any connected products
                if (products.isEmpty()) {
                    message = new StringBuffer();
                    message.append("Emergency! Test node " + hostname + ":" + port);

                    if (description != null) {
                        String testNodeDescription = description.getDescription();
                        
                        if (testNodeDescription != null && !testNodeDescription.isEmpty()) {
                            message.append(" (\"" + testNodeDescription  + "\")");
                        }
                    }

                    message.append(" running in the Test Automation Service " + testAutomationServiceHostname + ":" + testAutomationServicePort);
                    message.append(" has lost all of its connected products!");

                    p(message.toString());
                    testAutomationService.createMessage("NoProduct_"+hostname,message.toString());
                }
            }
        }
    }

    /**
     * Removes specified product from the list of permanently disconnected products.
     *
     * @param product A product to be removed from the list of permanently disconnected products
     */
    private synchronized void removeFromPermanentlyDisconnectedProducts(Product product) {
        if (product != null && !product.getIMEI().isEmpty()) {
            // Check if this product was earlier permanently disconnected
            for (Product permanentlyDisconnectedProduct : permanentlyDisconnectedProducts) {
                if (permanentlyDisconnectedProduct.getIMEI().equals(product.getIMEI())) {
                    if (permanentlyDisconnectedProducts.remove(permanentlyDisconnectedProduct)) {
                        p("A product of type " + product.getRMCode()
                            + " and with IMEI " + product.getIMEI()
                            + " (SN:'" + product.getSn() + "')"
                            + " was removed from the list of permanently disconnected products");
                    }
                    break;
                }
            }
        }
    }

    /**
     * Shutdowns this test node.
     */
    protected synchronized void shutdown() {
        p("Got a request to shutdown. Stop working...");
        isRunning = false;
    }

    /**
     * Returns current status of this test node in textual form.
     *
     * @return Current status of this test node in textual form
     */
    protected String getCurrentStatus() {
        return currentStatus;
    }
    
    /**
     * Returns JSON descriptions of all products handled by this test node.
     *
     * @return JSON descriptions of all products handled by this instance of test node
     */
    protected String getProductDescriptions() {
        return productDescriptions;
    }

    /**
     * Returns true when this node is in the maintenance mode and false otherwise.
     * 
     * @return Returns true when this node is in the maintenance mode and false otherwise
     */
    protected boolean isMaintenanceMode() {
        return isMaintenanceMode;
    }

    /**
     * Sets maintenance mode on or off.
     * 
     * @param isMaintenanceMode Maintenance mode
     */
    protected void setMaintenanceMode(boolean isMaintenanceMode) {
        this.isMaintenanceMode = isMaintenanceMode;
        updateCurrentStatus();
    }

    /**
     * Updates current status of this test node.
     */
    protected void updateCurrentStatus() {
        try {
            StringBuffer status = new StringBuffer();
            StringBuffer productDescriptionsInJSON = new StringBuffer();

            status.append("\n\n<tr><td>\n<blockquote>\n<b>");

            if (isMaintenanceMode) {
                status.append("<font color=\"#ee0000\">");
            }

            status.append("<a href=\""
                    + HttpHandler.getTestNodeURI(description.getHostname(), description.getPort())
                    + "\">" + description.getHostname() + ":" + description.getPort() + "</a></b>");

            if (!description.getTestAutomationSoftwareVersion().isEmpty()) {
                status.append(" v" + description.getTestAutomationSoftwareVersion());
            }

            if (isMaintenanceMode) {
                status.append(" in maintenance mode");
            }

            if (description != null && !description.getDescription().isEmpty()) {
                status.append(" - " + description.getDescription());
            }

            status.append(" - " + totalNumberOfExecutedTests + " tests were executed so far");
            status.append(" with " + totalNumberOfFailedTests + " tests failed due to some technical problems.");
            status.append(" Current load is " + runningTests.size() + "/" + configuration.getMaximalNumberOfTestsPerNode());

            if (isMaintenanceMode) {
                status.append("</font>");
            }

            status.append("\n</blockquote>\n</td></tr>\n\n");

            // Tell what tests are currently under execition
            if (!runningTests.isEmpty()) {
                status.append("<tr><td>\n<blockquote>\n");

                if (runningTests.size() > 1) {
                    status.append("Currently is executing " + runningTests.size() + " tests:\n<br/>");
                } else {
                    status.append("Currently is executing " + runningTests.size() + " test:\n<br/>");
                }

                status.append("\n<blockquote>\n");

                for (Test runningTest : runningTests) {
                    String target = "Unknown";
                    if (runningTest.getTarget() == Test.Target.FLASH) {
                        target = Test.TARGET_FLASH;
                    } else if (runningTest.getTarget() == Test.Target.NOSE) {
                        target = Test.TARGET_NOSE;
                    }
                    if (!runningTest.getURL().isEmpty()) {
                        status.append("<a href=\"" + runningTest.getURL() + "\" target=_blank>" + runningTest.getRuntimeId() + "</a> - Target: " + target + "\n<br/>\n");
                    } else {
                        status.append(runningTest.getRuntimeId() + " - Target: " + target + "\n<br/>\n");
                    }
                }

                status.append("\n</blockquote>\n");
                status.append("\n</blockquote>\n\n</td></tr>\n");
            }

            // Show what products were permanently disconnected
            if (!permanentlyDisconnectedProducts.isEmpty()) {
                status.append("<tr><td>\n<blockquote>\n");

                for (Product currentProduct : permanentlyDisconnectedProducts) {
                    if (currentProduct.getRole() == Product.Role.REFERENCE) {
                        status.append("Has got a permanently disconnected reference product");
                    } else {
                        status.append("Has got a permanently disconnected product");
                    }

                    status.append(" with IMEI <a href=\""
                            + HttpHandler.getProductURI(hostname, port, currentProduct.getIMEI())
                            + "\">" + currentProduct.getIMEI() + "</a>");

                    status.append(" and of type " + currentProduct.getRMCode());

                    if (!currentProduct.getFuseConnectionName().isEmpty()) {
                        status.append(" (FUSE connection name was '" + currentProduct.getFuseConnectionName() + "')");
                    }

                    status.append(" in status");

                    Product.Status productStatus = currentProduct.getStatus();

                    if (currentProduct.isFree()) {
                        status.append(" <font color=\"#006600\">" + productStatus + "</font>");
                    } else if (productStatus == Product.Status.BUSY) {
                        status.append(" <font color=\"#ee0000\">" + productStatus + "</font>");
                        status.append(" - <b>" + currentProduct.getStatusDetails() + "</b>");
                        status.append(", disconnection occured at " + timestampFormat.format(new Date(currentProduct.getDisconnectionTime())));
                    } else if (productStatus == Product.Status.DISABLED) {
                        status.append(" <font color=\"#ff8800\">" + productStatus + "</font>");
                        status.append(" - <b>" + currentProduct.getStatusDetails() + "</b>");
                    } else {
                        status.append(" <font color=\"#000000\">" + productStatus + "</font>");
                    }

                    status.append("<br/>\n");

                    productDescriptionsInJSON.append(currentProduct.toJSON() + ",\n");
                }

                status.append("\n</blockquote>\n\n</td></tr>\n");
            }

            // Show what products were temporarly disconnected
            if (!temporarlyDisconnectedProducts.isEmpty()) {
                status.append("<tr><td>\n<blockquote>\n");

                for (Product currentProduct : temporarlyDisconnectedProducts) {
                    status.append("<i>");
                    if (currentProduct.getRole() == Product.Role.REFERENCE) {
                        status.append("Has temporarly disconnected reference product");
                    } else {
                        status.append("Has temporarly disconnected product");
                    }

                    status.append(" with IMEI <a href=\""
                            + HttpHandler.getProductURI(hostname, port, currentProduct.getIMEI())
                            + "\">" + currentProduct.getIMEI() + "</a>");

                    status.append(" and of type " + currentProduct.getRMCode());
                    if (!currentProduct.getFuseConnectionName().isEmpty()) {
                        status.append(" (FUSE connection name was '" + currentProduct.getFuseConnectionName() + "')");
                    }
                    status.append(" in status");

                    Product.Status productStatus = currentProduct.getStatus();

                    if (currentProduct.isFree()) {
                        status.append(" <font color=\"#006600\">" + productStatus + "</font>");
                    } else if (productStatus == Product.Status.BUSY) {
                        status.append(" <font color=\"#ee0000\">" + productStatus + "</font>");
                        status.append(" - <b>" + currentProduct.getStatusDetails() + "</b>");
                        status.append(", disconnection occured at " + timestampFormat.format(new Date(currentProduct.getDisconnectionTime())));

                    } else if (productStatus == Product.Status.DISABLED) {
                        status.append(" <font color=\"#ff8800\">" + productStatus + "</font>");
                        status.append(" - <b>" + currentProduct.getStatusDetails() + "</b>");
                    } else {
                        status.append(" <font color=\"#000000\">" + productStatus + "</font>");
                    }

                    status.append("</i><br/>\n");

                    productDescriptionsInJSON.append(currentProduct.toJSON() + ",\n");
                }

                status.append("\n</blockquote>\n\n</td></tr>\n");
            }

            // Show what products are currently available
            if (products.isEmpty()) {
                status.append("<tr><td><blockquote>Hasn't any connected products</blockquote></td></tr>\n\n");
            } else {
                status.append("<tr><td>\n<blockquote>\n");

                for (Product currentProduct : products) {
                    if (currentProduct.getRole() == Product.Role.REFERENCE) {
                        status.append("Has reference product");
                    } else {
                        status.append("Has product");
                    }

                    status.append(" with IMEI <a href=\""
                            + HttpHandler.getProductURI(hostname, port, currentProduct.getIMEI())
                            + "\">" + currentProduct.getIMEI() + "</a>");
                    status.append(" and of type " + currentProduct.getRMCode());

                    if (!currentProduct.getFuseConnectionName().isEmpty()) {
                        status.append(" ('" + currentProduct.getFuseConnectionName() + "')");
                    }

                    status.append(" in status");

                    Product.Status productStatus = currentProduct.getStatus();

                    if (currentProduct.isFree()) {
                        status.append(" <font color=\"#006600\">" + productStatus + "</font>");
                    } else if (productStatus == Product.Status.BUSY) {

                        status.append(" <font color=\"#ee0000\">" + productStatus + "</font>");
                        status.append(" - <b>" + currentProduct.getStatusDetails() + "</b>");
                        status.append(", reserved at " + timestampFormat.format(new Date(currentProduct.getReservationTime())));
                        status.append(", reservation timeout is " + Util.convert(currentProduct.getReservationTimeout()));

                    } else if (productStatus == Product.Status.DISABLED) {
                        status.append(" <font color=\"#ff8800\">" + productStatus + "</font>");
                        status.append(" - <b>" + currentProduct.getStatusDetails() + "</b>");
                    } else {
                        status.append(" <font color=\"#000000\">" + productStatus + "</font>");
                    }

                    status.append("<br/>\n");

                    productDescriptionsInJSON.append(currentProduct.toJSON() + ",\n");
                }

                status.append("\n</blockquote>\n\n</td></tr>");
            }

            currentStatus = status.toString();

            // Remove the trailing comma
            String jsonArrayOfProductDescriptions = productDescriptionsInJSON.toString().trim();
            if (!jsonArrayOfProductDescriptions.isEmpty()) {
                if (jsonArrayOfProductDescriptions.endsWith(",")) {
                    jsonArrayOfProductDescriptions = jsonArrayOfProductDescriptions.substring(0, jsonArrayOfProductDescriptions.lastIndexOf(","));
                }
            }

            productDescriptions = jsonArrayOfProductDescriptions;

        } catch (Exception e) {
            p("Got troubles with updating current status: " + e.getClass() + " - " + e.getMessage() + " - " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Returns detailed status of the whole test node in HTML form.
     *
     * @return Detailed status of the whole test node in HTML form
     */
    protected StringBuffer getDetailedStatus() {
        StringBuffer currentStatus = new StringBuffer();

        try {
            currentStatus.append("<!DOCTYPE html>\n<html>\n<head>\n<title>");
            currentStatus.append("Test node " + hostname + ":" + port);
            currentStatus.append("</title>\n");
            currentStatus.append("<link rel=\"shortcut icon\" href=\"http://wikis.in.nokia.com/pub/CI20Development/TASGuide/favicon.ico\" type=\"image/x-icon\" />\n");
            currentStatus.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
            currentStatus.append("</head>\n");
            currentStatus.append("<body>\n<table border=\"0\">\n\n");

            // Print out basic information about this Test Automation Service
            if (isMaintenanceMode) {
                currentStatus.append("<tr><td><font color=\"#ee0000\">Test Node " + hostname + ":" + port);
                currentStatus.append(" in maintenance mode");
            } else {
                currentStatus.append("<tr><td>Test Node " + hostname + ":" + port);
            }

            if (description != null) {
                if (!description.getTestAutomationSoftwareVersion().isEmpty()) {
                    currentStatus.append(" v" + description.getTestAutomationSoftwareVersion());
                }

                if (!description.getDescription().isEmpty()) {
                    currentStatus.append(" - " + description.getDescription());
                }
            }

            currentStatus.append(", status at " + timestampFormat.format(new Date(System.currentTimeMillis())));

            if (isMaintenanceMode) {
                currentStatus.append("</font>");
            }

            currentStatus.append("</td></tr>\n\n<tr><td>&nbsp;</td></tr>\n\n");

            currentStatus.append("<tr><td>\n<blockquote>\n");
            currentStatus.append(totalNumberOfExecutedTests + " tests were executed so far");
            currentStatus.append(" with " + totalNumberOfFailedTests + " tests failed due to some technical problems.");

            if (configuration != null) {
                currentStatus.append(" Current load is " + runningTests.size() + "/" + configuration.getMaximalNumberOfTestsPerNode());
            } else {
                currentStatus.append(" Current load is " + runningTests.size());
            }

            currentStatus.append("\n</blockquote>\n</td></tr>\n\n");

            // Tell what tests are currently under execition
            if (!runningTests.isEmpty()) {
                currentStatus.append("<tr><td>\n<blockquote>\n");

                if (runningTests.size() > 1) {
                    currentStatus.append("Currently is executing " + runningTests.size() + " tests:\n<br/>");
                } else {
                    currentStatus.append("Currently is executing " + runningTests.size() + " test:\n<br/>");
                }

                currentStatus.append("\n<blockquote>\n");

                for (Test runningTest : runningTests) {
                    String target = "Unknown";
                    if (runningTest.getTarget() == Test.Target.FLASH) {
                        target = Test.TARGET_FLASH;
                    } else if (runningTest.getTarget() == Test.Target.NOSE) {
                        target = Test.TARGET_NOSE;
                    }
                    if (!runningTest.getURL().isEmpty()) {
                        currentStatus.append("<a href=\"" + runningTest.getURL() + "\" target=_blank>" + runningTest.getRuntimeId() + "</a> - Target: " + target + "\n<br/>\n");
                    } else {
                        currentStatus.append(runningTest.getRuntimeId() + " - Target: " + target + "\n<br/>\n");
                    }
                }

                currentStatus.append("\n</blockquote>\n");
                currentStatus.append("\n</blockquote>\n\n</td></tr>\n");
            }

            // Show what products were permanently disconnected
            if (!permanentlyDisconnectedProducts.isEmpty()) {
                currentStatus.append("<tr><td>\n<blockquote>\n");

                for (Product currentProduct : permanentlyDisconnectedProducts) {
                    if (currentProduct.getRole() == Product.Role.REFERENCE) {
                        currentStatus.append("Has got a permanently disconnected reference product");
                    } else {
                        currentStatus.append("Has got a permanently disconnected product");
                    }

                    currentStatus.append(" with IMEI <a href=\""
                            + HttpHandler.getProductURI(hostname, port, currentProduct.getIMEI())
                            + "\">" + currentProduct.getIMEI() + "</a>");

                    currentStatus.append(" and of type " + currentProduct.getRMCode());

                    if (!currentProduct.getFuseConnectionName().isEmpty()) {
                        currentStatus.append(" (FUSE connection name was '" + currentProduct.getFuseConnectionName() + "')");
                    }

                    currentStatus.append(" in status");

                    Product.Status productStatus = currentProduct.getStatus();

                    if (currentProduct.isFree()) {
                        currentStatus.append(" <font color=\"#006600\">" + productStatus + "</font>");
                    } else if (productStatus == Product.Status.BUSY) {
                        currentStatus.append(" <font color=\"#ee0000\">" + productStatus + "</font>");
                        currentStatus.append(" - <b>" + currentProduct.getStatusDetails() + "</b>");
                        currentStatus.append(", disconnection occured at " + timestampFormat.format(new Date(currentProduct.getDisconnectionTime())));
                    } else if (productStatus == Product.Status.DISABLED) {
                        currentStatus.append(" <font color=\"#ff8800\">" + productStatus + "</font>");
                        currentStatus.append(" - <b>" + currentProduct.getStatusDetails() + "</b>");
                    } else {
                        currentStatus.append(" <font color=\"#000000\">" + productStatus + "</font>");
                    }

                    currentStatus.append("<br/>\n");
                }

                currentStatus.append("\n</blockquote>\n\n</td></tr>\n");
            }

            // Show what products were temporarly disconnected
            if (!temporarlyDisconnectedProducts.isEmpty()) {
                currentStatus.append("<tr><td>\n<blockquote>\n");

                for (Product currentProduct : temporarlyDisconnectedProducts) {
                    currentStatus.append("<i>");
                    if (currentProduct.getRole() == Product.Role.REFERENCE) {
                        currentStatus.append("Has temporarly disconnected reference product");
                    } else {
                        currentStatus.append("Has temporarly disconnected product");
                    }

                    currentStatus.append(" with IMEI <a href=\"" + HttpHandler.getProductURI(hostname, port, currentProduct.getIMEI()) + "\">" + currentProduct.getIMEI() + "</a>");
                    currentStatus.append(" and of type " + currentProduct.getRMCode());
                    if (!currentProduct.getFuseConnectionName().isEmpty()) {
                        currentStatus.append(" (FUSE connection name was '" + currentProduct.getFuseConnectionName() + "')");
                    }
                    currentStatus.append(" in status");

                    Product.Status status = currentProduct.getStatus();

                    if (currentProduct.isFree()) {
                        currentStatus.append(" <font color=\"#006600\">" + status + "</font>");
                    } else if (status == Product.Status.BUSY) {
                        currentStatus.append(" <font color=\"#ee0000\">" + status + "</font>");
                        currentStatus.append(" - <b>" + currentProduct.getStatusDetails() + "</b>");
                        currentStatus.append(", disconnection occured at " + timestampFormat.format(new Date(currentProduct.getDisconnectionTime())));

                    } else if (status == Product.Status.DISABLED) {
                        currentStatus.append(" <font color=\"#ff8800\">" + status + "</font>");
                        currentStatus.append(" - <b>" + currentProduct.getStatusDetails() + "</b>");
                    } else {
                        currentStatus.append(" <font color=\"#000000\">" + status + "</font>");
                    }

                    currentStatus.append("</i><br/>\n");
                }

                currentStatus.append("\n</blockquote>\n\n</td></tr>\n");
            }

            // Show what products are currently available
            if (products.isEmpty()) {
                currentStatus.append("<tr><td>\n<blockquote>\nHasn't any connected products\n</blockquote>\n</td></tr>\n\n");
            } else {
                currentStatus.append("<tr><td>\n<blockquote>\n");

                for (Product currentProduct : products) {
                    if (currentProduct.getRole() == Product.Role.REFERENCE) {
                        currentStatus.append("Has reference product");
                    } else {
                        currentStatus.append("Has product");
                    }

                    currentStatus.append(" with IMEI <a href=\"" + HttpHandler.getProductURI(hostname, port, currentProduct.getIMEI()) + "\">" + currentProduct.getIMEI() + "</a>");

                    currentStatus.append(" and of type " + currentProduct.getRMCode());

                    if (!currentProduct.getFuseConnectionName().isEmpty()) {
                        currentStatus.append(" ('" + currentProduct.getFuseConnectionName() + "')");
                    }

                    currentStatus.append(" in status");

                    Product.Status status = currentProduct.getStatus();

                    if (currentProduct.isFree()) {
                        currentStatus.append(" <font color=\"#006600\">" + status + "</font>");
                    } else if (status == Product.Status.BUSY) {
                        TestMonitor testMonitor = testAutomationService.getTestMonitor(currentProduct.getStatusDetails());

                        if (testMonitor != null) {
                            Test test = testMonitor.getTest();

                            currentStatus.append(" <font color=\"#ee0000\">" + status + "</font>");

                            if (!test.getURL().isEmpty()) {
                                currentStatus.append(" - <a href=\"" + test.getURL() + "\" target=\"_blank\">" + test.getRuntimeId() + "</a>");
                            } else {
                                currentStatus.append(" - <b>" + test.getRuntimeId() + "</b>");
                            }

                            currentStatus.append(", reserved at " + timestampFormat.format(new Date(currentProduct.getReservationTime())));

                            currentStatus.append(", reservation timeout is " + Util.convert(currentProduct.getReservationTimeout()));
                        } else {
                            // Just show that it is busy
                            currentStatus.append(" <font color=\"#ee0000\">" + status + "</font>");
                            currentStatus.append(" - <b>" + currentProduct.getStatusDetails()  + "</b>");
                            currentStatus.append(", reserved at " + timestampFormat.format(new Date(currentProduct.getReservationTime())));
                            currentStatus.append(", reservation timeout is " + Util.convert(currentProduct.getReservationTimeout()));

                        }
                    } else if (status == Product.Status.DISABLED) {
                        currentStatus.append(" <font color=\"#ff8800\">" + status + "</font>");
                        currentStatus.append(" - <b>" + currentProduct.getStatusDetails() + "</b>");
                    } else {
                        currentStatus.append(" <font color=\"#000000\">" + status + "</font>");
                    }

                    currentStatus.append("<br/>\n");
                }

                currentStatus.append("\n</blockquote>\n\n</td></tr>\n");
            }

            currentStatus.append("<tr><td>&nbsp;</td></tr>\n\n");

            // Show actions supported by this test node
            currentStatus.append("<tr><td>&nbsp;</td></tr>\n\n");
            currentStatus.append("<tr><td>Actions:</td></tr>\n\n");
            currentStatus.append("<tr><td>&nbsp;</td></tr>\n\n");
            currentStatus.append("<tr><td><blockquote>\n\n");

            if (isMaintenanceMode) {
                // Enable turning off the maintenance mode
                currentStatus.append("<a href=\"");
                currentStatus.append(HttpHandler.getTestNodeActionURI(HttpHandler.ACTION_TURN_MAINTENANCE_MODE_OFF, description.getHostname(), description.getPort()));
                currentStatus.append("\">Turn maintenance mode off</a><br/>\n");
            } else {
                // Enable turning on the maintenance mode
                currentStatus.append("<a href=\"");
                currentStatus.append(HttpHandler.getTestNodeActionURI(HttpHandler.ACTION_TURN_MAINTENANCE_MODE_ON, description.getHostname(), description.getPort()));
                currentStatus.append("\">Turn maintenance mode on</a><br/>\n");
            }

            currentStatus.append("</blockquote></td></tr>\n\n");
            currentStatus.append("<tr><td>&nbsp;</td></tr>\n\n");

            // Show daily workloads
            currentStatus.append("<tr><td>Daily workloads:</td></tr>\n\n");

            currentStatus.append("<tr><td>&nbsp;</td></tr>\n\n");

            long currentTime = System.currentTimeMillis();

            // Discover daily workloads for default number of days
            int workloadHistoryPeriodInDays = 7;

            if (configuration != null) {
                workloadHistoryPeriodInDays = (int) configuration.getWorkloadHistoryPeriod();
            }

            // Prevent negative values
            if (workloadHistoryPeriodInDays < 0) {
                workloadHistoryPeriodInDays = -workloadHistoryPeriodInDays;
            }

            // Convert workload history days into milliseconds
            long workloadHistoryStartTime = currentTime - (workloadHistoryPeriodInDays * Constant.ONE_DAY);

            // Get test entries for the whole period of workload history
            List<TestEntry> testEntries = statistics.getTestEntriesForPeriod(workloadHistoryStartTime, currentTime);

            GregorianCalendar calendar = (GregorianCalendar) Calendar.getInstance();
            calendar.setLenient(true); // Make it much smarter in day rolls
            calendar.setTimeInMillis(currentTime);

            StringBuffer workloadData = new StringBuffer();

            if (testEntries.isEmpty()) {
                currentStatus.append("<tr><td>\n<blockquote>\n<b>No data currently</b>\n</blockquote>\n</td></tr>\n\n");
            } else {
                currentStatus.append("<tr><td>\n<blockquote>\n");

                // Parse matching test entries
                long currentWorkloadDayStartTime = 0L;
                long endTime = 0L;
                SimpleDateFormat dateFormat = new SimpleDateFormat(Constant.DATE_FORMAT);
                String dateLabel;

                for (int i = 0; i < workloadHistoryPeriodInDays; i++) {

                    List<TestEntry> matchingTests = new ArrayList<TestEntry>(0);

                    currentWorkloadDayStartTime = currentTime - (i * Constant.ONE_DAY);

                    // Set calendar to the exact beginning of the date
                    calendar.setTimeInMillis(currentWorkloadDayStartTime);
                    calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
                    calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
                    calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

                    // Take exact beginning of the date in milliseconds
                    currentWorkloadDayStartTime = calendar.getTimeInMillis();

                    // Set calendar to the end of the same day
                    calendar.set(Calendar.HOUR_OF_DAY, calendar.getMaximum(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
                    calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
                    calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));

                    endTime = calendar.getTimeInMillis();

                    dateLabel = dateFormat.format(new Date(currentWorkloadDayStartTime));

                    for (TestEntry currentTestEntry : testEntries) {
                        Long currentTestCreationTime = currentTestEntry.getTestCreationTime();

                        if (currentTestCreationTime > currentWorkloadDayStartTime && currentTestCreationTime < endTime) {
                            // The corresponding test entry is within the specified range

                            String testNodeHostname = currentTestEntry.getTestNodeHostname();
                            String testNodePort = currentTestEntry.getTestNodePort();

                            if (testNodeHostname.equalsIgnoreCase(hostname)) {
                                if (testNodePort.equalsIgnoreCase("" + port)) {
                                    // Test was executed on this test node
                                    matchingTests.add(currentTestEntry);
                                }
                            }
                        }
                    }

                    // Display results
                    workloadData.append("<a href=\"" + HttpHandler.getDateStatisticsURI(dateLabel) + "\">" + dateLabel + "</a>:<br/>\n\n");

                    if (matchingTests.isEmpty()) {
                        workloadData.append("<i>No data for this test node</i><br/>\n\n");
                    } else {
                        for (TestEntry matchingTest : matchingTests) {
                            StringBuffer currentTestData = new StringBuffer();
                            String testRuntimeId = matchingTest.getTestRuntimeId();
                            String testURL = matchingTest.getTestURL();
                            boolean testWasSuccessful = matchingTest.testWasSuccessful();
                            String reasonOfFailure = matchingTest.getReasonOfFailure();

                            if (testURL != null && !testURL.isEmpty()) {
                                currentTestData.append("<a href=\"" + testURL + "\" target=\"_blank\">");
                            }

                            if (testWasSuccessful) {
                                currentTestData.append("<font color=\"#006600\">");
                                currentTestData.append(testRuntimeId);
                                currentTestData.append("</font>");
                            } else {
                                currentTestData.append("<font color=\"#ee0000\">");
                                currentTestData.append(testRuntimeId);
                                currentTestData.append("</font>");
                            }

                            if (testURL != null && !testURL.isEmpty()) {
                                currentTestData.append("</a>");
                            }

                            if (!testWasSuccessful) {
                                if (reasonOfFailure != null && !reasonOfFailure.isEmpty()) {
                                    currentTestData.append(" - " + reasonOfFailure);
                                } else {
                                    currentTestData.append(" - " + Constant.UNSPECIFIED_REASON_OF_FAILURE);
                                }
                            }

                            currentTestData.append("<br/>\n");

                            workloadData.append(currentTestData);
                        }
                    }

                    workloadData.append("<br/>\n\n");
                }

                currentStatus.append(workloadData);
                currentStatus.append("\n</blockquote>\n\n</td></tr>\n");
            }

            currentStatus.append("<tr><td>&nbsp;</td></tr>\n\n");

            currentStatus.append("\n</table>\n</body>\n</html>");
        } catch (Exception e) {
            p("Got troubles during extracting detailed status for the test node " + hostname + ":" + port + " - " + e.getClass() + " - " + e.getMessage());
            e.printStackTrace();
        }

        return currentStatus;
    }

    /**
     * Returns detailed status about the product with specified IMEI number.
     *
     * @param imei IMEI number of the product
     * @return Detailed status about the product with specified IMEI number
     */
    protected StringBuffer getDetailedProductStatus(String imei) throws Exception {
        StringBuffer currentStatus = new StringBuffer();

        currentStatus.append("<!DOCTYPE html>\n<html>\n<head>\n<title>");
        currentStatus.append("Product with IMEI " + imei + " on test node " + hostname + ":" + port);
        currentStatus.append("</title>\n");
        currentStatus.append("<link rel=\"shortcut icon\" href=\"http://wikis.in.nokia.com/pub/CI20Development/TASGuide/favicon.ico\" type=\"image/x-icon\" />\n");
        currentStatus.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n");
        currentStatus.append("</head>\n");
        currentStatus.append("<body>\n<table border=\"0\">\n\n");

        Product product = null;
        String productState = "";

        if (imei != null && !imei.isEmpty()) {
            // Search all available products
            for (Product availableProduct : products) {
                if (availableProduct.getIMEI().equals(imei)) {
                    product = availableProduct;
                    productState = "Connected";
                    break;
                }
            }

            if (product == null) {
                // Search temporarly disconnected products
                for (Product temporarlyDisconnectedProduct : temporarlyDisconnectedProducts) {
                    if (temporarlyDisconnectedProduct.getIMEI().equals(imei)) {
                        product = temporarlyDisconnectedProduct;
                        productState = "Temporarly disconnected";
                        break;
                    }
                }
            }

            if (product == null) {
                // Search permanently disconnected products
                for (Product permanentlyDisconnectedProduct : permanentlyDisconnectedProducts) {
                    if (permanentlyDisconnectedProduct.getIMEI().equals(imei)) {
                        product = permanentlyDisconnectedProduct;
                        productState = "Permanently disconnected";
                        break;
                    }
                }
            }
        }

        if (product != null) {
            currentStatus.append("<tr><td>\n");
            currentStatus.append(productState + " product with IMEI " + imei + " on test node " + hostname + ":" + port + "<br/><br/>\n");
            currentStatus.append("\n<blockquote>\n");

            currentStatus.append("IMEI: " + product.getIMEI() + "<br/>\n");
            currentStatus.append("RM-Code: " + product.getRMCode() + "<br/>\n");
            currentStatus.append("Hardware type: " + product.getHardwareType() + "<br/>\n");
            currentStatus.append("FUSE connection name: " + product.getFuseConnectionName() + "<br/>\n");
            currentStatus.append("FUSE connection id: " + product.getFuseConnectionId() + "<br/>\n");
            currentStatus.append("Hostname: " + product.getHostname() + "<br/>\n");
            currentStatus.append("IP address: " + product.getIPAddress() + "<br/>\n");
            currentStatus.append("TCP/IP port: " + product.getPort() + "<br/>\n");
            currentStatus.append("Role: " + product.getRole() + "<br/>\n");
            currentStatus.append("Environment: " + product.getEnvironment() + "<br/>\n");
            currentStatus.append("Status: " + product.getStatus() + "<br/>\n");
            currentStatus.append("Status details: " + product.getStatusDetails() + "<br/>\n");

            if (product.getDisconnectionTime() > 0L) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constant.TIMESTAMP_FORMAT);
                currentStatus.append("Disconnected at: " + simpleDateFormat.format(new Date(product.getDisconnectionTime())) + "</br>");
            }

            currentStatus.append("TAS hostname: " + product.getTestAutomationServiceHostname() + "<br/>\n");
            currentStatus.append("TAS port: " + product.getTestAutomationServicePort() + "<br/>\n");

            String simCard1Status = product.getSim1().toString();
            simCard1Status = simCard1Status.replace("\n\t ", "<br/>\n");
            currentStatus.append("<br/>" + simCard1Status + "<br/>\n");

            String simCard2Status = product.getSim2().toString();
            simCard2Status = simCard2Status.replace("\n\t ", "<br/>\n");
            currentStatus.append("<br/>" + simCard2Status + "<br/>\n");

            currentStatus.append("\n</blockquote>\n</td></tr>\n\n");

            // Add information from the corresponding product entry
            currentStatus.append("<tr><td>Tests executed on this product:<br/>\n");
            currentStatus.append("\n<blockquote>\n");

            if (statistics != null) {
                ProductEntry productEntry = statistics.getProductEntry(imei);
                
                if (productEntry != null) {
                    List<TestEntry> testEntries = productEntry.getTestEntries();
                    
                    if (testEntries != null && !testEntries.isEmpty()) {
                        for (TestEntry testEntry : testEntries) {
                            StringBuffer currentTestData = new StringBuffer();
                            String testRuntimeId = testEntry.getTestRuntimeId();
                            String testURL = testEntry.getTestURL();
                            boolean testWasSuccessful = testEntry.testWasSuccessful();
                            String reasonOfFailure = testEntry.getReasonOfFailure();

                            if (testURL != null && !testURL.isEmpty()) {
                                currentTestData.append("<a href=\"" + testURL + "\" target=\"_blank\">");
                            }

                            if (testWasSuccessful) {
                                currentTestData.append("<font color=\"#006600\">");
                                currentTestData.append(testRuntimeId);
                                currentTestData.append("</font>");
                            } else {
                                currentTestData.append("<font color=\"#ee0000\">");
                                currentTestData.append(testRuntimeId);
                                currentTestData.append("</font>");
                            }

                            if (testURL != null && !testURL.isEmpty()) {
                                currentTestData.append("</a>");
                            }

                            if (!testWasSuccessful) {
                                if (reasonOfFailure != null && !reasonOfFailure.isEmpty()) {
                                    currentTestData.append(" - " + reasonOfFailure);
                                } else {
                                    currentTestData.append(" - " + Constant.UNSPECIFIED_REASON_OF_FAILURE);
                                }
                            }

                            currentTestData.append("<br/>\n");

                            currentStatus.append(currentTestData);
                        }
                    } else {
                        currentStatus.append("<i>No statistics data for this product</i><br/>\n\n");
                    }
                }
            } else {
                currentStatus.append("<i>Couldn't get statistics data for this product</i><br/>\n\n");
            }

            currentStatus.append("\n</blockquote>\n</td></tr>\n\n");

        } else {
            currentStatus.append("<tr><td>\n<blockquote>\n");
            currentStatus.append("Cannot find any information about a product with IMEI " + imei);
            currentStatus.append("\n</blockquote>\n</td></tr>\n\n");
        }

        currentStatus.append("<tr><td>&nbsp;</td></tr>\n\n");
        
        currentStatus.append("\n</table>\n</body>\n</html>");

        return currentStatus;
    }

    /**
     * Compares this test node and specified one.
     * Will return 0 if this test node has been loaded in the same way as the other.
     * Will return 1 if this test node has been loaded more than the other.
     * Will return -1 if this test node has been loaded less than the other.
     *
     * @param testNode A test node to be compared to
     * @return 0, 1 or -1 values
     */
    @Override
    public int compareTo(TestNode testNode) {

        long otherNumberOfTests = testNode.getTotalNumberOfExecutedTests() + testNode.getNumberOfRunningTests();
        long otherNumberOfProducts = testNode.getNumberOfProducts();

        // Prevent division by zero and perform comparision on total number of tests
        if (otherNumberOfProducts <= 0) {
            otherNumberOfProducts = 1;
        }

        double otherLoad = (double) otherNumberOfTests / otherNumberOfProducts;

        long numberOfTests = totalNumberOfExecutedTests + runningTests.size();
        long numberOfProducts = products.size();

        // Prevent division by zero and perform comparision on total number of tests
        if (numberOfProducts <= 0) {
            numberOfProducts = 1;
        }

        double load = (double) numberOfTests / numberOfProducts;

        if (load < otherLoad) {
            // This test node was "less" loaded
            return -1;
        }

        if (load > otherLoad) {
            // This test node was "more" loaded
            return 1;
        }

        // This test node was "equaly" loaded
        return 0;
    }

    /**
     * Prints debug and informal messages to the console.
     *
     * @param text A message to be printed to the console
     */
    private void p(String text) {
        logger.log(Level.ALL, "Test node " + hostname + ":" + port + ": " + text);
    }
}
