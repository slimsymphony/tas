package com.nokia.ci.tas.commons;

/**
 * Incapsulates all information related to file descriptions used inside the Test Automation Service.
 *
 * The XML format used for representing a file description:

    <file>
        <!-- Name of the file together with its extension -->
        <name>filename.ext</name>
        <!-- Absolute or related path to the file, if any -->
        <path>/path/to/file/</path>
        <!-- Number of bytes in file or -1 if size is unknow -->
        <size>123456</size>
    </file>
 */
public class FileDescription {

    /**
     * XML tag indicating a file description.
     */
    public static final String XML_ELEMENT_FILE = "file";

    /**
     * XML tag indicating a name of the file.
     */
    public static final String XML_ELEMENT_FILENAME = "name";

    /**
     * XML tag indicating a path to the file, if available.
     */
    public static final String XML_ELEMENT_FILEPATH = "path";

    /**
     * XML tag indicating a size of the file.
     */
    public static final String XML_ELEMENT_FILESIZE = "size";

    /**
     * Indication about unknown file size.
     */
    public static final long UNKNOWN_FILE_SIZE = -1;

    /**
     * Name of the file.
     */
    private String fileName;

    /**
     * Path to the file, if available.
     */
    private String filePath;

    /**
     * Size of the file in bytes.
     */
    private long fileSize = UNKNOWN_FILE_SIZE;

    /**
     * Constructor.
     */
    public FileDescription() {
    }

    /**
     * Sets name of file.
     *
     * @param fileName Name of file
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns name of file.
     *
     * @return Name of file
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets path to the file.
     *
     * @param filePath Path to the file
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Returns path to the file.
     *
     * @return Path to the file
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Sets size of file (in number of bytes) or UNKNOWN_FILE_SIZE.
     *
     * @param fileSize Size of file (in number of bytes) or UNKNOWN_FILE_SIZE
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * Returns size of file (in number of bytes) or UNKNOWN_FILE_SIZE.
     *
     * @return Size of file (in number of bytes) or UNKNOWN_FILE_SIZE
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Returns a textual representation of the file description.
     *
     * @return A textual representation of the file description
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("\n\n File description:");

        if (fileName != null && !fileName.isEmpty()) {
            string.append("\n\t File name:         " + fileName);
        }

        if (filePath != null && !filePath.isEmpty()) {
            string.append("\n\t File path:         " + filePath);
        }

        if (fileSize == UNKNOWN_FILE_SIZE) {
            string.append("\n\t File size:         UNKNOWN");
        } else {
            string.append("\n\t File size:         " + fileSize + " bytes");
        }

        return string.toString();
    }

    /**
     * Returns XML representation of the file description.
     *
     * @return XML representation of the file description
     */
    public String toXML() {
        return toXML("");
    }

    /**
     * Returns XML representation of the file description with specified indentation.
     *
     * @param indentation Indentation to be used in XML outputs
     * @return XML representation of the file description with specified indentation
     */
    public String toXML(String indentation) {
        StringBuilder xml = new StringBuilder();

        xml.append(indentation + "<" + XML_ELEMENT_FILE + ">\n");

        if (fileName != null) {
            xml.append(indentation + "\t<" + XML_ELEMENT_FILENAME + ">" + fileName + "</" + XML_ELEMENT_FILENAME + ">\n");
        }

        if (filePath != null) {
            xml.append(indentation + "\t<" + XML_ELEMENT_FILEPATH + ">" + filePath + "</" + XML_ELEMENT_FILEPATH + ">\n");
        }

        xml.append(indentation + "\t<" + XML_ELEMENT_FILESIZE + ">" + fileSize + "</" + XML_ELEMENT_FILESIZE + ">\n");

        xml.append(indentation + "</" + XML_ELEMENT_FILE + ">\n");

        return xml.toString();
    }
}
