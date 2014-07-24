package com.nokia.ci.tas.commons;

import java.util.ArrayList;
import java.util.List;

/**
 * A single test package that could be executed in parallel or independently from each other.
 */
public class TestPackage {
    /**
     * XML tag indicating the test package description.
     */
    public static final String XML_ELEMENT_TEST_PACKAGE = "test-package";

    /**
     * XML tag indicating id of the test package.
     */
    public static final String XML_ELEMENT_ID = "id";

    /**
     * XML tag indicating the environment required by this test package.
     */
    public static final String XML_ELEMENT_REQUIRED_ENVIRONMENT = "required-environment";

    /**
     * XML tag indicating the list of files associated with this test package.
     */
    public static final String XML_ELEMENT_FILES = "files";

    /**
     * XML tag indicating a single file associated with this test package.
     */
    public static final String XML_ELEMENT_FILE = "file";

    /**
     * Id assigned to the test package.
     */
    private String id = "";

    /**
     * A list of files associated with this test package.
     */
    private List<String> files;

    /**
     * Environment required by this test package.
     */
    private String requiredEnvironment = "";

    /**
     * Constructor.
     *
     * @param id Id of the test package
     */
    public TestPackage(String id) {
        this.id = id;
        files = new ArrayList<String>(0);
        requiredEnvironment = "";
    }

    /**
     * Constructor.
     *
     * @param id Id of the test package
     * @param files A list of files associated with this test package
     * @param requiredEnvironment Environment required by this test package
     */
    public TestPackage(String id, List<String> files, String requiredEnvironment) {
        this(id);

        if (files != null) {
            this.files = files;
        }

        if (requiredEnvironment != null) {
            this.requiredEnvironment = requiredEnvironment;
        }
    }

    /**
     * Sets id of the test package.
     *
     * @param id Id of the test package
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns td of the test package.
     *
     * @return Id of the test package
     */
    public String getId() {
        return id;
    }

    /**
     * Sets a list of files associated with this test package.
     *
     * @param files A list of files associated with this test package
     */
    public void setFiles(List<String> files) {
        if (files != null) {
            this.files = files;
        }
    }

    public void addFile(String file) {
        if (file != null && !file.isEmpty()) {
            if (!files.contains(file)) {
                files.add(file);
            }
        }
    }

    /**
     * Returns a list of files associated with this test package.
     *
     * @return A list of files associated with this test package
     */
    public List<String> getFiles() {
        return files;
    }

    /**
     * Sets products and environment requirements demanded by this test package.
     *
     * @param requiredEnvironment Environmen required by this test package
     */
    public void setRequiredEnvironment(String requiredEnvironment) {
        if (requiredEnvironment != null) {
            this.requiredEnvironment = requiredEnvironment;
        }
    }

    /**
     * Returns products and environment requirements demanded by this test package.
     *
     * @return Products and environment requirements demanded by this test package
     */
    public String getRequiredEnvironment() {
        return requiredEnvironment;
    }

    /**
     * Returns textual representation of this test package object.
     *
     * @return Textual representation of this test package object
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("\n\t Test package:");
        string.append("\n\t\t Id:                   " + id);
        string.append("\n\t\t Required environment: " + requiredEnvironment);
        string.append("\n\t\t Contains " + files.size() + " files:");

        for (String file : files) {
            string.append("\n\t\t\t " + file);
        }

        return string.toString();
    }

    /**
     * Returns XML representation of the test.
     *
     * @return XML representation of the test
     */
    public String toXML() {
        return toXML("");
    }

    /**
     * Returns XML representation of the test with specified indentation.
     *
     * @param indentation Indentation to be used in XML outputs
     * @return XML representation of the test with specified indentation
     */
    public String toXML(String indentation) {
        StringBuilder xml = new StringBuilder();

        xml.append(indentation + "<" + XML_ELEMENT_TEST_PACKAGE + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_ID + ">" + id + "</" + XML_ELEMENT_ID + ">\n");
        xml.append(indentation + "\t<" + XML_ELEMENT_REQUIRED_ENVIRONMENT + ">" + requiredEnvironment + "</" + XML_ELEMENT_REQUIRED_ENVIRONMENT + ">\n");

        if (files != null && !files.isEmpty()) {
            xml.append(indentation + "\t<" + XML_ELEMENT_FILES + ">\n");
            for (String file : files) {
                if (file != null && !file.isEmpty()) {
                    xml.append(indentation + "\t\t<" + XML_ELEMENT_FILE + ">" + file + "</" + XML_ELEMENT_FILE + ">\n");
                }
            }
            xml.append(indentation + "\t</" + XML_ELEMENT_FILES + ">\n");
        }

        xml.append(indentation + "</" + XML_ELEMENT_TEST_PACKAGE + ">\n");

        return xml.toString();
    }
}
