package com.github.tsutomunakamura.tokenha;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FilePersistence.
 */
public class FilePersistenceTest {
    
    private static final String TEST_FILE = "test-persistence.json";
    
    @AfterEach
    public void cleanup() {
        // Clean up test file after each test
        FilePersistence fp = new FilePersistence(TEST_FILE);
        fp.deleteFile();
    }
    
    @Test
    public void testSaveAndLoad() {
        FilePersistence filePersistence = new FilePersistence(TEST_FILE);
        
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
    public void testLoadNonExistentFile() {
        FilePersistence filePersistence = new FilePersistence("non-existent-file.json");
        
        // Should not exist
        assertFalse(filePersistence.fileExists());
        
        // Load should return null
        String loadedData = filePersistence.load();
        assertNull(loadedData);
    }
}
