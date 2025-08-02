package com.bank.bayan;

public class AuthorizedPerson {
    private String id;
    private String name;
    private String nationalId;
    private String phoneNumber;
    private String relationship;
    private boolean isActive;
    private long createdAt;
    private String permissions;

    public AuthorizedPerson() {
    }

    public AuthorizedPerson(String name, String nationalId, String phoneNumber, String relationship) {
        this.name = name;
        this.nationalId = nationalId;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
        this.permissions = "view_balance,view_transactions";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}

