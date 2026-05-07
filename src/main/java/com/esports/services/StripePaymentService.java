package com.esports.services;

import com.esports.models.CartItem;
import com.esports.models.Product;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.util.List;

public class StripePaymentService {

    /*
     * DEMO ONLY.
     * Do NOT put a real key here before pushing to GitHub.
     */
    private static final String LOCAL_STRIPE_SECRET_KEY = "";

    private static final String STRIPE_CURRENCY = "eur";
    private static final double TND_TO_EUR_RATE = 0.30;

    public StripePaymentService() {
        String secretKey = readConfig("STRIPE_SECRET_KEY");

        if (secretKey == null || secretKey.trim().isEmpty()) {
            secretKey = LOCAL_STRIPE_SECRET_KEY;
        }

        Stripe.apiKey = secretKey;
    }

    public boolean isConfigured() {
        return Stripe.apiKey != null
                && !Stripe.apiKey.trim().isEmpty()
                && !Stripe.apiKey.equals("PUT_YOUR_STRIPE_SECRET_KEY_HERE");
    }

    public PaymentResult createCheckoutSession(List<CartItem> cartItems, String customerEmail, String paymentMethod) {
        if (!isConfigured()) {
            return PaymentResult.error("Online payment is not configured. Add your Stripe secret key.");
        }

        if (cartItems == null || cartItems.isEmpty()) {
            return PaymentResult.error("Cart is empty.");
        }

        boolean includePaypal = "PayPal".equalsIgnoreCase(paymentMethod);

        try {
            Session session = createSession(cartItems, customerEmail, includePaypal);
            return PaymentResult.success(session.getId(), session.getUrl());

        } catch (StripeException paypalError) {
            System.out.println("⚠ Online payment with selected method failed.");
            System.out.println("⚠ Error: " + paypalError.getMessage());

            try {
                Session session = createSession(cartItems, customerEmail, false);
                return PaymentResult.success(session.getId(), session.getUrl());

            } catch (StripeException cardError) {
                return PaymentResult.error("Online payment error: " + cardError.getMessage());
            }

        } catch (Exception e) {
            return PaymentResult.error("Online payment error: " + e.getMessage());
        }
    }

    public boolean isPaymentPaid(String sessionId) {
        if (!isConfigured()) {
            System.out.println("❌ Online payment is not configured.");
            return false;
        }

        if (sessionId == null || sessionId.trim().isEmpty()) {
            System.out.println("❌ Payment session id is empty.");
            return false;
        }

        try {
            Session session = Session.retrieve(sessionId);

            System.out.println("========== ONLINE PAYMENT DEBUG ==========");
            System.out.println("SESSION ID = " + session.getId());
            System.out.println("PAYMENT STATUS = " + session.getPaymentStatus());
            System.out.println("STATUS = " + session.getStatus());
            System.out.println("==========================================");

            return "paid".equalsIgnoreCase(session.getPaymentStatus());

        } catch (Exception e) {
            System.out.println("❌ Error while checking online payment: " + e.getMessage());
            return false;
        }
    }

    private Session createSession(List<CartItem> cartItems, String customerEmail, boolean includePaypal) throws StripeException {
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://example.com/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("https://example.com/cancel");

        if (customerEmail != null && !customerEmail.trim().isEmpty()) {
            builder.setCustomerEmail(customerEmail.trim());
        }

        builder.addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD);

        if (includePaypal) {
            builder.addPaymentMethodType(SessionCreateParams.PaymentMethodType.PAYPAL);
        }

        for (CartItem item : cartItems) {
            if (item == null || item.getProduct() == null || item.getQuantity() <= 0) {
                continue;
            }

            Product product = item.getProduct();

            double priceInTnd = product.getPrice();
            double priceInEur = priceInTnd * TND_TO_EUR_RATE;

            long unitAmount = toStripeAmount(priceInEur);

            if (unitAmount <= 0) {
                unitAmount = 1;
            }

            String description = safe(product.getCategory())
                    + " | Original price: "
                    + String.format("%.2f TND", priceInTnd)
                    + " | Charged approx: "
                    + String.format("%.2f EUR", priceInEur);

            SessionCreateParams.LineItem.PriceData.ProductData productData =
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName(safe(product.getName()))
                            .setDescription(description)
                            .build();

            SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(STRIPE_CURRENCY)
                            .setUnitAmount(unitAmount)
                            .setProductData(productData)
                            .build();

            SessionCreateParams.LineItem lineItem =
                    SessionCreateParams.LineItem.builder()
                            .setQuantity((long) item.getQuantity())
                            .setPriceData(priceData)
                            .build();

            builder.addLineItem(lineItem);
        }

        return Session.create(builder.build());
    }

    private long toStripeAmount(double amount) {
        return Math.round(amount * 100);
    }

    private String readConfig(String key) {
        String value = System.getenv(key);

        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }

        return value;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Product" : value.trim();
    }
}