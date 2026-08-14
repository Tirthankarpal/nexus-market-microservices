package com.nexusmarket.order.event;

public record PaymentRequiredEvent(String orderNumber, String userEmail, java.math.BigDecimal amount) {
}
