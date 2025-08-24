package com.github.tsutomunakamura.tokenha;

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
}