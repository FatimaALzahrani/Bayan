package com.bank.bayan.bill;

public class Bill {
    private String id;
    private String customerName;
    private String serviceProvider;
    private String accountNumber;
    private double amount;
    private String dueDate;
    private String status;

    public Bill(){

    }

    public Bill(String billId, String customerName, String serviceProvider, String accountNumber, double amount, String dueDate, String status) {
        this.id = billId;
        this.customerName = customerName;
        this.serviceProvider = serviceProvider;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getAmount() {
        return amount;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setBillId(String billId) {
        this.id = billId;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void printBillDetails() {
        System.out.println("Bill ID: " + id);
        System.out.println("Customer Name: " + customerName);
        System.out.println("Service Provider: " + serviceProvider);
        System.out.println("Account Number: " + accountNumber);
        System.out.println("Amount: " + amount + " ريال");
        System.out.println("Due Date: " + dueDate);
        System.out.println("Status: " + status);
    }
}
