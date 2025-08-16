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
public class TokenHa {

    Deque<TokenElement> fifoQueue = new ArrayDeque<>();
    private static final int DEFAUILT_EXPIREATION_TIME_SECONCDS = 60; // Expiration time in seconds
    private int numberOfLastTokens = 1; // Number of last tokens to keep
    private int maxTokens = 10; // Maximum number of tokens to keep
    private long coolTimeToAddSeconds = 1000; // Time in seconds to wait before adding a new token
    
    // Constructor registers this instance with singleton eviction thread
    public TokenHa() {
        EvictionThread.getInstance().register(this);
    }

    public synchronized void addIfAvailable(String token) {
        if (!availableToAdd()) {
            return;
        }
        add(token);
    }

    private synchronized void add(String token) {
        fifoQueue.add(new TokenElement(token, System.currentTimeMillis()));
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
            long timeSinceLastToken = (currentTime - newestToken.getTimeMillis()) / 1000; // Convert to seconds
            return timeSinceLastToken >= coolTimeToAddSeconds;
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
}