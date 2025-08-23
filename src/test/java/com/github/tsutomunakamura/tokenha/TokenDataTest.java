package com.github.tsutomunakamura.tokenha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tsutomunakamura.tokenha.element.TokenElement;

/**
 * Test class for TokenData.
 */
public class TokenDataTest {
    
    private TokenData tokenData;
    private List<TokenElement> sampleTokens;
    
    @BeforeEach
    public void setUp() {
        tokenData = new TokenData();
        
        // Create sample tokens for testing
        sampleTokens = new ArrayList<>();
        sampleTokens.add(new TokenElement("token1", 1692186615000L));
        sampleTokens.add(new TokenElement("token2", 1692186616000L));
        sampleTokens.add(new TokenElement("token3", 1692186617000L));
    }
    
    // Test cases for "public void setTokens(List<TokenElement> tokens)"
    
    @Test
    public void testSetTokensWithValidList() {
        // Test setting tokens with a valid list
        tokenData.setTokens(sampleTokens);
        
        List<TokenElement> retrievedTokens = tokenData.getTokens();
        assertNotNull(retrievedTokens, "Tokens should not be null after setting");
        assertEquals(3, retrievedTokens.size(), "Should have 3 tokens");
        assertSame(sampleTokens, retrievedTokens, "Should return the same list reference");
        
        // Verify individual tokens
        assertEquals("token1", retrievedTokens.get(0).getToken());
        assertEquals(1692186615000L, retrievedTokens.get(0).getTimeMillis());
        assertEquals("token2", retrievedTokens.get(1).getToken());
        assertEquals(1692186616000L, retrievedTokens.get(1).getTimeMillis());
        assertEquals("token3", retrievedTokens.get(2).getToken());
        assertEquals(1692186617000L, retrievedTokens.get(2).getTimeMillis());
    }
}
