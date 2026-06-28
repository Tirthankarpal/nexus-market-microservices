package com.nexusmarket.inventory.service;

import com.nexusmarket.inventory.dto.InventoryResponse;
import com.nexusmarket.inventory.model.Inventory;
import com.nexusmarket.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        return inventoryRepository.findBySkuCodeIn(skuCodes).stream()
                .map(inventory -> new InventoryResponse(
                        inventory.getSkuCode(),
                        inventory.getQuantity() > 0,
                        inventory.getQuantity()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Deducts stock for a given SKU with pessimistic concurrency lock guarantees.
     */
    @Transactional
    public void deductStock(String skuCode, Integer quantity) {
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
     * Adds or updates stock for a SKU.
     */
    @Transactional
    public void addStock(String skuCode, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElse(Inventory.builder().skuCode(skuCode).quantity(0).build());
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventoryRepository.save(inventory);
    }
    
    /**
     * Sets absolute stock level for a SKU.
     */
    @Transactional
    public void setStock(String skuCode, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElse(Inventory.builder().skuCode(skuCode).quantity(0).build());
        inventory.setQuantity(quantity);
        inventoryRepository.save(inventory);
    }
}
