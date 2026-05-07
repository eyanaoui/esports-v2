package com.esports.services;

public class PaymentResult {

    private final boolean success;
    private final String sessionId;
    private final String checkoutUrl;
    private final String message;

    public PaymentResult(boolean success, String sessionId, String checkoutUrl, String message) {
        this.success = success;
        this.sessionId = sessionId;
        this.checkoutUrl = checkoutUrl;
        this.message = message;
    }

    public static PaymentResult success(String sessionId, String checkoutUrl) {
        return new PaymentResult(true, sessionId, checkoutUrl, "Stripe Checkout session created successfully.");
    }

    public static PaymentResult error(String message) {
        return new PaymentResult(false, null, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public String getMessage() {
        return message;
    }
}