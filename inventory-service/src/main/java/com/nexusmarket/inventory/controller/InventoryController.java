package com.nexusmarket.inventory.controller;

import com.nexusmarket.inventory.dto.InventoryResponse;
import com.nexusmarket.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Endpoint to check stock availability for a list of SKU codes.
     */
    @GetMapping
    public List<InventoryResponse> isInStock(@RequestParam List<String> skuCode) {
        return inventoryService.isInStock(skuCode);
    }

    /**
     * Endpoint to deduct stock (typically called by Order Service during checkout).
     */
    @PutMapping("/deduct")
    public ResponseEntity<Void> deductStock(
            @RequestParam String skuCode,
            @RequestParam Integer quantity) {
        inventoryService.deductStock(skuCode, quantity);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint to add stock (restricted to ADMIN role).
     */
    @PostMapping
    public ResponseEntity<String> addStock(
            @RequestParam String skuCode,
            @RequestParam Integer quantity,
            @RequestHeader(value = "X-Authenticated-Role", required = false) String role) {
        
        // Enforce role-based boundary restriction
        if (role != null && !role.equalsIgnoreCase("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMIN can stock inventory items");
        }
        
        inventoryService.addStock(skuCode, quantity);
        return ResponseEntity.status(HttpStatus.CREATED).body("Stock updated successfully");
    }
}
