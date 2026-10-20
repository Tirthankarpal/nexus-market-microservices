package com.nexusmarket.notification.event;

public class OrderConfirmedEvent {
    private String orderNumber;
    private String email;

    public OrderConfirmedEvent() {
    }

    public OrderConfirmedEvent(String orderNumber, String email) {
        this.orderNumber = orderNumber;
        this.email = email;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String orderNumber() {
        return orderNumber;
    }

    public String email() {
        return email;
    }
}
