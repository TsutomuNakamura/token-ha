package com.github.tsutomunakamura.tokenha.config;

import java.util.Properties;
import org.slf4j.Logger;
import com.github.tsutomunakamura.tokenha.logging.TokenHaLogger;

/**
 * Configuration class for TokenHa with builder pattern support.
 * Supports loading from properties, environment variables, or programmatic configuration.
 */
public class TokenHaConfig {
    
    private static final Logger logger = TokenHaLogger.getLogger(TokenHaConfig.class);
    
    // Default values
    private static final long DEFAULT_EXPIRATION_TIME_MILLIS = 60000L;
    private static final int DEFAULT_NUMBER_OF_LAST_TOKENS = 1;
    private static final int DEFAULT_MAX_TOKENS = 10;
    private static final long DEFAULT_COOL_TIME_MILLIS = 1000L;
    private static final String DEFAULT_PERSISTENCE_FILE_PATH = "tokenha-data.json";
    private static final boolean DEFAULT_ENABLE_AUTO_EVICT_IF_QUEUE_IS_FULL = true;
    
    private final long expirationTimeMillis;
    private final int numberOfLastTokens;
    private final int maxTokens;
    private final long coolTimeToAddMillis;
    private final String persistenceFilePath;
    private final EvictionThreadConfig evictionThreadConfig;
    private final boolean enableAutoEvictIfQueueIsFull;
    
    private TokenHaConfig(Builder builder) {
        this.expirationTimeMillis = builder.expirationTimeMillis;
        this.numberOfLastTokens = builder.numberOfLastTokens;
        this.maxTokens = builder.maxTokens;
        this.coolTimeToAddMillis = builder.coolTimeToAddMillis;
        this.persistenceFilePath = builder.persistenceFilePath;
        this.evictionThreadConfig = builder.evictionThreadConfig;
        this.enableAutoEvictIfQueueIsFull = builder.enableAutoEvictIfQueueIsFull;
    }
    
