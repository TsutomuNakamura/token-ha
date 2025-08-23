package com.github.tsutomunakamura.tokenha;

import java.util.Properties;

/**
 * Configuration class for TokenHa with builder pattern support.
 * Supports loading from properties, environment variables, or programmatic configuration.
 */
public class TokenHaConfig {
    
    // Default values
    private static final int DEFAULT_EXPIRATION_TIME_SECONDS = 60000;
    private static final int DEFAULT_NUMBER_OF_LAST_TOKENS = 1;
    private static final int DEFAULT_MAX_TOKENS = 10;
    private static final long DEFAULT_COOL_TIME_MILLIS = 1000;
    private static final String DEFAULT_PERSISTENCE_FILE_PATH = "tokenha-data.json";
    
    private final int expirationTimeSeconds;
    private final int numberOfLastTokens;
    private final int maxTokens;
    private final long coolTimeToAddMillis;
    private final String persistenceFilePath;
    
    private TokenHaConfig(Builder builder) {
        this.expirationTimeSeconds = builder.expirationTimeSeconds;
        this.numberOfLastTokens = builder.numberOfLastTokens;
        this.maxTokens = builder.maxTokens;
        this.coolTimeToAddMillis = builder.coolTimeToAddMillis;
        this.persistenceFilePath = builder.persistenceFilePath;
    }
    
    // Getters
    public int getExpirationTimeSeconds() { return expirationTimeSeconds; }
    public int getNumberOfLastTokens() { return numberOfLastTokens; }
    public int getMaxTokens() { return maxTokens; }
    public long getCoolTimeToAddMillis() { return coolTimeToAddMillis; }
    public String getPersistenceFilePath() { return persistenceFilePath; }
    
    /**
     * Create a default configuration.
     */
    public static TokenHaConfig defaultConfig() {
        return new Builder().build();
    }
    
    /**
     * Create a builder from this configuration for modification.
     */
    public Builder toBuilder() {
        return new Builder()
            .expirationTimeSeconds(this.expirationTimeSeconds)
            .numberOfLastTokens(this.numberOfLastTokens)
            .maxTokens(this.maxTokens)
            .coolTimeToAddMillis(this.coolTimeToAddMillis)
            .persistenceFilePath(this.persistenceFilePath);
    }
    
    /**
     * Load configuration from properties file or system properties.
     */
    public static TokenHaConfig fromProperties(Properties properties) {
        Builder builder = new Builder();
        
        // Use helper methods that handle invalid values gracefully
        int expirationTime = getIntProperty(properties, "tokenha.expiration.time.seconds", DEFAULT_EXPIRATION_TIME_SECONDS);
        int numberOfLastTokens = getIntProperty(properties, "tokenha.number.of.last.tokens", DEFAULT_NUMBER_OF_LAST_TOKENS);
        int maxTokens = getIntProperty(properties, "tokenha.max.tokens", DEFAULT_MAX_TOKENS);
        long coolTime = getLongProperty(properties, "tokenha.cool.time.millis", DEFAULT_COOL_TIME_MILLIS);
        String filePath = properties.getProperty("tokenha.persistence.file.path", DEFAULT_PERSISTENCE_FILE_PATH);
        
        // Apply values with validation - use defaults if validation fails
        try {
            builder.expirationTimeSeconds(expirationTime);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid expiration time, using default: " + DEFAULT_EXPIRATION_TIME_SECONDS);
            builder.expirationTimeSeconds(DEFAULT_EXPIRATION_TIME_SECONDS);
        }
        
        try {
            builder.numberOfLastTokens(numberOfLastTokens);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid number of last tokens, using default: " + DEFAULT_NUMBER_OF_LAST_TOKENS);
            builder.numberOfLastTokens(DEFAULT_NUMBER_OF_LAST_TOKENS);
        }
        
        try {
            builder.maxTokens(maxTokens);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid max tokens, using default: " + DEFAULT_MAX_TOKENS);
            builder.maxTokens(DEFAULT_MAX_TOKENS);
        }
        
        try {
            builder.coolTimeToAddMillis(coolTime);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid cool time, using default: " + DEFAULT_COOL_TIME_MILLIS);
            builder.coolTimeToAddMillis(DEFAULT_COOL_TIME_MILLIS);
        }
        
        try {
            builder.persistenceFilePath(filePath);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid persistence file path, using default: " + DEFAULT_PERSISTENCE_FILE_PATH);
            builder.persistenceFilePath(DEFAULT_PERSISTENCE_FILE_PATH);
        }
        
        return builder.build();
    }
    
