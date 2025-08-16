package com.github.tsutomunakamura.tokenha;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FilePersistence.
 */
public class FilePersistenceTest {
    
    private static final String TEST_FILE = "test-persistence.json";
    private FilePersistence currentTestInstance; // Track the current test instance
    
    @AfterEach
    public void cleanup() {
        // Close the current test instance first to release the lock
        if (currentTestInstance != null) {
            currentTestInstance.close();
            currentTestInstance = null;
        }
        
        // Clean up test files after each test
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(TEST_FILE));
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("new-file.json"));
        } catch (java.io.IOException e) {
            System.err.println("Failed to delete test file: " + e.getMessage());
        }
    }
    
    @Test
    public void testSaveAndLoad() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // Test save
            String testData = "{\"tokens\":[{\"token\":\"test123\",\"timeMillis\":1692186615000}]}";
            filePersistence.save(testData);
            
            // Test file exists
            assertTrue(filePersistence.fileExists());
            
            // Test load
            String loadedData = filePersistence.load();
            assertNotNull(loadedData);
            assertEquals(testData, loadedData);
            
            System.out.println("Saved data: " + testData);
            System.out.println("Loaded data: " + loadedData);
        }
    }
    
    @Test
    public void testTokenHaWithFilePersistence() {
        try (TokenHa tokenHa = new TokenHa()) {
            tokenHa.setPersistenceFilePath(TEST_FILE);
            
            // Add a token (should trigger save)
            tokenHa.addIfAvailable("testToken");
            
            // Check file was created
            assertTrue(tokenHa.persistenceFileExists());
            
            // Load and verify
            tokenHa.loadFromFile();
            
            System.out.println("Current JSON: " + tokenHa.toJson());
            
            // Clean up
            assertTrue(tokenHa.deletePersistenceFile());
        }
    }
    
    @Test
    public void testLoadFromNewFile() {
        try (FilePersistence filePersistence = new FilePersistence("new-file.json")) {
            currentTestInstance = filePersistence;
            
            // File gets created when FilePersistence is instantiated (for locking)
            assertTrue(filePersistence.fileExists());
            
            // Load from new/empty file should return empty content
            String loadedData = filePersistence.load();
            assertNotNull(loadedData);
            assertEquals("", loadedData); // Empty file returns empty string
        }
    }
}
