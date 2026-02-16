package com.frnholding.pocketaccount.accounting.api.dto;

import java.util.UUID;

public class BankTransactionLinkResponse {
    private UUID bankTransactionId;
    private Long statementTransactionId;
    private UUID receiptId;

    public BankTransactionLinkResponse() {
    }

    public BankTransactionLinkResponse(UUID bankTransactionId, Long statementTransactionId, UUID receiptId) {
        this.bankTransactionId = bankTransactionId;
        this.statementTransactionId = statementTransactionId;
        this.receiptId = receiptId;
    }

    public UUID getBankTransactionId() {
        return bankTransactionId;
    }

    public void setBankTransactionId(UUID bankTransactionId) {
        this.bankTransactionId = bankTransactionId;
    }

    public Long getStatementTransactionId() {
        return statementTransactionId;
    }

    public void setStatementTransactionId(Long statementTransactionId) {
        this.statementTransactionId = statementTransactionId;
    }

    public UUID getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(UUID receiptId) {
        this.receiptId = receiptId;
    }
}
