package com.nexusmarket.inventory.service;

import com.nexusmarket.inventory.dto.InventoryResponse;
import com.nexusmarket.inventory.model.Inventory;
import com.nexusmarket.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.nexusmarket.inventory.exception.InsufficientStockException;
import com.nexusmarket.inventory.exception.InventoryNotFoundException;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Checks if products corresponding to skuCodes are in stock.
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCodes) {
        if (skuCodes == null || skuCodes.isEmpty()) {
            return List.of();
        }

        Map<String, Inventory> inventoryMap = inventoryRepository.findBySkuCodeIn(skuCodes).stream()
                .collect(Collectors.toMap(Inventory::getSkuCode, inv -> inv, (a, b) -> a));

        List<InventoryResponse> responses = new ArrayList<>();
        for (String sku : skuCodes) {
            Inventory inv = inventoryMap.get(sku);
            if (inv != null) {
                responses.add(new InventoryResponse(inv.getSkuCode(), inv.getQuantity() > 0, inv.getQuantity()));
            } else {
                responses.add(new InventoryResponse(sku, false, 0));
            }
        }
        return responses;
    }

    /**
     * Deducts stock for a given SKU with pessimistic concurrency lock guarantees.
     */
    @Transactional
    public void deductStock(String skuCode, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to deduct must be greater than zero");
        }

        // Lock database row to prevent concurrent modifications during decrement
        Inventory inventory = inventoryRepository.findBySkuCodeWithLock(skuCode)
                .orElseThrow(() -> new InventoryNotFoundException("Product not found in inventory: " + skuCode));

        if (inventory.getQuantity() < quantity) {
            throw new InsufficientStockException("Insufficient stock for SKU " + skuCode + ". Available: " + inventory.getQuantity() + ", Requested: " + quantity);
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventoryRepository.save(inventory);
    }

    /**
     * Restores stock for a given SKU (used for Saga compensation).
     */
    @Transactional
    public void restoreStock(String skuCode, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to restore must be greater than zero");
        }

        Inventory inventory = inventoryRepository.findBySkuCodeWithLock(skuCode)
                .orElseThrow(() -> new InventoryNotFoundException("Product not found in inventory: " + skuCode));

        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);
    }
    
    /**
     * Adds or updates stock for a SKU with pessimistic lock to prevent lost updates.
     */
    @Transactional
    public void addStock(String skuCode, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be greater than zero");
        }

        Inventory inventory = inventoryRepository.findBySkuCodeWithLock(skuCode)
                .orElse(Inventory.builder().skuCode(skuCode).quantity(0).build());
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);
    }
    
    /**
     * Sets absolute stock level for a SKU with pessimistic lock.
     */
    @Transactional
    public void setStock(String skuCode, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        Inventory inventory = inventoryRepository.findBySkuCodeWithLock(skuCode)
                .orElse(Inventory.builder().skuCode(skuCode).quantity(0).build());
        inventory.setQuantity(quantity);
        inventoryRepository.save(inventory);
    }
}
