package com.github.tsutomunakamura.tokenha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized logging utility for TokenHa library.
 * Provides adaptive logging that follows the user's existing logging configuration.
 * Automatically detects and uses the logging framework configured in the consuming project
 * (logback, log4j2, etc.) with fallback to simple console output.
 */
public class TokenHaLogger {
    
    /**
     * Get a logger for the specified class.
     * This follows SLF4J best practices and automatically adapts to the 
     * logging framework configured by the consuming application.
     * 
     * @param clazz the class for which to create a logger
     * @return a logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * Get a logger for the specified name.
     * 
     * @param name the logger name
     * @return a logger instance
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
    
    /**
     * Utility method for logging configuration errors with consistent formatting.
     * 
     * @param logger the logger to use
     * @param parameter the configuration parameter name
     * @param value the invalid value
     * @param defaultValue the default value being used
     */
    public static void logConfigurationError(Logger logger, String parameter, String value, Object defaultValue) {
        logger.warn("Invalid {} '{}', using default: {}", parameter, value, defaultValue);
    }
    
    /**
     * Utility method for logging file persistence operations.
     * 
     * @param logger the logger to use
     * @param operation the operation name (save, load, delete)
     * @param filePath the file path
     * @param details additional details about the operation
     */
    public static void logFilePersistence(Logger logger, String operation, String filePath, String details) {
        logger.debug("File {}: {} - {}", operation, filePath, details);
    }
    
    /**
     * Utility method for logging eviction thread operations.
     * 
     * @param logger the logger to use
     * @param message the log message
     * @param args message arguments
     */
    public static void logEvictionThread(Logger logger, String message, Object... args) {
        logger.debug(message, args);
    }
}
