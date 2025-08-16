package com.github.tsutomunakamura.tokenha;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A separate thread class for handling token eviction tasks.
 * Currently outputs simple messages for testing purposes.
 */
public class EvictionThread {
    
    private ScheduledExecutorService executorService;
    private static final long EVICTION_INTERVAL_SECONDS = 10; // Run every 10 seconds
    private final TokenHa tokenHa;
    
    public EvictionThread(TokenHa tokenHa) {
        this.tokenHa = tokenHa;
    }
    
    public void start() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(
                this::evictionTask,
                EVICTION_INTERVAL_SECONDS,
                EVICTION_INTERVAL_SECONDS,
                TimeUnit.SECONDS
            );
            System.out.println("Eviction thread started at " + getCurrentTimeString());
        }
    }
    
    public void stop() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            System.out.println("Eviction thread stopped at " + getCurrentTimeString());
        }
    }
    
    private void evictionTask() {
        // Currently just outputs a message and current time for testing
        System.out.println("Eviction task running at " + getCurrentTimeString() + 
                          " - Queue size: " + getQueueSize());
    }
    
    private String getCurrentTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    private int getQueueSize() {
        // Access queue size through TokenHa's getter method
        return tokenHa.getQueueSize();
    }
}
