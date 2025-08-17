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
    
    public void register(TokenHa tokenHa) {
        synchronized(this) {
            registeredInstances.add(new WeakReference<>(tokenHa));
            cleanupDeadReferences();
            System.out.println("TokenHa instance registered. Total instances: " + getActiveInstanceCount());
            
            // Start the thread if this is the first instance
            if (getActiveInstanceCount() >= 1 && (executorService == null || executorService.isShutdown())) {
                start();
            }
        }
    }
    
    public void unregister(TokenHa tokenHa) {
        synchronized(this) {
            registeredInstances.removeIf(ref -> ref.get() == tokenHa || ref.get() == null);
            System.out.println("TokenHa instance unregistered. Total instances: " + getActiveInstanceCount());
            
            // Stop the thread if no more instances
            if (getActiveInstanceCount() == 0) {
                stop();
            }
        }

    }
    
    private void cleanupDeadReferences() {
        // Remove synchronized - caller should already hold the lock
        // This method should only be called from within synchronized blocks
        registeredInstances.removeIf(ref -> ref.get() == null);
    }
    
    private int getActiveInstanceCount() {
        // This method may be called from synchronized and unsynchronized contexts
        // Use stream operations on the concurrent collection for thread safety
        return (int) registeredInstances.stream()
            .mapToInt(ref -> ref.get() != null ? 1 : 0)
            .sum();
    }
    
    private synchronized void start() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(
                this::evictTokens,
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
    
    private String getCurrentTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void evictTokens() {
        // Clean up dead references first (needs synchronization)
        synchronized(this) {
            cleanupDeadReferences();
            
            // Stop thread if no active instances remain
            if (registeredInstances.isEmpty()) {
                stop();
                return;
            }
        }

        evictTokensFromEachTokenHa();
    }

    public void evictTokensFromEachTokenHa() {
        // Create a snapshot of current instances to avoid concurrent modification
        Set<WeakReference<TokenHa>> currentInstances;
        synchronized(this) {
            currentInstances = Set.copyOf(registeredInstances);
        }
        
        System.out.println("Eviction task running at " + getCurrentTimeString() + " - Managing " + getActiveInstanceCount() + " TokenHa instances");
        
        int totalTokensBefore = 0;
        int totalTokensAfter = 0;
        int totalEvicted = 0;

        for (WeakReference<TokenHa> ref : currentInstances) {
            TokenHa tokenHa = ref.get();
            if (tokenHa != null) {
                EvictedCounter counter = evictTokensFromTokenHa(tokenHa);

                totalTokensBefore += counter.getSizeBefore();
                totalTokensAfter += counter.getSizeAfter();
                totalEvicted += counter.getSizeEvicted();

                System.out.println("  TokenHa instance: " + counter.getSizeBefore() + " -> "
                    + counter.getSizeAfter() + " (evicted " + counter.getSizeEvicted() + " expired tokens)");
            }
        }

        System.out.println("  Total: " + totalTokensBefore + " -> " + totalTokensAfter + " tokens (evicted " + totalEvicted + " expired)");
    }

    public EvictedCounter evictTokensFromTokenHa(TokenHa tokenHa) {
        EvictedCounter counter = new EvictedCounter();

        if (tokenHa != null) {
            try {
                int sizeBefore = tokenHa.getQueueSize();
                List<TokenElement> evictedTokens = tokenHa.evictExpiredTokens();
                int sizeAfter = tokenHa.getQueueSize();
                
                counter.setSizeBefore(sizeBefore);
                counter.setSizeAfter(sizeAfter);
                counter.setSizeEvicted(evictedTokens != null ? evictedTokens.size() : 0);
            } catch (Exception e) {
                // Handle case where TokenHa might be in an inconsistent state during cleanup
                System.err.println("Error during token eviction: " + e.getMessage());
                counter.setSizeBefore(0);
                counter.setSizeAfter(0);
                counter.setSizeEvicted(0);
            }
        }

        return counter;
    }

    private static class EvictedCounter {
        private int sizeBefore;
        private int sizeAfter;
        private int sizeEvicted;
        
        // Getters
        public int getSizeBefore() {
            return sizeBefore;
        }
        
        public int getSizeAfter() {
            return sizeAfter;
        }
        
        public int getSizeEvicted() {
            return sizeEvicted;
        }
        
        // Setters
        public void setSizeBefore(int sizeBefore) {
            this.sizeBefore = sizeBefore;
        }
        
        public void setSizeAfter(int sizeAfter) {
            this.sizeAfter = sizeAfter;
        }
        
        public void setSizeEvicted(int sizeEvicted) {
            this.sizeEvicted = sizeEvicted;
        }
    }
}
