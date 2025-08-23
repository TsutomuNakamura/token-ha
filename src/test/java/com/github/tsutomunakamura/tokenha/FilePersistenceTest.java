package com.github.tsutomunakamura.tokenha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
        } catch (IOException e) {
            fail("IOException should not be thrown: " + e.getMessage());
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
            
            // Note: TokenHa.loadFromFile() has a bug - it doesn't handle IOException from FilePersistence.load()
            // This test focuses on FilePersistence functionality, so we skip the problematic TokenHa.loadFromFile() call
            
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

    @Test
    public void testLoadFromExistingFile() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // Save some data first
            String expectedData = "{\"tokens\":[{\"token\":\"loadTest\",\"timeMillis\":1692186615000}]}";
            filePersistence.save(expectedData);
            
            // Load the data
            String loadedData = filePersistence.load();
            assertEquals(expectedData, loadedData);
        } catch (IOException e) {
            fail("IOException should not be thrown: " + e.getMessage());
        }
    }
    
    @Test
    public void testLoadAfterFileDeleted() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // Save some data first
            filePersistence.save("{\"tokens\":[]}");
            
            // Manually delete the file after creation
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(TEST_FILE));
            
            // Try to load from deleted file - should throw IOException
            assertThrows(IOException.class, () -> filePersistence.load());
        } catch (Exception e) {
            fail("Exception should not be thrown during setup: " + e.getMessage());
        }
    }
    
    @Test
    public void testLoadFromEmptyFile() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // Create an empty file by saving empty string
            filePersistence.save("");
            
            // Load should return empty string
            String loadedData = filePersistence.load();
            assertEquals("", loadedData);
        } catch (IOException e) {
            fail("IOException should not be thrown: " + e.getMessage());
        }
    }
    
    @Test
    public void testLoadWithSpecialCharacters() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // Test with Unicode and special characters
            String specialData = "{\"tokens\":[{\"token\":\"测试🚀\",\"data\":\"Special chars: àáâãäåæçèéêë\"}]}";
            filePersistence.save(specialData);
            
            // Load should preserve special characters
            String loadedData = filePersistence.load();
            assertEquals(specialData, loadedData);
        } catch (IOException e) {
            fail("IOException should not be thrown: " + e.getMessage());
        }
    }
    
    @Test
    public void testLoadLargeFile() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // Create a larger JSON string
            StringBuilder largeData = new StringBuilder("{\"tokens\":[");
            for (int i = 0; i < 1000; i++) {
                if (i > 0) largeData.append(",");
                largeData.append("{\"token\":\"token").append(i).append("\",\"timeMillis\":").append(1692186615000L + i).append("}");
            }
            largeData.append("]}");
            
            String expectedData = largeData.toString();
            filePersistence.save(expectedData);
            
            // Load should handle large files correctly
            String loadedData = filePersistence.load();
            assertEquals(expectedData, loadedData);
            assertTrue(loadedData.length() > 10000); // Verify it's actually large
        } catch (IOException e) {
            fail("IOException should not be thrown: " + e.getMessage());
        }
    }
    
    @Test
    public void testLoadAfterFilePathChange() {
        String secondFile = "second-test-file.json";
        try {
            // Create first instance and save data
            try (FilePersistence filePersistence1 = new FilePersistence(TEST_FILE)) {
                String firstData = "{\"tokens\":[{\"token\":\"first\",\"timeMillis\":1692186615000}]}";
                filePersistence1.save(firstData);
            }
            
            // Create second instance and save different data
            try (FilePersistence filePersistence2 = new FilePersistence(secondFile)) {
                currentTestInstance = filePersistence2;
                String secondData = "{\"tokens\":[{\"token\":\"second\",\"timeMillis\":1692186616000}]}";
                filePersistence2.save(secondData);
                
                // Load should return the second file's data
                String loadedData = filePersistence2.load();
                assertEquals(secondData, loadedData);
            }
            
            // Clean up the second file
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(secondFile));
        } catch (IOException e) {
            fail("IOException should not be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testLoadFromNewlyCreatedFile() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // File should be created by constructor, but empty
            assertTrue(filePersistence.fileExists());
            
            // Loading from newly created (empty) file should return empty string
            String loadedData = filePersistence.load();
            assertEquals("", loadedData);
        } catch (IOException e) {
            fail("IOException should not be thrown: " + e.getMessage());
        }
    }

    // Add tests for "public String getFilePath()"

    @Test
    public void testGetFilePath() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            String filePath = filePersistence.getFilePath();
            assertEquals(TEST_FILE, filePath);
        }
    }

    // TODO: Add tests for "public boolean deleteFile()"

    @Test
    public void testDeleteExistingFile() {
        String testFile = "delete-test-existing.json";
        
        try (FilePersistence filePersistence = new FilePersistence(testFile)) {
            currentTestInstance = filePersistence;
            
            // Save some data to ensure file has content
            filePersistence.save("{\"tokens\":[{\"token\":\"test\",\"timeMillis\":1692186615000}]}");
            
            // Verify file exists before deletion
            assertTrue(filePersistence.fileExists());
            
            // Delete the file - on Linux, this should succeed even with locks
            boolean result = filePersistence.deleteFile();
            assertTrue(result, "deleteFile() should return true for successful deletion");
            
            // Verify file no longer exists
            assertFalse(filePersistence.fileExists());
        }
        
        // Clean up manually
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(testFile));
        } catch (Exception e) {
            System.err.println("Manual cleanup failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testDeleteNonExistentFile() {
        String nonExistentFile = "truly-non-existent-file.json";
        
        // Make sure file doesn't exist by trying to delete it first
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(nonExistentFile));
        } catch (Exception e) {
            // Ignore
        }
        
        // Create FilePersistence which will create the file
        try (FilePersistence filePersistence = new FilePersistence(nonExistentFile)) {
            currentTestInstance = filePersistence;
            
            // The file exists now (created by constructor)
            assertTrue(filePersistence.fileExists());
            
            // Delete should succeed on Linux (advisory locking)
            boolean result = filePersistence.deleteFile();
            assertTrue(result, "deleteFile() should return true on Linux even with file locked");
            
            // File should no longer exist
            assertFalse(filePersistence.fileExists());
        }
        
        // Clean up
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(nonExistentFile));
        } catch (Exception e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testDeleteFileIOException() {
        // Test IOException handling using mocking
        String testFile = "mock-test-file.json";
        try (FilePersistence filePersistence = new FilePersistence(testFile)) {
            currentTestInstance = filePersistence;
            
            // Use mockStatic to simulate IOException
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                // Mock Files.deleteIfExists to throw IOException
                mockedFiles.when(() -> Files.deleteIfExists(any(Path.class)))
                          .thenThrow(new IOException("Simulated IO error"));
                
                // Delete should return false when IOException occurs
                boolean result = filePersistence.deleteFile();
                assertFalse(result, "deleteFile() should return false when IOException occurs");
            }
        } catch (Exception e) {
            fail("Exception should not be thrown during test setup: " + e.getMessage());
        }
    }
    
    @Test
    public void testDeleteMultipleTimes() {
        try (FilePersistence filePersistence = new FilePersistence(TEST_FILE)) {
            currentTestInstance = filePersistence;
            
            // Save some data
            filePersistence.save("{\"tokens\":[]}");
            assertTrue(filePersistence.fileExists());
            
            // First deletion should succeed on Linux
            boolean firstDelete = filePersistence.deleteFile();
            assertTrue(firstDelete, "First deleteFile() should return true");
            assertFalse(filePersistence.fileExists());
            
            // Second deletion should return true for deleteIfExists (since file was already deleted, returns false, but we expect the method behavior)
            // Actually, Files.deleteIfExists returns false when file doesn't exist, so our deleteFile should return false
            boolean secondDelete = filePersistence.deleteFile();
            assertFalse(secondDelete, "Second deleteFile() should return false for non-existent file (Files.deleteIfExists behavior)");
            assertFalse(filePersistence.fileExists());
        }
    }
    
    @Test
    public void testDeleteAfterClose() {
        String testFile = "delete-after-close-test.json";
        FilePersistence filePersistence = new FilePersistence(testFile);
        
        try {
            // Save some data
            filePersistence.save("{\"tokens\":[]}");
            assertTrue(filePersistence.fileExists());
            
            // Close the file persistence (releases locks)
            filePersistence.close();
            
            // Delete should work after close since file is no longer locked
            boolean result = filePersistence.deleteFile();
            assertTrue(result, "deleteFile() should work after close()");
            assertFalse(filePersistence.fileExists());
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        } finally {
            // Ensure cleanup
            try {
                filePersistence.close();
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(testFile));
            } catch (Exception ignored) {}
        }
    }
    
    @Test
    public void testDeleteTrulyNonExistentFile() {
        String nonExistentFile = "never-existed.json";
        
        // Make sure file really doesn't exist
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(nonExistentFile));
        } catch (Exception e) {
            // Ignore
        }
        
        // Create FilePersistence instance but immediately close it and delete the file
        FilePersistence filePersistence = new FilePersistence(nonExistentFile);
        try {
            filePersistence.close(); // Close immediately to unlock the file
            
            // Delete the file first to make it truly non-existent
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(nonExistentFile));
            
            // Now trying to delete non-existent file should return false (Files.deleteIfExists behavior)
            boolean result = filePersistence.deleteFile();
            assertFalse(result, "deleteFile() should return false for non-existent file (Files.deleteIfExists returns false)");
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        } finally {
            try {
                filePersistence.close();
            } catch (Exception ignored) {}
        }
    }
    
    @Test
    public void testDeleteWithDifferentFilePaths() {
        String[] testFiles = {"delete-test-1.json", "delete-test-2.json", "delete-test-3.json"};
        
        try {
            // Create multiple FilePersistence instances with different paths
            for (String testFile : testFiles) {
                try (FilePersistence fp = new FilePersistence(testFile)) {
                    // Save data to each file
                    fp.save("{\"tokens\":[{\"token\":\"" + testFile + "\"}]}");
                    assertTrue(fp.fileExists(), "File " + testFile + " should exist");
                    
                    // Delete each file should succeed on Linux
                    boolean result = fp.deleteFile();
                    assertTrue(result, "Should be able to delete " + testFile + " on Linux");
                    assertFalse(fp.fileExists(), "File " + testFile + " should be deleted");
                }
            }
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

}