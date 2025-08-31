package com.github.tsutomunakamura.tokenha;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

/**
 * Demonstration test to show the adaptive logging behavior.
 * This test shows how the TokenHa library adapts to different logging configurations.
 */
public class AdaptiveLoggingDemoTest {
    
    @Test
    public void demonstrateAdaptiveLogging() {
        // This test demonstrates that our library uses SLF4J
        // and will automatically adapt to whatever logging framework
        // the consuming application configures
        
        Logger logger = TokenHaLogger.getLogger(AdaptiveLoggingDemoTest.class);
        
        // These will appear in the appropriate format based on the 
        // logging configuration of the consuming application
        logger.trace("This is a TRACE level message");
        logger.debug("This is a DEBUG level message");
        logger.info("This is an INFO level message");
        logger.warn("This is a WARN level message");
        logger.error("This is an ERROR level message");
        
        // Parameterized logging (efficient - no string concatenation unless logged)
        String param1 = "test";
        int param2 = 42;
        logger.info("Parameterized logging example: {} and {}", param1, param2);
        
        System.out.println("✅ Adaptive logging demo completed - check output format above");
        System.out.println("📝 Note: Log format depends on the logging framework configured by the consuming application");
        System.out.println("   - With logback: structured JSON or custom patterns");
        System.out.println("   - With log4j2: XML, JSON, or pattern layouts");
        System.out.println("   - With slf4j-simple: simple console format (as shown above)");
        System.out.println("   - With JUL: Java Util Logging format");
    }
    
    @Test 
    public void demonstrateTokenHaLogging() throws Exception {
        // Create a TokenHa instance to trigger logging
        TokenHaConfig config = new TokenHaConfig.Builder()
            .expirationTimeMillis(5000L)
            .maxTokens(5)
            .build();
            
        try (TokenHa tokenHa = new TokenHa(config)) {
            // Add a token to trigger some logging
            tokenHa.addIfAvailable("demo-token");
            
            System.out.println("✅ TokenHa logging demo completed");
            System.out.println("📝 Check the log output above to see how TokenHa operations are logged");
        }
    }
}
