package com.github.tsutomunakamura.tokenha;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import com.github.tsutomunakamura.tokenha.config.TokenHaConfig;
import com.github.tsutomunakamura.tokenha.data.TokenData;
import com.github.tsutomunakamura.tokenha.element.TokenElement;
import com.github.tsutomunakamura.tokenha.logging.TokenHaLogger;
import com.github.tsutomunakamura.tokenha.persistence.FilePersistence;
import com.github.tsutomunakamura.tokenha.eviction.EvictionThread;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * A simple token handling utility class.
 */
public class TokenHa implements AutoCloseable {

    private static final Logger logger = TokenHaLogger.getLogger(TokenHa.class);
    
    // Configuration parameters - now configurable
    private final long expirationTimeMillis;
    private final int numberOfLastTokens;
    private final int maxTokens;
    private final long coolTimeToAddMillis;
    private final String persistenceFilePath;

    private Deque<TokenElement> fifoQueue = new ArrayDeque<>();
    private FilePersistence filePersistence; // File persistence handler
    
    // Gson for JSON serialization/deserialization
    private static final Gson gson = new Gson();

    // Strategy 1: Cached snapshot for iteration
    private List<TokenElement> snapshotList = new ArrayList<>();
    private Iterator<TokenElement> descIterator;
    
    /**
     * Constructor with default configuration.
     */
    public TokenHa() throws IOException {
        this(TokenHaConfig.defaultConfig());
    }
    
    /**
     * Constructor with custom configuration.
     */
    public TokenHa(TokenHaConfig config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        
        this.expirationTimeMillis = config.getExpirationTimeMillis();
        this.numberOfLastTokens = config.getNumberOfLastTokens();
        this.maxTokens = config.getMaxTokens();
        this.coolTimeToAddMillis = config.getCoolTimeToAddMillis();
        this.persistenceFilePath = config.getPersistenceFilePath();
        
        EvictionThread.getInstance(config.getEvictionThreadConfig()).register(this);
        filePersistence = new FilePersistence(persistenceFilePath);
        
        // Initialize snapshot
        updateSnapshot();
    }

    /**
     * Add token if available according to cool time and max tokens constraints.
     * This method is now synchronized to prevent race conditions.
     */
    public synchronized boolean addIfAvailable(String token) {
        if (!passedCoolTimeToAdd()) {
            return false;
        }

        if (isFilled()) {
            fifoQueue.poll();
        }
        
        // Add the new token
        fifoQueue.add(new TokenElement(token, System.currentTimeMillis()));
        filePersistence.save(toJson());
        
        // Update snapshot after modification
        updateSnapshot();

        return true;
    }

    /**
     * Internal method to update the snapshot after queue modifications.
     * This creates a new snapshot list and iterator for thread-safe iteration.
     */
    private void updateSnapshot() {
        snapshotList = new ArrayList<>(fifoQueue);
        descIterator = snapshotList.descendingIterator();
    }

    /**
     * @deprecated Use updateSnapshot() internally instead.
     * This method is kept for backward compatibility.
     */
    @Deprecated
    public synchronized void generateDescIterator(Deque<TokenElement> queue) {
        updateSnapshot();
    }

    /**
     * Get the newest token in the queue.
     * Synchronized for thread-safe read.
     */
    public synchronized TokenElement newestToken() {
        return fifoQueue.peekLast();
    }

    /**
     * Get a descending iterator over the tokens.
     * This returns a pre-computed iterator from the last snapshot.
     * The iterator is thread-safe and won't throw ConcurrentModificationException.
     * 
     * @return Iterator for read-only traversal in descending order
     */
    public Iterator<TokenElement> getDescIterator() {
        return descIterator;
    }

    /**
     * Get the current queue size.
     * Synchronized for thread-safe read.
     */
    public synchronized int getQueueSize() {
        return fifoQueue.size();
    }

    // Cleanup method to unregister from singleton eviction thread
    public void close() {
        EvictionThread.getInstance().unregister(this);
        if (filePersistence != null) {
            filePersistence.close();
        }
    }

    /**
     * Check if available to add a new token.
     * Synchronized for consistent read of multiple conditions.
     */
    public synchronized boolean availableToAdd() {
        return !isFilled() && passedCoolTimeToAdd();
    }

    /**
     * Check if the queue is filled to capacity.
     * Made synchronized for thread-safe read.
     */
    public synchronized boolean isFilled() {
        return fifoQueue.size() >= maxTokens;
    }

    /**
     * Check if enough time has passed since the last token was added.
     * Made synchronized for thread-safe read.
     */
    public synchronized boolean passedCoolTimeToAdd() {
        TokenElement newestToken = fifoQueue.peekLast();
        if (newestToken != null) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastToken = currentTime - newestToken.getTimeMillis();
            return timeSinceLastToken >= coolTimeToAddMillis;
        }
        return true;
    }

    /**
     * Evict expired tokens from the queue.
     * Already synchronized, updated to use updateSnapshot().
     */
    public synchronized List<TokenElement> evictExpiredTokens() {
        List<TokenElement> expiredTokens = new ArrayList<>();

        long currentTime = System.currentTimeMillis();
        while (!fifoQueue.isEmpty()) {
            if (fifoQueue.size() <= numberOfLastTokens) {
                // Do not remove if we have only the last tokens left
                break;
            }

            // Get the oldest element
            TokenElement element = fifoQueue.peek();
            if (element != null && (currentTime - element.getTimeMillis()) > expirationTimeMillis) {
                expiredTokens.add(fifoQueue.poll());
            } else {
                break;
            }
        }

        if (expiredTokens.size() == 0) {
            return null;
        }

        filePersistence.save(toJson());
        
        // Update snapshot after modification
        updateSnapshot();
        
        return expiredTokens;
    }
    
    /**
     * Load tokens from file if it exists and deserialize using Gson.
     */
    public synchronized void loadFromFile() throws IOException {
        String content = filePersistence.load();
        if (content != null) {
            logger.trace("Loaded content: {}", content);
            
            try {
                TokenData data = gson.fromJson(content, TokenData.class);
                
                if (data != null && data.getTokens() != null && !data.getTokens().isEmpty()) {
                    // Clear current queue
                    fifoQueue.clear();
                    
                    // If tokens exceed maxTokens, keep only the newest ones
                    List<TokenElement> tokens = data.getTokens();
                    if (tokens.size() > maxTokens) {
                        tokens = tokens.subList(tokens.size() - maxTokens, tokens.size());
                    }
                    
                    // Add all tokens to queue in proper order (oldest to newest)
                    for (TokenElement token : tokens) {
                        fifoQueue.add(token);
                    }
                    
                    // Update snapshot after loading
                    updateSnapshot();
                    
                    logger.debug("Loaded {} tokens from file", fifoQueue.size());
                }
            } catch (JsonSyntaxException e) {
                logger.warn("Error parsing JSON with Gson: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Set the file path for persistence.
     * Note: This method is deprecated. Use TokenHaConfig to set persistence file path during construction.
     * @param filePath the file path to use for saving/loading tokens
     * @deprecated Use TokenHaConfig instead
     */
    @Deprecated
    public void setPersistenceFilePath(String filePath) throws IOException {
        // Only update the FilePersistence instance, not the field (which is final)
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
     * Serialize TokenHa to JSON using Gson.
     * @return JSON string representation of tokens
     */
    public synchronized String toJson() {
        List<TokenElement> tokenList = new ArrayList<>(fifoQueue);
        TokenData data = new TokenData(tokenList);
        return gson.toJson(data);
    }
}
