package com.github.tsutomunakamura.tokenha;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import com.github.tsutomunakamura.tokenha.element.TokenElement;

/**
 * A simple token handling utility class.
 */
public class TokenHa implements AutoCloseable {

    Deque<TokenElement> fifoQueue = new ArrayDeque<>();
    private static final int DEFAUILT_EXPIREATION_TIME_SECONCDS = 60; // Expiration time in seconds
    private int numberOfLastTokens = 1; // Number of last tokens to keep
    private int maxTokens = 10; // Maximum number of tokens to keep
    private long coolTimeToAddMillis = 1000; // Time in milliseconds to wait before adding a new token
    
    // File path for persisting tokens
    private String persistenceFilePath = "tokenha-data.json"; // Default file path
    private FilePersistence filePersistence; // File persistence handler
    
    // Constructor registers this instance with singleton eviction thread
    public TokenHa() {
        EvictionThread.getInstance().register(this);
        filePersistence = new FilePersistence(persistenceFilePath);
    }

    public synchronized boolean addIfAvailable(String token) {
        if (!passedCoolTimeToAdd()) {
            return false;
        }

        if (isFilled()) {
            fifoQueue.poll();
        }
        add(token);

        return true;
    }

    private synchronized void add(String token) {
        fifoQueue.add(new TokenElement(token, System.currentTimeMillis()));
        filePersistence.save(toJson());
    }

    public TokenElement newestToken() {
        return fifoQueue.peekLast();
    }

    public Iterator<TokenElement> getDescIterator() {
        return fifoQueue.descendingIterator();
    }

    public int getQueueSize() {
        return fifoQueue.size();
    }

    // Cleanup method to unregister from singleton eviction thread
    public void close() {
        EvictionThread.getInstance().unregister(this);
        if (filePersistence != null) {
            filePersistence.close();
        }
    }

    public boolean availableToAdd() {
        return !isFilled() && passedCoolTimeToAdd();
    }

    public boolean isFilled() {
        return fifoQueue.size() >= maxTokens;
    }

    public boolean passedCoolTimeToAdd() {
        TokenElement newestToken = fifoQueue.peekLast();
        if (newestToken != null) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastToken = currentTime - newestToken.getTimeMillis(); // Keep in milliseconds
            return timeSinceLastToken >= coolTimeToAddMillis;
        }
        return true;
    }

    public List<TokenElement> evictExpiredTokens() {
        List<TokenElement> expiredTokens = new ArrayList<>();

        long currentTime = System.currentTimeMillis();
        while (!fifoQueue.isEmpty()) {

            if (fifoQueue.size() <= numberOfLastTokens) {
                // Do not remove if we have only the last tokens left
                break;
            }

            // Get the oldest element
            TokenElement element = fifoQueue.peek();
            if (element != null && (currentTime - element.getTimeMillis()) / 1000 > DEFAUILT_EXPIREATION_TIME_SECONCDS) {
                expiredTokens.add(fifoQueue.poll());
            } else {
                break;
            }
        }

        return expiredTokens.size() > 0 ? expiredTokens : null;
    }
    
    /**
     * Load tokens from file if it exists.
     */
    public void loadFromFile() {
        String content = filePersistence.load();
        if (content != null) {
            // For now, just log the content. Deserialization can be implemented later.
            System.out.println("Loaded content: " + content);
        }
    }
    
    /**
     * Set the file path for persistence.
     * @param filePath the file path to use for saving/loading tokens
     */
    public void setPersistenceFilePath(String filePath) {
        this.persistenceFilePath = filePath;
        filePersistence.setFilePath(filePath);
    }
    
    /**
     * Get the current persistence file path.
     * @return the file path used for saving/loading tokens
     */
    public String getPersistenceFilePath() {
        return persistenceFilePath;
    }
    
    /**
     * Check if the persistence file exists.
     * @return true if the file exists, false otherwise
     */
    public boolean persistenceFileExists() {
        return filePersistence.fileExists();
    }
    
    /**
     * Delete the persistence file.
     * @return true if file was deleted or didn't exist, false if deletion failed
     */
    public boolean deletePersistenceFile() {
        return filePersistence.deleteFile();
    }

    /**
     * Serialize TokenHa to simple JSON containing only the tokens.
     * @return JSON string representation of tokens
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"tokens\":[");
        
        boolean first = true;
        for (TokenElement element : fifoQueue) {
            if (!first) {
                json.append(",");
            }
            json.append(element.toJson());
            first = false;
        }
        
        json.append("]}");
        return json.toString();
    }
}
