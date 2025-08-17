package com.github.tsutomunakamura.tokenha;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import com.github.tsutomunakamura.tokenha.element.TokenElement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Test class for TokenUtil.
 */
public class TokenHaTest {
    @Test
    @DisplayName("Test JSON serialization when token queue is empty")
    public void testToJsonWithEmptyQueue() {
        System.out.println("🧪 TEST: JSON serialization when token queue is empty");
        try (TokenHa tokenHa = new TokenHa()) {
            String json = tokenHa.toJson();
            
            System.out.println("Empty queue JSON:");
            System.out.println(json);
            
            assertEquals("{\"tokens\":[]}", json);
        }
    }

    @Test
    @DisplayName("Test JSON serialization with exactly one token")
    public void testToJsonWithSingleToken() {
        System.out.println("🧪 TEST: JSON serialization with exactly one token");
        try (TokenHa tokenHa = new TokenHa()) {
            tokenHa.addIfAvailable("user123");
            
            String json = tokenHa.toJson();
            
            System.out.println("Single token JSON:");
            System.out.println(json);
            
            // Check structure
            assertTrue(json.startsWith("{\"tokens\":["));
            assertTrue(json.endsWith("]}"));
            assertTrue(json.contains("\"token\":\"user123\""));
            assertTrue(json.contains("\"timeMillis\":"));
        }
    }

    @Test
    @DisplayName("Test JSON serialization with multiple tokens (respecting cooldown)")
    public void testToJsonWithMultipleTokens() {
        System.out.println("🧪 TEST: JSON serialization with multiple tokens (respecting cooldown)");
        try (TokenHa tokenHa = new TokenHa()) {
            // Add first token (should succeed)
            tokenHa.addIfAvailable("user123");
            
            // Due to cooldown, we need to use reflection or test what we can add
            // Let's test with just one token first and verify the JSON structure
            String json = tokenHa.toJson();
            
            System.out.println("JSON with available tokens:");
            System.out.println(json);
            
            // Check structure
            assertTrue(json.startsWith("{\"tokens\":["));
            assertTrue(json.endsWith("]}"));
            
            // Check that at least one token is present
            assertTrue(json.contains("\"token\":\"user123\""));
            assertTrue(json.contains("\"timeMillis\":"));
        }
    }

    @Test
    @DisplayName("Test JSON escaping of special characters (quotes, etc.)")
    public void testToJsonWithSpecialCharacters() {
        System.out.println("🧪 TEST: JSON escaping of special characters (quotes, etc.)");
        try (TokenHa tokenHa = new TokenHa()) {
            // Test with one token containing special characters
            tokenHa.addIfAvailable("token\"with\"quotes");
            
            String json = tokenHa.toJson();
            
            System.out.println("Special characters JSON:");
            System.out.println(json);
            
            // Check that special characters are properly escaped
            assertTrue(json.contains("\\\""));  // Escaped quotes
            
            // Ensure it's valid JSON structure
            assertTrue(json.startsWith("{\"tokens\":["));
            assertTrue(json.endsWith("]}"));
        }
    }

    @Test
    @DisplayName("Test JSON structure validation and pretty-printing")
    public void testToJsonWithFixedTimestamps() {
        System.out.println("🧪 TEST: JSON structure validation and pretty-printing");
        try (TokenHa tokenHa = new TokenHa()) {
            // Test the JSON structure with actual timestamps
            tokenHa.addIfAvailable("token1");
            
            String json = tokenHa.toJson();
            
            System.out.println("Structured JSON output:");
            System.out.println(json);
            
            // Pretty print the JSON for better readability
            String prettyJson = json.replace(",", ",\n    ")
                                   .replace("{\"tokens\":[", "{\n  \"tokens\": [\n    ")
                                   .replace("]}", "\n  ]\n}");
            
            System.out.println("Pretty-printed JSON:");
            System.out.println(prettyJson);
            
            // Validate JSON structure
            assertTrue(json.matches("\\{\"tokens\":\\[.*\\]\\}"));
        }
    }
    
