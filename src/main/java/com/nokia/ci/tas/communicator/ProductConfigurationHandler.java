package com.nokia.ci.tas.communicator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.nokia.ci.tas.commons.Constant;
import com.nokia.ci.tas.commons.Converter;
import com.nokia.ci.tas.commons.Product;
import com.nokia.ci.tas.commons.SimCard;

/**
 * Handler of product configurations used in Test Automation Communicators.
 */
public class ProductConfigurationHandler {

    /**
     * Path to product configurations directory.
     */
    private File productConfigurationsDirectory;

    /**
     * Instance of the Test Automation Communicator's global logger.
     */
    private Logger logger = Logger.getLogger(TestAutomationCommunicator.GLOBAL_LOGGER_NAME);

    /**
     * Creates product configuration handler.
     *
     * @param productConfigurationsDirectory Path to the product configurations directory
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public ProductConfigurationHandler(File productConfigurationsDirectory) throws IOException,
                                                                                   ParserConfigurationException,
                                                                                   SAXException {
        this.productConfigurationsDirectory = productConfigurationsDirectory;
    }

    /**
     * Updates the specified product by checking contents of a configuration file.
     *
     * @param product Product to be updated
     * @return Instance of updated product
     */
    public Product update(Product product) {
        String imei = product.getIMEI();

        if (imei != null && !imei.isEmpty()) {
            // Check if a corresponding file is existing
            try {
                File configurationFile = new File(productConfigurationsDirectory + System.getProperty("file.separator") + imei + ".xml");
                if (!configurationFile.exists()) {
                    if (configurationFile.createNewFile()) {
                        p("A new configuraiton file was created for product at " + configurationFile.getAbsolutePath());

                        // Store there all the information we have gathered so far about the product
                        try {
                            PrintWriter productConfiguration = new PrintWriter(configurationFile, "UTF-8");
                            productConfiguration.append(Constant.XML_DECLARATION + "\n" + product.toXML());
                            productConfiguration.flush();
                            productConfiguration.close();
                            p("A product configuration file is created at " + configurationFile.getAbsolutePath());

                        } catch (Exception e) {
                            // This issue is not a critical one, but we should explain
                            p("Warning: Got some troubles while tried to update a product configuration file at " + configurationFile.getAbsolutePath());
                        }
                    } else {
                        p("Warning: Couldn't create a product configuration file at " + configurationFile.getAbsolutePath());
                    }
                } else {
                    // Try to update product's information from this file
                    try {
                        FileInputStream fileInputStream = null;
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((int) configurationFile.length());

                        try {
                            fileInputStream = new FileInputStream(configurationFile);
                            byte[] buffer = new byte[Constant.DEFAULT_BUFFER_SIZE];
                            int numberOfBytesInBuffer = 0;
                            while ((numberOfBytesInBuffer = fileInputStream.read(buffer)) != -1) {
                                byteArrayOutputStream.write(buffer, 0, numberOfBytesInBuffer);
                            }
                        } finally {
                            if (fileInputStream != null) {
                                fileInputStream.close();
                            }
                        }

                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                        // Create converters on the fly to avoid any issues related to asynchronizided parsing of file data
                        Converter converter = new Converter();
                        Object parsedObject = converter.handle(byteArrayInputStream);

                        if (parsedObject != null) {
                            if (parsedObject instanceof Product) {
                                Product parsedProduct = (Product) parsedObject;
                                boolean updateConfigurationFile = false;

                                // Try to update parameters for the SIM cards from the configuration file
                                SimCard sim1 = product.getSim1();
                                SimCard sim2 = product.getSim2();

                                SimCard parsedSim1 = parsedProduct.getSim1();
                                SimCard parsedSim2 = parsedProduct.getSim2();

                                if (!parsedSim1.equals(sim1)) {
                                    // Always set SIM cards data from the configuration file
                                    product.setSim1(parsedSim1);
                                    p("The 1st SIM card data was updated from product's configuration file. Used SIM card data was:\n" + parsedSim1);
                                }

                                if (!parsedSim2.equals(sim2)) {
                                    // Always set SIM cards data from the configuration file
                                    product.setSim2(parsedSim2);
                                    p("The 2nd SIM card data was updated from product's configuration file. Used SIM card data was:\n" + parsedSim2);
                                }

                                // Try to update hardware type from the configuration file
                                product.setHardwareType(parsedProduct.getHardwareType());

                                // Try to update product role from the configuration file
                                product.setRole(parsedProduct.getRole());

                                // Try to update product's description of the environment the configuration file
                                product.setEnvironment(parsedProduct.getEnvironment());

                                // Now check if some parameters should be stored in configuration file

                                // The following parameters are always up to date on the side of Test Automation Communicator:
                                // FUSE connection name
                                // FUSE connection id
                                // Test Automation Communicator's hostname
                                // Product IP address
                                // Product IP port number
                                // Test Automation Service's hostname
                                // Test Automation Service's port number

                                String parsedFuseConnectionName = parsedProduct.getFuseConnectionName();
                                if (!product.getFuseConnectionName().equals(parsedFuseConnectionName)) {
                                    p("Product's FUSE connection name is not up to date");
                                    updateConfigurationFile = true;
                                }

                                String parsedFuseConnectionId = parsedProduct.getFuseConnectionId();
                                if (!product.getFuseConnectionId().equals(parsedFuseConnectionId)) {
                                    p("Product's FUSE connection id is not up to date");
                                    updateConfigurationFile = true;
                                }

                                String parsedHostname = parsedProduct.getHostname();
                                if (!product.getHostname().equals(parsedHostname)) {
                                    p("Product's hostname is not up to date");
                                    updateConfigurationFile = true;
                                }

                                String parsedIPAddress = parsedProduct.getIPAddress();
                                if (!product.getIPAddress().equals(parsedIPAddress)) {
                                    p("Product's IP address is not up to date");
                                    updateConfigurationFile = true;
                                }

                                String parsedPort = parsedProduct.getPort();
                                if (!product.getPort().equals(parsedPort)) {
                                    p("Product's IP port number is not up to date");
                                    updateConfigurationFile = true;
                                }

                                String parsedTestAutomationServiceHostname = parsedProduct.getTestAutomationServiceHostname();
                                if (!product.getTestAutomationServiceHostname().equals(parsedTestAutomationServiceHostname)) {
                                    p("Product's Test Automation Service hostname is not up to date");
                                    updateConfigurationFile = true;
                                }

                                int parsedTASPort = parsedProduct.getTestAutomationServicePort();
                                if (product.getTestAutomationServicePort() != parsedTASPort) {
                                    p("Product's Test Automation Service port number is not up to date");
                                    updateConfigurationFile = true;
                                }

                                Product.Status parsedProductStatus = parsedProduct.getStatus();
                                if (parsedProductStatus != product.getStatus()) {
                                    p("Product's status is not up to date");
                                    updateConfigurationFile = true;
                                }

                                String parsedProductStatusDetails = parsedProduct.getStatusDetails();
                                if (!product.getStatusDetails().equals(parsedProductStatusDetails)) {
                                    p("Product's status details are not up to date");
                                    updateConfigurationFile = true;
                                }
                                
                                String parsedTraceConnectionId = parsedProduct.getTraceConnectionId();
                                if( !product.getTraceConnectionId().equals( parsedTraceConnectionId ) ) {
                                	p("Product's trace connection are not up to date");
                                	updateConfigurationFile = true;
                                }
                                
                                if (updateConfigurationFile) {
                                    p("A product configuration file at " + configurationFile.getAbsolutePath() + " contains some outdated information and should be updated");
                                    p("Product data to be stored in configuration file will be:\n" + product);

                                    // Product object already contains the correct information,
                                    // so we are simply overwriting the configuration file
                                    PrintWriter productConfiguration = null;
                                    try {
                                        productConfiguration = new PrintWriter(configurationFile, "UTF-8");
                                        productConfiguration.append(Constant.XML_DECLARATION + "\n" + product.toXML());
                                        productConfiguration.flush();
                                        productConfiguration.close();
                                        p("Product configuration data is successfully stored in file " + configurationFile.getAbsolutePath());
                                    } finally {
                                        if (productConfiguration != null) {
                                            productConfiguration.close();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // This issue is not a critical one, but we should complain anyway
                        p("Warning: Got some troubles while tried to update a product configuration file at " + configurationFile.getAbsolutePath());
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                p("Error: Couldn't update information of the product with IMEI " + imei + " due to: " + e.getClass() + " - " + e.getMessage());
            }
        }

        return product;
    }

    private void p(String text) {
        logger.log(Level.ALL, "Product Configuration Handler: " + text);
    }
}
