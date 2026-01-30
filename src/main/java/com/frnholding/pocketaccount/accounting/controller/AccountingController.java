package com.frnholding.pocketaccount.accounting.controller;

import com.frnholding.pocketaccount.accounting.api.dto.AccountDTO;
import com.frnholding.pocketaccount.accounting.api.dto.BankTransactionDTO;
import com.frnholding.pocketaccount.accounting.api.dto.CreateAccountRequestDTO;
import com.frnholding.pocketaccount.accounting.service.AccountingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting")
@CrossOrigin(origins = "*")
public class AccountingController {
    
    private final AccountingService accountingService;
    
    @Autowired
    public AccountingController(AccountingService accountingService) {
        this.accountingService = accountingService;
    }
    
    @PostMapping("/accounts")
    public ResponseEntity<AccountDTO> createAccount(@RequestBody CreateAccountRequestDTO request) {
        AccountDTO account = accountingService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }
    
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDTO>> getAllAccounts() {
        List<AccountDTO> accounts = accountingService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }
    
    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable UUID id) {
        AccountDTO account = accountingService.getAccountById(id);
        return ResponseEntity.ok(account);
    }
    
    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<BankTransactionDTO>> getTransactionsByAccountId(@PathVariable UUID accountId) {
        List<BankTransactionDTO> transactions = accountingService.getTransactionsByAccountId(accountId);
        return ResponseEntity.ok(transactions);
    }
}
