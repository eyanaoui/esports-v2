package com.esports.models;

import java.time.LocalDateTime;

public class ProductForecast {

    private int id;
    private int productId;
    private String productName;
    private String category;
    private int currentStock;
    private double price;
    private int forecastDays;
    private double predictedQty;
    private int recommendedReorderQty;
    private LocalDateTime generatedAt;

    public ProductForecast() {
    }

    public ProductForecast(int id, int productId, String productName, String category,
                           int currentStock, double price, int forecastDays,
                           double predictedQty, int recommendedReorderQty,
                           LocalDateTime generatedAt) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.currentStock = currentStock;
        this.price = price;
        this.forecastDays = forecastDays;
        this.predictedQty = predictedQty;
        this.recommendedReorderQty = recommendedReorderQty;
        this.generatedAt = generatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }


    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }


    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }


    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }


    public int getForecastDays() {
        return forecastDays;
    }

    public void setForecastDays(int forecastDays) {
        this.forecastDays = forecastDays;
    }


    public double getPredictedQty() {
        return predictedQty;
    }

    public void setPredictedQty(double predictedQty) {
        this.predictedQty = predictedQty;
    }


    public int getRecommendedReorderQty() {
        return recommendedReorderQty;
    }

    public void setRecommendedReorderQty(int recommendedReorderQty) {
        this.recommendedReorderQty = recommendedReorderQty;
    }


    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getRiskLevel() {
        if (recommendedReorderQty > 0) {
            return "RESTOCK NEEDED";
        }

        if (predictedQty >= currentStock * 0.8) {
            return "WATCH STOCK";
        }

        return "SAFE";
    }

    public String getGeneratedAtText() {
        return generatedAt == null ? "" : generatedAt.toString().replace("T", " ");
    }
}