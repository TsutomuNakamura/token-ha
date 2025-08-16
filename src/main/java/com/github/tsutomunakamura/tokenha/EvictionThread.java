package com.github.tsutomunakamura.tokenha;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A singleton thread class for handling token eviction tasks across all TokenHa instances.
 * Currently outputs simple messages for testing purposes.
 */
public class EvictionThread {
    
    // Singleton instance
    private static final EvictionThread INSTANCE = new EvictionThread();
    
    private ScheduledExecutorService executorService;
    private static final long EVICTION_INTERVAL_SECONDS = 10; // Run every 10 seconds
    
    // Registry of all TokenHa instances
    private final Set<TokenHa> registeredInstances = ConcurrentHashMap.newKeySet();
    
    // Private constructor for singleton
    private EvictionThread() {
    }
    
    public static EvictionThread getInstance() {
        return INSTANCE;
    }
    
    public void register(TokenHa tokenHa) {
        registeredInstances.add(tokenHa);
        System.out.println("TokenHa instance registered. Total instances: " + registeredInstances.size());
        
        // Start the thread if this is the first instance
        if (registeredInstances.size() == 1) {
            start();
        }
    }
    
    public void unregister(TokenHa tokenHa) {
        registeredInstances.remove(tokenHa);
        System.out.println("TokenHa instance unregistered. Total instances: " + registeredInstances.size());
        
        // Stop the thread if no more instances
        if (registeredInstances.isEmpty()) {
            stop();
        }
    }
    
    private void start() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(
                this::evictionTask,
                EVICTION_INTERVAL_SECONDS,
                EVICTION_INTERVAL_SECONDS,
                TimeUnit.SECONDS
            );
            System.out.println("Singleton eviction thread started at " + getCurrentTimeString());
        }
    }
    
    private void stop() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            System.out.println("Singleton eviction thread stopped at " + getCurrentTimeString());
        }
    }
    
    private void evictionTask() {
        // Currently just outputs a message and current time for testing
        System.out.println("Eviction task running at " + getCurrentTimeString() + 
                          " - Managing " + registeredInstances.size() + " TokenHa instances");
        
        int totalTokens = 0;
        for (TokenHa tokenHa : registeredInstances) {
            int queueSize = tokenHa.getQueueSize();
            totalTokens += queueSize;
            System.out.println("  TokenHa instance queue size: " + queueSize);
        }
        
        System.out.println("  Total tokens across all instances: " + totalTokens);
    }
    
    private String getCurrentTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
