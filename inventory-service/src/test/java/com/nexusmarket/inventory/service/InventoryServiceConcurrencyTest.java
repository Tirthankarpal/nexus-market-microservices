package com.nexusmarket.inventory.service;

import com.nexusmarket.inventory.model.Inventory;
import com.nexusmarket.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class InventoryServiceConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    private static final String SKU_CODE = "IPHONE_16";

    @BeforeEach
    void setUp() {
        inventoryRepository.findBySkuCode(SKU_CODE)
                .ifPresent(inv -> inventoryRepository.delete(inv));

        Inventory inventory = Inventory.builder()
                .skuCode(SKU_CODE)
                .quantity(10) // Total stock is 10
                .build();
        inventoryRepository.save(inventory);
    }

    @Test
    void testConcurrentStockDeduction() throws InterruptedException {
        int numberOfThreads = 15; // 15 threads attempt to deduct 1 item each
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    latch.await(); // Wait for all threads to start at the exact same moment
                    inventoryService.deductStock(SKU_CODE, 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // Release the latch!
        doneLatch.await(); // Wait for all threads to finish

        // Since we have only 10 items in stock:
        // - Exactly 10 threads should succeed.
        // - Exactly 5 threads should fail.
        assertEquals(10, successCount.get(), "Exactly 10 stock deductions should succeed");
        assertEquals(5, failureCount.get(), "Exactly 5 stock deductions should fail due to out of stock");

        // Stock quantity in database should be exactly 0
        Inventory updatedInventory = inventoryRepository.findBySkuCode(SKU_CODE).orElseThrow();
        assertEquals(0, updatedInventory.getQuantity(), "Database quantity must be exactly 0");
    }
}
