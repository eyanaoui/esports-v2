package com.esports.utils;

public class ProductValidator {

    public static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Product name is required.";
        }
        if (!name.matches("[a-zA-Z0-9\\s]+")) {
            return "Only letters, numbers and spaces are allowed.";
        }
        return "";
    }

    public static String validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "Category is required.";
        }
        return "";
    }

    public static String validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "Description is required.";
        }
        return "";
    }

    public static String validateImage(String image) {
        if (image == null || image.trim().isEmpty()) {
            return "Image is required.";
        }
        return "";
    }

    public static String validatePrice(String priceText) {
        if (priceText == null || priceText.trim().isEmpty()) {
            return "Price is required.";
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                return "Price must be greater than 0.";
            }
        } catch (NumberFormatException e) {
            return "Price must be a valid number.";
        }

        return "";
    }

    public static String validateStock(String stockText) {
        if (stockText == null || stockText.trim().isEmpty()) {
            return "Stock is required.";
        }

        try {
            int stock = Integer.parseInt(stockText);
            if (stock < 0) {
                return "Stock cannot be negative.";
            }
        } catch (NumberFormatException e) {
            return "Stock must be an integer.";
        }

        return "";
    }
}