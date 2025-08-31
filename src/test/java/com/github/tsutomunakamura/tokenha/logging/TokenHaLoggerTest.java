package com.github.tsutomunakamura.tokenha.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class TokenHaLoggerTest {
    
    @Test
    @DisplayName("getLogger() should return a default logger")
    public void testGetDefaultLogger() {
        Logger logger = TokenHaLogger.getDefaultLogger();
        assertNotNull(logger);
        assertEquals("com.github.tsutomunakamura.tokenha", logger.getName());
    }

    @Test
    @DisplayName("getLogger(Class) should return a logger for the specified class")
    public void testGetLoggerForClass() {
        Logger logger = TokenHaLogger.getLogger(TokenHaLoggerTest.class);
        assertNotNull(logger);
        assertEquals(TokenHaLoggerTest.class.getName(), logger.getName());
    }

    @Test
    @DisplayName("isDebugEnabled() should return a state of debug logging")
    public void testIsDebugEnabled() {
        Logger logger = TokenHaLogger.getDefaultLogger();
        boolean isDebugEnabled = TokenHaLogger.isDebugEnabled();
        assertEquals(logger.isDebugEnabled(), isDebugEnabled);
    }

    @Test
    @DisplayName("isTraceEnabled() should return a state of trace logging")
    public void testIsTraceEnabled() {
        Logger logger = TokenHaLogger.getDefaultLogger();
        boolean isTraceEnabled = TokenHaLogger.isTraceEnabled();
        assertEquals(logger.isTraceEnabled(), isTraceEnabled);
    }
}
