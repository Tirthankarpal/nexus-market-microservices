package com.nexusmarket.payment.event;

import java.math.BigDecimal;

public class PaymentRequiredEvent {
    private String orderNumber;
    private String userEmail;
    private BigDecimal amount;

    public PaymentRequiredEvent() {}

    public PaymentRequiredEvent(String orderNumber, String userEmail, BigDecimal amount) {
        this.orderNumber = orderNumber;
        this.userEmail = userEmail;
        this.amount = amount;
    }

    public String orderNumber() { return orderNumber; }
    public String userEmail() { return userEmail; }
    public BigDecimal amount() { return amount; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
