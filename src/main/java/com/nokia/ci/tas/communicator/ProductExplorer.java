package com.nokia.ci.tas.communicator;

import java.text.SimpleDateFormat;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.Util;

import com.nokia.ci.tas.commons.message.Message;
import com.nokia.ci.tas.commons.message.ProductOperation;

/**
 * Product explorer is responsible for discovery of all products and prototypes connected to the testing node.
 */
public class ProductExplorer extends Thread {

    /**
     * Fuse module access point
     */
    //private FuseProxy fuseProxy;
	private ProductDetector productDetector;

    /**
     * Map of products where key is IMEI code and values are products.
     */
    private Map<String, Product> products;

    /**
     * A pool of messages to be handled by product explorer.
     */
    private ConcurrentLinkedQueue<Message> messagePool;

    /**
     * Local instance of the sending module.
     */
    private Sender sender;

    /**
     * Variable which keeps process explorer running
     */
    private boolean isRunning = true;

    /**
     * Hostname of the Test Automation Communicator.
     */
    private String testAutomationCommunicatorHostname;

    /**
     * Port number of the Test Automation Communicator.
     */
    private int testAutomationCommunicatorPort;

    /**
     * Hostname of the Test Automation Service.
     */
    private String testAutomationServiceHostname;

    /**
     * Port number of the Test Automation Service.
     */
    private int testAutomationServicePort;

    /**
     * Handler of the product configurations.
     */
    private ProductConfigurationHandler productConfigurationHandler;

    /**
     * Timestamp of the moment when a list of currently available products was send to the Test Automation Service.
     */
    private long lastNotificationTime;

    /**
     * Instance of the Test Automation Communicator's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationCommunicator.GLOBAL_LOGGER_NAME);
    
    /**
     * Product without valid Imei.
     */
    private Product nonImeiProduct;
    
    /**
     * Save the last candidate without seperated trace connection.
     */
    private Product traceCandidate;

    /**
     * Creates an instance of the process exlorer.
     *
     * @param sender Instance of the message sending module
     */
    public ProductExplorer(Sender sender) {
        super(); // Start as anonymous thread

        this.sender = sender;

        messagePool = new ConcurrentLinkedQueue();

        products = new HashMap<String, Product>(0);

        // Extract the following parameters to eliminate unnecessary method calls
        TestAutomationCommunicator communicator = TestAutomationCommunicator.getInstance();

        testAutomationCommunicatorHostname = communicator.getHostname();
        testAutomationCommunicatorPort = communicator.getPort();
        testAutomationServiceHostname = communicator.getTestAutomationServiceHostname();
        testAutomationServicePort = communicator.getTestAutomationServicePort();

        try {
            productConfigurationHandler = new ProductConfigurationHandler(communicator.getProductConfigurationsDirectory());
        } catch (Exception e) {
            p("Product Explorer got troubles with initialization of the products configuration handler: " + e.getClass() + " " + e.getMessage());
        }

        // Fuse communicator will immediatly request all connected products from
        // fuse server. Results will be returned to allConnectedProducts-method.
        //fuseProxy = new FuseProxy(this);
        productDetector = new ProductDetector(this);

        lastNotificationTime = System.currentTimeMillis();

        p("Product Explorer was successfully created");
    }