    // Getters
    public long getExpirationTimeMillis() { return expirationTimeMillis; }
    public int getNumberOfLastTokens() { return numberOfLastTokens; }
    public int getMaxTokens() { return maxTokens; }
    public long getCoolTimeToAddMillis() { return coolTimeToAddMillis; }
    public String getPersistenceFilePath() { return persistenceFilePath; }
    public EvictionThreadConfig getEvictionThreadConfig() { return evictionThreadConfig; }
    public boolean isEnableAutoEvictIfQueueIsFull() { return enableAutoEvictIfQueueIsFull; }
    
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
            .expirationTimeMillis(this.expirationTimeMillis)
            .numberOfLastTokens(this.numberOfLastTokens)
            .maxTokens(this.maxTokens)
            .coolTimeToAddMillis(this.coolTimeToAddMillis)
            .persistenceFilePath(this.persistenceFilePath)
            .evictionThreadConfig(this.evictionThreadConfig);
    }
    
    /**
     * Load configuration from properties file or system properties.
     */
    public static TokenHaConfig fromProperties(Properties properties) {
        Builder builder = new Builder();
        
        // Use helper methods that handle invalid values gracefully
        long expirationTime = getLongProperty(properties, "tokenha.expiration.time.millis", DEFAULT_EXPIRATION_TIME_MILLIS);
        int numberOfLastTokens = getIntProperty(properties, "tokenha.number.of.last.tokens", DEFAULT_NUMBER_OF_LAST_TOKENS);
        int maxTokens = getIntProperty(properties, "tokenha.max.tokens", DEFAULT_MAX_TOKENS);
        long coolTime = getLongProperty(properties, "tokenha.cool.time.millis", DEFAULT_COOL_TIME_MILLIS);
        String filePath = properties.getProperty("tokenha.persistence.file.path", DEFAULT_PERSISTENCE_FILE_PATH);
        boolean enableAutoEvict = getBooleanProperty(properties, "tokenha.enable.auto.evict.if.queue.is.full", DEFAULT_ENABLE_AUTO_EVICT_IF_QUEUE_IS_FULL);
        
        // Load eviction thread configuration from properties
        EvictionThreadConfig evictionConfig = EvictionThreadConfig.fromProperties(properties);
        
        // Apply values with validation - use defaults if validation fails
        builder.expirationTimeMillis(expirationTime)
            .numberOfLastTokens(numberOfLastTokens)
            .maxTokens(maxTokens)
            .coolTimeToAddMillis(coolTime)
            .persistenceFilePath(filePath)
            .evictionThreadConfig(evictionConfig)
            .enableAutoEvictIfQueueIsFull(enableAutoEvict);
                                
        return builder.build();
    }
    
    /**
     * Load configuration from environment variables.
     */
    public static TokenHaConfig fromEnvironment() {
        Builder builder = new Builder();
        
        // Use helper methods that handle invalid values gracefully
        long expirationTime = getLongEnv("TOKENHA_EXPIRATION_TIME_MILLIS", DEFAULT_EXPIRATION_TIME_MILLIS);
        int numberOfLastTokens = getIntEnv("TOKENHA_NUMBER_OF_LAST_TOKENS", DEFAULT_NUMBER_OF_LAST_TOKENS);
        int maxTokens = getIntEnv("TOKENHA_MAX_TOKENS", DEFAULT_MAX_TOKENS);
        long coolTime = getLongEnv("TOKENHA_COOL_TIME_MILLIS", DEFAULT_COOL_TIME_MILLIS);
        String filePath = getEnv("TOKENHA_PERSISTENCE_FILE_PATH", DEFAULT_PERSISTENCE_FILE_PATH);
        boolean enableAutoEvict = getBooleanEnv("TOKENHA_ENABLE_AUTO_EVICT_IF_QUEUE_IS_FULL", DEFAULT_ENABLE_AUTO_EVICT_IF_QUEUE_IS_FULL);
        
        // Load eviction thread configuration from environment
        EvictionThreadConfig evictionConfig = EvictionThreadConfig.fromEnvironment();
        
        // Apply values with validation - use defaults if validation fails
        builder.expirationTimeMillis(expirationTime)
            .numberOfLastTokens(numberOfLastTokens)
            .maxTokens(maxTokens)
            .coolTimeToAddMillis(coolTime)
            .persistenceFilePath(filePath)
            .evictionThreadConfig(evictionConfig)
            .enableAutoEvictIfQueueIsFull(enableAutoEvict);
        
        return builder.build();
    }
    
    /**
     * Builder pattern for programmatic configuration.
     */
    public static class Builder {
        private long expirationTimeMillis = DEFAULT_EXPIRATION_TIME_MILLIS;
        private int numberOfLastTokens = DEFAULT_NUMBER_OF_LAST_TOKENS;
        private int maxTokens = DEFAULT_MAX_TOKENS;
        private long coolTimeToAddMillis = DEFAULT_COOL_TIME_MILLIS;
        private String persistenceFilePath = DEFAULT_PERSISTENCE_FILE_PATH;
        private EvictionThreadConfig evictionThreadConfig = EvictionThreadConfig.defaultConfig();
        private boolean enableAutoEvictIfQueueIsFull = DEFAULT_ENABLE_AUTO_EVICT_IF_QUEUE_IS_FULL;
        
        public Builder expirationTimeMillis(long expirationTimeMillis) {
            if (expirationTimeMillis <= 0) {
                throw new IllegalArgumentException("Expiration time must be positive and non-zero");
            }
            this.expirationTimeMillis = expirationTimeMillis;
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
                throw new IllegalArgumentException("Max tokens must be positive and non-zero");
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
        
        public Builder evictionThreadConfig(EvictionThreadConfig evictionThreadConfig) {
            if (evictionThreadConfig == null) {
                throw new IllegalArgumentException("Eviction thread configuration cannot be null");
            }
            this.evictionThreadConfig = evictionThreadConfig;
            return this;
        }
        
        public Builder enableAutoEvictIfQueueIsFull(boolean enableAutoEvictIfQueueIsFull) {
            this.enableAutoEvictIfQueueIsFull = enableAutoEvictIfQueueIsFull;
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
            return Integer.parseInt(value.trim());
        }
        return defaultValue;
    }
    
    private static long getLongProperty(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            return Long.parseLong(value.trim());
        }
        return defaultValue;
    }
    
    private static boolean getBooleanProperty(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }
    
    public static int getIntEnv(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value != null) {
            return Integer.parseInt(value.trim());
        }
        return defaultValue;
    }
    
    public static long getLongEnv(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value != null) {
            return Long.parseLong(value.trim());
        }
        return defaultValue;
    }

    public static String getEnv(String key, String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }
    
    public static boolean getBooleanEnv(String key, boolean defaultValue) {
        String value = System.getenv(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }
    
    @Override
    public String toString() {
        return "TokenHaConfig{" +
                "expirationTimeMillis=" + expirationTimeMillis +
                ", numberOfLastTokens=" + numberOfLastTokens +
                ", maxTokens=" + maxTokens +
                ", coolTimeToAddMillis=" + coolTimeToAddMillis +
                ", persistenceFilePath='" + persistenceFilePath + '\'' +
                ", evictionThreadConfig=" + evictionThreadConfig +
                '}';
    }
}
