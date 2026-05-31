package com.nexusmarket.notification.event;

public record OrderPlacedEvent(String orderNumber, String email) {}