    /**
     * Product explorer's main routine.
     */
    @Override
    public void run() {

        //fuseProxy.start();
    	productDetector.start();

        while (isRunning) {
            try {
                if (!messagePool.isEmpty()) {
                    Message message = messagePool.poll(); // Always get the first message

                    if (message != null) {
                        if (message instanceof ProductOperation) {
                            ProductOperation productOperation = (ProductOperation) message;
                            Product product = productOperation.getProduct();

                            if (product != null) {
                                if (products.containsKey(product.getIMEI())) {
                                    Product alreadyAvailableProduct = products.get(product.getIMEI());

                                    alreadyAvailableProduct.setStatus(product.getStatus());
                                    alreadyAvailableProduct.setStatusDetails(product.getStatusDetails());
                                    alreadyAvailableProduct.setReservation(product.getReservationTime(), product.getReservationTimeout());

                                    String productCurrentStatus = "Test Automation Service sets a product with IMEI " + alreadyAvailableProduct.getIMEI()
                                                + " and of type " + alreadyAvailableProduct.getRMCode()
                                                + " into status " + alreadyAvailableProduct.getStatus();

                                    if (product.getStatus() == Product.Status.BUSY) {
                                        productCurrentStatus += " - " + alreadyAvailableProduct.getStatusDetails();

                                        if (alreadyAvailableProduct.getReservationTime() > 0L) {
                                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constant.TIMESTAMP_FORMAT);
                                            productCurrentStatus += ", reserved at " + simpleDateFormat.format(new Date(alreadyAvailableProduct.getReservationTime()));
                                        } else {
                                            productCurrentStatus += ", reserved at unknown date and time";
                                        }

                                        if (alreadyAvailableProduct.getReservationTimeout() > 0L) {
                                            productCurrentStatus += ", reservation timeout is " + Util.convert(alreadyAvailableProduct.getReservationTimeout());
                                        } else {
                                            productCurrentStatus += ", reservation timeout is not specified";
                                        }
                                    }

                                    // Update configuration file
                                    alreadyAvailableProduct = productConfigurationHandler.update(alreadyAvailableProduct);

                                    // Store any changes
                                    products.put(product.getIMEI(), alreadyAvailableProduct);

                                    // Print current status
                                    p(productCurrentStatus);
                                }
                            }
                        } else {
                            p("Received message is not supported and will be ignored...");
                        }
                    }
                } else {
                    // If nothing has happened for the last 30 seconds, notify Test Automation Service about current products
                    if ((System.currentTimeMillis() - lastNotificationTime) > Constant.THIRTY_SECONDS) {

                        // Notify Test Automation Service about all currently available products
                        for (Product product : products.values()) {
                            ProductOperation updateProductOperation = new ProductOperation(ProductOperation.Id.UPDATE, product);
                            updateProductOperation.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);
                            updateProductOperation.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
                            sender.handle(updateProductOperation);
                        }

                        lastNotificationTime = System.currentTimeMillis();
                    }
                }

                sleep(Constant.DECISECOND); // Wait for any updates
            }
            catch (Exception e) {
                p("Troubles in Product Explorer:");
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles specified message.
     *
     * @param message A message to be handled
     */
    public synchronized void handle(Message message) {
        messagePool.add(message);
        notify();
    }

    /**
     * Set assigned Product state to Free.
     * 
     * @param imei assigned product IMEI number.
     */
    public synchronized void setProductFree( String imei ) {
    	 Collection<Product> availableProducts = products.values();
    	 for (Product current : availableProducts) {
             if ( current.getIMEI().equals( imei ) && current.getStatus() == Product.Status.BUSY) {
                 current.setStatus(Product.Status.FREE, "");
                 current.setReservation(0, Constant.DEFAULT_TEST_TIMEOUT);

                 // Update configuration file as well
                 current = productConfigurationHandler.update(current);

                 // Store changes
                 products.put(current.getIMEI(), current);

                 p("Product with IMEI " + current.getIMEI() + " and " + current.getRMCode() + " was set as FREE");
                 break;
             }
         }
    }
    
    /**
     * Sets all available products as freely available ones.
     */
    public synchronized void setAllProductsFree() {
        p("Setting all available products free...");

        Collection<Product> availableProducts = products.values();

        for (Product current : availableProducts) {
            if (current.getStatus() == Product.Status.BUSY) {
                current.setStatus(Product.Status.FREE, "");
                current.setReservation(0, Constant.DEFAULT_TEST_TIMEOUT);

                // Update configuration file as well
                current = productConfigurationHandler.update(current);

                // Store changes
                products.put(current.getIMEI(), current);

                p("Product with IMEI " + current.getIMEI() + " and " + current.getRMCode() + " was set as FREE");
            }
        }
    }

    /**
     * Handles a list of products discovered on test node.
     *
     * @param discoveredProducts A list of products discovered on test node
     */
    protected synchronized void updateProducts(List<Product> discoveredProducts) {
        //p("Handling a set of discovered products...");
        for (Product discoveredProduct : discoveredProducts) {
            if (products.containsKey(discoveredProduct.getIMEI())) {
                // Already has such product, simply update product data
                Product alreadyAvailableProduct = (Product) products.get(discoveredProduct.getIMEI());

                if (alreadyAvailableProduct != null) {

                    // Update FUSE settings
                    alreadyAvailableProduct.setFuseConnectionId(discoveredProduct.getFuseConnectionId());
                    alreadyAvailableProduct.setFuseConnectionName(discoveredProduct.getFuseConnectionName());
                    alreadyAvailableProduct.setIPAddress(discoveredProduct.getIPAddress());
                    alreadyAvailableProduct.setPort(discoveredProduct.getPort());

                    // Update product configuration file
                    alreadyAvailableProduct = productConfigurationHandler.update(alreadyAvailableProduct);

                    // Store any possible updates
                    products.put(alreadyAvailableProduct.getIMEI(), alreadyAvailableProduct);
                }
            } else {
                // Add as a new product
                addProduct(discoveredProduct);
            }
        }
    }

    /**
     * Handles a product discovered on the test node.
     *
     * @param discoveredProduct A product discoverd on the test node
     */
    protected synchronized void updateProduct(Product discoveredProduct) {
        //p("Handling a discovered product...");
        if (products.containsKey(discoveredProduct.getIMEI())) {
            // Already has such product, simply update product data
            Product alreadyAvailableProduct = (Product) products.get(discoveredProduct.getIMEI());

            if (alreadyAvailableProduct != null) {

                // Update FUSE settings
                //alreadyAvailableProduct.setFuseConnectionId(discoveredProduct.getFuseConnectionId());
                //alreadyAvailableProduct.setFuseConnectionName(discoveredProduct.getFuseConnectionName());
                //FIXME:? open or not?
                //alreadyAvailableProduct.setTraceConnectionId(discoveredProduct.getTraceConnectionId());
                //
                alreadyAvailableProduct.setIPAddress(discoveredProduct.getIPAddress());
                alreadyAvailableProduct.setPort(discoveredProduct.getPort());

                // Update product configuration file
                alreadyAvailableProduct = productConfigurationHandler.update(alreadyAvailableProduct);

                // Store any possible updates
                products.put(alreadyAvailableProduct.getIMEI(), alreadyAvailableProduct);
            }
        } else {
            // Add as a new product
            addProduct(discoveredProduct);
        }
    }

    /**
     * Handles a new product connected to test node.
     *
     * @param connectedProduct A new product connected to test node
     */
    protected synchronized void addProduct(Product connectedProduct) {
        p("Handling a new connected product of type " + connectedProduct.getRMCode() + " and with IMEI " + connectedProduct.getIMEI());
        
        // Add all required configuration parameters
        connectedProduct.setTestAutomationService(testAutomationServiceHostname, testAutomationServicePort);
	
        // Always update product configuration file
        connectedProduct = productConfigurationHandler.update(connectedProduct);
        
        // Store changes
       	products.put(connectedProduct.getIMEI(), connectedProduct);
       	// Notify Test Automation Service
        ProductOperation addProductOperation = new ProductOperation(ProductOperation.Id.ADD, connectedProduct);
        addProductOperation.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);
        addProductOperation.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
        sender.handle(addProductOperation);
    }

