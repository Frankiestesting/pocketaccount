package com.frnholding.pocketaccount.accounting.controller;

import com.frnholding.pocketaccount.accounting.api.dto.AccountResponse;
import com.frnholding.pocketaccount.accounting.api.dto.BankTransactionDTO;
import com.frnholding.pocketaccount.accounting.api.dto.BankTransactionLinkResponse;
import com.frnholding.pocketaccount.accounting.api.dto.BankTransactionResponse;
import com.frnholding.pocketaccount.accounting.api.dto.CreateAccountRequest;
import com.frnholding.pocketaccount.accounting.api.dto.CreateReceiptMatchRequest;
import com.frnholding.pocketaccount.accounting.api.dto.CreateReceiptRequest;
import com.frnholding.pocketaccount.accounting.api.dto.ImportBankStatementRequest;
import com.frnholding.pocketaccount.accounting.api.dto.ImportBankStatementResponse;
import com.frnholding.pocketaccount.accounting.api.dto.MatchStatusResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ReceiptMatchCandidateResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ReceiptMatchResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ReceiptResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ReconciliationRowResponse;
import com.frnholding.pocketaccount.accounting.service.AccountingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AccountingController {
    
    private final AccountingService accountingService;
    
    public AccountingController(AccountingService accountingService) {
        this.accountingService = accountingService;
    }
    
    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = accountingService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }
    
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<AccountResponse> accounts = accountingService.getAccounts(page, size);
        return ResponseEntity.ok(accounts);
    }
    
    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable UUID id) {
        AccountResponse account = accountingService.getAccountById(id);
        return ResponseEntity.ok(account);
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID id) {
        accountingService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<BankTransactionDTO>> getTransactionsByAccountId(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<BankTransactionDTO> transactions = accountingService.getTransactionsByAccountId(accountId, page, size);
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
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<BankTransactionResponse> transactions = accountingService.getTransactionsByDateRange(accountId, from, to, page, size);
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
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<ReceiptResponse> receipts = accountingService.getReceiptsByDateRange(from, to, page, size);
        return ResponseEntity.ok(receipts);
    }

    @DeleteMapping("/receipts/{receiptId}")
    public ResponseEntity<Void> deleteReceipt(@PathVariable UUID receiptId) {
        accountingService.deleteReceipt(receiptId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/receipts/{receiptId}")
    public ResponseEntity<ReceiptResponse> getReceiptById(@PathVariable UUID receiptId) {
        ReceiptResponse receipt = accountingService.getReceiptById(receiptId);
        return ResponseEntity.ok(receipt);
    }

    @PostMapping("/receipts/{receiptId}/reject")
    public ResponseEntity<ReceiptResponse> rejectReceipt(@PathVariable UUID receiptId) {
        ReceiptResponse receipt = accountingService.rejectReceipt(receiptId);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/receipts/{receiptId}/matches")
    public ResponseEntity<List<ReceiptMatchResponse>> getReceiptMatches(@PathVariable UUID receiptId) {
        List<ReceiptMatchResponse> matches = accountingService.getReceiptMatches(receiptId);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/receipts/{receiptId}/match-candidates")
    public ResponseEntity<List<ReceiptMatchCandidateResponse>> getReceiptMatchCandidates(
            @PathVariable UUID receiptId) {
        List<ReceiptMatchCandidateResponse> candidates = accountingService.getReceiptMatchCandidates(receiptId);
        return ResponseEntity.ok(candidates);
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

    @GetMapping("/bank-transactions/{id}/links")
    public ResponseEntity<BankTransactionLinkResponse> getBankTransactionLinks(@PathVariable UUID id) {
        BankTransactionLinkResponse links = accountingService.getBankTransactionLinks(id);
        return ResponseEntity.ok(links);
    }
    
    @GetMapping("/reconciliation")
    public ResponseEntity<List<ReconciliationRowResponse>> getReconciliation(
            @RequestParam UUID accountId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        List<ReconciliationRowResponse> reconciliation = accountingService.getReconciliationData(accountId, from, to, page, size);
        return ResponseEntity.ok(reconciliation);
    }
    
    @GetMapping("/reconciliation/export")
    public ResponseEntity<byte[]> getReconciliationExport(
            @RequestParam UUID accountId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "csv") String format) {
        String normalizedFormat = format == null ? "csv" : format.trim().toLowerCase();

        HttpHeaders headers = new HttpHeaders();
        if ("csv".equals(normalizedFormat)) {
            String csv = accountingService.getReconciliationCsv(accountId, from, to);
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "reconciliation.csv");
            return new ResponseEntity<>(csv.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
        }

        if ("xlsx".equals(normalizedFormat) || "excel".equals(normalizedFormat)) {
            byte[] excel = accountingService.getReconciliationExcel(accountId, from, to);
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "reconciliation.xlsx");
            return new ResponseEntity<>(excel, headers, HttpStatus.OK);
        }

        throw new IllegalArgumentException("Unsupported format: " + format);
    }
}
