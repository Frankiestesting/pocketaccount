package com.frnholding.pocketaccount.accounting.mapper;

import com.frnholding.pocketaccount.accounting.api.dto.AccountResponse;
import com.frnholding.pocketaccount.accounting.api.dto.BankTransactionDTO;
import com.frnholding.pocketaccount.accounting.api.dto.BankTransactionResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ReceiptMatchResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ReceiptResponse;
import com.frnholding.pocketaccount.accounting.domain.Account;
import com.frnholding.pocketaccount.accounting.domain.BankTransaction;
import com.frnholding.pocketaccount.accounting.domain.Receipt;
import com.frnholding.pocketaccount.accounting.domain.ReceiptMatch;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class AccountingMapper {
    
    public AccountResponse toResponse(Account account) {
        if (account == null) {
            return null;
        }
        OffsetDateTime createdAt = account.getCreatedAt() != null 
            ? account.getCreatedAt().atOffset(ZoneOffset.UTC) 
            : null;
        return new AccountResponse(
            account.getId(),
            account.getName(),
            account.getCurrency(),
            account.getAccountNo(),
            createdAt
        );
    }
    

    
    public BankTransactionDTO toDTO(BankTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        BankTransactionDTO dto = new BankTransactionDTO();
        dto.setId(transaction.getId());
        dto.setAccountId(transaction.getAccount() != null ? transaction.getAccount().getId() : null);
        dto.setBookingDate(transaction.getBookingDate());
        dto.setValueDate(transaction.getValueDate());
        dto.setAmount(transaction.getAmount());
        dto.setCurrency(transaction.getCurrency());
        dto.setCounterparty(transaction.getCounterparty());
        dto.setDescription(transaction.getDescription());
        dto.setReference(transaction.getReference());
        dto.setSourceDocumentId(transaction.getSourceDocumentId());
        dto.setSourceLineHash(transaction.getSourceLineHash());
        dto.setCreatedAt(transaction.getCreatedAt());
        return dto;
    }
    
    public BankTransactionResponse toBankTransactionResponse(BankTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new BankTransactionResponse(
            transaction.getId(),
            transaction.getAccount() != null ? transaction.getAccount().getId() : null,
            transaction.getBookingDate(),
            transaction.getValueDate(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getCounterparty(),
            transaction.getDescription(),
            transaction.getReference(),
            transaction.getSourceDocumentId(),
            transaction.getSourceLineHash(),
            transaction.getCreatedAt()
        );
    }
    
    public ReceiptResponse toReceiptResponse(Receipt receipt) {
        if (receipt == null) {
            return null;
        }
        return new ReceiptResponse(
            receipt.getId(),
            receipt.getDocumentId(),
            receipt.getPurchaseDate(),
            receipt.getTotalAmount(),
            receipt.getCurrency(),
            receipt.getMerchant(),
            receipt.getDescription(),
            receipt.getCreatedAt()
        );
    }
    
    public ReceiptMatchResponse toReceiptMatchResponse(ReceiptMatch match) {
        if (match == null) {
            return null;
        }
        return new ReceiptMatchResponse(
            match.getId(),
            match.getReceipt() != null ? match.getReceipt().getId() : null,
            match.getBankTransaction() != null ? match.getBankTransaction().getId() : null,
            match.getMatchedAmount(),
            match.getMatchType(),
            match.getConfidence(),
            match.getCreatedAt()
        );
    }
}
