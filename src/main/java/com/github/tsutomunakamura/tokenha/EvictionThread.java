package com.github.tsutomunakamura.tokenha;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.lang.ref.WeakReference;

/**
 * A singleton thread class for handling token eviction tasks across all TokenHa instances.
 * Currently outputs simple messages for testing purposes.
 */
public class EvictionThread {
    
    // Singleton instance
    private static final EvictionThread INSTANCE = new EvictionThread();
    
    private ScheduledExecutorService executorService;
    private static final long EVICTION_INTERVAL_SECONDS = 10; // Run every 10 seconds
    
    // Registry of all TokenHa instances using WeakReferences for automatic cleanup
    private final Set<WeakReference<TokenHa>> registeredInstances = ConcurrentHashMap.newKeySet();
    
    // Private constructor for singleton
    private EvictionThread() {
    }
    
    public static EvictionThread getInstance() {
        return INSTANCE;
    }
    
    public synchronized void register(TokenHa tokenHa) {
        registeredInstances.add(new WeakReference<>(tokenHa));
        cleanupDeadReferences();
        System.out.println("TokenHa instance registered. Total instances: " + getActiveInstanceCount());
        
        // Start the thread if this is the first instance
        if (getActiveInstanceCount() >= 1 && (executorService == null || executorService.isShutdown())) {
            start();
        }
    }
    
    public synchronized void unregister(TokenHa tokenHa) {
        registeredInstances.removeIf(ref -> ref.get() == tokenHa || ref.get() == null);
        System.out.println("TokenHa instance unregistered. Total instances: " + getActiveInstanceCount());
        
        // Stop the thread if no more instances
        if (getActiveInstanceCount() == 0) {
            stop();
        }
    }
    
    private void cleanupDeadReferences() {
        // This does not needed 
        registeredInstances.removeIf(ref -> ref.get() == null);
    }
    
    private int getActiveInstanceCount() {
        cleanupDeadReferences();
        return registeredInstances.size();
    }
    
    private synchronized void start() {
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

    public synchronized void stopIfInstancesEmpty() {
        // Check if there are no active instances and stop the thread
        if (getActiveInstanceCount() == 0) {
            stop();
        }
    }
    
    private synchronized void stop() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            System.out.println("Singleton eviction thread stopped at " + getCurrentTimeString());
        }
    }
    
    private void evictionTask() {
        // Clean up dead references first
        cleanupDeadReferences();

        // Stop thread if no active instances remain
        if (registeredInstances.isEmpty()) {
            stop();
        }
    }
    
    private String getCurrentTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
