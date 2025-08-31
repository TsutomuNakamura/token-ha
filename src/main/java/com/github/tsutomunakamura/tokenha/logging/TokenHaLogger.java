package com.github.tsutomunakamura.tokenha.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized logging utility for TokenHa library.
 * 
 * This class provides adaptive logging that automatically uses whatever logging 
 * framework is available in the user's project:
 * - If Logback is present → uses Logback with user's configuration
 * - If Log4j2 is present → uses Log4j2 with user's configuration
 * - If java.util.logging is present → uses JUL
 * - If no logging framework → falls back to simple console output
 * 
 * Usage:
 * <pre>
 * private static final Logger logger = TokenHaLogger.getLogger(MyClass.class);
 * logger.info("This follows user's logging configuration");
 * logger.warn("Warning message");
 * logger.error("Error message", exception);
 * </pre>
 */
public final class TokenHaLogger {

    private TokenHaLogger() {
        // Prevent instantiation
    }
    
    // Default logger for the library
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger("com.github.tsutomunakamura.tokenha");

    /**
     * Get the default logger for the TokenHa library.
     *
     * @return Default logger for general library messages
     */
    public static Logger getDefaultLogger() {
        return DEFAULT_LOGGER;
    }

    /**
     * Get a logger for the specified class.
     * This follows standard SLF4J practices and will use whatever logging 
     * implementation is available in the user's project.
     * 
     * @param clazz The class to create a logger for
     * @return Logger instance that adapts to user's logging configuration
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * Check if debug logging is enabled.
     * This can be used to avoid expensive string operations when debug is disabled.
     * 
     * @return true if debug logging is enabled
     */
    public static boolean isDebugEnabled() {
        return DEFAULT_LOGGER.isDebugEnabled();
    }
    
    /**
     * Check if trace logging is enabled.
     * 
     * @return true if trace logging is enabled
     */
    public static boolean isTraceEnabled() {
        return DEFAULT_LOGGER.isTraceEnabled();
    }
}
