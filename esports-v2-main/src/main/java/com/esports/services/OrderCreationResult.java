package com.esports.services;

public class OrderCreationResult {

    private final boolean success;
    private final int orderId;
    private final String reference;
    private final double totalAmount;
    private final String message;

    public OrderCreationResult(boolean success, int orderId, String reference, double totalAmount, String message) {
        this.success = success;
        this.orderId = orderId;
        this.reference = reference;
        this.totalAmount = totalAmount;
        this.message = message;
    }

    public static OrderCreationResult success(int orderId, String reference, double totalAmount) {
        return new OrderCreationResult(true, orderId, reference, totalAmount, "Order created successfully.");
    }

    public static OrderCreationResult error(String message) {
        return new OrderCreationResult(false, -1, null, 0.0, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getReference() {
        return reference;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getMessage() {
        return message;
    }
}