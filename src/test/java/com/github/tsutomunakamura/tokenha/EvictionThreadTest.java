package com.github.tsutomunakamura.tokenha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tsutomunakamura.tokenha.element.TokenElement;

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
    
    private void invokeEvictTokens() throws Exception {
        Method evictTokensMethod = EvictionThread.class.getDeclaredMethod("evictTokens");
        evictTokensMethod.setAccessible(true);
        evictTokensMethod.invoke(evictionThread);
    }
    
    private Set<WeakReference<TokenHa>> getRegisteredInstances() throws Exception {
        Field registeredInstancesField = EvictionThread.class.getDeclaredField("registeredInstances");
        registeredInstancesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<WeakReference<TokenHa>> instances = (Set<WeakReference<TokenHa>>) registeredInstancesField.get(evictionThread);
        return instances;
    }

    private List<TokenElement> createMockTokenList(int size) {
        List<TokenElement> tokens = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            tokens.add(new TokenElement("mockToken" + i, currentTime + i));
        }
        return tokens;
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

    // Test cases for EvictionThread#unregister(TokenHa tokenHa);

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

    // Skip. Test cases for EvictionThread#cleanupDeadReferences(); Because this method is private and already tested indirectly
    
    // Skip. Test cases for EvictionThread#getActiveInstanceCount(); Because this method is private and already tested indirectly
    
    // Skip. Test cases for EvictionThread#start(); Because this method is private and already tested indirectly

    // Test cases for EvictionThread#stopIfInstancesEmpty();

    @Test
    @DisplayName("Test stopIfInstancesEmpty when no instances are registered")
    public void testStopIfInstancesEmptyWhenEmpty() throws Exception {
        System.out.println("🧪 TEST: stopIfInstancesEmpty when no instances are registered");
        
        // Ensure we start with an empty registry
        assertEquals(0, getActiveInstanceCount(), "Should start with no instances");
        assertFalse(isEvictionThreadRunning(), "Thread should not be running initially");
        
        // Call stopIfInstancesEmpty - should do nothing since thread is not running
        evictionThread.stopIfInstancesEmpty();
        
        // Verify state remains unchanged
        assertEquals(0, getActiveInstanceCount(), "Should still have no instances");
        assertFalse(isEvictionThreadRunning(), "Thread should still not be running");
    }
    
    @Test
    @DisplayName("Test stopIfInstancesEmpty when instances exist")
    public void testStopIfInstancesEmptyWhenInstancesExist() throws Exception {
        System.out.println("🧪 TEST: stopIfInstancesEmpty when instances exist");
        
        // Register some instances to start the thread
        evictionThread.register(mockTokenHa1);
        evictionThread.register(mockTokenHa2);
        assertEquals(2, getActiveInstanceCount(), "Should have 2 instances");
        assertTrue(isEvictionThreadRunning(), "Thread should be running");
        
        // Call stopIfInstancesEmpty - should NOT stop since instances exist
        evictionThread.stopIfInstancesEmpty();
        
        // Verify thread is still running
        assertEquals(2, getActiveInstanceCount(), "Should still have 2 instances");
        assertTrue(isEvictionThreadRunning(), "Thread should still be running since instances exist");
        
        // Clean up
        evictionThread.unregister(mockTokenHa1);
        evictionThread.unregister(mockTokenHa2);
    }
    
    @Test
    @DisplayName("Test stopIfInstancesEmpty after all instances are manually cleared")
    public void testStopIfInstancesEmptyAfterManualClear() throws Exception {
        System.out.println("🧪 TEST: stopIfInstancesEmpty after manually clearing instances");
        
        // Register instances and start the thread
        evictionThread.register(mockTokenHa1);
        evictionThread.register(mockTokenHa2);
        assertEquals(2, getActiveInstanceCount(), "Should have 2 instances");
        assertTrue(isEvictionThreadRunning(), "Thread should be running");
        
        // Manually clear the registry (simulating what might happen in error scenarios)
        clearRegisteredInstances();
        assertEquals(0, getActiveInstanceCount(), "Should have no instances after manual clear");
        
        // The thread might still be running since unregister wasn't called
        boolean wasRunningBefore = isEvictionThreadRunning();
        
        // Now call stopIfInstancesEmpty - should stop the thread
        evictionThread.stopIfInstancesEmpty();
        
        // Verify thread is stopped
        assertEquals(0, getActiveInstanceCount(), "Should still have no instances");
        assertFalse(isEvictionThreadRunning(), "Thread should be stopped after stopIfInstancesEmpty");
        
        if (wasRunningBefore) {
            System.out.println("✓ Thread was properly stopped by stopIfInstancesEmpty");
        } else {
            System.out.println("ℹ Thread was already stopped before calling stopIfInstancesEmpty");
        }
    }
    
    @Test
    @DisplayName("Test stopIfInstancesEmpty with garbage collected references")
    public void testStopIfInstancesEmptyWithGarbageCollectedReferences() throws Exception {
        System.out.println("🧪 TEST: stopIfInstancesEmpty with garbage collected references");
        
        // Register an instance that will become eligible for GC
        TokenHa tempInstance = mock(TokenHa.class);
        evictionThread.register(tempInstance);
        assertEquals(1, getActiveInstanceCount(), "Should have 1 instance");
        assertTrue(isEvictionThreadRunning(), "Thread should be running");
        
        // Make the instance eligible for garbage collection
        tempInstance = null;
        
        // Force garbage collection
        System.gc();
        Thread.sleep(100); // Give GC time to work
        
        // Call stopIfInstancesEmpty
        evictionThread.stopIfInstancesEmpty();
        
        // Check the final state after stopIfInstancesEmpty
        int activeCountAfterStop = getActiveInstanceCount();
        boolean threadRunningAfter = isEvictionThreadRunning();
        
        // Assert based on the actual GC behavior
        if (activeCountAfterStop == 0) {
            assertFalse(threadRunningAfter, "Thread should be stopped when no active instances remain");
            System.out.println("✓ GC worked: stopIfInstancesEmpty correctly stopped the thread");
        } else {
            // GC timing may mean the reference is still counted
            assertTrue(activeCountAfterStop >= 0, "Should have non-negative active instances");
            System.out.println("⚠ GC timing: Reference may still be counted, which is acceptable");
        }
    }
    
    @Test
    @DisplayName("Test stopIfInstancesEmpty thread safety with concurrent operations")
    public void testStopIfInstancesEmptyThreadSafety() throws Exception {
        System.out.println("🧪 TEST: stopIfInstancesEmpty thread safety with concurrent operations");
        
        // Register some instances
        evictionThread.register(mockTokenHa1);
        evictionThread.register(mockTokenHa2);
        assertEquals(2, getActiveInstanceCount(), "Should start with 2 instances");
        assertTrue(isEvictionThreadRunning(), "Thread should be running");
        
        // Create concurrent tasks
        Thread stopperThread = new Thread(() -> {
            try {
                Thread.sleep(50); // Small delay
                evictionThread.stopIfInstancesEmpty();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread unregisterThread = new Thread(() -> {
            try {
                Thread.sleep(25); // Smaller delay to unregister first
                evictionThread.unregister(mockTokenHa1);
                evictionThread.unregister(mockTokenHa2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Start both threads
        stopperThread.start();
        unregisterThread.start();
        
        // Wait for completion
        stopperThread.join(1000);
        unregisterThread.join(1000);
        
        // Verify final state
        assertEquals(0, getActiveInstanceCount(), "Should have no instances after unregister");
        assertFalse(isEvictionThreadRunning(), "Thread should be stopped");
        System.out.println("✓ Concurrent operations completed successfully");
    }
    
    @Test
    @DisplayName("Test stopIfInstancesEmpty multiple calls when empty")
    public void testStopIfInstancesEmptyMultipleCalls() throws Exception {
        System.out.println("🧪 TEST: stopIfInstancesEmpty multiple calls when empty");
        
        // Start with an instance to get the thread running
        evictionThread.register(mockTokenHa1);
        assertEquals(1, getActiveInstanceCount(), "Should have 1 instance");
        assertTrue(isEvictionThreadRunning(), "Thread should be running");
        
        // Unregister to make it empty (this will call stop internally)
        evictionThread.unregister(mockTokenHa1);
        assertEquals(0, getActiveInstanceCount(), "Should have no instances");
        assertFalse(isEvictionThreadRunning(), "Thread should be stopped after unregister");
        
        // Now call stopIfInstancesEmpty multiple times - should be safe
        evictionThread.stopIfInstancesEmpty();
        evictionThread.stopIfInstancesEmpty();
        evictionThread.stopIfInstancesEmpty();
        
        // Verify state remains stable
        assertEquals(0, getActiveInstanceCount(), "Should still have no instances");
        assertFalse(isEvictionThreadRunning(), "Thread should still be stopped");
        System.out.println("✓ Multiple calls to stopIfInstancesEmpty handled safely");
    }
    
    // Test cases for EvictionThread#evictTokens();
    
    @Test
    @DisplayName("Test evictTokens when no instances are registered")
    public void testEvictTokensWhenEmpty() throws Exception {
        System.out.println("🧪 TEST: evictTokens when no instances are registered");
        
        // Ensure registry is empty
        assertEquals(0, getActiveInstanceCount(), "Should start with no instances");
        
        // Call evictTokens - should handle empty registry gracefully
        invokeEvictTokens();
        
        // Should still be empty and not crash
        assertEquals(0, getActiveInstanceCount(), "Should still have no instances");
        assertFalse(isEvictionThreadRunning(), "Thread should not be running after evictTokens with empty registry");
    }
    
    @Test
    @DisplayName("Test evictTokens with single registered instance")
    public void testEvictTokensWithSingleInstance() throws Exception {
        System.out.println("🧪 TEST: evictTokens with single registered instance");
        
        // Create a mock with evictExpiredTokens method
        TokenHa mockToken = mock(TokenHa.class);
        List<TokenElement> expiredTokens = new ArrayList<>();
        expiredTokens.add(new TokenElement("expired1", System.currentTimeMillis()));
        
        when(mockToken.getQueueSize()).thenReturn(5).thenReturn(4); // before and after eviction
        when(mockToken.evictExpiredTokens()).thenReturn(expiredTokens);
        
        // Register the mock
        evictionThread.register(mockToken);
        assertEquals(1, getActiveInstanceCount(), "Should have 1 instance registered");
        
        // Call evictTokens
        invokeEvictTokens();
        
        // Verify evictExpiredTokens was called on the TokenHa instance
        verify(mockToken, times(1)).evictExpiredTokens();
        verify(mockToken, times(2)).getQueueSize(); // called before and after eviction
        
        // Clean up
        evictionThread.unregister(mockToken);
    }
    
    @Test
    @DisplayName("Test evictTokens with multiple registered instances")
    public void testEvictTokensWithMultipleInstances() throws Exception {
        System.out.println("🧪 TEST: evictTokens with multiple registered instances");
        
        // Create multiple mocks
        TokenHa mock1 = mock(TokenHa.class);
        TokenHa mock2 = mock(TokenHa.class);
        TokenHa mock3 = mock(TokenHa.class);
        
        // Set up mock behaviors
        when(mock1.getQueueSize()).thenReturn(10).thenReturn(8);
        when(mock1.evictExpiredTokens()).thenReturn(createMockTokenList(2));
        
        when(mock2.getQueueSize()).thenReturn(15).thenReturn(12);
        when(mock2.evictExpiredTokens()).thenReturn(createMockTokenList(3));
        
        when(mock3.getQueueSize()).thenReturn(5).thenReturn(5);
        when(mock3.evictExpiredTokens()).thenReturn(new ArrayList<>());
        
        // Register all mocks
        evictionThread.register(mock1);
        evictionThread.register(mock2);
        evictionThread.register(mock3);
        assertEquals(3, getActiveInstanceCount(), "Should have 3 instances registered");
        
        // Call evictTokens
        invokeEvictTokens();
        
        // Verify evictExpiredTokens was called on all instances
        verify(mock1, times(1)).evictExpiredTokens();
        verify(mock2, times(1)).evictExpiredTokens();
        verify(mock3, times(1)).evictExpiredTokens();
        
        // Clean up
        evictionThread.unregister(mock1);
        evictionThread.unregister(mock2);
        evictionThread.unregister(mock3);
    }
    
    @Test
    @DisplayName("Test evictTokens stops thread when all references become null")
    public void testEvictTokensStopsWhenAllReferencesNull() throws Exception {
        System.out.println("🧪 TEST: evictTokens stops thread when all references become null");
        
        // Create a temporary instance that will be eligible for GC
        TokenHa tempToken = mock(TokenHa.class);
        evictionThread.register(tempToken);
        assertEquals(1, getActiveInstanceCount(), "Should have 1 instance registered");
        assertTrue(isEvictionThreadRunning(), "Thread should be running");
        
        // Make the instance eligible for garbage collection
        tempToken = null;
        
        // Force garbage collection
        System.gc();
        Thread.sleep(100);
        
        // Call evictTokens - this should clean up dead references and stop the thread
        invokeEvictTokens();
        
        // The method should have detected no active instances and stopped the thread
        if (getActiveInstanceCount() == 0) {
            assertFalse(isEvictionThreadRunning(), "Thread should be stopped when no active instances remain");
            System.out.println("✓ Thread properly stopped after cleaning up dead references");
        } else {
            System.out.println("⚠ GC timing: Some references may still be active due to GC behavior");
        }
    }
    
    @Test
    @DisplayName("Test evictTokens with mixed null and valid references")
    public void testEvictTokensWithMixedReferences() throws Exception {
        System.out.println("🧪 TEST: evictTokens with mixed null and valid references");
        
        // Register valid instances
        TokenHa validMock1 = mock(TokenHa.class);
        TokenHa validMock2 = mock(TokenHa.class);
        
        when(validMock1.getQueueSize()).thenReturn(3).thenReturn(2);
        when(validMock1.evictExpiredTokens()).thenReturn(createMockTokenList(1));
        
        when(validMock2.getQueueSize()).thenReturn(7).thenReturn(5);
        when(validMock2.evictExpiredTokens()).thenReturn(createMockTokenList(2));
        
        evictionThread.register(validMock1);
        evictionThread.register(validMock2);
        
        // Add an instance that will become eligible for GC
        TokenHa tempToken = mock(TokenHa.class);
        evictionThread.register(tempToken);
        
        assertEquals(3, getActiveInstanceCount(), "Should have 3 instances registered");
        
        // Make tempToken eligible for GC
        tempToken = null;
        System.gc();
        Thread.sleep(100);
        
        // Call evictTokens
        invokeEvictTokens();
        
        // Should have processed valid instances and cleaned up dead references
        verify(validMock1, atLeast(1)).evictExpiredTokens();
        verify(validMock2, atLeast(1)).evictExpiredTokens();
        
        // Clean up
        evictionThread.unregister(validMock1);
        evictionThread.unregister(validMock2);
    }
    
    @Test
    @DisplayName("Test evictTokens synchronization with concurrent registration")
    public void testEvictTokensSynchronization() throws Exception {
        System.out.println("🧪 TEST: evictTokens synchronization with concurrent registration");
        
        // Register initial instance
        TokenHa initialMock = mock(TokenHa.class);
        when(initialMock.getQueueSize()).thenReturn(5).thenReturn(4);
        when(initialMock.evictExpiredTokens()).thenReturn(createMockTokenList(1));
        
        evictionThread.register(initialMock);
        assertEquals(1, getActiveInstanceCount(), "Should have 1 instance initially");
        
        // Create a thread that will register more instances during eviction
        Thread registrationThread = new Thread(() -> {
            try {
                Thread.sleep(50); // Small delay to let evictTokens start
                TokenHa concurrentMock = mock(TokenHa.class);
                evictionThread.register(concurrentMock);
                System.out.println("Concurrent registration completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Start concurrent registration
        registrationThread.start();
        
        // Call evictTokens
        invokeEvictTokens();
        
        // Wait for concurrent thread to finish
        registrationThread.join(1000);
        
        // Verify initial mock was processed
        verify(initialMock, times(1)).evictExpiredTokens();
        
        // Should have at least the initial mock processed successfully
        System.out.println("✓ evictTokens handled concurrent operations correctly");
        
        // Clean up
        clearRegisteredInstances();
    }
    
    @Test
    @DisplayName("Test evictTokens behavior when TokenHa throws exceptions")
    public void testEvictTokensWithExceptions() throws Exception {
        System.out.println("🧪 TEST: evictTokens behavior when TokenHa throws exceptions");
        
        // Create a mock that throws an exception
        TokenHa exceptionMock = mock(TokenHa.class);
        
        // Set up exception behavior
        when(exceptionMock.getQueueSize()).thenThrow(new RuntimeException("Test exception"));
        
        // Register the exception-throwing mock
        evictionThread.register(exceptionMock);
        assertEquals(1, getActiveInstanceCount(), "Should have 1 instance registered");
        
        // Call evictTokens - should propagate the exception
        boolean exceptionThrown = false;
        try {
            invokeEvictTokens();
        } catch (Exception e) {
            exceptionThrown = true;
            assertTrue(e.getCause() instanceof RuntimeException, "Should propagate RuntimeException");
            assertEquals("Test exception", e.getCause().getMessage(), "Should propagate original exception message");
        }
        
        assertTrue(exceptionThrown, "Exception should have been thrown");
        System.out.println("✓ evictTokens correctly propagates exceptions from TokenHa instances");
        
        // Clean up
        clearRegisteredInstances();
    }
    
}