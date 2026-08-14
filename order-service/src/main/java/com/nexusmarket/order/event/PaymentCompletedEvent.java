package com.nexusmarket.order.event;

public class PaymentCompletedEvent {
    private String orderNumber;
    private String userEmail;
    private boolean success;
    private String message;

    public PaymentCompletedEvent() {}

    public PaymentCompletedEvent(String orderNumber, String userEmail, boolean success, String message) {
        this.orderNumber = orderNumber;
        this.userEmail = userEmail;
        this.success = success;
        this.message = message;
    }

    public String orderNumber() { return orderNumber; }
    public String userEmail() { return userEmail; }
    public boolean success() { return success; }
    public String message() { return message; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
