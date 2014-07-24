package com.nokia.ci.tas.client;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.Test;
import com.nokia.ci.tas.commons.TestAutomationServiceListener;
import com.nokia.ci.tas.commons.TestPackage;
import com.nokia.ci.tas.commons.Util;

import com.nokia.ci.tas.commons.message.ProductOperation;
import com.nokia.ci.tas.commons.message.RegistryOperation;
import com.nokia.ci.tas.commons.message.TestOperation;
import com.nokia.ci.tas.commons.message.TextMessage;

/**
 * Test Automation Client.
 */
public class TestAutomationClient extends Thread {

    /**
     * Name of the host where Test Automation Client is running.
     */
    private String clientHostname;

    /**
     * Port number that Test Automation Client is using for listening all incoming messages.
     */
    private int clientPort = 0;

    /**
     * Variable which keeps Test Automation Client running.
     */
    private boolean isRunning = true;

    /**
     * Server socket used for incoming messages.
     */
    private ServerSocket socketListener;

    /**
     * A list of all currently running tests from this instance of Test Automation Client.
     * Each Test Registry tells which test is running on which remote Test Automation Service
     * and to which Test Automation Service Listener it belongs.
     */
    private ConcurrentLinkedQueue<TestRegistry> executingTests;

    /**
     * List of Test Automatin Services registered with this instance of the Test Automation Client.
     * There could be as many remote services as listeners has requested,
     * so the same listener may issue many tests on different Test Automation Services at the same time.
     */
    private ConcurrentLinkedQueue<RemoteService> remoteServices;

    /**
     * Handler of all incoming messages.
     */
    private Receiver receiver;

    /**
     * Handler of all outcoming file transfer messages.
     */
    private FileSender fileSender;

    /**
     * Debugging print stream.
     */
    private PrintStream printStream;

    /**
     * Initialization dispatcher.
     */
    private boolean isInitialized = false;

    /**
     * Creates an instance of the Test Automation Client on local host and default port number.
     *
     * @param printStream Print stream for output and debugging messages
     */
    public TestAutomationClient(PrintStream printStream) {
        this(null, -1, printStream);
    }

    /**
     * Creates an instance of the Test Automation Client on specified hostname and port number.
     *
     * @param hostname Name of host where this instance of client will be created
     * @param port Port number on which client will be created
     * @param printStream Print stream for output and debugging messages
     */
    public TestAutomationClient(String hostname, int port, PrintStream printStream) {

        super(); // Start as anonymous thread

        // Set a debugging print stream
        this.printStream = printStream;

        // Set the port number
        if (port < 0 || port > 0xffff) {
            // Choose client's port randomly
            Random random = new Random();
            clientPort = -1;
            while (clientPort < 0 || clientPort > 0xffff) {
                clientPort = Constant.TEST_AUTOMATION_CLIENT_DEFAULT_PORT_NUMBER + Math.abs(random.nextInt());
            }
            p("Randomly selected port number is " + clientPort);
        } else {
            clientPort = port;
        }

        // Set the hostname
        if (hostname == null || hostname.isEmpty()) {
            p("Trying to get the hostname of Test Automation Client");

            try {
                // Clients are started from Jenkins, so Java can help with getting hostnames
                clientHostname = Util.getValidHostIp();//java.net.InetAddress.getLocalHost().getCanonicalHostName();
                p("Extracted hostname is " + clientHostname);
            } catch (Exception e) {
                p("Got troubles while trying to extract local hostname: " + e.getClass() + " " + e.getMessage());
                p("Will try to extract local hostname from environment variables");

                // Get name of the host where the Test Automation Client will be running
                clientHostname = (String) System.getenv().get("HOST"); // By default we are running on Linux

                p("HOST environment variable returned " + clientHostname);

                if (clientHostname == null || clientHostname.isEmpty()) {
                    clientHostname = (String) System.getenv().get("HOSTNAME"); // Try another Linux environment variable
                    p("HOSTNAME environment variable returned " + clientHostname);
                }

                if (clientHostname == null || clientHostname.isEmpty()) {
                    clientHostname = (String) System.getenv().get("COMPUTERNAME"); // Otherwise we are probably running on Windows
                    p("COMPUTERNAME environment variable returned " + clientHostname);
                }

                if (clientHostname == null || clientHostname.isEmpty()) {
                    p("Warning: Couldn't extract a hostname for the Test Automation Client out of environment. Please specify one either in HOST, HOSTNAME or in COMPUTERNAME system variables.");
                    clientHostname = "localhost";
                }
            }
        } else {
            clientHostname = hostname;
            p("Will use provided hostname " + clientHostname);
        }

        p("Trying to launch Test Automation Client v" + Constant.TEST_AUTOMATION_RELEASE_VERSION + " on " + clientHostname + ":" + clientPort);

        // Create handlers

        // There could be as many remote services as test issues has specified
        remoteServices = new ConcurrentLinkedQueue();

        // There could be as may test registries as tests were given
        executingTests = new ConcurrentLinkedQueue();

        // Always work with normal priority
        setPriority(NORM_PRIORITY);

        start();
    }

