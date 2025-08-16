package com.github.tsutomunakamura.tokenha;

import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Handles file persistence operations for TokenHa instances.
 * Responsible for saving and loading token data to/from files.
 */
public class FilePersistence {
    
    private String filePath;
    
    /**
     * Constructor with default file path.
     */
    public FilePersistence() {
        this.filePath = "tokenha-data.json";
    }
    
    /**
     * Constructor with custom file path.
     * @param filePath the file path to use for persistence
     */
    public FilePersistence(String filePath) {
        this.filePath = filePath;
    }
    
    /**
     * Save JSON data to the file.
     * @param jsonData the JSON string to save
     */
    public void save(String jsonData) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(jsonData);
            writer.flush();
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
     * @param filePath the file path to use
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
