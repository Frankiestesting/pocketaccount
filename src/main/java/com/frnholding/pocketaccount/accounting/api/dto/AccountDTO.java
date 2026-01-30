package com.frnholding.pocketaccount.accounting.api.dto;

import java.time.Instant;
import java.util.UUID;

public class AccountDTO {
    
    private UUID id;
    private String name;
    private String currency;
    private Instant createdAt;
    
    public AccountDTO() {
    }
    
    public AccountDTO(UUID id, String name, String currency, Instant createdAt) {
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
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
