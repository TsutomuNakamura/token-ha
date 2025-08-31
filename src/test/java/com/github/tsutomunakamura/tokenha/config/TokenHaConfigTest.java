package com.github.tsutomunakamura.tokenha.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class TokenHaConfigTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;
    
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
    @DisplayName("fromProperties() should create config from valid properties")
    void testFromPropertiesValid() {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("tokenha.expiration.time.millis", "120000");
        props.setProperty("tokenha.number.of.last.tokens", "3");
        props.setProperty("tokenha.max.tokens", "15");
        props.setProperty("tokenha.cool.time.millis", "500");
        props.setProperty("tokenha.persistence.file.path", "props-tokens.json");

        // Create a mock to return a specific EvictionThreadConfig when EvictionThreadConfig.fromProperties(properties) has called
        EvictionThreadConfig expectedEvictionConfig = new EvictionThreadConfig.Builder()
                                                            .initialDelayMillis(1500)
                                                            .intervalMillis(15000)
                                                            .build();

        try (MockedStatic<EvictionThreadConfig> mockedEvictionThreadConfig = mockStatic(EvictionThreadConfig.class)) {
            mockedEvictionThreadConfig
                .when(() -> EvictionThreadConfig.fromProperties(props))
                .thenReturn(expectedEvictionConfig);
            
            // When
            TokenHaConfig config = TokenHaConfig.fromProperties(props);
            // Then
            assertEquals(120000, config.getExpirationTimeMillis());
            assertEquals(3, config.getNumberOfLastTokens());
            assertEquals(15, config.getMaxTokens());
            assertEquals(500, config.getCoolTimeToAddMillis());
            assertEquals("props-tokens.json", config.getPersistenceFilePath());
            assertEquals(expectedEvictionConfig, config.getEvictionThreadConfig());
        }
    }

    @Test
    @DisplayName("fromProperties() should return default values for missing properties")
    void testFromPropertiesWithMissingProperties() {
        java.util.Properties props = new java.util.Properties();
        
        // Before mocking, get the default EvictionThreadConfig prevent null pointer
        EvictionThreadConfig evictionConfig = EvictionThreadConfig.defaultConfig();

        // Create a mock to return null when EvictionThreadConfig.fromProperties(properties) has called
        try (MockedStatic<EvictionThreadConfig> mockedEvictionThreadConfig = mockStatic(EvictionThreadConfig.class)) {
            mockedEvictionThreadConfig.when(() -> EvictionThreadConfig.fromProperties(props)).thenReturn(evictionConfig);
            mockedEvictionThreadConfig.when(EvictionThreadConfig::defaultConfig).thenReturn(evictionConfig);

            // When - should throw IllegalArgumentException when "tokenha.expiration.time.millis" is negative
            TokenHaConfig.fromProperties(props);

            // Then - should use all default values
            TokenHaConfig defaultConfig = TokenHaConfig.defaultConfig();
            assertEquals(defaultConfig.getExpirationTimeMillis(), TokenHaConfig.fromProperties(props).getExpirationTimeMillis());
            assertEquals(defaultConfig.getNumberOfLastTokens(), TokenHaConfig.fromProperties(props).getNumberOfLastTokens());
            assertEquals(defaultConfig.getMaxTokens(), TokenHaConfig.fromProperties(props).getMaxTokens());
            assertEquals(defaultConfig.getCoolTimeToAddMillis(), TokenHaConfig.fromProperties(props).getCoolTimeToAddMillis());
            assertEquals(defaultConfig.getPersistenceFilePath(), TokenHaConfig.fromProperties(props).getPersistenceFilePath());
            assertEquals(defaultConfig.getEvictionThreadConfig(), TokenHaConfig.fromProperties(props).getEvictionThreadConfig());
        }
    }
    
    @Test
    @DisplayName("fromProperties() should throw IllegalArgumentException when properties are missing")
    void testFromPropertiesMissingExpirationTime() {
        java.util.Properties props = new java.util.Properties();

        // Before mocking, get the default EvictionThreadConfig prevent null pointer
        EvictionThreadConfig evictionConfig = EvictionThreadConfig.defaultConfig();

        // Create a mock to return null when EvictionThreadConfig.fromProperties(properties) has called
        try (MockedStatic<EvictionThreadConfig> mockedEvictionThreadConfig = mockStatic(EvictionThreadConfig.class)) {
            mockedEvictionThreadConfig
                .when(() -> EvictionThreadConfig.fromProperties(props))
                .thenReturn(null);
                mockedEvictionThreadConfig
                .when(EvictionThreadConfig::defaultConfig)
                .thenReturn(evictionConfig);
         
            // Intentionally not setting tokenha.expiration.time.millis
            // props.setProperty("tokenha.expiration.time.millis", "-1");
            // props.setProperty("tokenha.number.of.last.tokens", "-1");
            // props.setProperty("tokenha.max.tokens", "0");
            // props.setProperty("tokenha.cool.time.millis", "-1");
            // props.setProperty("tokenha.persistence.file.path", "");

            props.setProperty("tokenha.expiration.time.millis", "-1");    // Cannot be negative or zero
            props.setProperty("tokenha.number.of.last.tokens", "2");
            props.setProperty("tokenha.max.tokens", "11");
            props.setProperty("tokenha.cool.time.millis", "1000");
            props.setProperty("tokenha.persistence.file.path", "tokenha-data.json");

            // When - should throw IllegalArgumentException when "tokenha.expiration.time.millis" is negative
            try {
                TokenHaConfig.fromProperties(props);
                fail("Should throw IllegalArgumentException for missing or invalid expiration time");
            } catch (IllegalArgumentException e) {
                // Expected exception
                assert e.getMessage().contains("Expiration time must be positive and non-zero");                
            }

            props.setProperty("tokenha.expiration.time.millis", "60000");
            props.setProperty("tokenha.number.of.last.tokens", "-1");      // Cann not be negative
            props.setProperty("tokenha.max.tokens", "11");
            props.setProperty("tokenha.cool.time.millis", "1000");
            props.setProperty("tokenha.persistence.file.path", "tokenha-data.json");

            // When - should throw IllegalArgumentException when "tokenha.number.of.last.tokens" is negative
            try {
                TokenHaConfig.fromProperties(props);
                fail("Should throw IllegalArgumentException for negative number of last tokens");
            } catch (IllegalArgumentException e) {
                // Expected exception
                assert e.getMessage().contains("Number of last tokens cannot be negative");                
            }

            props.setProperty("tokenha.expiration.time.millis", "60000");
            props.setProperty("tokenha.number.of.last.tokens", "2");
            props.setProperty("tokenha.max.tokens", "0");                // Cannot be negative or zero
            props.setProperty("tokenha.cool.time.millis", "1000");
            props.setProperty("tokenha.persistence.file.path", "tokenha-data.json");

            // When - should throw IllegalArgumentException when "tokenha.max.tokens" is negative
            try {
                TokenHaConfig.fromProperties(props);
                fail("Should throw IllegalArgumentException for non-positive max tokens");
            } catch (IllegalArgumentException e) {
                // Expected exception
                assert e.getMessage().contains("Max tokens must be positive and non-zero");
            }

            props.setProperty("tokenha.expiration.time.millis", "60000");
            props.setProperty("tokenha.number.of.last.tokens", "2");
            props.setProperty("tokenha.max.tokens", "11");
            props.setProperty("tokenha.cool.time.millis", "-1");
            props.setProperty("tokenha.persistence.file.path", "tokenha-data.json");

            // When - should throw IllegalArgumentException when "tokenha.cool.time.millis" is negative
            try {
                TokenHaConfig.fromProperties(props);
                fail("Should throw IllegalArgumentException for negative cool time");
            } catch (IllegalArgumentException e) {
                // Expected exception
                assert e.getMessage().contains("Cool time cannot be negative");
            }

            props.setProperty("tokenha.expiration.time.millis", "60000");
            props.setProperty("tokenha.number.of.last.tokens", "2");
            props.setProperty("tokenha.max.tokens", "11");
            props.setProperty("tokenha.cool.time.millis", "1000");
            props.setProperty("tokenha.persistence.file.path", "");

            // When - should throw IllegalArgumentException when "tokenha.persistence.file.path" is empty
            try {
                TokenHaConfig.fromProperties(props);
                fail("Should throw IllegalArgumentException for empty persistence file path");
            } catch (IllegalArgumentException e) {
                // Expected exception
                assert e.getMessage().contains("Persistence file path cannot be null or empty");
            }
        }
    }

    // Test cases for fromEnvironment() method

    @Test
    @DisplayName("fromEnvironment() should use mocked environment variables")
    void testFromEnvironmentWithMocking() {
        // Fake environment variables by using SystemStubs.
        // https://www.baeldung.com/java-system-stubs
        environmentVariables.set("TOKENHA_EXPIRATION_TIME_MILLIS", "120000");
        environmentVariables.set("TOKENHA_NUMBER_OF_LAST_TOKENS", "3");
        environmentVariables.set("TOKENHA_MAX_TOKENS", "15");
        environmentVariables.set("TOKENHA_COOL_TIME_MILLIS", "500");
        environmentVariables.set("TOKENHA_PERSISTENCE_FILE_PATH", "env-tokens.json");

        environmentVariables.set("TOKENHA_EVICTION_INITIAL_DELAY_MILLIS", "2000");
        environmentVariables.set("TOKENHA_EVICTION_INTERVAL_MILLIS", "20000");

        // Test fromEnvironment() set each value from environment variables
        TokenHaConfig config = TokenHaConfig.fromEnvironment();
        assertEquals(120000, config.getExpirationTimeMillis());
        assertEquals(3, config.getNumberOfLastTokens());
        assertEquals(15, config.getMaxTokens());
        assertEquals(500, config.getCoolTimeToAddMillis());
        assertEquals("env-tokens.json", config.getPersistenceFilePath());
        assertEquals(2000, config.getEvictionThreadConfig().getInitialDelayMillis());
        assertEquals(20000, config.getEvictionThreadConfig().getIntervalMillis());
    }

    // Test casees for TokenHaConfig.Builder.persistenceFilePath(String persistenceFilePath)

    @Test
    @DisplayName("Builder.persistenceFilePath() should throw IllegalArgumentException for null or empty path")
    void testBuilderPersistenceFilePathValidation() {
        // Test null path
        try {
            new TokenHaConfig.Builder().persistenceFilePath(null);
            fail("Should throw IllegalArgumentException for null persistence file path");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assert e.getMessage().contains("Persistence file path cannot be null or empty");
        }

        // Test empty path
        try {
            new TokenHaConfig.Builder().persistenceFilePath("");
            fail("Should throw IllegalArgumentException for empty persistence file path");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assert e.getMessage().contains("Persistence file path cannot be null or empty");
        }
    }

    // Test cases for TokenHaConfig.Builder.evictionThreadConfig(EvictionThreadConfig evictionThreadConfig)

    @Test
    @DisplayName("Builder.evictionThreadConfig() should throw IllegalArgumentException for null config")
    void testBuilderEvictionThreadConfigValidation() {
        // Test null config
        try {
            new TokenHaConfig.Builder().evictionThreadConfig(null);
            fail("Should throw IllegalArgumentException for null eviction thread config");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assert e.getMessage().contains("Eviction thread configuration cannot be null");
        }
    }

    // Test cases for TokenHaConfig.Builder.build()

    @Test
    @DisplayName("Builder.build() should throw IllegalArgumentException when numberOfLastTokens >= maxTokens")
    void testBuilderBuildValidation() {
        try {
            new TokenHaConfig.Builder()
                .maxTokens(5)
                .numberOfLastTokens(5)  // Must be less than max tokens
                .build();
            fail("Should throw IllegalArgumentException for numberOfLastTokens >= maxTokens");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assert e.getMessage().contains("Number of last tokens must be less than max tokens");
        }
    }

    // Test cases for toString()

    @Test
    @DisplayName("toString() should include all configuration parameters")
    void testToStringIncludesAllParameters() {
        TokenHaConfig config = new TokenHaConfig.Builder()
            .maxTokens(10)
            .coolTimeToAddMillis(1000)
            .numberOfLastTokens(2)
            .expirationTimeMillis(60000)
            .persistenceFilePath("test-config.json")
            .evictionThreadConfig(new EvictionThreadConfig.Builder()
                                      .initialDelayMillis(1500)
                                      .intervalMillis(15000)
                                      .build())
            .build();

        String toString = config.toString();
        assert toString.contains("maxTokens=10");
        assert toString.contains("coolTimeToAddMillis=1000");
        assert toString.contains("numberOfLastTokens=2");
        assert toString.contains("expirationTimeMillis=60000");
        assert toString.contains("persistenceFilePath='test-config.json'");
        assert toString.contains("evictionThreadConfig=EvictionThreadConfig{initialDelayMillis=1500, intervalMillis=15000}");
    }
}