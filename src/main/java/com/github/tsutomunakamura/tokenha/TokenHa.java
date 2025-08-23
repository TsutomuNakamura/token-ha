package com.github.tsutomunakamura.tokenha;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import com.github.tsutomunakamura.tokenha.element.TokenElement;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

/**
 * A simple token handling utility class.
 */
public class TokenHa implements AutoCloseable {

    // Configuration parameters - now configurable
    private final int expirationTimeMillis;
    private final int numberOfLastTokens;
    private final int maxTokens;
    private final long coolTimeToAddMillis;
    private final String persistenceFilePath;

    private Deque<TokenElement> fifoQueue = new ArrayDeque<>();
    private FilePersistence filePersistence; // File persistence handler
    
    // Gson for JSON serialization/deserialization
    private static final Gson gson = new Gson();
    
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
            if (element != null && (currentTime - element.getTimeMillis()) > expirationTimeMillis) {
                expiredTokens.add(fifoQueue.poll());
            } else {
                break;
            }
        }

        return expiredTokens.size() > 0 ? expiredTokens : null;
    }
    
    /**
     * Load tokens from file if it exists and deserialize using Gson.
     */
    public void loadFromFile() {
        String content = filePersistence.load();
        if (content != null) {
            System.out.println("Loaded content: " + content);
            
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
                    
                    System.out.println("Loaded " + fifoQueue.size() + " tokens from file");
                }
            } catch (JsonSyntaxException e) {
                System.err.println("Error parsing JSON with Gson: " + e.getMessage());
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
    public String toJson() {
        try {
            List<TokenElement> tokenList = new ArrayList<>(fifoQueue);
            TokenData data = new TokenData(tokenList);
            return gson.toJson(data);
        } catch (Exception e) {
            System.err.println("Error serializing to JSON: " + e.getMessage());
            return "{\"tokens\":[]}";
        }
    }
}
