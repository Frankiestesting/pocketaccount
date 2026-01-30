package com.frnholding.pocketaccount.accounting.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class ImportBankStatementRequest {
    
    @NotNull(message = "Account ID is required")
    private UUID accountId;
    
    private UUID sourceDocumentId;
    
    @NotEmpty(message = "At least one transaction line is required")
    @Valid
    private List<BankStatementLineDto> lines;
    
    public ImportBankStatementRequest() {
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
    
    public UUID getSourceDocumentId() {
        return sourceDocumentId;
    }
    
    public void setSourceDocumentId(UUID sourceDocumentId) {
        this.sourceDocumentId = sourceDocumentId;
    }
    
    public List<BankStatementLineDto> getLines() {
        return lines;
    }
    
    public void setLines(List<BankStatementLineDto> lines) {
        this.lines = lines;
    }
}
