package com.github.tsutomunakamura.tokenha;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.io.RandomAccessFile;

/**
 * Handles file persistence operations for TokenHa instances.
 * Responsible for saving and loading token data to/from files.
 * Maintains an exclusive file lock during the instance lifetime.
 */
public class FilePersistence implements AutoCloseable {
    
    private String filePath;
    
    // File handling for persistence with locking
    private RandomAccessFile persistenceFile;
    private FileChannel fileChannel;
    private FileLock fileLock;
    
    /**
     * Constructor with default file path.
     */
    public FilePersistence() throws IOException {
        this.filePath = "tokenha-data.json";
        initializeFile();
    }
    
    /**
     * Constructor with custom file path.
     * @param filePath the file path to use for persistence
     */
    public FilePersistence(String filePath) throws IOException {
        this.filePath = filePath;
        initializeFile();
    }
    
    /**
     * Initialize the persistence file with exclusive locking.
     * Creates the file only if we're going to save data, not just for loading.
     */
    private void initializeFile() throws IOException {
        try {
            // 1. This can throw IOException if file cannot be created/opened
            persistenceFile = new RandomAccessFile(filePath, "rw");
            
            // 2. This can throw IOException if channel cannot be obtained
            fileChannel = persistenceFile.getChannel();
            System.out.println(fileChannel);
            
            // 3. This can throw IOException if locking operation fails
            fileLock = fileChannel.tryLock();
            if (fileLock == null) {
                System.err.println("Warning: Could not acquire file lock for " + filePath + 
                                 ". Another instance may be using this file.");
                // Could throw exception here or implement retry logic
                // For now, continue without lock but operations may be unsafe
                throw new IOException("Failed to acquire file lock");
            } else {
                System.out.println("Acquired exclusive lock for persistence file: " + filePath);
                System.out.println("Working directory: " + System.getProperty("user.dir"));
                System.out.println("Absolute file path: " + Paths.get(filePath).toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize persistence file: " + filePath + ". Error: " + e.getMessage());
            close();
            throw e;
        }
    }
    
    /**
     * Close the file and release the lock.
     * Implements AutoCloseable interface.
     */
    @Override
    public void close() {
        try {
            if (fileLock != null) {
                fileLock.release();
                fileLock = null;
                System.out.println("File lock released for: " + filePath);
            }
            if (fileChannel != null) {
                fileChannel.close();
                fileChannel = null;
            }
            if (persistenceFile != null) {
                persistenceFile.close();
                persistenceFile = null;
            }
        } catch (IOException e) {
            System.err.println("Error closing persistence file: " + e.getMessage());
        }
    }
    
    /**
     * Save JSON data to the locked file.
     * File remains open and data is flushed immediately to prevent data loss.
     * @param jsonData the JSON string to save
     */
    public void save(String jsonData) {
        if (persistenceFile == null) {
            throw new IllegalStateException("Persistence file not initialized. Cannot save data.");
        }
        
        if (fileLock == null) {
            System.err.println("Warning: Saving without file lock. Data may be corrupted by concurrent access.");
        }

        try {
            // Reset file position to beginning and truncate
            persistenceFile.seek(0);
            persistenceFile.setLength(0);
            
            // Write JSON data
            persistenceFile.write(jsonData.getBytes("UTF-8"));
            
            // Force data to be written to disk immediately (flush)
            persistenceFile.getFD().sync();
            
            System.out.println("File saved successfully. Size: " + persistenceFile.length() + " bytes");
            
        } catch (IOException e) {
            System.err.println("Failed to save data to file: " + filePath + 
                             ". Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load JSON data from the file if it exists.
     * @return the JSON content, or null if file doesn't exist or error occurs
     */
    public String load() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        System.out.println("Loaded data from file: " + filePath);
        System.out.println("Content: " + content);
        return content;
    }
    
    /**
     * Set the file path for persistence.
     * Note: This will close the current file and reinitialize with the new path.
     * @param filePath the file path to use
     */
    public void setFilePath(String filePath) throws IOException {
        // Close current file if open
        close();
        
        // Set new path and reinitialize
        this.filePath = filePath;
        initializeFile();
    }
    
    /**
     * Get the current file path.
     * @return the file path being used
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * Check if the persistence file exists.
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists() {
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * Delete the persistence file if it exists.
     * @return true if file was deleted or didn't exist, false if deletion failed
     */
    public boolean deleteFile() {
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + filePath + 
                             ". Error: " + e.getMessage());
            return false;
        }
    }
}
