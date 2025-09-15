package com.github.tsutomunakamura.tokenha;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tsutomunakamura.tokenha.config.TokenHaConfig;
import com.github.tsutomunakamura.tokenha.data.TokenData;
import com.github.tsutomunakamura.tokenha.element.TokenElement;
import com.google.gson.Gson;

public class LongRunDemo {

    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    
    @Test
    @DisplayName("Long run demo")
    @Disabled("Run manually with \"mvn test -Dtest=LongRunDemo#longRunDemo -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition\"")
    void longRunDemo() throws InterruptedException, IOException {
        /**
         * This is a long run demonstration of TokenHa usage.
         * TokenHa will run during 60 minutes with properties like below.
         * 
         * - expirationTimeMillis: 1 minutes
         * - maxTokens: 3
         * - coolTimeToAddMillis: 10 seconds
         * - numberOfLastTokens: 1
         * - persistenceFilePath: "long-run-test-tokens.json"
         * 
         * New tokens will be added every 6 seconds but some of them will be rejected due to the cool time.
         * With some retries, the new tokens will be added successfully.
         * This test case will print the status of TokenHaConfig each 1 second.
         * So we can see how TokenHa behaves over time.
         */

        System.out.println("=== Long Run Demo ===");
        TokenHaConfig config = new TokenHaConfig.Builder()
            .expirationTimeMillis(60000)  // 1 minute
            .maxTokens(15)
            .coolTimeToAddMillis(10000)   // 10 seconds
            .numberOfLastTokens(1)
            .persistenceFilePath("long-run-test-tokens.json")
            .build();
        
        // Start the loop to add tokens and print status during 60 minutes.
        try (TokenHa tokenHa = new TokenHa(config)) {
            System.out.println("TokenHa started with config: " + config);
            long endTime = System.currentTimeMillis() + 60 * 60 * 1000; // 60 minutes from now
            int tokenCounter = 0;

            while (System.currentTimeMillis() < endTime) {
                String newToken = "token-" + tokenCounter;
                boolean added = tokenHa.addIfAvailable(newToken);
                if (added) {
                    System.out.println("Added new token: " + newToken);
                    tokenCounter++;
                } else {
                    System.out.println("Failed to add token (cool time?): " + newToken);
                }
                

                // Convert json string to GSON
                String jsonTokenHa = tokenHa.toJson();
                Gson gson = new Gson();
                TokenData tokenData = gson.fromJson(jsonTokenHa, TokenData.class);
                
                // Get the descending list from TokenHa
                List<TokenElement> descList = tokenHa.getDescList();
                
                // Get tokens from TokenData (these are in ascending order - oldest to newest)
                List<TokenElement> tokensFromJson = tokenData.getTokens();
                
                // Reverse the JSON tokens to match the descending order
                List<TokenElement> reversedJsonTokens = new ArrayList<>(tokensFromJson);
                java.util.Collections.reverse(reversedJsonTokens);
                
                // Assert that both lists have the same size
                assertEquals(descList.size(), reversedJsonTokens.size(), 
                    "Size mismatch between getDescList() and reversed tokenData.getTokens()");
                
                // Assert that both lists contain the same elements in the same order
                for (int i = 0; i < descList.size(); i++) {
                    TokenElement descElement = descList.get(i);
                    TokenElement jsonElement = reversedJsonTokens.get(i);
                    
                    assertEquals(descElement.getToken(), jsonElement.getToken(),
                        String.format("Token mismatch at index %d: descList='%s', jsonList='%s'", 
                            i, descElement.getToken(), jsonElement.getToken()));
                    
                    assertEquals(descElement.getTimeMillis(), jsonElement.getTimeMillis(),
                        String.format("Time mismatch at index %d for token '%s': descList=%d, jsonList=%d", 
                            i, descElement.getToken(), descElement.getTimeMillis(), jsonElement.getTimeMillis()));
                }
                
                // Additional assertion: verify the order is correct (newest to oldest in descList)
                for (int i = 1; i < descList.size(); i++) {
                    assertTrue(descList.get(i-1).getTimeMillis() >= descList.get(i).getTimeMillis(),
                        String.format("Descending order violation at index %d: %d should be >= %d",
                            i, descList.get(i-1).getTimeMillis(), descList.get(i).getTimeMillis()));
                }
                
                // Print status. Print size of queue and tokens in it with CSV format.
                System.out.print(dateFormatter.format(new Date()) + " - Size: " + tokenHa.getQueueSize() + " ");
                tokenData.getTokens().forEach(te -> {
                    System.out.print(te.getToken() + "(" + (60000 - (System.currentTimeMillis() - te.getTimeMillis())) + "ms),");
                });
                System.out.println();
                
                System.out.println("✓ Assertions passed: getDescList() and tokenData.getTokens() are equivalent");

                // Wait for 6 seconds before next attempt
                Thread.sleep(6000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
