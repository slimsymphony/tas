package com.nokia.ci.tas.commons.log;

import java.text.SimpleDateFormat;

import java.util.Date;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formatter of all log files used by Test Automation Service and its components.
 */
public class LogFileFormatter extends Formatter {

    /**
     * Date format used for timestamping log messages.
     */
    private SimpleDateFormat dateFormat;

    /**
     * Default constructor.
     *
     * @param dateFormat Date format used for timestamping log messages
     */
    public LogFileFormatter(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * Implementation of the single record formatting.
     *
     * @param record A record given for formatting
     * @return A string representing a formatted record.
     */
    @Override
    public String format(LogRecord record) {
        return dateFormat.format(new Date(record.getMillis())) + " " + record.getMessage() + "\n";
    }
}
