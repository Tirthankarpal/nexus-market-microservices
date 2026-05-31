package com.nexusmarket.order.event;

public record OrderPlacedEvent(String orderNumber, String email) {}