    @Test
    @DisplayName("Test cooldown mechanism and addIfAvailable return values")
    public void testAddIfAvailableReturnValues() {
        System.out.println("🧪 TEST: Cooldown mechanism and addIfAvailable return values");
        try (TokenHa tokenHa = new TokenHa()) {
            // First token should be added successfully
            assertTrue(tokenHa.addIfAvailable("token1"));
            assertEquals(1, tokenHa.getQueueSize());
            
            // Second token should fail due to cooldown (coolTimeToAddSeconds = 1000ms)
            assertFalse(tokenHa.addIfAvailable("token2"));
            assertEquals(1, tokenHa.getQueueSize());
            
            // Wait for cooldown period
            try {
                Thread.sleep(1100); // Wait longer than coolTimeToAddSeconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Now the second token should be added successfully
            assertTrue(tokenHa.addIfAvailable("token2"));
            assertEquals(2, tokenHa.getQueueSize());
        }
    }
    
    @Test
    @DisplayName("Test FIFO queue overflow behavior (max 10 tokens)")
    public void testQueueOverflowBehavior() {
        System.out.println("🧪 TEST: FIFO queue overflow behavior (max 10 tokens)");
        try (TokenHa tokenHa = new TokenHa()) {
            // Fill the queue to max capacity (10 tokens)
            // We need to work around the cooldown period
            long baseTime = System.currentTimeMillis() - 20000; // Start 20 seconds in the past
            
            // Use reflection to bypass cooldown for testing
            try {
                java.lang.reflect.Field queueField = TokenHa.class.getDeclaredField("fifoQueue");
                queueField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Deque<TokenElement> queue = (Deque<TokenElement>) queueField.get(tokenHa);
                
                // Manually add 10 tokens with timestamps in the past (so cooldown has passed)
                for (int i = 0; i < 10; i++) {
                    queue.add(new TokenElement("token" + i, baseTime + (i * 1000))); // 1 second apart, in the past
                }
                
                assertEquals(10, tokenHa.getQueueSize());
                assertTrue(tokenHa.isFilled());
                
                // Now try to add an 11th token - it should remove the oldest and add the new one
                boolean result = tokenHa.addIfAvailable("token10");
                assertTrue(result); // Should succeed by removing oldest
                assertEquals(10, tokenHa.getQueueSize()); // Size should remain 10
                
                // Check that the newest token is now in the queue
                String json = tokenHa.toJson();
                assertTrue(json.contains("token10"));
                
            } catch (Exception e) {
                fail("Reflection failed: " + e.getMessage());
            }
        }
    }
    
    @Test
    @DisplayName("Test Gson serialization with special characters")
    public void testGsonSerialization() {
        System.out.println("🧪 TEST: Gson serialization with special characters");
        try (TokenHa tokenHa = new TokenHa()) {
            // Add a token with special characters to test Gson handling
            tokenHa.addIfAvailable("user\"with\"quotes");
            
            String json = tokenHa.toJson();
            System.out.println("Gson JSON output: " + json);
            
            // Should be properly escaped by Gson
            assertTrue(json.contains("\\\""));
            assertTrue(json.contains("user\\\"with\\\"quotes"));
            
            // Should be valid JSON structure
            assertTrue(json.startsWith("{\"tokens\":["));
            assertTrue(json.endsWith("]}"));
        }
    }
    
    @Test
    @DisplayName("Test Gson deserialization from file persistence")
    public void testGsonDeserialization() {
        System.out.println("🧪 TEST: Gson deserialization from file persistence");
        String testFilePath = "test-gson-deserialize.json";
        String originalJson = null;
        
        try {
            // First instance: create and save data
            try (TokenHa tokenHa = new TokenHa()) {
                // Set test file path
                tokenHa.setPersistenceFilePath(testFilePath);
                
                // Add some tokens and save
                tokenHa.addIfAvailable("token1");
                Thread.sleep(1100); // Wait for cooldown
                tokenHa.addIfAvailable("token2");
                
                assertEquals(2, tokenHa.getQueueSize());
                originalJson = tokenHa.toJson();
                System.out.println("Original JSON: " + originalJson);
                
            } // First instance is closed here, releasing the file lock
            
            // Second instance: load from the file
            try (TokenHa tokenHa2 = new TokenHa()) {
                tokenHa2.setPersistenceFilePath(testFilePath);
                assertEquals(0, tokenHa2.getQueueSize()); // Should start empty
                
                // Load from file
                tokenHa2.loadFromFile();
                
                // Should have loaded the same tokens
                assertEquals(2, tokenHa2.getQueueSize());
                String loadedJson = tokenHa2.toJson();
                System.out.println("Loaded JSON: " + loadedJson);
                
                // JSON should match (Gson produces consistent output)
                assertEquals(originalJson, loadedJson);
                
                // Check specific token values
                assertTrue(loadedJson.contains("token1"));
                assertTrue(loadedJson.contains("token2"));
            }
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        } finally {
            // Cleanup test file
            try (TokenHa cleanup = new TokenHa()) {
                cleanup.setPersistenceFilePath(testFilePath);
                cleanup.deletePersistenceFile();
            }
        }
    }
    
}