    /**
     * Returns a hostname on which client started working.
     *
     * @return A hostname on which client started working
     */
    public synchronized String getHostname() {
        return clientHostname;
    }

    /**
     * Returns a port number on which client started working.
     *
     * @return A port number on which client started working
     */
    public synchronized int getPort() {
        return clientPort;
    }

    /**
     * Returns a number of currently executing tests.
     *
     * @return A number of currently executing tests
     */
    public synchronized int getNumberOfExecutingTests() {
        return executingTests.size();
    }

    /**
     * Returns a list of test ids being currently issued by this Test Automation Client.
     *
     * @return A list of test ids being currently issued by this Test Automation Client
     */
    public synchronized List<String> getExecutingTestIds() {
        List<String> testIds = new ArrayList<String>(0);

        for (TestRegistry testRegistry : executingTests) {
            testIds.add(testRegistry.getTestId());
        }

        return testIds;
    }

    /**
     * Returns true if client is able to send and receive messages, or false otherwise.
     *
     * @return True if client is able to send and receive messages, or false otherwise
     */
    public synchronized boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Main routine of the Test Automation Client.
     */
    @Override
    public void run() {

        p("Trying to start Test Automation Client on hostname " + clientHostname + " and port " + clientPort);

        // Find a free port number and create listening socket
        while (true) {
            try {
                socketListener = new ServerSocket(clientPort);
                p("Test Automation Client will work on " + clientHostname + ":" + clientPort);
                break;
            } catch (Exception e) {
                //p("Got a problem during establishing server socket: " + e.getClass() + " " + e.getMessage());
                //p("Port " + clientPort + " is probably already occupied");
                clientPort += 1;

                if (clientPort > 0xffff) {
                    clientPort = Constant.TEST_AUTOMATION_CLIENT_DEFAULT_PORT_NUMBER;
                }

                //p("Will try to use port " + clientPort);
            }
        }

        p("Listerner started working on port " + clientPort);

        // Create and start file sender for processing all incoming messages
        fileSender = new FileSender(this);
        fileSender.start();

        p("File sender was successfully started");

        // Create and start receiver for processing all incoming messages
        receiver = new Receiver(this);
        receiver.start();

        p("Receiver was successfully started");

        isInitialized = true;

        try {
            notify();
        } catch (Exception e) {
            // Ignore
        }

        // Start listening for any incoming messages
        try {
            while (isRunning) {
                // Accept all incoming requests
                Socket connection = socketListener.accept();

                // And process them asynchronously
                receiver.handle(connection);

                sleep(Constant.MILLISECOND); // Wait for any updates
            }

            if (socketListener != null) {
                socketListener.close();
                p("Listener or incoming messages was successfully closed");
            }

        } catch (Exception e) {
            p("Network listener is not running anymore. Stop working");

            for (TestRegistry current : executingTests) {
                TestAutomationServiceListener tasListener = current.getListener();
                if (tasListener != null) {
                    tasListener.testFailed(current.getTest(), "Test Automation Client couldn't continue its work due to "
                            + e.getClass() + " " + e.getMessage());
                }
            }

            stopWorking();

            //self = null;
            System.gc();
            p("Stopped working");
        }
    }

    /**
     * Stops work of Test Automation Client.
     */
    protected synchronized void stopWorking() {

        p("Stopping work of Test Automation Client at " + clientHostname + ":" + clientPort);

        isRunning = false;
        isInitialized = false;

        if (remoteServices != null && !remoteServices.isEmpty()) {
            for (RemoteService remoteService : remoteServices) {
                remoteService.stopWorking();
            }
            remoteServices.clear();
        }

        if (receiver != null) {
            receiver.stopWorking();
            p("Receiver of Test Automation Client at " + clientHostname + ":" + clientPort + " was successfully stopped");
        }

        if (fileSender != null) {
            fileSender.stopWorking();
            p("File sender of Test Automation Client at " + clientHostname + ":" + clientPort + " was successfully stopped");
        }

        if (socketListener != null) {
            try {
                socketListener.close();
                socketListener = null;
                p("Listener of Test Automation Client at " + clientHostname + ":" + clientPort + " was successfully stopped");
            } catch (Exception e) {
                // Ignore
            }
        }

        yield();
    }