    /**
     * Handles a product disconnected from test node.
     *
     * @param disconnectedProduct A product disconnected from test node
     */
    protected synchronized void removeProduct(Product disconnectedProduct) {
        p("Handling a disconnected product of type " + disconnectedProduct.getRMCode() + " and with IMEI " + disconnectedProduct.getIMEI());

        if (products.containsKey(disconnectedProduct.getIMEI())) {
            Product alreadyAvailableProduct = (Product) products.get(disconnectedProduct.getIMEI());

            if (alreadyAvailableProduct != null) {
                // Remove it from the list of available products
                products.remove(alreadyAvailableProduct.getIMEI());

                // Remember the time of disconnection
                alreadyAvailableProduct.setDisconnectionTime(System.currentTimeMillis());

                // Notify Test Automation Service about a removed product
                ProductOperation removeProductOperation = new ProductOperation(ProductOperation.Id.REMOVE, alreadyAvailableProduct);
                removeProductOperation.setSender(testAutomationCommunicatorHostname, testAutomationCommunicatorPort);
                removeProductOperation.setReceiver(testAutomationServiceHostname, testAutomationServicePort);
                sender.handle(removeProductOperation);
            }
        }
    }

    /**
     * Prints class specific output.
     *
     * @param text A test to be printed out
     */
    private void p(String text) {
        logger.log(Level.ALL, "Product Explorer: " + text);
    }
}
