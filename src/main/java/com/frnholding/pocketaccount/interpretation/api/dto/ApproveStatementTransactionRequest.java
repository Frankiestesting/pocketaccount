package com.frnholding.pocketaccount.interpretation.api.dto;

import java.util.UUID;

public class ApproveStatementTransactionRequest {
    private UUID accountId;

    public ApproveStatementTransactionRequest() {
    }

    public ApproveStatementTransactionRequest(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
}
