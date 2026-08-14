package com.nexusmarket.order.event;

public record OrderConfirmedEvent(String orderNumber, String email) {
}
