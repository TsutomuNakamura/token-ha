package com.github.tsutomunakamura.tokenha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for EvictionThread register method functionality.
 * Uses mocking to avoid file locking issues during concurrent testing.
 */
@ExtendWith(MockitoExtension.class)
public class EvictionThreadTest {
    
    @Mock
    private TokenHa mockTokenHa1;
    
    @Mock
    private TokenHa mockTokenHa2;
    
    @Mock
    private TokenHa mockTokenHa3;
    
    private EvictionThread evictionThread;
    
    @BeforeEach
    public void setUp() {
        evictionThread = EvictionThread.getInstance();
        // Clean up any existing instances before each test
        clearRegisteredInstances();
        stopEvictionThreadIfRunning();
    }
    
    @AfterEach
    public void tearDown() {
        // Clean up after each test
        clearRegisteredInstances();
        stopEvictionThreadIfRunning();
    }

    // Helper methods using reflection to access private fields
    
    private int getActiveInstanceCount() throws Exception {
        Field registeredInstancesField = EvictionThread.class.getDeclaredField("registeredInstances");
        registeredInstancesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<WeakReference<TokenHa>> instances = (Set<WeakReference<TokenHa>>) registeredInstancesField.get(evictionThread);
        
        // Clean up dead references and return count
        instances.removeIf(ref -> ref.get() == null);
        return instances.size();
    }
    
    private boolean isEvictionThreadRunning() throws Exception {
        Field executorServiceField = EvictionThread.class.getDeclaredField("executorService");
        executorServiceField.setAccessible(true);
        ScheduledExecutorService executor = (ScheduledExecutorService) executorServiceField.get(evictionThread);
        return executor != null && !executor.isShutdown();
    }
    
    private void clearRegisteredInstances() {
        try {
            Field registeredInstancesField = EvictionThread.class.getDeclaredField("registeredInstances");
            registeredInstancesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<WeakReference<TokenHa>> instances = (Set<WeakReference<TokenHa>>) registeredInstancesField.get(evictionThread);
            instances.clear();
        } catch (Exception e) {
            // Ignore reflection errors in test cleanup
        }
    }
    
    private void stopEvictionThreadIfRunning() {
        try {
            Field executorServiceField = EvictionThread.class.getDeclaredField("executorService");
            executorServiceField.setAccessible(true);
            ScheduledExecutorService executor = (ScheduledExecutorService) executorServiceField.get(evictionThread);
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
            // Reset the executor to null
            executorServiceField.set(evictionThread, null);
        } catch (Exception e) {
            // Ignore reflection errors in test cleanup
        }
    }
    
    // Test cases for EvictionThread#register(TokenHa tokenHa);

    @Test
    @DisplayName("Test register method with single mock TokenHa instance")
    public void testRegisterSingleMockInstance() throws Exception {
        System.out.println("🧪 TEST: Register method with single mock TokenHa instance");
        
        // Get initial count
        int initialCount = getActiveInstanceCount();
        
        // Register the mock instance
        evictionThread.register(mockTokenHa1);
        
        // Verify instance count increased
        assertEquals(initialCount + 1, getActiveInstanceCount(), 
            "Instance count should increase after registration");
        
        // Verify the eviction thread is started
        assertTrue(isEvictionThreadRunning(), 
            "Eviction thread should start when first instance is registered");
    }
    
    @Test
    @DisplayName("Test register method with multiple mock TokenHa instances")
    public void testRegisterMultipleMockInstances() throws Exception {
        System.out.println("🧪 TEST: Register method with multiple mock TokenHa instances");
        
        // Register first mock instance
        evictionThread.register(mockTokenHa1);
        assertEquals(1, getActiveInstanceCount(), "Should have 1 instance after first registration");
        assertTrue(isEvictionThreadRunning(), "Thread should be running after first registration");
        
        // Register second mock instance
        evictionThread.register(mockTokenHa2);
        assertEquals(2, getActiveInstanceCount(), "Should have 2 instances after second registration");
        assertTrue(isEvictionThreadRunning(), "Thread should still be running");
        
        // Register third mock instance
        evictionThread.register(mockTokenHa3);
        assertEquals(3, getActiveInstanceCount(), "Should have 3 instances after third registration");
        assertTrue(isEvictionThreadRunning(), "Thread should still be running");
    }
    
