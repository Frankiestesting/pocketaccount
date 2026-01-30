package com.frnholding.pocketaccount.accounting.service;

import com.frnholding.pocketaccount.accounting.api.dto.AccountDTO;
import com.frnholding.pocketaccount.accounting.api.dto.BankTransactionDTO;
import com.frnholding.pocketaccount.accounting.api.dto.CreateAccountRequestDTO;
import com.frnholding.pocketaccount.accounting.domain.Account;
import com.frnholding.pocketaccount.accounting.domain.BankTransaction;
import com.frnholding.pocketaccount.accounting.mapper.AccountingMapper;
import com.frnholding.pocketaccount.accounting.repository.AccountRepository;
import com.frnholding.pocketaccount.accounting.repository.BankTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountingService {
    
    private final AccountRepository accountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final AccountingMapper mapper;
    
    @Autowired
    public AccountingService(AccountRepository accountRepository,
                           BankTransactionRepository bankTransactionRepository,
                           AccountingMapper mapper) {
        this.accountRepository = accountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.mapper = mapper;
    }
    
    @Transactional
    public AccountDTO createAccount(CreateAccountRequestDTO request) {
        Account account = new Account();
        account.setName(request.getName());
        account.setCurrency(request.getCurrency());
        account.setCreatedAt(Instant.now());
        
        Account saved = accountRepository.save(account);
        return mapper.toDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        return mapper.toDTO(account);
    }
    
    @Transactional(readOnly = true)
    public List<BankTransactionDTO> getTransactionsByAccountId(UUID accountId) {
        return bankTransactionRepository.findByAccountId(accountId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
}
