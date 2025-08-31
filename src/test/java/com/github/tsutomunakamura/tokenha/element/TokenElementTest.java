package com.github.tsutomunakamura.tokenha.element;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.tsutomunakamura.tokenha.element.TokenElement;

/**
 * Test class for TokenElement functionality.
 * Tests the getTimeMillis() method and related functionality.
 */
public class TokenElementTest {
    
    private TokenElement tokenElement;
    private static final String TEST_TOKEN = "testToken123";
    private static final long TEST_TIME_MILLIS = 1692186615000L; // Fixed timestamp for consistent testing
    
    @BeforeEach
    public void setUp() {
        tokenElement = new TokenElement(TEST_TOKEN, TEST_TIME_MILLIS);
    }
    
    // Test cases for getTimeMillis() method
    
    @Test
    @DisplayName("Test getTimeMillis returns correct timestamp")
    public void testGetTimeMillisReturnsCorrectValue() {
        System.out.println("🧪 TEST: getTimeMillis returns correct timestamp");
        
        long actualTimeMillis = tokenElement.getTimeMillis();
        
        assertEquals(TEST_TIME_MILLIS, actualTimeMillis, 
            "getTimeMillis should return the exact timestamp provided in constructor");
    }

    @Test
    @DisplayName("Test getTimeMillis returns different values for different tokens")
    public void testGetTokenReturnsCorrectValue() {
        System.out.println("🧪 TEST: getToken returns correct value");
        
        String actualToken = tokenElement.getToken();
        
        assertEquals(TEST_TOKEN, actualToken, 
            "getToken should return the exact token provided in constructor");
    }
}
