package com.nokia.ci.tas.commons.log;

import java.io.File;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.TimerTask;

import java.util.logging.Formatter;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nokia.ci.tas.commons.Util;

/**
 * Handler of the log file switches up on requests from the Timer.
 */
public class LogFileSwitcher extends TimerTask {

    /**
     * Constant value indicating that no cleanup is required for log files.
     */
    public static final long NO_CLEANUP = -1;

    /**
     * Instance of the logger.
     */
    private Logger logger;

    /**
     * Reference to the directory where all logs should be stored.
     */
    private File logsDirectory;

    /**
     * Log file's formatter.
     */
    private Formatter formatter;

    /**
     * Format used for log files
     */
    private String logFilenameFormat;

    /**
     * Cleanup period in milliseconds.
     * If no cleanup is required, than this value should be assigned to NO_CLEANUP.
     */
    private long cleanupPeriod = NO_CLEANUP;

    /**
     * Default constructor.
     *
     * @param loggerName Name of this logger instance
     * @param logsDirectory Reference to the directory where all logs should be stored
     * @param formatter Log file's formatter
     * @param logFilenameFormat Format used for log files
     * @param cleanupPeriod Period of log directory cleanup in milliseconds. If -1 will be specified, the log directory will be never cleaned up
     */
    public LogFileSwitcher(String loggerName, File logsDirectory, Formatter formatter, String logFilenameFormat, long cleanupPeriod) {
        this.logger = Logger.getLogger(loggerName);
        this.logsDirectory = logsDirectory;
        this.formatter = formatter;
        this.logFilenameFormat = logFilenameFormat;

        if (cleanupPeriod > 0L) {
            this.cleanupPeriod = cleanupPeriod;
        }
    }

    /**
     * Implementation of the log switching.
     */
    @Override
    public void run() {
        try {
            // Create a new log file handler
            SimpleDateFormat logNameFormat = new SimpleDateFormat(logFilenameFormat);
            File logFile = new File(logsDirectory.getAbsolutePath() + System.getProperty("file.separator") + logNameFormat.format(new Date()) + ".log");

            p("Trying to access log file at " + logFile.getAbsolutePath());

            if (!logFile.exists()) {
                if (logFile.createNewFile()) {
                    p("A new log file was successfully created at " + logFile.getAbsolutePath());
                }
            }

            Handler fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
            fileHandler.setFormatter(formatter);

            // Remove old log file handlers and append the new one
            p("Removing old log file handlers...");

            Handler[] handlers = logger.getHandlers();
            for (Handler current : handlers) {
                if (current instanceof FileHandler) {
                    FileHandler currentFileHandler = (FileHandler) current;
                    currentFileHandler.close();
                    logger.removeHandler(currentFileHandler);

                    p("Has removed old log file handler");
                }
            }

            logger.addHandler(fileHandler);

            p("Has successfully added new log file handler");

            // Now, when log is successfully switched, try to perform a cleanup
            if (cleanupPeriod != NO_CLEANUP) {
                p("Trying to perform automatic cleanup of the logs directory. All logs older than " + Util.convert(cleanupPeriod) + " will be deleted...");
                long currentTime = System.currentTimeMillis();

                File[] logDirectoryEntries = logsDirectory.listFiles();

                if (logDirectoryEntries != null && logDirectoryEntries.length > 0) {
                    for (File logDirectoryEntry : logDirectoryEntries) {
                        long lastModified = logDirectoryEntry.lastModified();

                        if ((currentTime - lastModified) >= cleanupPeriod) {
                            p("Log directory contains an entry " + logDirectoryEntry.getAbsolutePath()
                                + " which was last modified at " + new Date(lastModified) + " and so, can be deleted");
                            if (deleteRecursively(logDirectoryEntry)) {
                                p("Log directory entry " + logDirectoryEntry.getAbsolutePath() + " was successfully deleted");
                            }
                        }
                    }
                } else {
                    p("Couldn't list entries in logs directory. Will skip automatic cleanup");
                }
            } else {
                p("Cleanup period was not specified. Skipping cleanups of the logs directory");
            }

        } catch (Exception e) {
            p("Got troubles while tried to switch log files: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Deletes all files and directories in specified directory path.
     *
     * @param path Directory path to be deleted
     * @return True if deletion was successful or false otherwise
     */
    private synchronized boolean deleteRecursively(File path) {
        boolean result = true;

        if (path.exists()) {
            if (path.isDirectory()) {

                // Delete each sub-element in the directory
                for (File pathElement : path.listFiles()) {
                    result = deleteRecursively(pathElement);
                }
            }

            // Delete the element itself
            result = result & path.delete();
        }

        return result;
    }

    /**
     * Debugging method for casual prints.
     *
     * @param text Text to be printed out
     */
    private void p(String text) {
        logger.log(Level.ALL, "LogFileSwitcher: " + text);
    }
}
