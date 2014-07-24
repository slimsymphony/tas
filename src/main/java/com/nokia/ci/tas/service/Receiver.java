package com.nokia.ci.tas.service;

import java.io.InputStream;

import java.net.Socket;
import java.net.ServerSocket;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Converter;

import com.nokia.ci.tas.commons.message.ProductOperation;
import com.nokia.ci.tas.commons.message.RegistryOperation;
import com.nokia.ci.tas.commons.message.TestOperation;

/**
 * Handles and dispatches all messages received by the instance of Testing Automation Service.
 */
public class Receiver extends Thread {

    /**
     * Listening socket for all incoming messages.
     */
    private ServerSocket listener;

    /**
     * Variable which keeps receiver running.
     */
    private boolean isRunning = true;

    /**
     * Current instance of the Test Automation Service.
     */
    private TestAutomationService testAutomationService;

    /**
     * Converter of input XML streams.
     */
    private Converter converter;

    /**
     * Handler of incoming HTTP streams.
     */
    private HttpHandler httpHandler;

    /**
     * Instance of the Test Automation Service's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationService.GLOBAL_LOGGER_NAME);

    /**
     * Default constructor.
     *
     * @param testAutomationService Instance of running Test Automation Service
     * @param listener Instance of service socket for listening all incoming messages
     */
    public Receiver(TestAutomationService testAutomationService, ServerSocket listener) {

        super(); // Start as anonymous thread

        this.testAutomationService = testAutomationService;

        this.listener = listener;

        converter = new Converter();

        httpHandler = new HttpHandler(testAutomationService);

        setPriority(Thread.MIN_PRIORITY); // Always work with minimal priority
    }

    /**
     * Receiver's main routine.
     */
    @Override
    public void run() {
        p("Started working");

        httpHandler.start();

        while (isRunning) {
            try {
                // Accept incoming request
                Socket connection = listener.accept();

                // Always ensure that connection is still alive, since it may be accepted after being closed
                if (!connection.isClosed()) {
                    connection.setSoLinger(true, 10); // Socket's close() is allowed to block for 10 seconds in the worst case
                    connection.setTcpNoDelay(true); // Less buffering, more packets

                    InputStream inputStream = null;
                    boolean immediatelyCloseConnection = true;

                    try {
                        inputStream = connection.getInputStream();
                        Object message = null;

                        // Parse the first line
                        int ch = -1;
                        StringBuffer buffer = new StringBuffer();

                        do {
                            ch = inputStream.read();

                            if (ch != '\n' && ch != -1) {
                                buffer.append(Character.toChars(ch));
                            } else {
                                buffer.trimToSize();
                                break;
                            }
                        } while (ch != -1);

                        String request = buffer.toString();

                        if (request != null && !request.isEmpty()) {
                            // XML messages always start with the <?xml declaration
                            if (request.startsWith("<?")) {

                                message = converter.handle(inputStream);

                                if (message != null) {

                                    if (message instanceof ProductOperation) {

                                        testAutomationService.handleProductOperation((ProductOperation) message);

                                    } else if (message instanceof TestOperation) {

                                        testAutomationService.handleTestOperation((TestOperation) message);

                                    } else if (message instanceof RegistryOperation) {

                                        testAutomationService.handleRegistryOperation((RegistryOperation) message);
                                    }

                                    // Any other types of messages are just ignored

                                    else {
                                        p("Warning: received message is of type " + message.getClass().getCanonicalName() + " and is not supported!");
                                    }
                                }

                                if (inputStream != null) {
                                    inputStream.close();
                                }

                                connection.close();

                            } else if (request.contains("HTTP") || request.contains("http")) {
                                // Otherwise we are interpreting them as plain HTTP messages
                                // The input stream and connection will be closed by the handler

                                // Don't close connections, input or output streams immediately here,
                                // since HTTP 1.1 is using pervasive connections and HTTP handler will make all necessary cleanups
                                immediatelyCloseConnection = false;

                                // Forward connection processing to HTTP handler
                                httpHandler.handle(connection, request.trim());

                            } else {
                                p("Remote part at " + connection.getInetAddress() + " has issued unsupported request: " + request);

                                if (inputStream != null) {
                                    inputStream.close();
                                }

                                connection.close();
                            }
                        }
                    } catch (Exception e) {
                        p("Got troubles during processing incoming connection from "
                            + connection.getInetAddress().getHostName() + ":" + connection.getPort()
                            + " (" + connection.getInetAddress().getHostAddress() + ":" + connection.getPort() + ") - "
                            + e.getClass() + " - " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        if (immediatelyCloseConnection) {
                            // Always ensure that input stream is closed
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (Exception e) {
                                    p("Got troubles while tried to close input stream from "
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
                }
                
                sleep(Constant.MILLISECOND); // Wait for any updates

            } catch (Exception e) {
                p("Got troubles while tried to handle incoming connections: " + e.getMessage());
                e.printStackTrace();
                isRunning = false;
            }
        }
    }

    /**
     * Stops receiver running.
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
        logger.log(Level.ALL, "Receiver: " + text);
    }
}