    /**
     * Starts specified test on a specified Test Automation Service.
     * The listener specified in this metho will receive all messages generated during test performances.
     *
     * @param test Test to be performed
     * @param serviceHostname Name of the host where remote Test Automation Service is running
     * @param servicePort Port number used by remote Test Automation Service for all incoming messages
     * @param listener Listener of all messages generated during test performances
     */
    public synchronized void startTest(Test test, String serviceHostname, int servicePort, TestAutomationServiceListener listener) {

        // Check that all test parameters are fine

        if (listener == null) {
            p("Test execution listener is not specified. Test cannot be executed.");
            return;
        }

        if (test == null) {
            String failureReason = "Specified test object is NULL. Test cannot be executed.";
            p(failureReason);
            listener.testFailed(test, failureReason);
            return;
        }

        if (test.getId() == null || test.getId().isEmpty()) {
            String failureReason = "Test has empty id. Test cannot be executed.";
            p(failureReason);
            listener.testFailed(test, failureReason);
            return;
        } else {
            // Check that test id doesn't contain any bad characters
            String failureReason = Util.checkTestId(test.getId());

            if (failureReason != null) {
                failureReason += " Test cannot be executed.";
                p(failureReason);
                listener.testFailed(test, failureReason);
                return;
            }
        }

        if (serviceHostname == null || serviceHostname.isEmpty()) {
            String failureReason = "Hostname of the remote Test Automation Service is not specified. Test cannot be executed.";
            p(failureReason);
            listener.testFailed(test, failureReason);
            return;
        }

        if (servicePort <= 0) {
            String failureReason = "Port of the remote Test Automation Service is invalid: " + servicePort + ". Test cannot be executed.";
            p(failureReason);
            listener.testFailed(test, failureReason);
            return;
        }

        if (test.getTarget() == Test.Target.FLASH) {
            // Flash testing always involves physical products
            List<Product> requiredProducts = test.getRequiredProducts();

            if (requiredProducts == null || requiredProducts.isEmpty()) {
                // Check if an expression for required products was set
                String requiredEnvironment = test.getRequiredEnvironment();

                if (requiredEnvironment == null || requiredEnvironment.isEmpty()) {
                    // Check that environments are set in test packages
                    List<TestPackage> testPackages = test.getTestPackages();

                    boolean testIsInvalid = false;

                    if (!testPackages.isEmpty()) {
                        for (TestPackage testPackage : testPackages) {
                            if (testPackage.getRequiredEnvironment().isEmpty()) {
                                testIsInvalid = false;
                                break;
                            }
                        }
                    }

                    if (testIsInvalid) {
                        String failureReason = "Test target is " + test.getTarget() + ", but no required environments were specified. Test cannot be executed.";
                        p(failureReason);
                        listener.testFailed(test, failureReason);
                        return;
                    }
                }
            }
        }

        // Ensure that the same test was not issued already for the same remote service
        for (TestRegistry alreadyExecutingTest : executingTests) {
            if (alreadyExecutingTest.getTestId().equals(test.getId())) {
                // We have a test with the same id
                if (alreadyExecutingTest.getRemoteServiceHostname().equals(serviceHostname)) {
                    if (alreadyExecutingTest.getRemoteServicePort() == servicePort) {
                        // We have a test with the same id on the same remote Test Automation Service
                        String failureReason = "Test Automation Client at " + clientHostname + ":" + clientPort
                                               + " already executing a test with id "
                                               + test.getId() + " on remote Test Automation Service at "
                                               + serviceHostname + ":" + servicePort;
                        p(failureReason);
                        listener.testFailed(test, failureReason);

                        return;
                    }
                }
            }
        }

        // Otherwise we may be sure that test can be started

        listener.messageFromTestAutomationService(test, "Trying to start test on remote Test Automation Server at " + serviceHostname + ":" + servicePort);

        while (!isInitialized) {
            try {
                p("Waiting for client's initialization...");
                wait(Constant.ONE_SECOND);
            } catch (Exception e) {
                // Ignore
            }
        }

        // Check if we already have required Test Automation Service
        RemoteService remoteService = getRemoteService(serviceHostname, servicePort);

        if (remoteService != null) {
            // Add new test into the list of executing tests
            TestRegistry newTestRegistry = new TestRegistry(test.getId(), test, listener, serviceHostname, servicePort);

            if (executingTests.add(newTestRegistry)) {
                p("Successfully created a new test registry for the test " + newTestRegistry.getTestId());
                p("STARTING TEST from client at " + clientHostname + ":" + clientPort + " on service at " + remoteService.getServiceHostnameAndPort());
                p("THE TEST TO BE STARTED:\n" + test);

                // Start test immediatelly
                TestOperation startTest = new TestOperation(TestOperation.Id.START, test);
                startTest.setReceiver(remoteService.getServiceHostname(), remoteService.getServicePort());
                startTest.setSender(clientHostname, clientPort);
                remoteService.handle(startTest);

                listener.messageFromTestAutomationService(test, "Start Test message was send to remote Test Automation Server at " + remoteService.getServiceHostnameAndPort());
            } else {
                listener.testFailed(test, "Couldn't handle add test '" + test.getId() + "' to the list of running ones due to memory corruption issues");
            }
        } else {
            String failureReason = "Couldn't establish a connection to the remote Test Automation Service at "
                                   + serviceHostname + ":" + servicePort + ". "
                                   + "Please check network address, settings or if Test Automation Service is actually running there.";
            p(failureReason);
            listener.testFailed(test, failureReason);
        }
    }

