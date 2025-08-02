package com.bank.bayan.models;

public class Transaction {
    private String id;
    private String type;
    private double amount;
    private String category;
    private String date;
    private String description;
    private String icon;
    private String userId;

    public Transaction() {

    }

    public Transaction(String type, double amount, String category, String description, String icon, String userId) {
        this.id = generateTransactionId();
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.icon = icon;
        this.userId = userId;
        this.date = getCurrentDateString();
    }

    private String generateTransactionId() {
        return "txn" + System.currentTimeMillis();
    }

    private String getCurrentDateString() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMMM yyyy hh:mm a", new java.util.Locale("ar"));
        return sdf.format(new java.util.Date());
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}