package com.frnholding.pocketaccount.accounting.controller;

import com.frnholding.pocketaccount.accounting.api.dto.AccountResponse;
import com.frnholding.pocketaccount.accounting.api.dto.BankTransactionDTO;
import com.frnholding.pocketaccount.accounting.api.dto.BankTransactionResponse;
import com.frnholding.pocketaccount.accounting.api.dto.CreateAccountRequest;
import com.frnholding.pocketaccount.accounting.api.dto.CreateReceiptMatchRequest;
import com.frnholding.pocketaccount.accounting.api.dto.CreateReceiptRequest;
import com.frnholding.pocketaccount.accounting.api.dto.ImportBankStatementRequest;
import com.frnholding.pocketaccount.accounting.api.dto.ImportBankStatementResponse;
import com.frnholding.pocketaccount.accounting.api.dto.MatchStatusResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ReceiptMatchResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ReceiptResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ReconciliationRowResponse;
import com.frnholding.pocketaccount.accounting.service.AccountingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AccountingController {
    
    private final AccountingService accountingService;
    
    @Autowired
    public AccountingController(AccountingService accountingService) {
        this.accountingService = accountingService;
    }
    
    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = accountingService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }
    
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> accounts = accountingService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }
    
    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable UUID id) {
        AccountResponse account = accountingService.getAccountById(id);
        return ResponseEntity.ok(account);
    }
    
    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<BankTransactionDTO>> getTransactionsByAccountId(@PathVariable UUID accountId) {
        List<BankTransactionDTO> transactions = accountingService.getTransactionsByAccountId(accountId);
        return ResponseEntity.ok(transactions);
    }
    
    @PostMapping("/bank-transactions/import")
    public ResponseEntity<ImportBankStatementResponse> importBankStatement(@Valid @RequestBody ImportBankStatementRequest request) {
        ImportBankStatementResponse response = accountingService.importBankStatement(request.getAccountId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/bank-transactions")
    public ResponseEntity<List<BankTransactionResponse>> getBankTransactions(
            @RequestParam UUID accountId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        List<BankTransactionResponse> transactions = accountingService.getTransactionsByDateRange(accountId, from, to);
        return ResponseEntity.ok(transactions);
    }
    
    @PostMapping("/receipts")
    public ResponseEntity<ReceiptResponse> createReceipt(@Valid @RequestBody CreateReceiptRequest request) {
        ReceiptResponse receipt = accountingService.createReceipt(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
    }
    
    @GetMapping("/receipts")
    public ResponseEntity<List<ReceiptResponse>> getReceipts(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        List<ReceiptResponse> receipts = accountingService.getReceiptsByDateRange(from, to);
        return ResponseEntity.ok(receipts);
    }
    
    @PostMapping("/matches")
    public ResponseEntity<ReceiptMatchResponse> createReceiptMatch(@Valid @RequestBody CreateReceiptMatchRequest request) {
        ReceiptMatchResponse match = accountingService.createReceiptMatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(match);
    }
    
    @DeleteMapping("/matches/{matchId}")
    public ResponseEntity<Void> deleteReceiptMatch(@PathVariable UUID matchId) {
        accountingService.deleteReceiptMatch(matchId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/bank-transactions/{id}/match-status")
    public ResponseEntity<MatchStatusResponse> getMatchStatus(@PathVariable UUID id) {
        MatchStatusResponse status = accountingService.getMatchStatus(id);
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/reconciliation")
    public ResponseEntity<List<ReconciliationRowResponse>> getReconciliation(
            @RequestParam UUID accountId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        List<ReconciliationRowResponse> reconciliation = accountingService.getReconciliationData(accountId, from, to);
        return ResponseEntity.ok(reconciliation);
    }
    
    @GetMapping("/reconciliation/export")
    public ResponseEntity<String> getReconciliationExport(
            @RequestParam UUID accountId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "csv") String format) {
        String csv = accountingService.getReconciliationCsv(accountId, from, to);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "reconciliation.csv");
        
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }
}
