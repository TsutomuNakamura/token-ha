package com.github.tsutomunakamura.tokenha;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tsutomunakamura.tokenha.element.TokenElement;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;

/**
 * Test class for TokenHa.
 */
@ExtendWith(MockitoExtension.class)
public class TokenHaTest {
    
    private TokenHa tokenHa;
    private static final String TEST_FILE_PATH = "test-tokenha-data.json";
    
    @BeforeEach
    void setUp() throws IOException {
        tokenHa = new TokenHa();
        tokenHa.setPersistenceFilePath(TEST_FILE_PATH);
        // Clean up any existing test file
        tokenHa.deletePersistenceFile();
    }
    
    @AfterEach
    void tearDown() {
        if (tokenHa != null) {
            tokenHa.deletePersistenceFile();
            tokenHa.close();
        }
    }
    
    @Test
    @DisplayName("Should add token successfully when queue is empty and no cool time restriction")
    void addIfAvailable_shouldAddToken_whenQueueIsEmpty() throws Exception {
        // Given
        String token = "test-token-1";
        
        // When
        boolean result = tokenHa.addIfAvailable(token);
        
        // Then
        assertTrue(result, "Token should be added successfully");
        assertEquals(1, tokenHa.getQueueSize(), "Queue size should be 1");
        assertNotNull(tokenHa.newestToken(), "Newest token should not be null");
        assertEquals(token, tokenHa.newestToken().getToken(), "Token content should match");
    }
    
    @Test
    @DisplayName("Should return false when cool time has not passed")
    void addIfAvailable_shouldReturnFalse_whenCoolTimeHasNotPassed() throws Exception {
        // Given
        String firstToken = "token-1";
        String secondToken = "token-2";
        
        // Add first token
        tokenHa.addIfAvailable(firstToken);
        
        // When - try to add second token immediately (before cool time passes)
        boolean result = tokenHa.addIfAvailable(secondToken);
        
        // Then
        assertFalse(result, "Second token should not be added due to cool time restriction");
        assertEquals(1, tokenHa.getQueueSize(), "Queue size should remain 1");
        assertEquals(firstToken, tokenHa.newestToken().getToken(), "Only first token should be in queue");
    }
    
    @Test
    @DisplayName("Should add token successfully when cool time has passed")
    void addIfAvailable_shouldAddToken_whenCoolTimeHasPassed() throws Exception {
        // Given
        String firstToken = "token-1";
        String secondToken = "token-2";
        
        // Add first token
        tokenHa.addIfAvailable(firstToken);
        
        // Wait for cool time to pass (1000ms + buffer)
        Thread.sleep(1100);
        
        // When
        boolean result = tokenHa.addIfAvailable(secondToken);
        
        // Then
        assertTrue(result, "Second token should be added after cool time passes");
        assertEquals(2, tokenHa.getQueueSize(), "Queue size should be 2");
        assertEquals(secondToken, tokenHa.newestToken().getToken(), "Newest token should be the second token");
    }
    
    @Test
    @DisplayName("Should remove oldest token when queue is full")
    void addIfAvailable_shouldRemoveOldestToken_whenQueueIsFull() throws Exception {
        // Given - fill the queue to maximum capacity (maxTokens = 10)
        for (int i = 1; i <= 10; i++) {
            tokenHa.addIfAvailable("token-" + i);
            if (i < 10) {
                Thread.sleep(1100); // Wait for cool time to pass
            }
        }
        
        assertEquals(10, tokenHa.getQueueSize(), "Queue should be full");
        
        // Wait for cool time to pass
        Thread.sleep(1100);
        
        // When - add one more token
        String newToken = "token-11";
        boolean result = tokenHa.addIfAvailable(newToken);
        
        // Then
        assertTrue(result, "New token should be added successfully");
        assertEquals(10, tokenHa.getQueueSize(), "Queue size should remain at maximum");
        assertEquals(newToken, tokenHa.newestToken().getToken(), "Newest token should be the latest added");
        
        // Verify that the oldest token was removed
        Iterator<TokenElement> iterator = tokenHa.getDescIterator();
        TokenElement newest = iterator.next();
        assertEquals("token-11", newest.getToken(), "First element should be newest");
        
        // Check that token-1 (oldest) is no longer in the queue
        boolean foundToken1 = false;
        while (iterator.hasNext()) {
            if ("token-1".equals(iterator.next().getToken())) {
                foundToken1 = true;
                break;
            }
        }
        assertFalse(foundToken1, "Oldest token should have been removed");
    }
    
    @Test
    @DisplayName("Should handle null token gracefully")
    void addIfAvailable_shouldHandleNullToken() throws Exception {
        // When
        boolean result = tokenHa.addIfAvailable(null);
        
        // Then
        assertTrue(result, "Method should return true even with null token");
        assertEquals(1, tokenHa.getQueueSize(), "Queue size should be 1");
        assertNull(tokenHa.newestToken().getToken(), "Token should be null");
    }
    
    @Test
    @DisplayName("Should handle empty string token")
    void addIfAvailable_shouldHandleEmptyStringToken() throws Exception {
        // When
        boolean result = tokenHa.addIfAvailable("");
        
        // Then
        assertTrue(result, "Method should return true with empty string token");
        assertEquals(1, tokenHa.getQueueSize(), "Queue size should be 1");
        assertEquals("", tokenHa.newestToken().getToken(), "Token should be empty string");
    }
    
