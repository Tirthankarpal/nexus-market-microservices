package com.nexusmarket.order.event;

public record PaymentCompletedEvent(String orderNumber, String userEmail, boolean success, String message) {
}
