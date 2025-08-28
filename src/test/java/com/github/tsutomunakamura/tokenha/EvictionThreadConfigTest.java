package com.github.tsutomunakamura.tokenha;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EvictionThreadConfigTest {
    
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
    
}