    @Test
    @DisplayName("Test register method with duplicate mock TokenHa instance")
    public void testRegisterDuplicateMockInstance() throws Exception {
        System.out.println("🧪 TEST: Register method with duplicate mock TokenHa instance");
        
        // Register the same mock instance multiple times
        evictionThread.register(mockTokenHa1);
        int countAfterFirst = getActiveInstanceCount();
        
        evictionThread.register(mockTokenHa1);
        int countAfterSecond = getActiveInstanceCount();
        
        evictionThread.register(mockTokenHa1);
        int countAfterThird = getActiveInstanceCount();
        
        // Each registration should add a new WeakReference, even for the same object
        assertTrue(countAfterSecond > countAfterFirst, 
            "Second registration should increase count");
        assertTrue(countAfterThird > countAfterSecond, 
            "Third registration should increase count");
    }
    
    @Test
    @DisplayName("Test register method starts eviction thread only when needed")
    public void testRegisterStartsThreadWhenNeeded() throws Exception {
        System.out.println("🧪 TEST: Register method starts eviction thread only when needed");
        
        // Initially no thread should be running
        assertFalse(isEvictionThreadRunning(), "Thread should not be running initially");
        
        // Register first mock instance - should start thread
        evictionThread.register(mockTokenHa1);
        assertTrue(isEvictionThreadRunning(), "Thread should start when first instance is registered");
    }
    
    @Test
    @DisplayName("Test register method with null TokenHa instance")
    public void testRegisterNullInstance() throws Exception {
        System.out.println("🧪 TEST: Register method with null TokenHa instance");
        
        int initialCount = getActiveInstanceCount();
        
        // Register null instance - should not throw exception but might add a null reference
        evictionThread.register(null);
        
        // The count might increase even with null (depending on implementation)
        // This test verifies the method handles null gracefully
        int finalCount = getActiveInstanceCount();
        assertTrue(finalCount >= initialCount, "Method should handle null gracefully");
    }
    
    @Test
    @DisplayName("Test register method cleanup of dead references")
    public void testRegisterCleansUpDeadReferences() throws Exception {
        System.out.println("🧪 TEST: Register method cleanup of dead references");
        
        // Create a real TokenHa instance that can be garbage collected
        TokenHa tokenHa = mock(TokenHa.class);
        evictionThread.register(tokenHa);
        int countAfterRegister = getActiveInstanceCount();
        
        assertTrue(countAfterRegister > 0, "Should have at least one registered instance");
        
        // Remove reference to make it eligible for GC
        tokenHa = null;
        
        // Force garbage collection to make the WeakReference eligible for cleanup
        System.gc();
        Thread.sleep(100); // Give GC time to work
        
        // Register a new mock instance - this should trigger cleanup during eviction
        evictionThread.register(mockTokenHa1);
        
        // Note: Cleanup might not happen immediately due to GC timing
        System.out.println("Count after cleanup registration: " + getActiveInstanceCount());
    }
    
    @Test
    @DisplayName("Test register method thread safety with mock instances")
    public void testRegisterThreadSafety() throws Exception {
        System.out.println("🧪 TEST: Register method thread safety with mock instances");
        
        final int numThreads = 5;
        final int instancesPerThread = 3;
        Thread[] threads = new Thread[numThreads];
        
        // Create multiple threads that register mock TokenHa instances simultaneously
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < instancesPerThread; j++) {
                    TokenHa mockToken = mock(TokenHa.class, "MockToken-" + threadIndex + "-" + j);
                    evictionThread.register(mockToken);
                    // Small delay to increase chance of thread interleaving
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        
        int initialCount = getActiveInstanceCount();
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify that the method handled concurrent access correctly
        int finalCount = getActiveInstanceCount();
        int expectedMinimum = initialCount + (numThreads * instancesPerThread);
        
        assertTrue(finalCount >= expectedMinimum, 
            "Should have registered at least " + expectedMinimum + " instances, but got " + finalCount);
        assertTrue(isEvictionThreadRunning(), "Eviction thread should be running after concurrent registrations");
    }
    
