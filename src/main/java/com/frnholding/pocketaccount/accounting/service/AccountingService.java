package com.frnholding.pocketaccount.accounting.service;

import com.frnholding.pocketaccount.accounting.api.dto.AccountResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ApproveReceiptWaiverRequest;
import com.frnholding.pocketaccount.accounting.api.dto.BankStatementLineDto;
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
import com.frnholding.pocketaccount.accounting.api.dto.ReceiptWaiverReasonResponse;
import com.frnholding.pocketaccount.accounting.api.dto.ReconciliationRowResponse;
import com.frnholding.pocketaccount.accounting.domain.Account;
import com.frnholding.pocketaccount.accounting.domain.BankTransaction;
import com.frnholding.pocketaccount.accounting.domain.Receipt;
import com.frnholding.pocketaccount.accounting.domain.ReceiptMatch;
import com.frnholding.pocketaccount.accounting.domain.ReceiptMatchStatus;
import com.frnholding.pocketaccount.accounting.mapper.AccountingMapper;
import com.frnholding.pocketaccount.accounting.repository.AccountRepository;
import com.frnholding.pocketaccount.accounting.repository.BankTransactionRepository;
import com.frnholding.pocketaccount.accounting.repository.ReceiptWaiverReasonRepository;
import com.frnholding.pocketaccount.accounting.repository.ReceiptMatchRepository;
import com.frnholding.pocketaccount.accounting.repository.ReceiptRepository;
import com.frnholding.pocketaccount.interpretation.repository.StatementTransactionRepository;
import com.frnholding.pocketaccount.exception.EntityNotFoundException;
import com.frnholding.pocketaccount.exception.ConflictException;
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
import java.time.OffsetDateTime;
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
    private final ReceiptWaiverReasonRepository receiptWaiverReasonRepository;
    private final StatementTransactionRepository statementTransactionRepository;
    private final AccountingMapper mapper;
    
    public AccountingService(AccountRepository accountRepository,
                           BankTransactionRepository bankTransactionRepository,
                           ReceiptRepository receiptRepository,
                           ReceiptMatchRepository receiptMatchRepository,
                           ReceiptWaiverReasonRepository receiptWaiverReasonRepository,
                           StatementTransactionRepository statementTransactionRepository,
                           AccountingMapper mapper) {
        this.accountRepository = accountRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.receiptRepository = receiptRepository;
        this.receiptMatchRepository = receiptMatchRepository;
        this.receiptWaiverReasonRepository = receiptWaiverReasonRepository;
        this.statementTransactionRepository = statementTransactionRepository;
        this.mapper = mapper;
    }
    
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account();
        account.setName(request.getName());
        account.setAccountNo(request.getAccountNo());
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

    @Transactional
    public void deleteAccount(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
        accountRepository.delete(account);
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
        receipt.setRejected(false);
        
        Receipt saved = receiptRepository.save(receipt);
        return mapper.toReceiptResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsByDateRange(LocalDate from, LocalDate to) {
        return receiptRepository.findByDateRange(from, to, ReceiptMatchStatus.ACTIVE).stream()
                .map(mapper::toReceiptResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsByDateRange(LocalDate from, LocalDate to, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        return receiptRepository.findByDateRange(from, to, ReceiptMatchStatus.ACTIVE, PageRequest.of(page, size, sort)).stream()
                .map(mapper::toReceiptResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReceiptWaiverReasonResponse> getReceiptWaiverReasons() {
        return receiptWaiverReasonRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
            .map(reason -> new ReceiptWaiverReasonResponse(reason.getCode(), reason.getLabel()))
            .collect(Collectors.toList());
    }
    
    @Transactional
    public ReceiptMatchResponse createReceiptMatch(CreateReceiptMatchRequest request) {
        if (receiptMatchRepository.existsByReceiptIdAndStatus(request.getReceiptId(), ReceiptMatchStatus.ACTIVE)) {
            throw new ConflictException("Receipt is already matched to a bank transaction");
        }
        if (receiptMatchRepository.existsByBankTransactionIdAndStatus(request.getBankTransactionId(), ReceiptMatchStatus.ACTIVE)) {
            throw new ConflictException("Bank transaction is already matched to a receipt");
        }
        // Check if match already exists (unique constraint: receiptId + bankTransactionId)
        if (receiptMatchRepository.existsByReceiptIdAndBankTransactionIdAndStatus(
            request.getReceiptId(),
            request.getBankTransactionId(),
            ReceiptMatchStatus.ACTIVE)) {
            throw new IllegalArgumentException("Receipt match already exists for this receipt and transaction");
        }
        
        // Verify receipt exists
        Receipt receipt = receiptRepository.findById(request.getReceiptId())
                .orElseThrow(() -> new EntityNotFoundException("Receipt not found: " + request.getReceiptId()));

        if (receipt.isRejected()) {
            throw new ConflictException("Cannot approve receipt: it is rejected");
        }
        
        // Verify bank transaction exists
        BankTransaction bankTransaction = bankTransactionRepository.findById(request.getBankTransactionId())
                .orElseThrow(() -> new EntityNotFoundException("Bank transaction not found: " + request.getBankTransactionId()));

        if (bankTransaction.isReceiptWaived()) {
            clearReceiptWaiverFields(bankTransaction);
            bankTransactionRepository.save(bankTransaction);
        }
        
        // Create match
        ReceiptMatch match = new ReceiptMatch();
        match.setReceipt(receipt);
        match.setBankTransaction(bankTransaction);
        match.setMatchedAmount(request.getMatchedAmount());
        match.setMatchType(request.getMatchType());
        match.setConfidence(request.getConfidence());
        match.setCreatedAt(Instant.now());
        match.setStatus(ReceiptMatchStatus.ACTIVE);
        
        ReceiptMatch saved = receiptMatchRepository.save(match);
        return mapper.toReceiptMatchResponse(saved);
    }

    @Transactional
    public BankTransactionResponse approveReceiptWaiver(UUID bankTransactionId, ApproveReceiptWaiverRequest request) {
        BankTransaction transaction = bankTransactionRepository.findById(bankTransactionId)
            .orElseThrow(() -> new EntityNotFoundException("Bank transaction not found: " + bankTransactionId));

        if (receiptMatchRepository.existsByBankTransactionIdAndStatus(bankTransactionId, ReceiptMatchStatus.ACTIVE)) {
            throw new ConflictException("Cannot approve without receipt: bank transaction is already matched");
        }

        transaction.setReceiptWaived(true);
        transaction.setReceiptWaiverReason(request.getReason());
        transaction.setReceiptWaiverNote(request.getNote());
        transaction.setReceiptWaivedAt(OffsetDateTime.now());

        BankTransaction saved = bankTransactionRepository.save(transaction);
        return mapper.toBankTransactionResponse(saved);
    }

    @Transactional
    public BankTransactionResponse clearReceiptWaiver(UUID bankTransactionId) {
        BankTransaction transaction = bankTransactionRepository.findById(bankTransactionId)
            .orElseThrow(() -> new EntityNotFoundException("Bank transaction not found: " + bankTransactionId));

        if (!transaction.isReceiptWaived()) {
            return mapper.toBankTransactionResponse(transaction);
        }

        clearReceiptWaiverFields(transaction);
        BankTransaction saved = bankTransactionRepository.save(transaction);
        return mapper.toBankTransactionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReceiptMatchResponse> getReceiptMatches(UUID receiptId) {
        receiptRepository.findById(receiptId)
                .orElseThrow(() -> new EntityNotFoundException("Receipt not found: " + receiptId));
        return receiptMatchRepository.findByReceiptIdAndStatus(receiptId, ReceiptMatchStatus.ACTIVE).stream()
                .map(mapper::toReceiptMatchResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteReceipt(UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new EntityNotFoundException("Receipt not found: " + receiptId));

        if (receiptMatchRepository.existsByReceiptIdAndStatus(receiptId, ReceiptMatchStatus.ACTIVE)) {
            throw new ConflictException("Cannot delete receipt: it is matched and locked");
        }

        receiptRepository.delete(receipt);
    }

    @Transactional
    public ReceiptResponse rejectReceipt(UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new EntityNotFoundException("Receipt not found: " + receiptId));

        if (receiptMatchRepository.existsByReceiptIdAndStatus(receiptId, ReceiptMatchStatus.ACTIVE)) {
            throw new ConflictException("Cannot reject receipt: it is matched and locked");
        }

        if (!receipt.isRejected()) {
            receipt.setRejected(true);
        }

        return mapper.toReceiptResponse(receiptRepository.save(receipt));
    }

        @Transactional(readOnly = true)
        public ReceiptResponse getReceiptById(UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
            .orElseThrow(() -> new EntityNotFoundException("Receipt not found: " + receiptId));
        return mapper.toReceiptResponse(receipt);
        }

        @Transactional(readOnly = true)
        public BankTransactionLinkResponse getBankTransactionLinks(UUID bankTransactionId) {
        bankTransactionRepository.findById(bankTransactionId)
            .orElseThrow(() -> new EntityNotFoundException("Bank transaction not found: " + bankTransactionId));

        Long statementTransactionId = statementTransactionRepository.findFirstByBankTransactionId(bankTransactionId)
            .map(statement -> statement.getId())
            .orElse(null);
        UUID receiptId = receiptMatchRepository.findFirstByBankTransactionIdAndStatus(
            bankTransactionId,
            ReceiptMatchStatus.ACTIVE)
            .map(match -> match.getReceipt().getId())
            .orElse(null);

        return new BankTransactionLinkResponse(bankTransactionId, statementTransactionId, receiptId);
        }

    @Transactional(readOnly = true)
    public List<ReceiptMatchCandidateResponse> getReceiptMatchCandidates(UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new EntityNotFoundException("Receipt not found: " + receiptId));

        LocalDate from = receipt.getPurchaseDate() != null
            ? receipt.getPurchaseDate().minusDays(7)
            : LocalDate.now().minusDays(90);
        LocalDate to = receipt.getPurchaseDate() != null
            ? receipt.getPurchaseDate().plusDays(7)
            : LocalDate.now().plusDays(1);

        List<BankTransaction> candidates;
        if (receipt.getCurrency() != null) {
            candidates = bankTransactionRepository
                .findUnmatchedByCurrencyAndBookingDateBetween(receipt.getCurrency(), from, to, PageRequest.of(0, 50));
        } else {
            candidates = bankTransactionRepository
                .findUnmatchedByBookingDateBetween(from, to, PageRequest.of(0, 50));
        }

        if (candidates.isEmpty()) {
            candidates = bankTransactionRepository
                .findUnmatched(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "bookingDate")));
        }

        return candidates.stream()
                .map(bt -> new ReceiptMatchCandidateResponse(
                        bt.getId(),
                        bt.getAccount() != null ? bt.getAccount().getId() : null,
                        bt.getBookingDate(),
                        bt.getAmount(),
                        bt.getCurrency(),
                        bt.getDescription(),
                        computeMatchPrediction(receipt, bt)
                ))
                .sorted((a, b) -> Integer.compare(b.getMatchPrediction(), a.getMatchPrediction()))
                .collect(Collectors.toList());
    }

    private int computeMatchPrediction(Receipt receipt, BankTransaction transaction) {
        int score = 0;

        if (receipt.getCurrency() != null && receipt.getCurrency().equalsIgnoreCase(transaction.getCurrency())) {
            score += 20;
        }

        BigDecimal receiptAmount = receipt.getTotalAmount();
        BigDecimal transactionAmount = transaction.getAmount() != null ? transaction.getAmount().abs() : null;
        if (receiptAmount != null && transactionAmount != null) {
            BigDecimal diff = receiptAmount.subtract(transactionAmount).abs();
            if (diff.compareTo(new BigDecimal("1.00")) <= 0) {
                score += 50;
            } else if (diff.compareTo(new BigDecimal("5.00")) <= 0) {
                score += 40;
            } else if (diff.compareTo(new BigDecimal("10.00")) <= 0) {
                score += 30;
            } else if (diff.compareTo(new BigDecimal("25.00")) <= 0) {
                score += 15;
            }
        }

        if (receipt.getPurchaseDate() != null && transaction.getBookingDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(receipt.getPurchaseDate(), transaction.getBookingDate());
            long diffDays = Math.abs(days);
            if (diffDays == 0) {
                score += 20;
            } else if (diffDays == 1) {
                score += 15;
            } else if (diffDays == 2) {
                score += 10;
            } else if (diffDays == 3) {
                score += 5;
            }
        }

        String receiptDesc = (receipt.getDescription() != null && !receipt.getDescription().isBlank())
                ? receipt.getDescription()
                : receipt.getMerchant();
        if (receiptDesc != null && transaction.getDescription() != null) {
            int overlap = computeTokenOverlap(receiptDesc, transaction.getDescription());
            if (overlap >= 3) {
                score += 10;
            } else if (overlap == 2) {
                score += 7;
            } else if (overlap == 1) {
                score += 4;
            }
        }

        return Math.min(score, 100);
    }

    private int computeTokenOverlap(String a, String b) {
        String[] aTokens = a.toLowerCase().split("\\W+");
        String[] bTokens = b.toLowerCase().split("\\W+");
        java.util.Set<String> aSet = new java.util.HashSet<>();
        for (String token : aTokens) {
            if (!token.isBlank()) {
                aSet.add(token);
            }
        }
        int overlap = 0;
        for (String token : bTokens) {
            if (aSet.contains(token)) {
                overlap++;
            }
        }
        return overlap;
    }
    
    @Transactional
    public void unmatchReceiptMatch(UUID matchId) {
        ReceiptMatch match = receiptMatchRepository.findById(matchId)
            .orElseThrow(() -> new EntityNotFoundException("Receipt match not found: " + matchId));

        if (!ReceiptMatchStatus.ACTIVE.equals(match.getStatus())) {
            throw new ConflictException("Receipt match is already unmatched");
        }

        match.setStatus(ReceiptMatchStatus.UNMATCHED);
        receiptMatchRepository.save(match);
    }
    
    @Transactional(readOnly = true)
    public MatchStatusResponse getMatchStatus(UUID bankTransactionId) {
        BankTransaction transaction = bankTransactionRepository.findById(bankTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("Bank transaction not found: " + bankTransactionId));
        
        BigDecimal transactionAmountAbs = transaction.getAmount().abs();
        BigDecimal sumMatched = receiptMatchRepository.sumMatchedAmountByBankTransactionId(
            bankTransactionId,
            ReceiptMatchStatus.ACTIVE);
        
        String status;
        if (transaction.isReceiptWaived() && sumMatched.compareTo(BigDecimal.ZERO) == 0) {
            status = "APPROVED_NO_RECEIPT";
        } else if (sumMatched.compareTo(BigDecimal.ZERO) == 0) {
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
                    BigDecimal sumMatched = receiptMatchRepository.sumMatchedAmountByBankTransactionId(
                        transaction.getId(),
                        ReceiptMatchStatus.ACTIVE);
                    BigDecimal transactionAmountAbs = transaction.getAmount().abs();
                    
                    String status;
                    if (transaction.isReceiptWaived() && sumMatched.compareTo(BigDecimal.ZERO) == 0) {
                        status = "APPROVED_NO_RECEIPT";
                    } else if (sumMatched.compareTo(BigDecimal.ZERO) == 0) {
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
                            status,
                            transaction.isReceiptWaived(),
                            transaction.getReceiptWaiverReason(),
                            transaction.getReceiptWaiverNote(),
                            transaction.getReceiptWaivedAt()
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
                    BigDecimal sumMatched = receiptMatchRepository.sumMatchedAmountByBankTransactionId(
                        transaction.getId(),
                        ReceiptMatchStatus.ACTIVE);
                    BigDecimal transactionAmountAbs = transaction.getAmount().abs();

                    String status;
                    if (transaction.isReceiptWaived() && sumMatched.compareTo(BigDecimal.ZERO) == 0) {
                        status = "APPROVED_NO_RECEIPT";
                    } else if (sumMatched.compareTo(BigDecimal.ZERO) == 0) {
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
                            status,
                            transaction.isReceiptWaived(),
                            transaction.getReceiptWaiverReason(),
                            transaction.getReceiptWaiverNote(),
                            transaction.getReceiptWaivedAt()
                    );
                })
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public String getReconciliationCsv(UUID accountId, LocalDate from, LocalDate to) {
        List<ReconciliationRowResponse> rows = getReconciliationData(accountId, from, to);
        
        StringBuilder csv = new StringBuilder();
        
        // Header row
        csv.append("transactionId,bookingDate,amount,description,sumMatched,status,receiptWaived,receiptWaiverReason,receiptWaiverNote,receiptWaivedAt\n");
        
        // Data rows
        for (ReconciliationRowResponse row : rows) {
            csv.append(escapeCsvField(row.getTransactionId().toString())).append(",");
            csv.append(escapeCsvField(row.getBookingDate().toString())).append(",");
            csv.append(escapeCsvField(row.getAmount().toString())).append(",");
            csv.append(escapeCsvField(row.getDescription())).append(",");
            csv.append(escapeCsvField(row.getSumMatched().toString())).append(",");
            csv.append(escapeCsvField(row.getStatus())).append(",");
            csv.append(escapeCsvField(Boolean.toString(row.isReceiptWaived()))).append(",");
            csv.append(escapeCsvField(row.getReceiptWaiverReason() != null ? row.getReceiptWaiverReason().name() : null)).append(",");
            csv.append(escapeCsvField(row.getReceiptWaiverNote())).append(",");
            csv.append(escapeCsvField(row.getReceiptWaivedAt() != null ? row.getReceiptWaivedAt().toString() : null)).append("\n");
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
            header.createCell(6).setCellValue("receiptWaived");
            header.createCell(7).setCellValue("receiptWaiverReason");
            header.createCell(8).setCellValue("receiptWaiverNote");
            header.createCell(9).setCellValue("receiptWaivedAt");

            for (ReconciliationRowResponse row : rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                excelRow.createCell(0).setCellValue(row.getTransactionId().toString());
                excelRow.createCell(1).setCellValue(row.getBookingDate().toString());
                excelRow.createCell(2).setCellValue(row.getAmount().toPlainString());
                excelRow.createCell(3).setCellValue(row.getDescription());
                excelRow.createCell(4).setCellValue(row.getSumMatched().toPlainString());
                excelRow.createCell(5).setCellValue(row.getStatus());
                excelRow.createCell(6).setCellValue(Boolean.toString(row.isReceiptWaived()));
                excelRow.createCell(7).setCellValue(row.getReceiptWaiverReason() != null ? row.getReceiptWaiverReason().name() : "");
                excelRow.createCell(8).setCellValue(row.getReceiptWaiverNote() != null ? row.getReceiptWaiverNote() : "");
                excelRow.createCell(9).setCellValue(row.getReceiptWaivedAt() != null ? row.getReceiptWaivedAt().toString() : "");
            }

            for (int i = 0; i <= 9; i++) {
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

    private void clearReceiptWaiverFields(BankTransaction transaction) {
        transaction.setReceiptWaived(false);
        transaction.setReceiptWaiverReason(null);
        transaction.setReceiptWaiverNote(null);
        transaction.setReceiptWaivedAt(null);
    }
}
