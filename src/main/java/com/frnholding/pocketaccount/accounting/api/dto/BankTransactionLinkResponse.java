package com.frnholding.pocketaccount.accounting.api.dto;

import java.util.UUID;

public class BankTransactionLinkResponse {
    private UUID bankTransactionId;
    private UUID statementTransactionId;
    private UUID receiptId;

    public BankTransactionLinkResponse() {
    }

    public BankTransactionLinkResponse(UUID bankTransactionId, UUID statementTransactionId, UUID receiptId) {
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

    public UUID getStatementTransactionId() {
        return statementTransactionId;
    }

    public void setStatementTransactionId(UUID statementTransactionId) {
        this.statementTransactionId = statementTransactionId;
    }

    public UUID getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(UUID receiptId) {
        this.receiptId = receiptId;
    }
}
