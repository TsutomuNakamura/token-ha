package com.github.tsutomunakamura.tokenha.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.io.RandomAccessFile;
import org.slf4j.Logger;
import com.github.tsutomunakamura.tokenha.logging.TokenHaLogger;

/**
 * Handles file persistence operations for TokenHa instances.
 * Responsible for saving and loading token data to/from files.
 * Maintains an exclusive file lock during the instance lifetime.
 */
public class FilePersistence implements AutoCloseable {
    
    private static final Logger logger = TokenHaLogger.getLogger(FilePersistence.class);
    
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
            logger.trace("FileChannel created: {}", fileChannel);
            
            // 3. This can throw IOException if locking operation fails
            fileLock = fileChannel.tryLock();
            if (fileLock == null) {
                logger.warn("Could not acquire file lock for {}. Another instance may be using this file.", filePath);
                // Could throw exception here or implement retry logic
                // For now, continue without lock but operations may be unsafe
                throw new IOException("Failed to acquire file lock");
            } else {
                logger.debug("Acquired exclusive lock for persistence file: {}", filePath);
                logger.trace("Working directory: {}", System.getProperty("user.dir"));
                logger.trace("Absolute file path: {}", Paths.get(filePath).toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to initialize persistence file: {}. Error: {}", filePath, e.getMessage());
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
                logger.debug("File lock released for: {}", filePath);
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
            logger.error("Error closing persistence file: {}", e.getMessage());
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
            logger.warn("Saving without file lock. Data may be corrupted by concurrent access.");
        }

        try {
            // Reset file position to beginning and truncate
            persistenceFile.seek(0);
            persistenceFile.setLength(0);
            
            // Write JSON data
            persistenceFile.write(jsonData.getBytes("UTF-8"));
            
            // Force data to be written to disk immediately (flush)
            persistenceFile.getFD().sync();
            
            logger.debug("File saved successfully. Size: {} bytes", persistenceFile.length());
            
        } catch (IOException e) {
            logger.error("Failed to save data to file: {}. Error: {}", filePath, e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load JSON data from the file if it exists.
     * @return the JSON content, or null if file doesn't exist or error occurs
     */
    public String load() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        logger.debug("Loaded data from file: {}", filePath);
        logger.trace("Content: {}", content);
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
            logger.error("Failed to delete file: {}. Error: {}", filePath, e.getMessage());
            return false;
        }
    }
}
