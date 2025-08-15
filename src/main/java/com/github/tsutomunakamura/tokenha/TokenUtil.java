package com.github.tsutomunakamura.tokenha;

/**
 * A simple token handling utility class.
 */
public class TokenUtil {
    
    /**
     * Validates if a token is not null and not empty.
     * 
     * @param token the token to validate
     * @return true if the token is valid, false otherwise
     */
    public static boolean isValidToken(String token) {
        return token != null && !token.trim().isEmpty();
    }
    
    /**
     * Generates a simple token with the given prefix.
     * 
     * @param prefix the prefix for the token
     * @return a formatted token string
     */
    public static String generateToken(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Prefix cannot be null or empty");
        }
        return prefix + "-" + System.currentTimeMillis();
    }
}