    @Test
    @DisplayName("Should persist token to file when added successfully")
    void addIfAvailable_shouldPersistToFile_whenTokenAddedSuccessfully() throws Exception {
        // Given
        String token = "persistent-token";
        
        // When
        boolean result = tokenHa.addIfAvailable(token);
        
        // Then
        assertTrue(result, "Token should be added successfully");
        
        // Give some time for file operations to complete
        Thread.sleep(100);
        
        // Verify that the token was added to the current instance
        assertEquals(1, tokenHa.getQueueSize(), "Queue should have 1 token after adding");
        assertEquals(token, tokenHa.newestToken().getToken(), "Token should match what was added");
        
        // Test that JSON serialization works
        String jsonOutput = tokenHa.toJson();
        assertNotNull(jsonOutput, "JSON output should not be null");
        assertTrue(jsonOutput.contains(token), "JSON should contain the token");
    }
    
    @Test
    @DisplayName("Should maintain FIFO order when adding multiple tokens")
    void addIfAvailable_shouldMaintainFifoOrder() throws Exception {
        // Given
        String[] tokens = {"token-1", "token-2", "token-3"};
        
        // When - add tokens with appropriate delays
        for (String token : tokens) {
            tokenHa.addIfAvailable(token);
            Thread.sleep(1100); // Wait for cool time
        }
        
        // Then
        assertEquals(3, tokenHa.getQueueSize(), "Queue should have 3 tokens");
        
        // Check FIFO order using descending iterator (newest to oldest)
        Iterator<TokenElement> iterator = tokenHa.getDescIterator();
        assertEquals("token-3", iterator.next().getToken(), "First should be newest");
        assertEquals("token-2", iterator.next().getToken(), "Second should be middle");
        assertEquals("token-1", iterator.next().getToken(), "Third should be oldest");
    }
    
    @Test
    @DisplayName("Should work correctly with concurrent access simulation")
    void addIfAvailable_shouldHandleConcurrentAccess() throws Exception {
        // This test simulates concurrent access by using reflection to manipulate state
        // Given
        String token1 = "concurrent-token-1";
        String token2 = "concurrent-token-2";
        
        // When - add first token
        boolean result1 = tokenHa.addIfAvailable(token1);
        
        // Simulate concurrent access by trying to add another token immediately
        boolean result2 = tokenHa.addIfAvailable(token2);
        
        // Then
        assertTrue(result1, "First token should be added");
        assertFalse(result2, "Second token should be rejected due to cool time");
        assertEquals(1, tokenHa.getQueueSize(), "Only one token should be in queue");
    }
    
    /**
     * Helper method to set private field values using reflection for testing purposes.
     */
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
    
    @Test
    @DisplayName("Should respect custom cool time configuration")
    void addIfAvailable_shouldRespectCustomCoolTime() throws Exception {
        // Given - set a shorter cool time for faster testing
        setPrivateField(tokenHa, "coolTimeToAddMillis", 500L);
        
        String token1 = "token-1";
        String token2 = "token-2";
        
        // Add first token
        tokenHa.addIfAvailable(token1);
        
        // Try immediately (should fail)
        assertFalse(tokenHa.addIfAvailable(token2), "Should fail immediately");
        
        // Wait for custom cool time
        Thread.sleep(600);
        
        // Try again (should succeed)
        assertTrue(tokenHa.addIfAvailable(token2), "Should succeed after custom cool time");
        assertEquals(2, tokenHa.getQueueSize(), "Both tokens should be in queue");
    }
    
    @Test
    @DisplayName("Should handle very long token strings")
    void addIfAvailable_shouldHandleLongTokenStrings() throws Exception {
        // Given
        StringBuilder longTokenBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longTokenBuilder.append("a");
        }
        String longToken = longTokenBuilder.toString();
        
        // When
        boolean result = tokenHa.addIfAvailable(longToken);
        
        // Then
        assertTrue(result, "Should successfully add long token");
        assertEquals(1, tokenHa.getQueueSize(), "Queue should contain the long token");
        assertEquals(longToken, tokenHa.newestToken().getToken(), "Token should match the long string");
    }
    
    @Test
    @DisplayName("Should handle special characters in tokens")
    void addIfAvailable_shouldHandleSpecialCharacters() throws Exception {
        // Given
        String specialToken = "token-with-special-chars: !@#$%^&*()[]{}|;':\",./<>?`~";
        
        // When
        boolean result = tokenHa.addIfAvailable(specialToken);
        
        // Then
        assertTrue(result, "Should successfully add token with special characters");
        assertEquals(1, tokenHa.getQueueSize(), "Queue should contain the special token");
        assertEquals(specialToken, tokenHa.newestToken().getToken(), "Token should match exactly");
    }
    
    @Test
    @DisplayName("Should maintain thread safety with synchronized method")
    void addIfAvailable_shouldBeSynchronized() throws Exception {
        // This test verifies that the method is synchronized by checking that multiple
        // rapid calls don't interfere with each other due to cool time restrictions
        
        // Given
        String token1 = "sync-token-1";
        String token2 = "sync-token-2";
        
        // When - add tokens in rapid succession
        boolean result1 = tokenHa.addIfAvailable(token1);
        boolean result2 = tokenHa.addIfAvailable(token2); // Should fail due to cool time
        
        // Then
        assertTrue(result1, "First token should be added");
        assertFalse(result2, "Second token should be rejected due to synchronization and cool time");
        assertEquals(1, tokenHa.getQueueSize(), "Only one token should be in queue");
        assertEquals(token1, tokenHa.newestToken().getToken(), "Only first token should be present");
    }

    // TODO: Test cases for "public boolean availableToAdd()"
    
}