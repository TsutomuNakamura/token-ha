package com.github.tsutomunakamura.tokenha;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Properties;

/**
 * Test class demonstrating different configuration strategies for TokenHa.
 */
public class TokenHaConfigurationTest {
    
    private TokenHa tokenHa;
    
    @AfterEach
    void tearDown() {
        if (tokenHa != null) {
            tokenHa.deletePersistenceFile();
            tokenHa.close();
        }
    }
    
    @Test
    @DisplayName("Should use default configuration when no config provided")
    void shouldUseDefaultConfiguration() throws IOException {
        // When
        tokenHa = new TokenHa();
        
        // Then - verify default values are used
        assertEquals("tokenha-data.json", tokenHa.getPersistenceFilePath());
        // We can't directly test other values without getters, but we can test behavior
        assertFalse(tokenHa.isFilled(), "Queue should not be filled with default max tokens");
    }
    
    @Test
    @DisplayName("Should use builder pattern for programmatic configuration")
    void shouldUseBuilderPatternConfiguration() throws IOException {
        // Given
        TokenHaConfig config = new TokenHaConfig.Builder()
            .maxTokens(5)
            .coolTimeToAddMillis(500)
            .numberOfLastTokens(1)
            .expirationTimeMillis(30000)
            .persistenceFilePath("test-builder-tokens.json")
            .build();
        
        // When
        tokenHa = new TokenHa(config);
        
        // Then
        assertEquals("test-builder-tokens.json", tokenHa.getPersistenceFilePath());
        
        // Test that custom maxTokens is applied
        for (int i = 0; i < 5; i++) {
            tokenHa.addIfAvailable("token-" + i);
            if (i < 4) {
                try { Thread.sleep(600); } catch (InterruptedException e) { /* ignore */ }
            }
        }
        assertTrue(tokenHa.isFilled(), "Queue should be filled with custom max tokens (5)");
    }
    
    @Test
    @DisplayName("Should load configuration from properties")
    void shouldLoadFromProperties() throws IOException {
        // Given
        Properties props = new Properties();
        props.setProperty("tokenha.max.tokens", "3");
        props.setProperty("tokenha.cool.time.millis", "200");
        props.setProperty("tokenha.number.of.last.tokens", "1");
        props.setProperty("tokenha.expiration.time.millis", "25000");
        props.setProperty("tokenha.persistence.file.path", "test-props-tokens.json");
        
        TokenHaConfig config = TokenHaConfig.fromProperties(props);
        
        // When
        tokenHa = new TokenHa(config);
        
        // Then
        assertEquals("test-props-tokens.json", tokenHa.getPersistenceFilePath());
        
        // Test that custom maxTokens from properties is applied
        for (int i = 0; i < 3; i++) {
            tokenHa.addIfAvailable("token-" + i);
            if (i < 2) {
                try { Thread.sleep(250); } catch (InterruptedException e) { /* ignore */ }
            }
        }
        assertTrue(tokenHa.isFilled(), "Queue should be filled with properties max tokens (3)");
    }
    
    @Test
    @DisplayName("fromProperties() should throw IllegalArgumentException on invalid properties")
    void shouldHandleInvalidProperties() throws IOException {
        // Given
        Properties props = new Properties();
        props.setProperty("tokenha.max.tokens", "invalid");  // Invalid integer
        props.setProperty("tokenha.cool.time.millis", "10000");
        
        // When - should throw IllegalArgumentException when "tokenha.max.tokens" is invalid
        assertThrows(IllegalArgumentException.class, () -> {
            TokenHaConfig config = TokenHaConfig.fromProperties(props);
            tokenHa = new TokenHa(config);
        }, "Should throw IllegalArgumentException for invalid properties");

        // Given
        props.setProperty("tokenha.max.tokens", "3");
        props.setProperty("tokenha.cool.time.millis", "-100");    // Invalid negative value

        // When - should throw IllegalArgumentException when "tokenha.cool.time.millis" is negative
        assertThrows(IllegalArgumentException.class, () -> {
            TokenHaConfig config = TokenHaConfig.fromProperties(props);
            tokenHa = new TokenHa(config);
        }, "Should throw IllegalArgumentException for negative cool time");
    }
    
