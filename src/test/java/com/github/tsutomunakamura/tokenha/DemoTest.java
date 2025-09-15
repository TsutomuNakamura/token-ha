package com.github.tsutomunakamura.tokenha;

import org.junit.jupiter.api.Test;

import com.github.tsutomunakamura.tokenha.element.TokenElement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Iterator;

/**
 * Comprehensive demonstration of the TokenHa library features as test cases.
 * These tests serve as usage examples and are disabled by default.
 * 
 * To run these demo tests manually:
 * - In IDE: Remove @Disabled annotations or run individual test methods
 * - Maven: mvn test -Dtest=DemoTest#testMethodName 
 * - Or enable all: temporarily remove @Disabled annotations
 * 
 * These tests demonstrate:
 * - Basic token operations and cooldown handling
 * - Queue state management and navigation
 * - JSON serialization/deserialization with Gson
 * - File persistence with exclusive locking
 * - Overflow behavior and FIFO queue management
 * - Complete workflow integration
 */
public class DemoTest {
    
    private TokenHa tokenHa;
    private String testFilePath;
    
    @BeforeEach
    public void setUp() throws IOException {
        testFilePath = "demo-test-tokens.json";
        tokenHa = new TokenHa();
        tokenHa.setPersistenceFilePath(testFilePath);
    }
    
    @AfterEach
    public void tearDown() {
        if (tokenHa != null) {
            tokenHa.deletePersistenceFile(); // Cleanup test file
            tokenHa.close();
        }
    }
    
    @Test
    @Disabled("Run manyalli with \"mvn test -Dtest=DemoTest#testBasicTokenOperations -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition\"")
    public void testBasicTokenOperations() throws InterruptedException {
        System.out.println("=== Test: Basic Token Operations ===");
        
        // Check initial state
        assertTrue(tokenHa.availableToAdd(), "Should be able to add tokens initially");
        assertEquals(0, tokenHa.getQueueSize(), "Queue should start empty");
        
        // Add some tokens
        System.out.println("Adding tokens...");
        boolean result1 = tokenHa.addIfAvailable("user1");
        assertTrue(result1, "Should successfully add first token");
        assertEquals(1, tokenHa.getQueueSize(), "Queue size should be 1");
        
        boolean result2 = tokenHa.addIfAvailable("user2");
        assertFalse(result2, "Should fail due to cooldown");
        assertEquals(1, tokenHa.getQueueSize(), "Queue size should remain 1");
        
        // Wait for cooldown and try again
        Thread.sleep(1100);
        boolean result3 = tokenHa.addIfAvailable("user3");
        assertTrue(result3, "Should succeed after cooldown");
        assertEquals(2, tokenHa.getQueueSize(), "Queue size should be 2");
        
        System.out.println("✅ Basic token operations working correctly");
    }
    
    @Test
    @Disabled("Run manually with \"mvn test -Dtest=DemoTest#testQueueStateAndNavigation -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition\"")
    public void testQueueStateAndNavigation() throws InterruptedException {
        System.out.println("=== Test: Queue State and Navigation ===");
        
        // Add tokens with proper cooldown
        tokenHa.addIfAvailable("token1");
        Thread.sleep(1100);
        tokenHa.addIfAvailable("token2");
        Thread.sleep(1100);
        tokenHa.addIfAvailable("token3");
        
        // Test queue state
        assertEquals(3, tokenHa.getQueueSize(), "Should have 3 tokens");
        assertFalse(tokenHa.isFilled(), "Should not be filled (max is 10)");
        
        // Test newest token
        TokenElement newest = tokenHa.newestToken();
        assertNotNull(newest, "Should have a newest token");
        assertEquals("token3", newest.getToken(), "Newest should be token3");
        
        // Test iterator (newest to oldest)
        Iterator<TokenElement> iter = tokenHa.getDescIterator();
        assertTrue(iter.hasNext(), "Iterator should have elements");
        
        int count = 0;
        String[] expectedOrder = {"token3", "token2", "token1"};
        while (iter.hasNext()) {
            TokenElement element = iter.next();
            assertEquals(expectedOrder[count], element.getToken(), 
                "Token order should be newest to oldest");
            count++;
        }
        assertEquals(3, count, "Should iterate through all tokens");
        
        System.out.println("✅ Queue state and navigation working correctly");
    }
    
