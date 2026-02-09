package com.frnholding.pocketaccount.accounting.service;

import com.frnholding.pocketaccount.accounting.api.dto.AccountResponse;
import com.frnholding.pocketaccount.accounting.api.dto.BankStatementLineDto;
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
import com.frnholding.pocketaccount.accounting.domain.Account;
import com.frnholding.pocketaccount.accounting.domain.BankTransaction;
import com.frnholding.pocketaccount.accounting.domain.Receipt;
import com.frnholding.pocketaccount.accounting.domain.ReceiptMatch;
import com.frnholding.pocketaccount.accounting.mapper.AccountingMapper;
import com.frnholding.pocketaccount.accounting.repository.AccountRepository;
import com.frnholding.pocketaccount.accounting.repository.BankTransactionRepository;
import com.frnholding.pocketaccount.accounting.repository.ReceiptMatchRepository;
import com.frnholding.pocketaccount.accounting.repository.ReceiptRepository;
import com.frnholding.pocketaccount.exception.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountingService {
    
    private final AccountRepository accountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptMatchRepository receiptMatchRepository;
    private final AccountingMapper mapper;
    
    @Autowired
    public AccountingService(AccountRepository accountRepository,
                           BankTransactionRepository bankTransactionRepository,
                           ReceiptRepository receiptRepository,
                           ReceiptMatchRepository receiptMatchRepository,
                           AccountingMapper mapper) {
        this.accountRepository = accountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.receiptRepository = receiptRepository;
        this.receiptMatchRepository = receiptMatchRepository;
        this.mapper = mapper;
    }
    
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account();
        account.setName(request.getName());
        account.setCurrency(request.getCurrency());
        account.setCreatedAt(Instant.now());
        
        Account saved = accountRepository.save(account);
        return mapper.toResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        return accountRepository.findAll(sort).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        return accountRepository.findAll(PageRequest.of(page, size, sort)).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
        return mapper.toResponse(account);
    }
    
    @Transactional(readOnly = true)
    public List<BankTransactionDTO> getTransactionsByAccountId(UUID accountId) {
        return bankTransactionRepository.findByAccountId(accountId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BankTransactionDTO> getTransactionsByAccountId(UUID accountId, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "bookingDate");
        return bankTransactionRepository.findByAccountId(accountId, PageRequest.of(page, size, sort)).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ImportBankStatementResponse importBankStatement(UUID accountId, ImportBankStatementRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));
        
        int inserted = 0;
        int skipped = 0;
        List<String> skippedHashes = new ArrayList<>();
        
        for (BankStatementLineDto line : request.getLines()) {
            if (bankTransactionRepository.existsByAccountIdAndSourceLineHash(accountId, line.getSourceLineHash())) {
                skipped++;
                skippedHashes.add(line.getSourceLineHash());
            } else {
                BankTransaction transaction = new BankTransaction();
                transaction.setAccount(account);
                transaction.setBookingDate(line.getBookingDate());
                transaction.setValueDate(line.getValueDate());
                transaction.setAmount(line.getAmount());
                transaction.setCurrency(line.getCurrency());
                transaction.setCounterparty(line.getCounterparty());
                transaction.setDescription(line.getDescription());
                transaction.setReference(line.getReference());
                transaction.setSourceDocumentId(request.getSourceDocumentId());
                transaction.setSourceLineHash(line.getSourceLineHash());
                transaction.setCreatedAt(Instant.now());
                
                bankTransactionRepository.save(transaction);
                inserted++;
            }
        }
        
        return new ImportBankStatementResponse(accountId, inserted, skipped, skippedHashes);
    }
    
    @Transactional(readOnly = true)
    public List<BankTransactionResponse> getTransactionsByDateRange(UUID accountId, LocalDate from, LocalDate to) {
        // Verify account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        return bankTransactionRepository.findByAccountIdAndDateRange(accountId, from, to).stream()
                .map(mapper::toBankTransactionResponse)
                .collect(Collectors.toList());
    }

            @Transactional(readOnly = true)
            public List<BankTransactionResponse> getTransactionsByDateRange(UUID accountId, LocalDate from, LocalDate to, int page, int size) {
            // Verify account exists
            accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

            Sort sort = Sort.by(Sort.Direction.DESC, "bookingDate");
            return bankTransactionRepository.findByAccountIdAndDateRange(accountId, from, to, PageRequest.of(page, size, sort)).stream()
                .map(mapper::toBankTransactionResponse)
                .collect(Collectors.toList());
            }
    
    @Transactional
    public ReceiptResponse createReceipt(CreateReceiptRequest request) {
        Receipt receipt = new Receipt();
        receipt.setDocumentId(request.getDocumentId());
        receipt.setPurchaseDate(request.getPurchaseDate());
        receipt.setTotalAmount(request.getTotalAmount());
        receipt.setCurrency(request.getCurrency());
        receipt.setMerchant(request.getMerchant());
        receipt.setDescription(request.getDescription());
        receipt.setCreatedAt(Instant.now());
        
        Receipt saved = receiptRepository.save(receipt);
        return mapper.toReceiptResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsByDateRange(LocalDate from, LocalDate to) {
        return receiptRepository.findByDateRange(from, to).stream()
                .map(mapper::toReceiptResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsByDateRange(LocalDate from, LocalDate to, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        return receiptRepository.findByDateRange(from, to, PageRequest.of(page, size, sort)).stream()
                .map(mapper::toReceiptResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ReceiptMatchResponse createReceiptMatch(CreateReceiptMatchRequest request) {
        // Check if match already exists (unique constraint: receiptId + bankTransactionId)
        if (receiptMatchRepository.existsByReceiptIdAndBankTransactionId(request.getReceiptId(), request.getBankTransactionId())) {
            throw new IllegalArgumentException("Receipt match already exists for this receipt and transaction");
        }
        
        // Verify receipt exists
        Receipt receipt = receiptRepository.findById(request.getReceiptId())
                .orElseThrow(() -> new EntityNotFoundException("Receipt not found: " + request.getReceiptId()));
        
        // Verify bank transaction exists
        BankTransaction bankTransaction = bankTransactionRepository.findById(request.getBankTransactionId())
                .orElseThrow(() -> new EntityNotFoundException("Bank transaction not found: " + request.getBankTransactionId()));
        
        // Create match
        ReceiptMatch match = new ReceiptMatch();
        match.setReceipt(receipt);
        match.setBankTransaction(bankTransaction);
        match.setMatchedAmount(request.getMatchedAmount());
        match.setMatchType(request.getMatchType());
        match.setConfidence(request.getConfidence());
        match.setCreatedAt(Instant.now());
        
        ReceiptMatch saved = receiptMatchRepository.save(match);
        return mapper.toReceiptMatchResponse(saved);
    }
    
    @Transactional
    public void deleteReceiptMatch(UUID matchId) {
        if (!receiptMatchRepository.existsById(matchId)) {
            throw new EntityNotFoundException("Receipt match not found: " + matchId);
        }
        receiptMatchRepository.deleteById(matchId);
    }
    
    @Transactional(readOnly = true)
    public MatchStatusResponse getMatchStatus(UUID bankTransactionId) {
        BankTransaction transaction = bankTransactionRepository.findById(bankTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction not found: " + bankTransactionId));
        
        BigDecimal transactionAmountAbs = transaction.getAmount().abs();
        BigDecimal sumMatched = receiptMatchRepository.sumMatchedAmountByBankTransactionId(bankTransactionId);
        
        String status;
        if (sumMatched.compareTo(BigDecimal.ZERO) == 0) {
            status = "UNMATCHED";
        } else if (sumMatched.compareTo(transactionAmountAbs) < 0) {
            status = "PARTIAL";
        } else if (sumMatched.compareTo(transactionAmountAbs) == 0) {
            status = "MATCHED";
        } else {
            status = "OVER";
        }
        
        return new MatchStatusResponse(transactionAmountAbs, sumMatched, status);
    }
    
    @Transactional(readOnly = true)
    public List<ReconciliationRowResponse> getReconciliationData(UUID accountId, LocalDate from, LocalDate to) {
        // Verify account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        // Get all transactions for the account within date range
        List<BankTransaction> transactions = bankTransactionRepository.findByAccountIdAndDateRange(accountId, from, to);
        
        // Enrich each transaction with match status
        return transactions.stream()
                .map(transaction -> {
                    BigDecimal sumMatched = receiptMatchRepository.sumMatchedAmountByBankTransactionId(transaction.getId());
                    BigDecimal transactionAmountAbs = transaction.getAmount().abs();
                    
                    String status;
                    if (sumMatched.compareTo(BigDecimal.ZERO) == 0) {
                        status = "UNMATCHED";
                    } else if (sumMatched.compareTo(transactionAmountAbs) < 0) {
                        status = "PARTIAL";
                    } else if (sumMatched.compareTo(transactionAmountAbs) == 0) {
                        status = "MATCHED";
                    } else {
                        status = "OVER";
                    }
                    
                    return new ReconciliationRowResponse(
                            transaction.getId(),
                            transaction.getBookingDate(),
                            transaction.getAmount(),
                            transaction.getDescription(),
                            sumMatched,
                            status
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRowResponse> getReconciliationData(UUID accountId, LocalDate from, LocalDate to, int page, int size) {
        // Verify account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        Sort sort = Sort.by(Sort.Direction.DESC, "bookingDate");
        List<BankTransaction> transactions = bankTransactionRepository.findByAccountIdAndDateRange(
                accountId,
                from,
                to,
                PageRequest.of(page, size, sort)
        );

        return transactions.stream()
                .map(transaction -> {
                    BigDecimal sumMatched = receiptMatchRepository.sumMatchedAmountByBankTransactionId(transaction.getId());
                    BigDecimal transactionAmountAbs = transaction.getAmount().abs();

                    String status;
                    if (sumMatched.compareTo(BigDecimal.ZERO) == 0) {
                        status = "UNMATCHED";
                    } else if (sumMatched.compareTo(transactionAmountAbs) < 0) {
                        status = "PARTIAL";
                    } else if (sumMatched.compareTo(transactionAmountAbs) == 0) {
                        status = "MATCHED";
                    } else {
                        status = "OVER";
                    }

                    return new ReconciliationRowResponse(
                            transaction.getId(),
                            transaction.getBookingDate(),
                            transaction.getAmount(),
                            transaction.getDescription(),
                            sumMatched,
                            status
                    );
                })
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public String getReconciliationCsv(UUID accountId, LocalDate from, LocalDate to) {
        List<ReconciliationRowResponse> rows = getReconciliationData(accountId, from, to);
        
        StringBuilder csv = new StringBuilder();
        
        // Header row
        csv.append("transactionId,bookingDate,amount,description,sumMatched,status\n");
        
        // Data rows
        for (ReconciliationRowResponse row : rows) {
            csv.append(escapeCsvField(row.getTransactionId().toString())).append(",");
            csv.append(escapeCsvField(row.getBookingDate().toString())).append(",");
            csv.append(escapeCsvField(row.getAmount().toString())).append(",");
            csv.append(escapeCsvField(row.getDescription())).append(",");
            csv.append(escapeCsvField(row.getSumMatched().toString())).append(",");
            csv.append(escapeCsvField(row.getStatus())).append("\n");
        }
        
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public byte[] getReconciliationExcel(UUID accountId, LocalDate from, LocalDate to) {
        List<ReconciliationRowResponse> rows = getReconciliationData(accountId, from, to);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("reconciliation");
            int rowIndex = 0;

            Row header = sheet.createRow(rowIndex++);
            header.createCell(0).setCellValue("transactionId");
            header.createCell(1).setCellValue("bookingDate");
            header.createCell(2).setCellValue("amount");
            header.createCell(3).setCellValue("description");
            header.createCell(4).setCellValue("sumMatched");
            header.createCell(5).setCellValue("status");

            for (ReconciliationRowResponse row : rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                excelRow.createCell(0).setCellValue(row.getTransactionId().toString());
                excelRow.createCell(1).setCellValue(row.getBookingDate().toString());
                excelRow.createCell(2).setCellValue(row.getAmount().toPlainString());
                excelRow.createCell(3).setCellValue(row.getDescription());
                excelRow.createCell(4).setCellValue(row.getSumMatched().toPlainString());
                excelRow.createCell(5).setCellValue(row.getStatus());
            }

            for (int i = 0; i <= 5; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate reconciliation Excel export", e);
        }
    }
    
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if field contains comma, quote, or newline
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
