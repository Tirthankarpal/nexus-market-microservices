package com.nexusmarket.payment.event;

public record PaymentCompletedEvent(String orderNumber, String userEmail, boolean success, String message) {
}