    @Test
    @Disabled("Run manually with \"mvn test -Dtest=DemoTest#testCooldownMechanism -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition\"")
    public void testCooldownMechanism() throws InterruptedException {
        System.out.println("=== Test: Cooldown Mechanism ===");
        
        // Add first token
        assertTrue(tokenHa.addIfAvailable("user1"), "First token should succeed");
        assertTrue(tokenHa.availableToAdd() == false, "Should not be available immediately after adding");
        
        // Try to add immediately (should fail)
        assertFalse(tokenHa.addIfAvailable("user2"), "Should fail due to cooldown");
        
        // Wait for cooldown period
        Thread.sleep(1100);
        
        // Should now be available
        assertTrue(tokenHa.availableToAdd(), "Should be available after cooldown");
        assertTrue(tokenHa.addIfAvailable("user2"), "Should succeed after cooldown");
        
        System.out.println("✅ Cooldown mechanism working correctly");
    }
    
    @Test
    @Disabled("Run manually with \"mvn test -Dtest=DemoTest#testJsonSerializationWithGson -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition\"")
    public void testJsonSerializationWithGson() throws InterruptedException {
        System.out.println("=== Test: JSON Serialization with Gson ===");
        
        // Test empty queue JSON
        String emptyJson = tokenHa.toJson();
        assertEquals("{\"tokens\":[]}", emptyJson, "Empty queue should produce empty JSON array");
        
        // Add tokens and test JSON output
        tokenHa.addIfAvailable("user1");
        Thread.sleep(1100);
        tokenHa.addIfAvailable("user\"with\"quotes"); // Test special characters
        
        String json = tokenHa.toJson();
        
        // Verify JSON structure
        assertTrue(json.startsWith("{\"tokens\":["), "JSON should start with tokens array");
        assertTrue(json.endsWith("]}"), "JSON should end properly");
        
        // Verify content
        assertTrue(json.contains("\"token\":\"user1\""), "Should contain user1");
        assertTrue(json.contains("\"token\":\"user\\\"with\\\"quotes\""), "Should escape quotes properly");
        assertTrue(json.contains("\"timeMillis\":"), "Should contain timestamps");
        
        System.out.println("Generated JSON: " + json);
        System.out.println("✅ Gson JSON serialization working correctly");
    }
    
    @Test
    @Disabled("Run manually with \"mvn test -Dtest=DemoTest#testFilePersistenceWithDeserialization -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition\"")
    public void testFilePersistenceWithDeserialization() throws InterruptedException {
        System.out.println("=== Test: File Persistence with Gson Deserialization ===");
        
        // Add some tokens
        tokenHa.addIfAvailable("persistent1");
        Thread.sleep(1100);
        tokenHa.addIfAvailable("persistent2");
        
        assertEquals(2, tokenHa.getQueueSize(), "Should have 2 tokens before save");
        String originalJson = tokenHa.toJson();
        
        // Verify file exists
        assertTrue(tokenHa.persistenceFileExists(), "Persistence file should exist after adding tokens");
        
        try {
            // Close first instance and create new one
            tokenHa.close();
            tokenHa = new TokenHa();
            tokenHa.setPersistenceFilePath(testFilePath);
            
            // Should start empty
            assertEquals(0, tokenHa.getQueueSize(), "New instance should start empty");
            
            // Load from file
            tokenHa.loadFromFile();
        } catch (java.io.IOException e) {
            fail("IOException occurred during loadFromFile: " + e.getMessage());
        }
        
        // Should have loaded the tokens
        assertEquals(2, tokenHa.getQueueSize(), "Should have loaded 2 tokens");
        String loadedJson = tokenHa.toJson();
        
        // JSON should match
        assertEquals(originalJson, loadedJson, "Loaded JSON should match original");
        
        // Verify specific tokens
        assertTrue(loadedJson.contains("persistent1"), "Should contain persistent1");
        assertTrue(loadedJson.contains("persistent2"), "Should contain persistent2");
        
        System.out.println("Original JSON: " + originalJson);
        System.out.println("Loaded JSON: " + loadedJson);
        System.out.println("✅ File persistence with Gson deserialization working correctly");
    }
    
