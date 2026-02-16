package com.frnholding.pocketaccount.interpretation.api.dto;

import java.util.UUID;

public class ApproveStatementTransactionResponse {
    private Long statementTransactionId;
    private UUID bankTransactionId;
    private boolean approved;

    public ApproveStatementTransactionResponse() {
    }

    public ApproveStatementTransactionResponse(Long statementTransactionId, UUID bankTransactionId, boolean approved) {
        this.statementTransactionId = statementTransactionId;
        this.bankTransactionId = bankTransactionId;
        this.approved = approved;
    }

    public Long getStatementTransactionId() {
        return statementTransactionId;
    }

    public void setStatementTransactionId(Long statementTransactionId) {
        this.statementTransactionId = statementTransactionId;
    }

    public UUID getBankTransactionId() {
        return bankTransactionId;
    }

    public void setBankTransactionId(UUID bankTransactionId) {
        this.bankTransactionId = bankTransactionId;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
}
