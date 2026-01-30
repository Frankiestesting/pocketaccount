package com.frnholding.pocketaccount.accounting.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateAccountRequest {
    
    @NotBlank(message = "Account name is required")
    @Size(max = 200, message = "Account name must not exceed 200 characters")
    private String name;
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO code (e.g., USD, EUR, NOK)")
    private String currency;
    
    public CreateAccountRequest() {
    }
    
    public CreateAccountRequest(String name, String currency) {
        this.name = name;
        this.currency = currency;
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
}
