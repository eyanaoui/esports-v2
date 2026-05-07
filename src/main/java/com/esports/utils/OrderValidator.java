package com.esports.utils;

public class OrderValidator {

    public static String validateFirstName(String firstName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            return "First name is required.";
        }
        if (!firstName.matches("[a-zA-Z\\s]+")) {
            return "First name must contain only letters.";
        }
        return "";
    }

    public static String validateLastName(String lastName) {
        if (lastName == null || lastName.trim().isEmpty()) {
            return "Last name is required.";
        }
        if (!lastName.matches("[a-zA-Z\\s]+")) {
            return "Last name must contain only letters.";
        }
        return "";
    }

    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required.";
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return "Invalid email format.";
        }
        return "";
    }

    public static String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Phone is required.";
        }
        if (!phone.matches("\\d{8,15}")) {
            return "Phone must contain 8 to 15 digits.";
        }
        return "";
    }

    public static String validatePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            return "Payment method is required.";
        }
        return "";
    }
}