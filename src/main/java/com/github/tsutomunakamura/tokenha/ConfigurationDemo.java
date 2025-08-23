package com.github.tsutomunakamura.tokenha;

import java.io.IOException;
import java.util.Properties;

/**
 * Demo class showing different configuration strategies for TokenHa.
 */
public class ConfigurationDemo {
    
    public static void main(String[] args) {
        System.out.println("=== TokenHa Configuration Strategies Demo ===\n");
        
        try {
            // Strategy 1: Default Configuration
            demonstrateDefaultConfig();
            
            // Strategy 2: Builder Pattern
            demonstrateBuilderPattern();
            
            // Strategy 3: Properties Configuration
            demonstratePropertiesConfig();
            
            // Strategy 4: Environment Variables (simulated)
            demonstrateEnvironmentConfig();
            
            // Strategy 5: Mixed Configuration
            demonstrateMixedConfig();
            
        } catch (IOException e) {
            System.err.println("Error during demo: " + e.getMessage());
        }
    }
    
    private static void demonstrateDefaultConfig() throws IOException {
        System.out.println("1. DEFAULT CONFIGURATION");
        System.out.println("-------------------------");
        
        try (TokenHa tokenHa = new TokenHa()) {
            System.out.println("Default config file path: " + tokenHa.getPersistenceFilePath());
            System.out.println("Queue starts empty: " + (tokenHa.getQueueSize() == 0));
            System.out.println("Available to add: " + tokenHa.availableToAdd());
            tokenHa.deletePersistenceFile(); // Cleanup
        }
        System.out.println();
    }
    
    private static void demonstrateBuilderPattern() throws IOException {
        System.out.println("2. BUILDER PATTERN CONFIGURATION");
        System.out.println("---------------------------------");
        
        TokenHaConfig config = new TokenHaConfig.Builder()
            .maxTokens(5)
            .coolTimeToAddMillis(100) // Very short for demo
            .numberOfLastTokens(1)
            .expirationTimeSeconds(30000)
            .persistenceFilePath("demo-builder-tokens.json")
            .build();
        
        System.out.println("Custom configuration: " + config);
        
        try (TokenHa tokenHa = new TokenHa(config)) {
            System.out.println("Custom file path: " + tokenHa.getPersistenceFilePath());
            
            // Add tokens quickly due to short cool time
            tokenHa.addIfAvailable("token1");
            Thread.sleep(120);
            tokenHa.addIfAvailable("token2");
            
            System.out.println("Added 2 tokens: " + (tokenHa.getQueueSize() == 2));
            tokenHa.deletePersistenceFile(); // Cleanup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println();
    }
    
    private static void demonstratePropertiesConfig() throws IOException {
        System.out.println("3. PROPERTIES CONFIGURATION");
        System.out.println("----------------------------");
        
        Properties props = new Properties();
        props.setProperty("tokenha.max.tokens", "3");
        props.setProperty("tokenha.cool.time.millis", "50");
        props.setProperty("tokenha.number.of.last.tokens", "1");
        props.setProperty("tokenha.expiration.time.seconds", "20000");
        props.setProperty("tokenha.persistence.file.path", "demo-props-tokens.json");
        
        TokenHaConfig config = TokenHaConfig.fromProperties(props);
        System.out.println("Properties-based config: " + config);
        
        try (TokenHa tokenHa = new TokenHa(config)) {
            System.out.println("Properties file path: " + tokenHa.getPersistenceFilePath());
            tokenHa.deletePersistenceFile(); // Cleanup
        }
        System.out.println();
    }
    
    private static void demonstrateEnvironmentConfig() throws IOException {
        System.out.println("4. ENVIRONMENT VARIABLES CONFIGURATION");
        System.out.println("---------------------------------------");
        
        // Note: In real usage, these would be actual environment variables
        System.out.println("In real usage, you would set environment variables like:");
        System.out.println("export TOKENHA_MAX_TOKENS=25");
        System.out.println("export TOKENHA_COOL_TIME_MILLIS=500");
        System.out.println("export TOKENHA_PERSISTENCE_FILE_PATH=/tmp/tokens.json");
        
        TokenHaConfig config = TokenHaConfig.fromEnvironment();
        System.out.println("Environment-based config (using defaults): " + config);
        
        try (TokenHa tokenHa = new TokenHa(config)) {
            System.out.println("Environment file path: " + tokenHa.getPersistenceFilePath());
            tokenHa.deletePersistenceFile(); // Cleanup
        }
        System.out.println();
    }
    
    private static void demonstrateMixedConfig() throws IOException {
        System.out.println("5. MIXED CONFIGURATION (Override Pattern)");
        System.out.println("------------------------------------------");
        
        // Start with default config
        TokenHaConfig baseConfig = TokenHaConfig.defaultConfig();
        
        // Override specific values
        TokenHaConfig mixedConfig = baseConfig.toBuilder()
            .maxTokens(7)
            .coolTimeToAddMillis(300)
            .persistenceFilePath("demo-mixed-tokens.json")
            .build();
        
        System.out.println("Base config: " + baseConfig);
        System.out.println("Mixed config: " + mixedConfig);
        
        try (TokenHa tokenHa = new TokenHa(mixedConfig)) {
            System.out.println("Mixed config file path: " + tokenHa.getPersistenceFilePath());
            tokenHa.deletePersistenceFile(); // Cleanup
        }
        System.out.println();
    }
}