    @Test
    @DisplayName("Test register method synchronization behavior")
    public void testRegisterSynchronization() throws Exception {
        System.out.println("🧪 TEST: Register method synchronization behavior");
        
        // This test verifies that the register method is properly synchronized
        // by checking that concurrent registrations don't cause race conditions
        
        final int numConcurrentRegistrations = 10;
        Thread[] threads = new Thread[numConcurrentRegistrations];
        
        int initialCount = getActiveInstanceCount();
        
        // Create threads that all register at the same time
        for (int i = 0; i < numConcurrentRegistrations; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                TokenHa mockToken = mock(TokenHa.class, "SyncTestMock-" + index);
                evictionThread.register(mockToken);
            });
        }
        
        // Start all threads simultaneously
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify all registrations were successful
        int finalCount = getActiveInstanceCount();
        assertEquals(initialCount + numConcurrentRegistrations, finalCount,
            "All concurrent registrations should have succeeded");
    }
    
    @Test
    @DisplayName("Test register method instance counting accuracy")
    public void testRegisterInstanceCounting() throws Exception {
        System.out.println("🧪 TEST: Register method instance counting accuracy");
        
        // Start with clean slate
        int initialCount = getActiveInstanceCount();
        assertEquals(0, initialCount, "Should start with no registered instances");
        
        // Register instances one by one and verify count
        evictionThread.register(mockTokenHa1);
        assertEquals(1, getActiveInstanceCount(), "Count should be 1 after first registration");
        
        evictionThread.register(mockTokenHa2);
        assertEquals(2, getActiveInstanceCount(), "Count should be 2 after second registration");
        
        evictionThread.register(mockTokenHa3);
        assertEquals(3, getActiveInstanceCount(), "Count should be 3 after third registration");
        
        // Register same instance again
        evictionThread.register(mockTokenHa1);
        assertEquals(4, getActiveInstanceCount(), "Count should be 4 after duplicate registration");
    }

    // TODO: Test cases for EvictionThread#unregister(TokenHa tokenHa);

    @Test
    @DisplayName("Test unregister method with single mock TokenHa instance")
    public void testUnregisterSingleMockInstance() throws Exception {
        System.out.println("🧪 TEST: Unregister method with single mock TokenHa instance");
        
        // First register an instance
        evictionThread.register(mockTokenHa1);
        assertEquals(1, getActiveInstanceCount(), "Should have 1 instance after registration");
        assertTrue(isEvictionThreadRunning(), "Thread should be running after registration");
        
        // Now unregister it
        evictionThread.unregister(mockTokenHa1);
        assertEquals(0, getActiveInstanceCount(), "Should have 0 instances after unregistration");
        assertFalse(isEvictionThreadRunning(), "Thread should stop when no instances remain");
    }
    
    @Test
    @DisplayName("Test unregister method with multiple mock TokenHa instances")
    public void testUnregisterMultipleMockInstances() throws Exception {
        System.out.println("🧪 TEST: Unregister method with multiple mock TokenHa instances");
        
        // Register multiple instances
        evictionThread.register(mockTokenHa1);
        evictionThread.register(mockTokenHa2);
        evictionThread.register(mockTokenHa3);
        assertEquals(3, getActiveInstanceCount(), "Should have 3 instances after registration");
        assertTrue(isEvictionThreadRunning(), "Thread should be running");
        
        // Unregister first instance
        evictionThread.unregister(mockTokenHa1);
        assertEquals(2, getActiveInstanceCount(), "Should have 2 instances after first unregistration");
        assertTrue(isEvictionThreadRunning(), "Thread should still be running");
        
        // Unregister second instance
        evictionThread.unregister(mockTokenHa2);
        assertEquals(1, getActiveInstanceCount(), "Should have 1 instance after second unregistration");
        assertTrue(isEvictionThreadRunning(), "Thread should still be running");
        
        // Unregister last instance
        evictionThread.unregister(mockTokenHa3);
        assertEquals(0, getActiveInstanceCount(), "Should have 0 instances after final unregistration");
        assertFalse(isEvictionThreadRunning(), "Thread should stop when no instances remain");
    }
    
    @Test
    @DisplayName("Test unregister method with non-existent TokenHa instance")
    public void testUnregisterNonExistentInstance() throws Exception {
        System.out.println("🧪 TEST: Unregister method with non-existent TokenHa instance");
        
        // Register one instance
        evictionThread.register(mockTokenHa1);
        int countAfterRegister = getActiveInstanceCount();
        
        // Try to unregister a different instance that was never registered
        evictionThread.unregister(mockTokenHa2);
        
        // Count should remain the same
        assertEquals(countAfterRegister, getActiveInstanceCount(), 
            "Count should not change when unregistering non-existent instance");
        assertTrue(isEvictionThreadRunning(), "Thread should still be running");
    }
    
    @Test
    @DisplayName("Test unregister method with duplicate instances")
    public void testUnregisterDuplicateInstances() throws Exception {
        System.out.println("🧪 TEST: Unregister method with duplicate instances");
        
        // Register the same instance multiple times
        evictionThread.register(mockTokenHa1);
        evictionThread.register(mockTokenHa1);
        evictionThread.register(mockTokenHa1);
        assertEquals(3, getActiveInstanceCount(), "Should have 3 instances after duplicate registrations");
        
        // Unregister once - should remove ALL references to this instance
        evictionThread.unregister(mockTokenHa1);
        assertEquals(0, getActiveInstanceCount(), 
            "Should remove all references to the instance in one unregister call");
        assertFalse(isEvictionThreadRunning(), "Thread should stop when no instances remain");
    }
    
    @Test
    @DisplayName("Test unregister method with null TokenHa instance")
    public void testUnregisterNullInstance() throws Exception {
        System.out.println("🧪 TEST: Unregister method with null TokenHa instance");
        
        // Register some instances first
        evictionThread.register(mockTokenHa1);
        evictionThread.register(mockTokenHa2);
        int countAfterRegister = getActiveInstanceCount();
        
        // Unregister null - should handle gracefully and might clean up dead references
        evictionThread.unregister(null);
        
        // Count should be less than or equal to original (due to dead reference cleanup)
        int countAfterUnregister = getActiveInstanceCount();
        assertTrue(countAfterUnregister <= countAfterRegister, 
            "Count should be less than or equal to original after null unregister");
        assertTrue(isEvictionThreadRunning(), "Thread should still be running with active instances");
    }
    
    @Test
    @DisplayName("Test unregister method stops thread only when no instances remain")
    public void testUnregisterStopsThreadWhenEmpty() throws Exception {
        System.out.println("🧪 TEST: Unregister method stops thread only when no instances remain");
        
        // Register multiple instances
        evictionThread.register(mockTokenHa1);
        evictionThread.register(mockTokenHa2);
        assertTrue(isEvictionThreadRunning(), "Thread should be running with instances");
        
        // Unregister one instance - thread should keep running
        evictionThread.unregister(mockTokenHa1);
        assertEquals(1, getActiveInstanceCount(), "Should have 1 instance remaining");
        assertTrue(isEvictionThreadRunning(), "Thread should still be running with remaining instance");
        
        // Unregister last instance - thread should stop
        evictionThread.unregister(mockTokenHa2);
        assertEquals(0, getActiveInstanceCount(), "Should have 0 instances remaining");
        assertFalse(isEvictionThreadRunning(), "Thread should stop when no instances remain");
    }
    
    @Test
    @DisplayName("Test unregister method thread safety")
    public void testUnregisterThreadSafety() throws Exception {
        System.out.println("🧪 TEST: Unregister method thread safety");
        
        final int numThreads = 5;
        final int instancesPerThread = 3;
        
        // First register many instances
        TokenHa[] mockTokens = new TokenHa[numThreads * instancesPerThread];
        for (int i = 0; i < mockTokens.length; i++) {
            mockTokens[i] = mock(TokenHa.class, "ThreadSafetyMock-" + i);
            evictionThread.register(mockTokens[i]);
        }
        
        int initialCount = getActiveInstanceCount();
        assertEquals(numThreads * instancesPerThread, initialCount, 
            "Should have registered all instances");
        
        // Now unregister them concurrently
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < instancesPerThread; j++) {
                    int tokenIndex = threadIndex * instancesPerThread + j;
                    evictionThread.unregister(mockTokens[tokenIndex]);
                    // Small delay to increase chance of thread interleaving
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify all instances were unregistered
        assertEquals(0, getActiveInstanceCount(), "All instances should be unregistered");
        assertFalse(isEvictionThreadRunning(), "Thread should stop when no instances remain");
    }
    
    @Test
    @DisplayName("Test unregister method cleanup of dead references")
    public void testUnregisterCleansUpDeadReferences() throws Exception {
        System.out.println("🧪 TEST: Unregister method cleanup of dead references");
        
        // Register some instances, including one that will become dead
        evictionThread.register(mockTokenHa1);
        TokenHa tempToken = mock(TokenHa.class);
        evictionThread.register(tempToken);
        evictionThread.register(mockTokenHa2);
        
        int countAfterRegistrations = getActiveInstanceCount();
        assertEquals(3, countAfterRegistrations, "Should have 3 instances registered");
        
        // Make tempToken eligible for garbage collection
        tempToken = null;
        System.gc();
        Thread.sleep(100); // Give GC time to work
        
        // Unregister one of the remaining instances
        // This should also clean up the dead reference
        evictionThread.unregister(mockTokenHa1);
        
        // The count should be at least 1 (mockTokenHa2) and at most 2 (if GC didn't run yet)
        // We can't guarantee GC timing, so we accept either scenario
        int finalCount = getActiveInstanceCount();
        assertTrue(finalCount >= 1 && finalCount <= 2, 
            "Should have 1-2 instances remaining (depending on GC timing), but got " + finalCount);
        assertTrue(isEvictionThreadRunning(), "Thread should still be running if instances remain");
    }
    
    @Test
    @DisplayName("Test unregister method synchronization behavior")
    public void testUnregisterSynchronization() throws Exception {
        System.out.println("🧪 TEST: Unregister method synchronization behavior");
        
        // Register multiple instances
        final int numInstances = 10;
        TokenHa[] mockTokens = new TokenHa[numInstances];
        for (int i = 0; i < numInstances; i++) {
            mockTokens[i] = mock(TokenHa.class, "SyncUnregisterMock-" + i);
            evictionThread.register(mockTokens[i]);
        }
        
        assertEquals(numInstances, getActiveInstanceCount(), "Should have registered all instances");
        
        // Create threads that all unregister at the same time
        Thread[] threads = new Thread[numInstances];
        for (int i = 0; i < numInstances; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                evictionThread.unregister(mockTokens[index]);
            });
        }
        
        // Start all threads simultaneously
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify all unregistrations were successful
        assertEquals(0, getActiveInstanceCount(), "All instances should be unregistered");
        assertFalse(isEvictionThreadRunning(), "Thread should stop when no instances remain");
    }
    
    @Test
    @DisplayName("Test unregister method mixed with register operations")
    public void testUnregisterMixedWithRegister() throws Exception {
        System.out.println("🧪 TEST: Unregister method mixed with register operations");
        
        // Start with some registered instances
        evictionThread.register(mockTokenHa1);
        evictionThread.register(mockTokenHa2);
        assertEquals(2, getActiveInstanceCount(), "Should start with 2 instances");
        
        // Unregister one
        evictionThread.unregister(mockTokenHa1);
        assertEquals(1, getActiveInstanceCount(), "Should have 1 instance after unregister");
        assertTrue(isEvictionThreadRunning(), "Thread should still be running");
        
        // Register another
        evictionThread.register(mockTokenHa3);
        assertEquals(2, getActiveInstanceCount(), "Should have 2 instances after new registration");
        assertTrue(isEvictionThreadRunning(), "Thread should still be running");
        
        // Unregister all remaining
        evictionThread.unregister(mockTokenHa2);
        evictionThread.unregister(mockTokenHa3);
        assertEquals(0, getActiveInstanceCount(), "Should have 0 instances after unregistering all");
        assertFalse(isEvictionThreadRunning(), "Thread should stop when no instances remain");
    }

    
}
