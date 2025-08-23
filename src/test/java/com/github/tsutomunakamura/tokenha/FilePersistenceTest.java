package com.github.tsutomunakamura.tokenha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
    public void testSaveWritesDataToFile() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            String jsonData = "{\"tokens\":[{\"token\":\"abc\",\"timeMillis\":1234567890}]}";
            filePersistence.save(jsonData);

            // Read file content directly to verify
            String fileContent = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(TEST_FILE)), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(jsonData, fileContent);
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testSaveWarnsIfNoFileLock() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            // Simulate no file lock
            java.lang.reflect.Field lockField = FilePersistence.class.getDeclaredField("fileLock");
            lockField.setAccessible(true);
            lockField.set(filePersistence, null);

            String jsonData = "{\"tokens\":[]}";
            // Should still save, but print a warning (cannot assert warning, but should not throw)
            filePersistence.save(jsonData);

            String fileContent = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(TEST_FILE)), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(jsonData, fileContent);
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testSaveThrowsIfPersistenceFileNull() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            // Simulate persistenceFile == null
            java.lang.reflect.Field pfField = FilePersistence.class.getDeclaredField("persistenceFile");
            pfField.setAccessible(true);
            pfField.set(filePersistence, null);

            assertThrows(IllegalStateException.class, () -> filePersistence.save("{\"tokens\":[]}"));
        } catch (Exception e) {
            fail("Exception should not be thrown during test setup: " + e.getMessage());
        }
    }

    // Test cases for "public String load()"

    @Test
    public void testLoadFromExistingFile() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // First save some data
            String testData = "{\"tokens\":[{\"token\":\"loadTest\",\"timeMillis\":1234567890}]}";
            filePersistence.save(testData);
            
            // Then load it back
            String loadedData = filePersistence.load();
            assertNotNull(loadedData);
            assertEquals(testData, loadedData);
        }
    }
    
    @Test
    public void testLoadFromNonExistentFile() {
        String nonExistentFile = "non-existent-file.json";
        try (FilePersistence filePersistence = new FilePersistence(nonExistentFile)) {
            currentTestInstance = filePersistence;
            
            // Delete the file to ensure it doesn't exist
            filePersistence.deleteFile();
            
            // Load should return null for non-existent file
            String loadedData = filePersistence.load();
            assertEquals(null, loadedData);
        }
        
        // Clean up
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(nonExistentFile));
        } catch (java.io.IOException e) {
            // Ignore cleanup error
        }
    }
    
    @Test
    public void testLoadEmptyFile() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // Save empty string to create an empty file
            filePersistence.save("");
            
            // Load should return empty string
            String loadedData = filePersistence.load();
            assertNotNull(loadedData);
            assertEquals("", loadedData);
        }
    }
    
    @Test
    public void testLoadWithIOException() {
        // Test IOException handling by trying to load from an invalid/inaccessible path
        String invalidPath = "/proc/invalid-path/test.json";
        
        // Create FilePersistence with invalid path should throw RuntimeException during initialization
        assertThrows(RuntimeException.class, () -> {
            new FilePersistence(invalidPath);
        });
    }
    
    @Test
    public void testLoadLargeFile() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // Create and save a large JSON string
            StringBuilder largeJson = new StringBuilder("{\"tokens\":[");
            for (int i = 0; i < 1000; i++) {
                if (i > 0) largeJson.append(",");
                largeJson.append("{\"token\":\"token").append(i).append("\",\"timeMillis\":").append(System.currentTimeMillis() + i).append("}");
            }
            largeJson.append("]}");
            
            String largeData = largeJson.toString();
            filePersistence.save(largeData);
            
            // Load and verify
            String loadedData = filePersistence.load();
            assertNotNull(loadedData);
            assertEquals(largeData, loadedData);
        }
    }
    
    @Test
    public void testLoadWithSpecialCharacters() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // Test with special characters including Unicode
            String testData = "{\"tokens\":[{\"token\":\"测试token\",\"special\":\"!@#$%^&*()🚀\",\"timeMillis\":1234567890}]}";
            filePersistence.save(testData);
            
            String loadedData = filePersistence.load();
            assertNotNull(loadedData);
            assertEquals(testData, loadedData);
        }
    }
    
    @Test
    public void testLoadMultipleTimes() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            String testData = "{\"tokens\":[{\"token\":\"multiLoad\",\"timeMillis\":1234567890}]}";
            filePersistence.save(testData);
            
            // Load multiple times to ensure consistency
            for (int i = 0; i < 5; i++) {
                String loadedData = filePersistence.load();
                assertNotNull(loadedData);
                assertEquals(testData, loadedData);
            }
        }
    }

}
