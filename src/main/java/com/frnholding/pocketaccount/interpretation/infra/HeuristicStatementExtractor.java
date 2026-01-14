package com.frnholding.pocketaccount.interpretation.infra;

import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import com.frnholding.pocketaccount.interpretation.pipeline.StatementExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic-based bank statement transaction extraction using pattern matching.
 * Fast, deterministic extraction without external dependencies.
 * Works well for standardized statement formats.
 */
@Slf4j
@Component
@Primary
public class HeuristicStatementExtractor implements StatementExtractor {

    // Transaction line patterns - matches typical statement formats
    // Format: Date Amount Description or Date Description Amount
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
        // Date (various formats)
        "(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4})" +
        "\\s+" +
        // Amount (optional negative sign, thousands separator, decimal)
        "(?:([+-]?[€$£¥₣]?\\s*[\\d,']+\\.\\d{2})\\s+)?" +
        // Description (everything else on line)
        "(.+?)\\s*" +
        // Amount at end (if not at beginning)
        "(?:([+-]?[€$£¥₣]?\\s*[\\d,']+\\.\\d{2}))?$",
        Pattern.MULTILINE
    );

    // Alternative pattern for credit/debit columns
    private static final Pattern CREDIT_DEBIT_PATTERN = Pattern.compile(
        "(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4})" +
        "\\s+" +
        "(.+?)\\s+" +  // Description
        "([\\d,']+\\.\\d{2})?\\s*" +  // Debit
        "([\\d,']+\\.\\d{2})?",  // Credit
        Pattern.MULTILINE
    );

    // Balance line pattern (to identify statement structure)
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
        "(?:balance|saldo|solde|closing|opening)\\s*:?\\s*([€$£¥₣]?\\s*[\\d,']+\\.\\d{2})",
        Pattern.CASE_INSENSITIVE
    );

    // Currency detection
    private static final Pattern CURRENCY_PATTERN = Pattern.compile(
        "\\b(USD|EUR|GBP|CHF|CAD|AUD|JPY|CNY)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Date formatters for parsing
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yy"),
        DateTimeFormatter.ofPattern("dd.MM.yy"),
        DateTimeFormatter.ofPattern("dd/MM/yy"),
    };

    @Override
    public List<StatementTransaction> extract(InterpretedText text) {
        log.info("Extracting statement transactions using heuristic rules");

        if (text == null || text.getRawText() == null || text.getRawText().isEmpty()) {
            log.warn("No text provided for statement extraction");
            return new ArrayList<>();
        }

        String content = text.getRawText();
        List<StatementTransaction> transactions = new ArrayList<>();

        // Detect currency
        String currency = detectCurrency(content);
        log.debug("Detected currency: {}", currency);

        // Try main transaction pattern
        transactions.addAll(extractWithMainPattern(content, currency));

        // Try credit/debit column pattern if main pattern didn't find much
        if (transactions.size() < 3) {
            List<StatementTransaction> creditDebitTransactions = 
                extractWithCreditDebitPattern(content, currency);
            if (creditDebitTransactions.size() > transactions.size()) {
                transactions = creditDebitTransactions;
            }
        }

        // Filter out invalid transactions
        transactions = filterValidTransactions(transactions);

        log.info("Extracted {} valid transactions using heuristics", transactions.size());

        return transactions;
    }

    private String detectCurrency(String content) {
        Matcher matcher = CURRENCY_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        // Check for currency symbols
        if (content.contains("€") || content.toLowerCase().contains("eur")) return "EUR";
        if (content.contains("$") || content.toLowerCase().contains("usd")) return "USD";
        if (content.contains("£") || content.toLowerCase().contains("gbp")) return "GBP";
        if (content.contains("₣") || content.toLowerCase().contains("chf")) return "CHF";

        return "USD"; // Default
    }

    private List<StatementTransaction> extractWithMainPattern(String content, String currency) {
        List<StatementTransaction> transactions = new ArrayList<>();
        Matcher matcher = TRANSACTION_PATTERN.matcher(content);

        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String amountStr1 = matcher.group(2);
                String description = matcher.group(3);
                String amountStr2 = matcher.group(4);

                // Skip lines that look like headers
                if (description != null && isHeaderLine(description)) {
                    continue;
                }

                // Determine which amount to use
                String amountStr = amountStr1 != null ? amountStr1 : amountStr2;
                if (amountStr == null || description == null) {
                    continue;
                }

                // Parse date
                LocalDate date = parseDate(dateStr);
                if (date == null) {
                    continue;
                }

                // Parse amount
                Double amount = parseAmount(amountStr);
                if (amount == null) {
                    continue;
                }

                // Clean up description
                description = cleanDescription(description);
                if (description.length() < 3 || description.length() > 1000) {
                    continue;
                }

                // Create transaction
                StatementTransaction transaction = new StatementTransaction();
                transaction.setDate(date);
                transaction.setAmount(amount);
                transaction.setCurrency(currency);
                transaction.setDescription(description);

                transactions.add(transaction);
                log.debug("Extracted transaction: {} {} {} - {}", 
                         date, amount, currency, description);

            } catch (Exception e) {
                log.debug("Could not parse transaction line: {}", e.getMessage());
            }
        }

        return transactions;
    }

    private List<StatementTransaction> extractWithCreditDebitPattern(String content, String currency) {
        List<StatementTransaction> transactions = new ArrayList<>();
        Matcher matcher = CREDIT_DEBIT_PATTERN.matcher(content);

        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String description = matcher.group(2);
                String debitStr = matcher.group(3);
                String creditStr = matcher.group(4);

                // Skip header lines
                if (description != null && isHeaderLine(description)) {
                    continue;
                }

                // Must have date and description
                if (description == null || description.trim().isEmpty()) {
                    continue;
                }

                // Parse date
                LocalDate date = parseDate(dateStr);
                if (date == null) {
                    continue;
                }

                // Determine amount from debit or credit column
                Double amount = null;
                if (debitStr != null && !debitStr.trim().isEmpty()) {
                    amount = parseAmount(debitStr);
                    if (amount != null && amount > 0) {
                        amount = -amount; // Debits are negative
                    }
                } else if (creditStr != null && !creditStr.trim().isEmpty()) {
                    amount = parseAmount(creditStr);
                }

                if (amount == null) {
                    continue;
                }

                // Clean up description
                description = cleanDescription(description);
                if (description.length() < 3 || description.length() > 1000) {
                    continue;
                }

                // Create transaction
                StatementTransaction transaction = new StatementTransaction();
                transaction.setDate(date);
                transaction.setAmount(amount);
                transaction.setCurrency(currency);
                transaction.setDescription(description);

                transactions.add(transaction);
                log.debug("Extracted credit/debit transaction: {} {} {} - {}", 
                         date, amount, currency, description);

            } catch (Exception e) {
                log.debug("Could not parse credit/debit line: {}", e.getMessage());
            }
        }

        return transactions;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        dateStr = dateStr.trim();

        // Try each formatter
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                
                // Sanity check: date should be reasonable (within last 10 years and not future)
                LocalDate now = LocalDate.now();
                LocalDate tenYearsAgo = now.minusYears(10);
                
                if (date.isAfter(tenYearsAgo) && !date.isAfter(now.plusDays(1))) {
                    return date;
                }
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        return null;
    }

    private Double parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }

        try {
            // Remove currency symbols and whitespace
            amountStr = amountStr.trim()
                .replace("€", "")
                .replace("$", "")
                .replace("£", "")
                .replace("¥", "")
                .replace("₣", "")
                .replace(" ", "");

            // Handle negative sign
            boolean isNegative = amountStr.startsWith("-") || amountStr.startsWith("(");
            amountStr = amountStr.replace("-", "").replace("(", "").replace(")", "");

            // Remove thousand separators
            amountStr = amountStr.replace(",", "").replace("'", "");

            double amount = Double.parseDouble(amountStr);
            return isNegative ? -amount : amount;

        } catch (NumberFormatException e) {
            log.debug("Could not parse amount: {}", amountStr);
            return null;
        }
    }

    private String cleanDescription(String description) {
        if (description == null) {
            return "";
        }

        // Remove multiple spaces
        description = description.replaceAll("\\s+", " ").trim();

        // Remove common statement artifacts
        description = description
            .replaceAll("\\s*[€$£¥₣]?\\s*[\\d,']+\\.\\d{2}\\s*$", "") // Remove trailing amounts
            .replaceAll("^\\s*[€$£¥₣]?\\s*[\\d,']+\\.\\d{2}\\s*", "") // Remove leading amounts
            .trim();

        return description;
    }

    private boolean isHeaderLine(String line) {
        if (line == null) {
            return false;
        }

        String lower = line.toLowerCase();
        return lower.contains("date") && lower.contains("description") ||
               lower.contains("date") && lower.contains("amount") ||
               lower.contains("debit") && lower.contains("credit") ||
               lower.contains("transaction") && lower.contains("date") ||
               lower.equals("date") || lower.equals("description") || 
               lower.equals("amount") || lower.equals("balance");
    }

    private List<StatementTransaction> filterValidTransactions(List<StatementTransaction> transactions) {
        List<StatementTransaction> valid = new ArrayList<>();

        for (StatementTransaction transaction : transactions) {
            // Must have date and amount
            if (transaction.getDate() == null || transaction.getAmount() == null) {
                continue;
            }

            // Amount must not be zero
            if (transaction.getAmount() == 0.0) {
                continue;
            }

            // Description should exist and be reasonable
            if (transaction.getDescription() == null || 
                transaction.getDescription().trim().length() < 3) {
                continue;
            }

            // Date should be within reasonable range
            LocalDate now = LocalDate.now();
            LocalDate twentyYearsAgo = now.minusYears(20);
            if (transaction.getDate().isBefore(twentyYearsAgo) || 
                transaction.getDate().isAfter(now.plusYears(1))) {
                continue;
            }

            valid.add(transaction);
        }

        return valid;
    }
}
