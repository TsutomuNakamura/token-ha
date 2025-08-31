package com.github.tsutomunakamura.tokenha.core;

import java.util.List;
import com.github.tsutomunakamura.tokenha.core.element.TokenElement;

/**
 * Data class for JSON serialization/deserialization.
 * Simple wrapper for the tokens list.
 */
public class TokenData {
    private List<TokenElement> tokens;
    
    // Default constructor
    public TokenData() {}
    
    public TokenData(List<TokenElement> tokens) {
        this.tokens = tokens;
    }
    
    public List<TokenElement> getTokens() {
        return tokens;
    }
    
    public void setTokens(List<TokenElement> tokens) {
        this.tokens = tokens;
    }
}