    @Test
    @DisplayName("Should validate configuration parameters")
    void shouldValidateConfigurationParameters() {
        // Test invalid max tokens
        assertThrows(IllegalArgumentException.class, () -> {
            new TokenHaConfig.Builder().maxTokens(-1).build();
        }, "Should reject negative max tokens");
        
        // Test invalid cool time
        assertThrows(IllegalArgumentException.class, () -> {
            new TokenHaConfig.Builder().coolTimeToAddMillis(-1).build();
        }, "Should reject negative cool time");
        
        // Test invalid expiration time
        assertThrows(IllegalArgumentException.class, () -> {
            new TokenHaConfig.Builder().expirationTimeMillis(-1).build();
        }, "Should reject negative expiration time");
        
        // Test invalid number of last tokens vs max tokens
        assertThrows(IllegalArgumentException.class, () -> {
            new TokenHaConfig.Builder()
                .maxTokens(5)
                .numberOfLastTokens(5)  // Must be less than max tokens
                .build();
        }, "Should reject numberOfLastTokens >= maxTokens");
        
        // Test null persistence file path
        assertThrows(IllegalArgumentException.class, () -> {
            new TokenHaConfig.Builder().persistenceFilePath(null).build();
        }, "Should reject null persistence file path");
        
        // Test empty persistence file path
        assertThrows(IllegalArgumentException.class, () -> {
            new TokenHaConfig.Builder().persistenceFilePath("").build();
        }, "Should reject empty persistence file path");
    }
    
    @Test
    @DisplayName("Should create default config properly")
    void shouldCreateDefaultConfig() throws IOException {
        // Given
        TokenHaConfig defaultConfig = TokenHaConfig.defaultConfig();
        
        // When
        tokenHa = new TokenHa(defaultConfig);
        
        // Then
        assertEquals("tokenha-data.json", tokenHa.getPersistenceFilePath());
        assertFalse(tokenHa.isFilled(), "Should not be filled with default settings");
    }
    
    @Test
    @DisplayName("Should reject null configuration")
    void shouldRejectNullConfiguration() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            try (TokenHa testTokenHa = new TokenHa(null)) {
                // This should throw before reaching here
            }
        }, "Should reject null configuration");
    }
    
    @Test
    @DisplayName("Should demonstrate custom cool time behavior")
    void shouldDemonstrateCustomCoolTime() throws IOException, InterruptedException {
        // Given - very short cool time for fast testing
        TokenHaConfig config = new TokenHaConfig.Builder()
            .coolTimeToAddMillis(100)  // 100ms cool time
            .persistenceFilePath("test-cooltime.json")
            .build();
        
        tokenHa = new TokenHa(config);
        
        // When
        boolean first = tokenHa.addIfAvailable("token1");
        boolean second = tokenHa.addIfAvailable("token2");  // Should fail - too soon
        
        Thread.sleep(150);  // Wait for cool time to pass
        
        boolean third = tokenHa.addIfAvailable("token3");  // Should succeed
        
        // Then
        assertTrue(first, "First token should be added");
        assertFalse(second, "Second token should be rejected due to cool time");
        assertTrue(third, "Third token should be added after cool time passes");
        assertEquals(2, tokenHa.getQueueSize(), "Should have 2 tokens");
    }
    
    @Test
    @DisplayName("Should demonstrate configuration toString")
    void shouldDemonstrateConfigToString() {
        // Given
        TokenHaConfig config = new TokenHaConfig.Builder()
            .maxTokens(15)
            .coolTimeToAddMillis(1500)
            .numberOfLastTokens(2)
            .expirationTimeMillis(75000)
            .persistenceFilePath("test-toString.json")
            .build();
        
        // When
        String configString = config.toString();
        
        // Then
        assertNotNull(configString);
        assertTrue(configString.contains("maxTokens=15"));
        assertTrue(configString.contains("coolTimeToAddMillis=1500"));
        assertTrue(configString.contains("numberOfLastTokens=2"));
        assertTrue(configString.contains("expirationTimeMillis=75000"));
        assertTrue(configString.contains("test-toString.json"));
    }
}
