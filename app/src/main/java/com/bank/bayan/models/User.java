package com.bank.bayan.models;

public class User {
    private String userId;
    private String name;
    private long created_at;
    private String voiceprint_data;
    private Account account;

    public User() {

    }

    public User(String userId, String name, long created_at, String voiceprint_data) {
        this.userId = userId;
        this.name = name;
        this.created_at = created_at;
        this.voiceprint_data = voiceprint_data;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreated_at() {
        return created_at;
    }

    public void setCreated_at(long created_at) {
        this.created_at = created_at;
    }

    // Alternative method name for compatibility
    public long getCreatedAt() {
        return created_at;
    }

    public void setCreatedAt(long createdAt) {
        this.created_at = createdAt;
    }

    public String getVoiceprint_data() {
        return voiceprint_data;
    }

    public void setVoiceprint_data(String voiceprint_data) {
        this.voiceprint_data = voiceprint_data;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}