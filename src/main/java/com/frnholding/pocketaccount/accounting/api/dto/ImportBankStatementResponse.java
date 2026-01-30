package com.frnholding.pocketaccount.accounting.api.dto;

import java.util.List;
import java.util.UUID;

public class ImportBankStatementResponse {
    private UUID accountId;
    private int inserted;
    private int skipped;
    private List<String> skippedHashes;
    
    public ImportBankStatementResponse() {
    }
    
    public ImportBankStatementResponse(UUID accountId, int inserted, int skipped, List<String> skippedHashes) {
        this.accountId = accountId;
        this.inserted = inserted;
        this.skipped = skipped;
        this.skippedHashes = skippedHashes;
    }
    
    public UUID getAccountId() {
        return accountId;
    }
    
    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
    
    public int getInserted() {
        return inserted;
    }
    
    public void setInserted(int inserted) {
        this.inserted = inserted;
    }
    
    public int getSkipped() {
        return skipped;
    }
    
    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }
    
    public List<String> getSkippedHashes() {
        return skippedHashes;
    }
    
    public void setSkippedHashes(List<String> skippedHashes) {
        this.skippedHashes = skippedHashes;
    }
}
