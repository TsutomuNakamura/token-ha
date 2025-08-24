package com.github.tsutomunakamura.tokenha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
    @DisplayName("fromProperties() should use defaults when properties are missing")
    void testFromPropertiesMissingExpirationTime() {
        java.util.Properties props = new java.util.Properties();
        // Intentionally not setting tokenha.expiration.time.millis
        props.setProperty("tokenha.expiration.time.millis", "-1");
        props.setProperty("tokenha.number.of.last.tokens", "-1");
        props.setProperty("tokenha.max.tokens", "0");
        props.setProperty("tokenha.cool.time.millis", "-1");
        props.setProperty("tokenha.persistence.file.path", "");

        // Before mocking, get the default EvictionThreadConfig prevent null pointer
        EvictionThreadConfig evictionConfig = EvictionThreadConfig.defaultConfig();

        // Create a mock to return null when EvictionThreadConfig.fromProperties(properties) has called
        MockedStatic<EvictionThreadConfig> mockedEvictionThreadConfig = mockStatic(EvictionThreadConfig.class);
        mockedEvictionThreadConfig
            .when(() -> EvictionThreadConfig.fromProperties(props))
            .thenReturn(null);
        mockedEvictionThreadConfig
            .when(EvictionThreadConfig::defaultConfig)
            .thenReturn(evictionConfig);
         
        TokenHaConfig config = TokenHaConfig.fromProperties(props);

        try {
            Field fieldExpirationTime = TokenHaConfig.class.getDeclaredField("DEFAULT_EXPIRATION_TIME_MILLIS");
            fieldExpirationTime.setAccessible(true);
            Field fieldNumberOfLastTokens = TokenHaConfig.class.getDeclaredField("DEFAULT_NUMBER_OF_LAST_TOKENS");
            fieldNumberOfLastTokens.setAccessible(true);
            Field fieldMaxTokens = TokenHaConfig.class.getDeclaredField("DEFAULT_MAX_TOKENS");
            fieldMaxTokens.setAccessible(true);
            Field fieldCoolTimeMillis = TokenHaConfig.class.getDeclaredField("DEFAULT_COOL_TIME_MILLIS");
            fieldCoolTimeMillis.setAccessible(true);
            Field fieldPersistenceFilePath = TokenHaConfig.class.getDeclaredField("DEFAULT_PERSISTENCE_FILE_PATH");
            fieldPersistenceFilePath.setAccessible(true);

            assertEquals(fieldExpirationTime.getLong(null), config.getExpirationTimeMillis());
            assertEquals(fieldNumberOfLastTokens.getInt(null), config.getNumberOfLastTokens());
            assertEquals(fieldMaxTokens.getInt(null), config.getMaxTokens());
            assertEquals(fieldCoolTimeMillis.getLong(null), config.getCoolTimeToAddMillis());
            assertEquals(fieldPersistenceFilePath.get(null), config.getPersistenceFilePath());
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Failed to access defaulit values via reflection";
        }
    }

    // Test cases for fromEnvironment() method

}