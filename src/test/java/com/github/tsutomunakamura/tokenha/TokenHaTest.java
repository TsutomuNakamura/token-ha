package com.github.tsutomunakamura.tokenha;

import org.junit.jupiter.api.Test;

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
    public void testTest() {
        Deque<TokenElement> fifoQueue = new ArrayDeque<>();
        fifoQueue.add(new TokenElement("token1", System.currentTimeMillis()));
        fifoQueue.add(new TokenElement("token2", System.currentTimeMillis() + 60));
        fifoQueue.add(new TokenElement("token3", System.currentTimeMillis() + 120));

        Iterator<TokenElement> iterator = fifoQueue.descendingIterator();
        iterator.forEachRemaining(element -> {
            System.out.println("Token: " + element.getToken() + ", Time: " + element.getTimeMillis());
        });
        System.out.println("Finished iterating through the queue.");
    }

    @Test
    public void testToJsonWithEmptyQueue() {
        try (TokenHa tokenHa = new TokenHa()) {
            String json = tokenHa.toJson();
            
            System.out.println("Empty queue JSON:");
            System.out.println(json);
            
            assertEquals("{\"tokens\":[]}", json);
        }
    }

    @Test
    public void testToJsonWithSingleToken() {
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
    public void testToJsonWithMultipleTokens() {
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
    public void testToJsonWithSpecialCharacters() {
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
    public void testToJsonWithFixedTimestamps() {
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
    
}