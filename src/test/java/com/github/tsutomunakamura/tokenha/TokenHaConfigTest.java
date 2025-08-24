package com.github.tsutomunakamura.tokenha;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TokenHaConfigTest {

    // Test cases for toBuilder() method

    @Test
    @DisplayName("toBuilder() should create a modifiable builder from existing config")
    void testToBuilder() {
        TokenHaConfig originalConfig = new TokenHaConfig.Builder()
            .maxTokens(10)
            .coolTimeToAddMillis(1000)
            .numberOfLastTokens(2)
            .expirationTimeMillis(60000)
            .persistenceFilePath("test-config.json")
            .build();

        TokenHaConfig modifiedConfig = originalConfig.toBuilder()
            .maxTokens(20) // Change maxTokens
            .build();

        // Verify original config remains unchanged
        assert originalConfig.getMaxTokens() == 10;
        assert originalConfig.getCoolTimeToAddMillis() == 1000;
        assert originalConfig.getNumberOfLastTokens() == 2;
        assert originalConfig.getExpirationTimeMillis() == 60000;
        assert originalConfig.getPersistenceFilePath().equals("test-config.json");

        // Verify modified config has the updated value
        assert modifiedConfig.getMaxTokens() == 20;
        assert modifiedConfig.getCoolTimeToAddMillis() == 1000; // Unchanged
        assert modifiedConfig.getNumberOfLastTokens() == 2; // Unchanged
        assert modifiedConfig.getExpirationTimeMillis() == 60000; // Unchanged
        assert modifiedConfig.getPersistenceFilePath().equals("test-config.json"); // Unchanged
    }

    // Test cases for fromProperties() method
    
    @Test
    @DisplayName("fromProperties() should use DEFAULT_EXPIRATION_TIME_MILLIS when property tokenha.expiration.time.millis is missing")
    void testFromPropertiesMissingExpirationTime() {
        java.util.Properties props = new java.util.Properties();
        // Intentionally not setting tokenha.expiration.time.millis
        props.setProperty("tokenha.number.of.last.tokens", "3");
        props.setProperty("tokenha.max.tokens", "15");
        props.setProperty("tokenha.cool.time.millis", "2000");
        props.setProperty("tokenha.persistence.file.path", "props-config.json");

        TokenHaConfig config = TokenHaConfig.fromProperties(props);

        // Make TokenHaConfig.DEFAULT_EXPIRATION_TIME_MILLIS accessible for assertion
        int defaultExpirationTime = -1;
        try {
            Field field = TokenHaConfig.class.getDeclaredField("DEFAULT_EXPIRATION_TIME_MILLIS");
            field.setAccessible(true);
            defaultExpirationTime = field.getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Failed to access DEFAULT_EXPIRATION_TIME_MILLIS";
        }

        assert config.getExpirationTimeMillis() == defaultExpirationTime;
        assert config.getNumberOfLastTokens() == 3;
        assert config.getMaxTokens() == 15;
        assert config.getCoolTimeToAddMillis() == 2000;
        assert config.getPersistenceFilePath().equals("props-config.json");
    }
}