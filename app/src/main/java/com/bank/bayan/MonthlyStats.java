package com.bank.bayan;

import java.util.HashMap;
import java.util.Map;

public class MonthlyStats {
    private String month;
    private double totalIncome;
    private double totalExpenses;
    private double netIncome;
    private int transactionCount;
    private Map<String, Double> categories;

    public MonthlyStats() {
        this.totalIncome = 0.0;
        this.totalExpenses = 0.0;
        this.netIncome = 0.0;
        this.transactionCount = 0;
        this.categories = new HashMap<>();
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public double getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(double totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public double getNetIncome() {
        return netIncome;
    }

    public void setNetIncome(double netIncome) {
        this.netIncome = netIncome;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Map<String, Double> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, Double> categories) {
        this.categories = categories;
    }
}

