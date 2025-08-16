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
    public FilePersistence() {
        this.filePath = "tokenha-data.json";
        initializeFile();
    }
    
    /**
     * Constructor with custom file path.
     * @param filePath the file path to use for persistence
     */
    public FilePersistence(String filePath) {
        this.filePath = filePath;
        initializeFile();
    }
    
    /**
     * Initialize the persistence file with exclusive locking.
     * Creates the file only if we're going to save data, not just for loading.
     */
    private void initializeFile() {
        try {
            // Always create/open the file for exclusive access
            // This is necessary even for reading to prevent other processes from writing
            persistenceFile = new RandomAccessFile(filePath, "rw");
            fileChannel = persistenceFile.getChannel();
            
            // Try to acquire exclusive lock
            fileLock = fileChannel.tryLock();
            if (fileLock == null) {
                System.err.println("Warning: Could not acquire file lock for " + filePath + 
                                 ". Another instance may be using this file.");
                // Continue without lock, but warn user
            } else {
                System.out.println("Acquired exclusive lock for persistence file: " + filePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize persistence file: " + filePath + 
                             ". Error: " + e.getMessage());
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
            System.err.println("Persistence file not initialized. Cannot save data.");
            return;
        }
        
        try {
            // Reset file position to beginning and truncate
            persistenceFile.seek(0);
            persistenceFile.setLength(0);
            
            // Write JSON data
            persistenceFile.write(jsonData.getBytes("UTF-8"));
            
            // Force data to be written to disk immediately (flush)
            persistenceFile.getFD().sync();
            
        } catch (IOException e) {
            System.err.println("Failed to save data to file: " + filePath + 
                             ". Error: " + e.getMessage());
        }
    }
    
    /**
     * Load JSON data from the file if it exists.
     * @return the JSON content, or null if file doesn't exist or error occurs
     */
    public String load() {
        try {
            if (Files.exists(Paths.get(filePath))) {
                String content = new String(Files.readAllBytes(Paths.get(filePath)));
                System.out.println("Loaded data from file: " + filePath);
                System.out.println("Content: " + content);
                return content;
            } else {
                System.out.println("Persistence file does not exist: " + filePath);
                return null;
            }
        } catch (IOException e) {
            System.err.println("Failed to load data from file: " + filePath + 
                             ". Error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Set the file path for persistence.
     * Note: This will close the current file and reinitialize with the new path.
     * @param filePath the file path to use
     */
    public void setFilePath(String filePath) {
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