    /**
     * Load configuration from environment variables.
     */
    public static TokenHaConfig fromEnvironment() {
        Builder builder = new Builder();
        
        // Use helper methods that handle invalid values gracefully
        int expirationTime = getIntEnv("TOKENHA_EXPIRATION_TIME_SECONDS", DEFAULT_EXPIRATION_TIME_SECONDS);
        int numberOfLastTokens = getIntEnv("TOKENHA_NUMBER_OF_LAST_TOKENS", DEFAULT_NUMBER_OF_LAST_TOKENS);
        int maxTokens = getIntEnv("TOKENHA_MAX_TOKENS", DEFAULT_MAX_TOKENS);
        long coolTime = getLongEnv("TOKENHA_COOL_TIME_MILLIS", DEFAULT_COOL_TIME_MILLIS);
        String filePath = System.getenv().getOrDefault("TOKENHA_PERSISTENCE_FILE_PATH", DEFAULT_PERSISTENCE_FILE_PATH);
        
        // Apply values with validation - use defaults if validation fails
        try {
            builder.expirationTimeSeconds(expirationTime);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid expiration time from env, using default: " + DEFAULT_EXPIRATION_TIME_SECONDS);
            builder.expirationTimeSeconds(DEFAULT_EXPIRATION_TIME_SECONDS);
        }
        
        try {
            builder.numberOfLastTokens(numberOfLastTokens);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid number of last tokens from env, using default: " + DEFAULT_NUMBER_OF_LAST_TOKENS);
            builder.numberOfLastTokens(DEFAULT_NUMBER_OF_LAST_TOKENS);
        }
        
        try {
            builder.maxTokens(maxTokens);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid max tokens from env, using default: " + DEFAULT_MAX_TOKENS);
            builder.maxTokens(DEFAULT_MAX_TOKENS);
        }
        
        try {
            builder.coolTimeToAddMillis(coolTime);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid cool time from env, using default: " + DEFAULT_COOL_TIME_MILLIS);
            builder.coolTimeToAddMillis(DEFAULT_COOL_TIME_MILLIS);
        }
        
        try {
            builder.persistenceFilePath(filePath);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid persistence file path from env, using default: " + DEFAULT_PERSISTENCE_FILE_PATH);
            builder.persistenceFilePath(DEFAULT_PERSISTENCE_FILE_PATH);
        }
        
        return builder.build();
    }
    
    /**
     * Builder pattern for programmatic configuration.
     */
    public static class Builder {
        private int expirationTimeSeconds = DEFAULT_EXPIRATION_TIME_SECONDS;
        private int numberOfLastTokens = DEFAULT_NUMBER_OF_LAST_TOKENS;
        private int maxTokens = DEFAULT_MAX_TOKENS;
        private long coolTimeToAddMillis = DEFAULT_COOL_TIME_MILLIS;
        private String persistenceFilePath = DEFAULT_PERSISTENCE_FILE_PATH;
        
        public Builder expirationTimeSeconds(int expirationTimeSeconds) {
            if (expirationTimeSeconds <= 0) {
                throw new IllegalArgumentException("Expiration time must be positive");
            }
            this.expirationTimeSeconds = expirationTimeSeconds;
            return this;
        }
        
        public Builder numberOfLastTokens(int numberOfLastTokens) {
            if (numberOfLastTokens < 0) {
                throw new IllegalArgumentException("Number of last tokens cannot be negative");
            }
            this.numberOfLastTokens = numberOfLastTokens;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            if (maxTokens <= 0) {
                throw new IllegalArgumentException("Max tokens must be positive");
            }
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder coolTimeToAddMillis(long coolTimeToAddMillis) {
            if (coolTimeToAddMillis < 0) {
                throw new IllegalArgumentException("Cool time cannot be negative");
            }
            this.coolTimeToAddMillis = coolTimeToAddMillis;
            return this;
        }
        
        public Builder persistenceFilePath(String persistenceFilePath) {
            if (persistenceFilePath == null || persistenceFilePath.trim().isEmpty()) {
                throw new IllegalArgumentException("Persistence file path cannot be null or empty");
            }
            this.persistenceFilePath = persistenceFilePath;
            return this;
        }
        
        public TokenHaConfig build() {
            // Validation
            if (numberOfLastTokens >= maxTokens) {
                throw new IllegalArgumentException("Number of last tokens must be less than max tokens");
            }
            return new TokenHaConfig(this);
        }
    }
    
    // Helper methods for parsing
    private static int getIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer value for property " + key + ": " + value + ". Using default: " + defaultValue);
            }
        }
        return defaultValue;
    }
    
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
    
    private static int getIntEnv(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer value for environment variable " + key + ": " + value + ". Using default: " + defaultValue);
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
        return "TokenHaConfig{" +
                "expirationTimeSeconds=" + expirationTimeSeconds +
                ", numberOfLastTokens=" + numberOfLastTokens +
                ", maxTokens=" + maxTokens +
                ", coolTimeToAddMillis=" + coolTimeToAddMillis +
                ", persistenceFilePath='" + persistenceFilePath + '\'' +
                '}';
    }
}
