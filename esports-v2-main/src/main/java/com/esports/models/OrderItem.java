package com.esports.models;

public class OrderItem {

    private int id;
    private int orderRefId;
    private int productId;
    private String productName;
    private int quantity;
    private double unitPrice;

    public OrderItem() {
    }

    public OrderItem(int orderRefId, int productId, int quantity, double unitPrice) {
        this.orderRefId = orderRefId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public OrderItem(int id, int orderRefId, int productId, String productName, int quantity, double unitPrice) {
        this.id = id;
        this.orderRefId = orderRefId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public int getOrderRefId() {
        return orderRefId;
    }

    public void setOrderRefId(int orderRefId) {
        this.orderRefId = orderRefId;
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


    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }


    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }


    public double getSubtotal() {
        return quantity * unitPrice;
    }
}