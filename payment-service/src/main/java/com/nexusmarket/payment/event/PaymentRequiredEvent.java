package com.nexusmarket.payment.event;

public record PaymentRequiredEvent(String orderNumber, String userEmail, java.math.BigDecimal amount) {
}
