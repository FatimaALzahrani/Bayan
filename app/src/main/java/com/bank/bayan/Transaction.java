package com.bank.bayan;

public class Transaction {
    private String id;
    private String userId;
    private double amount;
    private String type;
    private String category;
    private String description;
    private String date;
    private String icon;

    public Transaction() {}

    public Transaction(String userId, double amount, String type, String category,
                       String description, String date, String icon) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
        this.date = date;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String dateString) {
        this.date = dateString;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isIncome() {
        return "income".equals(type);
    }

    public boolean isExpense() {
        return "expense".equals(type);
    }

    public String getFormattedAmount() {
        return String.format("﷼ %.2f", amount);
    }

    public String getDisplayAmount() {
        if (isIncome()) {
            return "+" + getFormattedAmount();
        } else {
            return "-" + getFormattedAmount();
        }
    }
}