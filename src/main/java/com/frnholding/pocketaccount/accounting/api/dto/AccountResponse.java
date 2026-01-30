package com.frnholding.pocketaccount.accounting.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AccountResponse {
    
    private UUID id;
    private String name;
    private String currency;
    private OffsetDateTime createdAt;
    
    public AccountResponse() {
    }
    
    public AccountResponse(UUID id, String name, String currency, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.currency = currency;
        this.createdAt = createdAt;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
