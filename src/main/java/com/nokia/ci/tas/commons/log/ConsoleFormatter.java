package com.nokia.ci.tas.commons.log;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formatter of all text log messages issued from some component to console.
 */
public class ConsoleFormatter extends Formatter {

    /**
     * Default constructor.
     */
    public ConsoleFormatter() {
    }

    /**
     * Implementation of the single record formatting.
     *
     * @param record A record given for formatting
     * @return A string representing a formatted record.
     */
    @Override
    public String format(LogRecord record) {
        // By default only a message will be send to the console
        return record.getMessage() + "\n";
    }
}
