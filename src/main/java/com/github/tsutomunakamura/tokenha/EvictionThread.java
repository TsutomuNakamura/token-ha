package com.github.tsutomunakamura.tokenha;

import java.util.Set;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.lang.ref.WeakReference;

import com.github.tsutomunakamura.tokenha.element.TokenElement;

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
            return;
        }
        
        // Evict expired tokens from each TokenHa instance
        System.out.println("Eviction task running at " + getCurrentTimeString() + 
                          " - Managing " + registeredInstances.size() + " TokenHa instances");
        
        int totalTokensBefore = 0;
        int totalTokensAfter = 0;
        int totalEvicted = 0;
        
        for (WeakReference<TokenHa> ref : registeredInstances) {
            TokenHa tokenHa = ref.get();
            
            // WeakReference can become null at any time due to GC
            if (tokenHa == null) {
                continue;
            }
            
            int queueSizeBefore = tokenHa.getQueueSize();
            totalTokensBefore += queueSizeBefore;
            
            // Perform actual eviction
            List<TokenElement> evictedTokens = tokenHa.evictExpiredTokens();
            int evictedCount = (evictedTokens != null) ? evictedTokens.size() : 0;
            totalEvicted += evictedCount;
            
            int queueSizeAfter = tokenHa.getQueueSize();
            totalTokensAfter += queueSizeAfter;
            
            if (evictedCount > 0) {
                System.out.println("  TokenHa instance: " + queueSizeBefore + " -> " + queueSizeAfter + 
                                 " (evicted " + evictedCount + " expired tokens)");
            } else {
                System.out.println("  TokenHa instance: " + queueSizeBefore + " tokens (no expired tokens)");
            }
        }
        
        System.out.println("  Total: " + totalTokensBefore + " -> " + totalTokensAfter + 
                          " tokens (evicted " + totalEvicted + " expired)");
    }
    
    private String getCurrentTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
