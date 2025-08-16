package com.github.tsutomunakamura.tokenha;

import org.junit.jupiter.api.Test;

import com.github.tsutomunakamura.tokenha.element.TokenElement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Test class for TokenUtil.
 */
public class TokenUtilTest {
    @Test
    public void testTest() {
        Deque<TokenElement> fifoQueue = new ArrayDeque<>();
        fifoQueue.add(new TokenElement("token1", System.currentTimeMillis()));
        fifoQueue.add(new TokenElement("token2", System.currentTimeMillis() + 60));
        fifoQueue.add(new TokenElement("token3", System.currentTimeMillis() + 120));

        Iterator<TokenElement> iterator = fifoQueue.descendingIterator();
        iterator.forEachRemaining(element -> {
            System.out.println("Token: " + element.getToken() + ", Time: " + element.getTimeMillis());
        });
        System.out.println("Finished iterating through the queue.");
    }

}