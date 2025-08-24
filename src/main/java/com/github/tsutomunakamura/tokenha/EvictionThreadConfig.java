package com.github.tsutomunakamura.tokenha;

import java.util.Properties;

/**
 * Configuration class for EvictionThread with builder pattern support.
 * Supports loading from properties, environment variables, or programmatic configuration.
 */
public class EvictionThreadConfig {
    
    // Default values
    private static final long DEFAULT_INITIAL_DELAY_MILLIS = 1000;
    private static final long DEFAULT_INTERVAL_MILLIS = 10000;
    
    private final long initialDelayMillis;
    private final long intervalMillis;
    
    private EvictionThreadConfig(Builder builder) {
        this.initialDelayMillis = builder.initialDelayMillis;
        this.intervalMillis = builder.intervalMillis;
    }
    
    // Getters
    public long getInitialDelayMillis() { return initialDelayMillis; }
    public long getIntervalMillis() { return intervalMillis; }
    
    /**
     * Create a default configuration.
     */
    public static EvictionThreadConfig defaultConfig() {
        return new Builder().build();
    }
    
    /**
     * Create a builder from this configuration for modification.
     */
    public Builder toBuilder() {
        return new Builder()
            .initialDelayMillis(this.initialDelayMillis)
            .intervalMillis(this.intervalMillis);
    }
    
    /**
     * Load configuration from properties file or system properties.
     */
    public static EvictionThreadConfig fromProperties(Properties props) {
        Builder builder = new Builder();
        
        // Use helper methods that handle invalid values gracefully
        long initialDelay = getLongProperty(props, "tokenha.eviction.initial.delay.millis", DEFAULT_INITIAL_DELAY_MILLIS);
        long interval = getLongProperty(props, "tokenha.eviction.interval.millis", DEFAULT_INTERVAL_MILLIS);
        
        // Apply values with validation - use defaults if validation fails
        try {
            builder.initialDelayMillis(initialDelay);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid initial delay, using default: " + DEFAULT_INITIAL_DELAY_MILLIS);
            builder.initialDelayMillis(DEFAULT_INITIAL_DELAY_MILLIS);
        }
        
        try {
            builder.intervalMillis(interval);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid interval, using default: " + DEFAULT_INTERVAL_MILLIS);
            builder.intervalMillis(DEFAULT_INTERVAL_MILLIS);
        }
        
        return builder.build();
    }
    
    /**
     * Load configuration from environment variables.
     */
    public static EvictionThreadConfig fromEnvironment() {
        Builder builder = new Builder();
        
        // Use helper methods that handle invalid values gracefully
        long initialDelay = getLongEnv("TOKENHA_EVICTION_INITIAL_DELAY_MILLIS", DEFAULT_INITIAL_DELAY_MILLIS);
        long interval = getLongEnv("TOKENHA_EVICTION_INTERVAL_MILLIS", DEFAULT_INTERVAL_MILLIS);
        
        // Apply values with validation - use defaults if validation fails
        try {
            builder.initialDelayMillis(initialDelay);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid initial delay from env, using default: " + DEFAULT_INITIAL_DELAY_MILLIS);
            builder.initialDelayMillis(DEFAULT_INITIAL_DELAY_MILLIS);
        }
        
        try {
            builder.intervalMillis(interval);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid interval from env, using default: " + DEFAULT_INTERVAL_MILLIS);
            builder.intervalMillis(DEFAULT_INTERVAL_MILLIS);
        }
        
        return builder.build();
    }
    
    /**
     * Builder pattern for programmatic configuration.
     */
    public static class Builder {
        private long initialDelayMillis = DEFAULT_INITIAL_DELAY_MILLIS;
        private long intervalMillis = DEFAULT_INTERVAL_MILLIS;
        
        public Builder initialDelayMillis(long initialDelayMillis) {
            if (initialDelayMillis < 0) {
                throw new IllegalArgumentException("Initial delay cannot be negative");
            }
            this.initialDelayMillis = initialDelayMillis;
            return this;
        }
        
        public Builder intervalMillis(long intervalMillis) {
            if (intervalMillis <= 0) {
                throw new IllegalArgumentException("Interval must be positive");
            }
            this.intervalMillis = intervalMillis;
            return this;
        }
        
        public EvictionThreadConfig build() {
            return new EvictionThreadConfig(this);
        }
    }
    
    // Helper methods for parsing
    private static long getLongProperty(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid long value for property " + key + ": " + value + ". Using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    
    private static long getLongEnv(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value != null) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid long value for environment variable " + key + ": " + value + ". Using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    
    @Override
    public String toString() {
        return "EvictionThreadConfig{" +
                "initialDelayMillis=" + initialDelayMillis +
                ", intervalMillis=" + intervalMillis +
                '}';
    }
}
