package com.nexusmarket.order.dto;

public record InventoryResponse(String skuCode, boolean isInStock, Integer quantity) {}
