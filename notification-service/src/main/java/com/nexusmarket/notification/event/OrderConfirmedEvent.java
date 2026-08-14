package com.nexusmarket.notification.event;

public record OrderConfirmedEvent(String orderNumber, String email) {
}
