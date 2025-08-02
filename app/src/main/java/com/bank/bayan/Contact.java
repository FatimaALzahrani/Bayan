package com.bank.bayan;


public class Contact {
    private String name;
    private String accountNumber;
    private double balance;
    private boolean favorite;

    public Contact() {
    }

    public Contact(String name, String accountNumber, double balance, boolean favorite) {
        this.name = name;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.favorite = favorite;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
}