    /**
     * Stops specified test.
     *
     * @param test Test to be stopped
     * @param serviceHostname Name of the host where remote Test Automation Service is running
     * @param servicePort Port number used by remote Test Automation Service for all incoming messages
     * @param listener Listener of events related to the test performances
     */
    public synchronized void stopTest(Test test, String serviceHostname, int servicePort, TestAutomationServiceListener listener) {

        // Check that all test parameters are fine

        if (listener == null) {
            p("Test execution listener is not specified. Test cannot be stopped.");
            return;
        }

        if (test == null) {
            String failureReason = "Specified test object is NULL. Test cannot be stopped.";
            p(failureReason);
            listener.testFailed(test, failureReason);
            return;
        }

        if (test.getId() == null || test.getId().isEmpty()) {
            String failureReason = "Test has empty id. Test cannot be stopped.";
            p(failureReason);
            listener.testFailed(test, failureReason);
            return;
        } else {
            // Check that test id doesn't contain any bad characters
            String failureReason = Util.checkTestId(test.getId());

            if (failureReason != null) {
                failureReason += " Test cannot be stopped.";
                p(failureReason);
                listener.testFailed(test, failureReason);
                return;
            }
        }

        if (serviceHostname == null || serviceHostname.isEmpty()) {
            String failureReason = "Hostname of the remote Test Automation Service is not specified. Test cannot be stopped.";
            p(failureReason);
            listener.messageFromTestAutomationService(test, failureReason);
            return;
        }

        if (servicePort <= 0) {
            String failureReason = "Port of the remote Test Automation Service is invalid: " + servicePort + ". Test cannot be stopped.";
            p(failureReason);
            listener.messageFromTestAutomationService(test, failureReason);
            return;
        }

        TestRegistry testToBeStopped = null;

        // Ensure that specified test is actually executing on specified remote service
        for (TestRegistry current : executingTests) {
            if (current.getTestId().equals(test.getId())) {
                // We have a test with the same id
                if (current.getRemoteServiceHostname().equals(serviceHostname)) {
                    if (current.getRemoteServicePort() == servicePort) {
                        // We have a test with specified id on the specified remote Test Automation Service
                        testToBeStopped = current;
                        break;
                    }
                }
            }
        }

        while (!isInitialized) {
            try {
                p("Waiting for client's initialization...");
                wait(Constant.ONE_SECOND);
            } catch (Exception e) {
                // Ignore
            }
        }

        if (testToBeStopped != null) {

            RemoteService remoteService = getRemoteService(serviceHostname, servicePort);

            if (remoteService != null) {

                // Stop test
                listener.messageFromTestAutomationService(test, "Sending a Stop Test message to remote Test Automation Server at " + remoteService.getServiceHostnameAndPort());

                TestOperation stopTest = new TestOperation(TestOperation.Id.STOP, test);
                stopTest.setReceiver(remoteService.getServiceHostname(), remoteService.getServicePort());
                stopTest.setSender(clientHostname, clientPort);
                remoteService.handle(stopTest);

                System.gc();

            } else {
                String failureMessage = "Couldn't establish a connection to the remote Test Automation Service at "
                                        + serviceHostname + ":" + servicePort + ". "
                                        + "Please check network address, settings or if Test Automation Service is actually running there.";
                p(failureMessage);
                listener.messageFromTestAutomationService(test, failureMessage);
            }
        } else {
            String failureMessage = "There is no test with id " + test.getId() + " under execution anymore";
            p(failureMessage);
            listener.messageFromTestAutomationService(test, failureMessage);
        }
    }

    /**
     * Set specified products as a freely available ones.
     *
     * @param products A list of products to be set as a freely available ones
     */
    public synchronized void freeProducts(List<Product> products) {

        if (products != null && !products.isEmpty()) {
            // Create product releasing messages
            List<ProductOperation> freeProductsOperations = new ArrayList<ProductOperation>(0);

            for (Product current : products) {
                // Print release message for better outputs
                String releaseMessage = "Trying to release product";
                String fuseConnectionName = current.getFuseConnectionName();
                String hostname = current.getHostname();

                releaseMessage += " with IMEI " + current.getIMEI();
                releaseMessage += " and of type " + current.getRMCode();

                if (fuseConnectionName != null && !fuseConnectionName.isEmpty()) {
                    releaseMessage += " (FUSE connection name '" + fuseConnectionName + "')";
                }

                if (hostname != null && !hostname.isEmpty()) {
                    releaseMessage += " on test node " + hostname;
                }

                p(releaseMessage);

                current.setStatus(Product.Status.FREE, ""); // Mark this product as free
                current.setReservation(0L, Constant.DEFAULT_TEST_TIMEOUT);
                current.setDisconnectionTime(0L);

                ProductOperation freeProductOperation = new ProductOperation(ProductOperation.Id.UPDATE, current);
                freeProductOperation.setSender(clientHostname, clientPort);
                freeProductOperation.setReceiver(current.getTestAutomationServiceHostname(), current.getTestAutomationServicePort());
                freeProductsOperations.add(freeProductOperation);
            }

            if (!isInitialized) {
                // Try to send messages immediately
                for (ProductOperation freeProductOperation : freeProductsOperations) {
                    Socket socket = null;
                    try {
                        socket = new Socket(InetAddress.getByName(freeProductOperation.getReceiverHostname()), freeProductOperation.getReceiverPort());
                        OutputStream output = socket.getOutputStream();

                        output.write(freeProductOperation.toXML().getBytes("UTF-8"));
                        output.flush();
                        output.close();
                        socket.close();

                        p("Product releasing messages were successfully send to " + freeProductOperation.getReceiverHostname() + ":" + freeProductOperation.getReceiverPort());
                    } catch (Exception e) {
                        // Ignore
                    } finally {
                        // Always ensure that connection is closed
                        if (socket != null && !socket.isClosed()) {
                            try {
                                socket.close();
                            } catch (Exception e) {
                                p("Got troubles while tried to close a connection to Test Automation Service at address " + socket.getInetAddress()
                                        + ": " + e.getClass() + " - " + e.getMessage());
                            }
                        }
                    }
                }
            } else {
                for (ProductOperation freeProductOperation : freeProductsOperations) {
                    Product product = freeProductOperation.getProduct();
                    RemoteService remoteService = getRemoteService(product.getTestAutomationServiceHostname(), product.getTestAutomationServicePort());

                    if (remoteService != null) {
                        freeProductOperation.setReceiver(remoteService.getServiceHostname(), remoteService.getServicePort());
                        remoteService.handle(freeProductOperation);
                        p("Product releasing message was successfully forwarded to " + remoteService.getServiceHostnameAndPort());
                    }
                }
            }
        }
    }

