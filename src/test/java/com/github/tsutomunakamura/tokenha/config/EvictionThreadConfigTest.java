package com.github.tsutomunakamura.tokenha.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class EvictionThreadConfigTest {
    
    @SystemStub
    private EnvironmentVariables environmentVariables;

    // Test cases for toBuilder() method

    @Test
    @DisplayName("toBuilder() should create a modifiable builder from existing config")
    void testToBuilder() {
        EvictionThreadConfig originalConfig = new EvictionThreadConfig.Builder()
            .initialDelayMillis(2000)
            .intervalMillis(15000)
            .build();

        EvictionThreadConfig modifiedConfig = originalConfig.toBuilder()
            .intervalMillis(30000) // Change intervalMillis
            .build();

        // Verify original config remains unchanged
        assert originalConfig.getInitialDelayMillis() == 2000;
        assert originalConfig.getIntervalMillis() == 15000;

        // Verify modified config has the updated value
        assert modifiedConfig.getInitialDelayMillis() == 2000; // Unchanged
        assert modifiedConfig.getIntervalMillis() == 30000; // Changed
    }

    // Test cases for fromProperties() method
    
    @Test
    @DisplayName("fromProperties() should set initialDelayMillis with defauilt value when builder.initialDelayMillis(initialDelay) throws IllegalArgumentException")
    void testFromPropertiesWithInvalidInitialDelay() {
        // Given properties with invalid initial delay
        java.util.Properties props = new java.util.Properties();
        props.setProperty("tokenha.eviction.initial.delay.millis", "-500"); // Invalid negative value
        props.setProperty("tokenha.eviction.interval.millis", "20000"); // Valid value

        // When
        EvictionThreadConfig config = EvictionThreadConfig.fromProperties(props);

        // Then - verify that default is used for invalid initial delay
        assert config.getInitialDelayMillis() == 1000; // Default value
        assert config.getIntervalMillis() == 20000; // From properties
    }

    @Test
    @DisplayName("fromProperties() should set intervalMillis with default value when builder.intervalMillis(interval) throws IllegalArgumentException")
    void testFromPropertiesWithInvalidInterval() {
        // Given properties with invalid interval
        java.util.Properties props = new java.util.Properties();
        props.setProperty("tokenha.eviction.initial.delay.millis", "3000"); // Valid value
        props.setProperty("tokenha.eviction.interval.millis", "0"); // Invalid zero value

        // When
        EvictionThreadConfig config = EvictionThreadConfig.fromProperties(props);

        // Then - verify that default is used for invalid interval
        assert config.getInitialDelayMillis() == 3000; // From properties
        assert config.getIntervalMillis() == 10000; // Default value
    }

    // Test cases for fromEnvironment() method

    @Test
    @DisplayName("fromEnvironment() should throw IllegalArgumentException when TOKENHA_EVICTION_INITIAL_DELAY_MILLIS is invalid")
    void testFromEnvironmentWithInvalidInitialDelay() {
        // Fake environment variables by using SystemStubs.
        // https://www.baeldung.com/java-system-stubs
        environmentVariables.set("TOKENHA_EVICTION_INITIAL_DELAY_MILLIS", "-1000"); // Invalid negative value
        environmentVariables.set("TOKENHA_EVICTION_INTERVAL_MILLIS", "25000");
        // When
        try {
            EvictionThreadConfig.fromEnvironment();
            assert false : "Expected IllegalArgumentException for invalid initial delay";
        } catch (IllegalArgumentException e) {
            // Expected exception
            assert e.getMessage().contains("Initial delay cannot be negative");
        }
    }

    @Test
    @DisplayName("fromEnvironment() should throw IllegalArgumentException when TOKENHA_EVICTION_INTERVAL_MILLIS is invalid")
    void testFromEnvironmentWithInvalidInterval() {
        // Fake environment variables by using SystemStubs.
        // https://www.baeldung.com/java/system-stubs
        environmentVariables.set("TOKENHA_EVICTION_INITIAL_DELAY_MILLIS", "4000");
        environmentVariables.set("TOKENHA_EVICTION_INTERVAL_MILLIS", "-2000"); // Invalid negative value

        // When
        try {
            EvictionThreadConfig.fromEnvironment();
            assert false : "Expected IllegalArgumentException for invalid interval";
        } catch (IllegalArgumentException e) {
            // Expected exception
            assert e.getMessage().contains("Interval must be positive");
        }
    }
}
