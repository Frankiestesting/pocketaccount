package com.frnholding.pocketaccount.accounting.mapper;

import com.frnholding.pocketaccount.accounting.api.dto.AccountDTO;
import com.frnholding.pocketaccount.accounting.api.dto.BankTransactionDTO;
import com.frnholding.pocketaccount.accounting.domain.Account;
import com.frnholding.pocketaccount.accounting.domain.BankTransaction;
import org.springframework.stereotype.Component;

@Component
public class AccountingMapper {
    
    public AccountDTO toDTO(Account account) {
        if (account == null) {
            return null;
        }
        return new AccountDTO(
            account.getId(),
            account.getName(),
            account.getCurrency(),
            account.getCreatedAt()
        );
    }
    
    public Account toEntity(AccountDTO dto) {
        if (dto == null) {
            return null;
        }
        Account account = new Account();
        account.setId(dto.getId());
        account.setName(dto.getName());
        account.setCurrency(dto.getCurrency());
        account.setCreatedAt(dto.getCreatedAt());
        return account;
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
}
