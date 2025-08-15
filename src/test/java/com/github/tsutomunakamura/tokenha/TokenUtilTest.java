package com.github.tsutomunakamura.tokenha;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for TokenUtil.
 */
public class TokenUtilTest {
    
    @Test
    public void testIsValidToken_ValidToken_ReturnsTrue() {
        assertTrue(TokenUtil.isValidToken("valid-token"));
        assertTrue(TokenUtil.isValidToken("  valid-token  "));
    }
    
    @Test
    public void testIsValidToken_InvalidToken_ReturnsFalse() {
        assertFalse(TokenUtil.isValidToken(null));
        assertFalse(TokenUtil.isValidToken(""));
        assertFalse(TokenUtil.isValidToken("   "));
    }
    
    @Test
    public void testGenerateToken_ValidPrefix_ReturnsFormattedToken() {
        String token = TokenUtil.generateToken("TEST");
        assertNotNull(token);
        assertTrue(token.startsWith("TEST-"));
        assertTrue(token.length() > 5); // "TEST-" + timestamp
    }
    
    @Test
    public void testGenerateToken_NullPrefix_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            TokenUtil.generateToken(null);
        });
    }
    
    @Test
    public void testGenerateToken_EmptyPrefix_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            TokenUtil.generateToken("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            TokenUtil.generateToken("   ");
        });
    }
}