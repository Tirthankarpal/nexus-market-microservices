package com.nexusmarket.inventory.dto;

public record InventoryResponse(String skuCode, boolean isInStock, Integer quantity) {}