    @Test
    @Disabled("Run manually with \"mvn test -Dtest=DemoTest#testTokenEviction -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition\"")
    public void testTokenEviction() throws InterruptedException {
        System.out.println("=== Test: Token Eviction ===");
        
        // Add a token and test immediate eviction (should find no expired tokens)
        tokenHa.addIfAvailable("recent_token");
        
        var expiredTokens = tokenHa.evictExpiredTokens();
        assertNull(expiredTokens, "Should not evict recently added tokens");
        
        // Note: Testing actual time-based eviction would require waiting 60+ seconds
        // or using reflection to manipulate timestamps, which is covered in other tests
        
        System.out.println("✅ Token eviction mechanism available (full time-based testing in other test cases)");
    }
    
    @Test
    @Disabled("Run manually with \"mvn test -Dtest=DemoTest#testOverflowBehaviorWithDeserialization -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition\"")
    public void testOverflowBehaviorWithDeserialization() throws InterruptedException {
        System.out.println("=== Test: Overflow Behavior with Deserialization ===");
        
        TokenHa setupTokenHa = null;
        try {
            // Setup: Create tokens that exceed maxTokens
            setupTokenHa = new TokenHa();
            setupTokenHa.setPersistenceFilePath("overflow-test.json");

            // Add more tokens than the limit (default maxTokens is 10)
            for (int i = 1; i <= 12; i++) {
                setupTokenHa.addIfAvailable("overflow_token_" + i);
                Thread.sleep(1100); // Wait longer than cooldown time (1000ms)
            }
            
            // Force save by using the existing mechanism
            String json = setupTokenHa.toJson();
            assertTrue(json.contains("overflow_token_12"), "Should contain the last token");
            
        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        } finally {
            // Ensure the setup instance is properly closed before creating load instance
            setupTokenHa.close();
        }
        
        // Now test loading with a fresh instance
        try (TokenHa loadTokenHa = new TokenHa()) {
            loadTokenHa.setPersistenceFilePath("overflow-test.json");
            try {
                loadTokenHa.loadFromFile();
            } catch (java.io.IOException e) {
                fail("IOException occurred during loadFromFile: " + e.getMessage());
            }
            
            // Should have limited to maxTokens (10)
            assertTrue(loadTokenHa.getQueueSize() <= 10, "Should not exceed maxTokens after loading");
            
            // Should contain the newest tokens
            String loadedJson = loadTokenHa.toJson();
            assertTrue(loadedJson.contains("overflow_token_12"), "Should keep the newest tokens");
            
            System.out.println("Loaded " + loadTokenHa.getQueueSize() + " tokens (limited from 12)");
            System.out.println("✅ Overflow behavior with deserialization working correctly");
            
            // Cleanup
            loadTokenHa.deletePersistenceFile();
        } catch (IOException e) {
            fail("IOException during load instance creation: " + e.getMessage());
        }
    }
    
    @Test
    @Disabled("Run manually with \"mvn test -Dtest=DemoTest#testCompleteWorkflow -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition\"")
    public void testCompleteWorkflow() throws InterruptedException {
        System.out.println("=== Test: Complete TokenHa Workflow ===");
        
        // This test demonstrates the complete workflow as a single test case
        
        // 1. Initial setup
        assertTrue(tokenHa.availableToAdd(), "✅ Initial state: ready to accept tokens");
        
        // 2. Add tokens with cooldown handling
        assertTrue(tokenHa.addIfAvailable("workflow_user1"), "✅ Added first token");
        assertFalse(tokenHa.addIfAvailable("workflow_user2"), "✅ Cooldown prevents immediate second token");
        
        Thread.sleep(1100);
        assertTrue(tokenHa.addIfAvailable("workflow_user2"), "✅ Added second token after cooldown");
        
        // 3. Verify queue state
        assertEquals(2, tokenHa.getQueueSize(), "✅ Queue contains expected number of tokens");
        assertEquals("workflow_user2", tokenHa.newestToken().getToken(), "✅ Newest token is correct");
        
        // 4. Test JSON serialization
        String json = tokenHa.toJson();
        assertTrue(json.contains("workflow_user1") && json.contains("workflow_user2"), 
                  "✅ JSON contains both tokens");
        
        // 5. Test persistence and loading
        String originalJson = tokenHa.toJson();
        tokenHa.close();

        try {
            tokenHa = new TokenHa();
            tokenHa.setPersistenceFilePath(testFilePath);
            tokenHa.loadFromFile();
        } catch (java.io.IOException e) {
            fail("IOException occurred during loadFromFile: " + e.getMessage());
        }
        
        assertEquals(originalJson, tokenHa.toJson(), "✅ Persistence and loading maintains data integrity");
        
        System.out.println("✅ Complete workflow test passed - all features working together!");
    }
}