    /**
     * Returns a test registry for specified test id, or null if such test registry is not existing.
     *
     * @param testId Id of the test
     * @param serviceHostname Hostname of the Test Automation Service where the test with specified id is executing
     * @param servicePort Port number of the Test Automation Service where the test with specified id is executing
     * @return A test registry for specified test id, or null if such test registry is not existing
     */
    protected synchronized TestRegistry getTestRegistry(String testId, String serviceHostname, int servicePort) {

        TestRegistry result = null;

        if (testId != null && !testId.isEmpty()) {
            if (serviceHostname != null && !serviceHostname.isEmpty() && servicePort > 0) {
                for (TestRegistry current : executingTests) {
                    if (current.getTestId().equals(testId)) {
                        // We have a test with the same id
                        if (current.getRemoteServiceHostname().startsWith(serviceHostname)) {
                            if (current.getRemoteServicePort() == servicePort) {
                                // We have a test with the same id executing on the specified hostname and port number
                                result = current;
                                break;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns a test registry for specified test id, or null if such test registry is not existing.
     *
     * @param testId Id of the test
     * @param serviceHostname Hostname of the Test Automation Service where the test with specified id is executing
     * @param servicePort Port number of the Test Automation Service where the test with specified id is executing
     * @return A test registry for specified test id, or null if such test registry is not existing
     */
    protected synchronized TestRegistry getTestRegistry(String testId) {

        TestRegistry result = null;

        if (testId != null && !testId.isEmpty()) {
            for (TestRegistry current : executingTests) {
                if (current.getTestId().equals(testId)) {
                    // We have a test with the specified id
                    result = current;
                    break;
                }
            }
        }

        return result;
    }

    protected synchronized ConcurrentLinkedQueue<TestRegistry> getTestRegistries() {
        return executingTests;
    }

    /**
     * Gets a remote Test Automation Service with specified hostname and port number
     * where to client with specified hostname and port is registered.
     *
     * @param serviceHostname Hostname of remote Test Automation Service
     * @param servicePort Port number of remote Test Automation Service
     * @return Remote Test Automation Service with specified hostname and port or null if such service is not available
     */
    private synchronized RemoteService getRemoteService(String serviceHostname, int servicePort) {
        RemoteService remoteService = null;

        // Check if we already having specified service
        for (RemoteService current : remoteServices) {
            if (current.getServiceHostname().equals(serviceHostname) && current.getServicePort() == servicePort) {
                remoteService = current;
                break;
            }
        }

        // If no such service was available, try to register
        if (remoteService == null) {
            p("Client at " + clientHostname + ":" + clientPort + " haven't yet registered to service at " + serviceHostname + ":" + servicePort);

            // Send a registration message to specified Test Automation Service
            Socket socket = null;

            try {
                RegistryOperation registerClient = new RegistryOperation(RegistryOperation.Id.REGISTER, RegistryOperation.Remote.CLIENT);
                registerClient.setReceiver(serviceHostname, servicePort);
                registerClient.setSender(clientHostname, clientPort);

                p("Sending a registration message:" + registerClient);
                p("Test Automation Client will register itself as " + clientHostname + ":" + clientPort);

                socket = new Socket(InetAddress.getByName(registerClient.getReceiverHostname()), registerClient.getReceiverPort());
                OutputStream output = socket.getOutputStream();

                // Send message and close connection immediately
                output.write(registerClient.toXML().getBytes("UTF-8"));
                output.flush();
                output.close();
                socket.close();

                p("Remote Client registration message was successfully send");

                // A successful send means that remote service runs at specified hostname and port number

                // Create a new remote service instance
                remoteService = new RemoteService(this, serviceHostname, servicePort);
                remoteService.start();

                // Add it to the list of available test services
                remoteServices.add(remoteService);

                p("Successfully registered to remote Test Automation Service at " + remoteService.getServiceHostnameAndPort());
            } catch (Exception e) {
                p("Couldn't send a registration message to remote Test Automation Service at " + serviceHostname + ":" + servicePort);
                //e.printStackTrace();

                // Something is wrong with remote Test Automation Service or network
                remoteService = null;
            } finally {
                // Always ensure that connection is closed
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        p("Got troubles while tried to close a connection to Test Automation Service at address " + socket.getInetAddress()
                                + ": " + e.getClass() + " - " + e.getMessage());
                    }
                }
            }
        }

        return remoteService;
    }

    protected synchronized void handleTestOperation(TestOperation testOperation) {
        //Too noisy: p("Handling incoming test operation: " + testOperation);
        Test test = testOperation.getTest();

        if (test != null) {
            Test.Status status = test.getStatus();

            if (status == Test.Status.STARTED) {
                testStarted(testOperation);
            } else if (status == Test.Status.FINISHED) {
                testFinished(testOperation);
            } else if (status == Test.Status.STOPPED) {
                // It's just the same as finished, but has been forced to finish its work
                testFinished(testOperation);
            } else if (status == Test.Status.FAILED) {
                testFailed(testOperation);
            } else {
                if (testOperation.getId() == TestOperation.Id.UPDATE) {
                    //Too noisy: p("Got an UPDATED test");
                    testUpdated(testOperation);
                } else {
                    p("Got unsupported test operation message!\n" + testOperation);
                }
            }
        } else {
            p("Got null test from the message instead of a proper test object!");
        }
    }

    /**
     * Called by receiver when test is started.
     *
     * @param message Message regarding the started test
     */
    private synchronized void testStarted(TestOperation message) {

        if (message != null) {
            Test test = message.getTest();

            if (test != null) {
                TestRegistry registry = getTestRegistry(test.getId());

                if (registry != null) {
                    TestAutomationServiceListener listener = registry.getListener();

                    if (listener != null) {
                        listener.testStarted(test);
                    }
                }
            }
        }
    }

    /**
     * Called by receiver when specified test has finished.
     * Please note, a "finished" test doesn't mean "successful" test results.
     * It just means that Test Automation Service has executed specified test
     * and test results are stored in the test issuer's workspace.
     *
     * @param message Message regarding the finished test
     */
    private synchronized void testFinished(TestOperation message) {

        if (message != null) {
            Test test = message.getTest();

            if (test != null) {
                TestRegistry registry = getTestRegistry(test.getId());

                if (registry != null) {
                    TestAutomationServiceListener listener = registry.getListener();

                    if (listener != null) {
                        listener.testFinished(test);
                    }

                    // Remove corresponding test registry
                    executingTests.remove(registry);
                    p("Test '" + test.getId() + "' has finished. Test registry for the test was successfully removed");

                    performCleanup();
                }
            }
        }
    }

    /**
     * Called by receiver when specified test has failed.
     *
     * @param message Message regarding the failed test
     */
    private synchronized void testFailed(TestOperation message) {

        if (message != null) {
            Test test = message.getTest();

            if (test != null) {
                TestRegistry registry = getTestRegistry(test.getId());

                if (registry != null) {
                    TestAutomationServiceListener listener = registry.getListener();

                    if (listener != null) {
                        listener.testFailed(test, test.getStatusDetails());
                    }

                    // Remove corresponding test registry
                    executingTests.remove(registry);
                    p("Test '" + test.getId() + "' had a failure. Test registry for the test was successfully removed");

                    performCleanup();
                }
            }
        }
    }

    /**
     * Called by receiver when a text message is received and should be handled.
     *
     * @param message A received text message to be handled
     */
    protected synchronized void handleTextMessage(TextMessage message) {

        if (message != null) {
            Test test = message.getTest();

            if (test != null) {
                TestRegistry registry = getTestRegistry(test.getId());

                if (registry != null) {
                    TestAutomationServiceListener testListener = registry.getListener();

                    if (testListener != null) {
                        testListener.messageFromTestAutomationService(registry.getTest(), message.getText().trim());
                    }
                }
            }
        }
    }

    /**
     * Called up on receiving of updated test description.
     *
     * @param message Test operation message containing updated test description
     */
    private synchronized void testUpdated(TestOperation message) {
        if (message != null) {
            Test test = message.getTest();

            if (test != null) {
                TestRegistry registry = getTestRegistry(test.getId());

                if (registry != null) {
                    registry.setTest(test);
                    p("Refreshed the test '" + test.getId() + "' according to received updates");
                }
            }
        }
    }

    /**
     * Handles disconnection of a remote Test Automation Service.
     *
     * @param disconnectedService
     */
    protected synchronized void handleRemoteServiceDisconnection(RemoteService disconnectedService) {

        if (disconnectedService != null) {
            String disconnectedServiceHostname = disconnectedService.getServiceHostname();
            int disconnectedServicePort = disconnectedService.getServicePort();

            for (RemoteService currentRemoteService : remoteServices) {
                if (currentRemoteService.getServiceHostname().equals(disconnectedServiceHostname)) {
                    if (currentRemoteService.getServicePort() == disconnectedServicePort) {

                        // Remove service from the list of available ones
                        remoteServices.remove(currentRemoteService);
                        p("Disconnected Test Automation Service at " + disconnectedServiceHostname + ":" + disconnectedServicePort
                          + " was successfully removed from the list of connected Test Automation Services");

                        // Notify all corresponding test listeners about failure

                        // Test registries cannot be removed right away,
                        // since more than one test registry may be using a disconnected Test Automation Service
                        List<TestRegistry> registriesToBeRemoved = new ArrayList<TestRegistry>(0);

                        for (TestRegistry currentTestRegistry : executingTests) {
                            if (currentTestRegistry.getRemoteServiceHostname().equals(disconnectedServiceHostname)) {
                                if (currentTestRegistry.getRemoteServicePort() == disconnectedServicePort) {
                                    TestAutomationServiceListener listener = currentTestRegistry.getListener();

                                    if (listener != null) {
                                        listener.testFailed(currentTestRegistry.getTest(),
                                                            "Test Automation Service at " + currentTestRegistry.getRemoteServiceHostname()
                                                            + ":" + currentTestRegistry.getRemoteServicePort() + " is not online anymore");

                                        registriesToBeRemoved.add(currentTestRegistry);
                                    }
                                }
                            }
                        }

                        if (executingTests.removeAll(registriesToBeRemoved)) {
                            p("All test registries under disconnected Test Automation Service were successfully removed from the list of executing tests");
                        }

                        performCleanup();

                        break;
                    }
                }
            }
        }
    }

    /**
     * Checks if Test Automation Client could be stopped.
     */
    private synchronized void performCleanup() {

        if (executingTests.isEmpty()) {
            p("The Test Automation Client at " + clientHostname + ":" + clientPort + " is not executing any tests. Stop working");
            stopWorking();
        }
    }

    /**
     * Returns instance of the file sender.
     *
     * @return Instance of the file sender
     */
    protected FileSender getFileSender() {
        return fileSender;
    }

    /**
     * Returns release version.
     *
     * @return Release version
     */
    public synchronized String getVersion() {
        return Constant.TEST_AUTOMATION_RELEASE_VERSION;
    }

    /**
     * Prints help message to console.
     */
    private synchronized static void printHelp() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\n\n Test Automation Client usage: java -jar TestAutomationClient.jar "
                + Constant.TEST_AUTOMATION_CLIENT_PORT_NUMBER_ARGUMENT + "=" + Constant.TEST_AUTOMATION_CLIENT_DEFAULT_PORT_NUMBER + "\n\n");
        stringBuilder.append(" Where " + Constant.TEST_AUTOMATION_CLIENT_PORT_NUMBER_ARGUMENT + " is a port number that Test Automation Client is using for all incoming messages\n\n");
        stringBuilder.append(" You can also type just: java -jar TestAutomationClient.jar \n\n");
        stringBuilder.append(" That will start Test Automation Client on this host under default port number " + Constant.TEST_AUTOMATION_CLIENT_DEFAULT_PORT_NUMBER + "\n\n");

        System.out.println(stringBuilder.toString());
    }

    /**
     * Used for debug or informal message prints to console.
     *
     * @param text A message to be printed on console
     */
    public synchronized void p(String text) {
        if (printStream != null) {
            printStream.println("TAC: " + clientHostname + ":" + clientPort + ": " + text);
        } else {
            System.out.println("TAC: " + clientHostname + ":" + clientPort + ": " + text);
        }
    }

    /**
     * Launch Test Automation Client as a standalone application.
     *
     * @param arguments Startup arguments used for Test Automation Client configuration
     */
    public static void main(String[] arguments) {

        String hostname = null;

        try {
            hostname = Util.getValidHostIp();//java.net.InetAddress.getLocalHost().getCanonicalHostName();
            System.out.println("RETURNED hostname is " + hostname);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (hostname == null || hostname.isEmpty()) {
            hostname = (String) System.getenv("HOST");
            System.out.println("HOST for hostname is " + hostname);
        }

        // Start demo
        try {
            TASListenerExampleImplementation example = new TASListenerExampleImplementation();
            example.start();
        }
        catch (Exception e) {
            System.out.println("Got troubles while tried to run TAS example: " + e.getClass());
            e.printStackTrace();
        }
    }
}

// A simple example of how TestAutomationServiceListener could be used
class TASListenerExampleImplementation extends Thread implements TestAutomationServiceListener {

    private TestAutomationClient client;
    private boolean isRunning = false;
    private java.util.HashMap<String, Test> executingTests;

    public TASListenerExampleImplementation() {
        isRunning = true;
        executingTests = new java.util.HashMap<String, Test>(0);
    }

    @Override
    public void run() {
        try {
            client = new TestAutomationClient(System.out);
            p("New client created on " + client.getHostname() + ":" + client.getPort());

            Random random = new Random();

            Test.ProductReleasingMode productReleasingMode = Test.ProductReleasingMode.AUTOMATICALLY_RELEASE_RESERVED_PRODUCTS;

            long currenTime = System.currentTimeMillis();

            for (int i = 0; i < 1; i++) {
                Test.Target target = Test.Target.FLASH;

                productReleasingMode = Test.ProductReleasingMode.AUTOMATICALLY_RELEASE_RESERVED_PRODUCTS;
                Test test = new Test("Retry test #" + i, productReleasingMode, target);

                test.setWorkspacePath("/nokia/ou_nmp/groups/tas/demo");
                test.setTimeout(2 * Constant.ONE_HOUR);
                test.setProductDisconnectionTimeout(Constant.FIVE_MINUTES);

                test.setExecutorScript("execute.py");
                test.addArtifact("data.zip");
                /*test.addArtifact("granite.zip");
                test.addArtifact("FuseHandler.dll");
                test.addArtifact("FuseLib.dll");*/

                test.setResultsFilename("results.zip");

                for (int p = 1; p <= 2; p++) {
                    TestPackage testPackage = new TestPackage("tests_" + p);
                    testPackage.addFile("tests_" + p + ".zip");
                    testPackage.addFile("tests_" + p + ".xml");
                    testPackage.setRequiredEnvironment("(rm-code:RM-902;)");

                    test.addTestPackage(testPackage);
                }

                executingTests.put(test.getId(), test);

                p("Starting test:\n" + test + "\n");

                // Start test on sanbox TAS
                //client.startTest(test, "oucitas01.europe.nokia.com", 33333, this);
                client.startTest(test, "4FID17569.NOE.nokia.com", 33333, this);
            }

            while (isRunning) {
                sleep(Constant.DECISECOND);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void testStarted(Test startedTest) {
        p("\n\n\n\n\n\n\n\n >>>>>>>>>>>>>>>>>>>>>>>>>>> TEST STARTED: " + startedTest + "\n\n\n\n\n\n\n\n");

        if (executingTests.containsKey(startedTest.getId())) {
            executingTests.put(startedTest.getId(), startedTest);
        }
    }

    @Override
    public void testFinished(Test finishedTest) {
        p("\n\n\n\n\n\n\n\n >>>>>>>>>>>>>>>>>>>>>>>>>>> TEST FINISHED: test ID = " + finishedTest.getId() + "\n\n\n\n\n\n\n\n");

        Test localCopy = null;

        if (executingTests.containsKey(finishedTest.getId())) {
            localCopy = executingTests.get(finishedTest.getId());

            if (localCopy.getProductReleasingMode() == Test.ProductReleasingMode.MANUALLY_RELEASE_RESERVED_PRODUCTS) {
                p("\n\n\n >>>>>>>>>>>>>>>>>>>>>>>>>>> SETTING ALL RESERVED PRODUCTS FREE \n\n\n");
                client.freeProducts(localCopy.getReservedProducts());
            }

            executingTests.remove(finishedTest.getId());
        }

        // Put statistics into workspace directory - a simple text file, indicating what's ended and when
        try {
            File debugFile = new File(finishedTest.getWorkspacePath() + System.getProperty("file.separator") + finishedTest.getId() + "_FINISHED_at_" + System.currentTimeMillis());
            if (!debugFile.exists()) {
                if (debugFile.createNewFile()) {
                    // Write the reason of failure into this file
                    PrintWriter fileWriter = new PrintWriter(debugFile);
                    if (localCopy != null) {
                        fileWriter.append(localCopy.toString());
                    }
                    fileWriter.close();
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        if (executingTests.isEmpty()) {
            isRunning = false;
        }
    }

    @Override
    public void testFailed(Test failedTest, String reason) {
        p("\n\n\n\n\n\n\n\n >>>>>>>>>>>>>>>>>>>>>>>>>>> TEST FAILED: test ID = " + failedTest.getId() + ", Reason: " + reason + "\n\n\n\n\n\n\n\n");

        if (executingTests.containsKey(failedTest.getId())) {
            Test test = executingTests.get(failedTest.getId());

            if (test.getProductReleasingMode() == Test.ProductReleasingMode.MANUALLY_RELEASE_RESERVED_PRODUCTS) {
                p("\n\n\n >>>>>>>>>>>>>>>>>>>>>>>>>>> SETTING ALL RESERVED PRODUCTS FREE \n\n\n");
                client.freeProducts(test.getReservedProducts());
            }

            executingTests.remove(failedTest.getId());
        }

        // Put statistics into workspace directory - a simple text file, indicating what's ended and when
        try {
            File debugFile = new File(failedTest.getWorkspacePath() + System.getProperty("file.separator") + failedTest.getId() + "_FAILED_at_" + System.currentTimeMillis());
            if (!debugFile.exists()) {
                if (debugFile.createNewFile()) {
                    // Write the reason of failure into this file
                    PrintWriter fileWriter = new PrintWriter(debugFile);
                    fileWriter.append(reason);
                    fileWriter.close();
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        if (executingTests.isEmpty()) {
            isRunning = false;
        }
    }

    @Override
    public void messageFromTestAutomationService(Test test, String message) {
        p("Message from Test Automation Service about test " + test.getRuntimeId() + ": " + message);
    }

    @Override
    public InputStream readFile(String directoryPath, String fileName) {
        p("readFile(" + directoryPath + ", " + fileName + ") is called");
        return null;
    }

    @Override
    public OutputStream createFile(String directoryPath, String fileName) {
        p("createFile(" + directoryPath + ", " + fileName + ") is called");
        return null;
    }

    public void p(String message) {
        System.out.println("TASListenerExampleImplementation: " + message);
    }
}
