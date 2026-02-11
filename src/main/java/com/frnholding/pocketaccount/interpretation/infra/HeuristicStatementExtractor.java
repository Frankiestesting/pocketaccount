package com.frnholding.pocketaccount.interpretation.infra;

import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import com.frnholding.pocketaccount.interpretation.pipeline.StatementExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Component
public class HeuristicStatementExtractor implements StatementExtractor {

    private static final Logger log = LoggerFactory.getLogger(HeuristicStatementExtractor.class);

    // Norwegian bank statement pattern with separate withdrawal/deposit columns
    // Columns: Description | Buy_Date(ddMM) | Withdrawal | Deposit | Bank_Date(ddMM) | Ref1 | Ref2
    // Withdrawal example: "Varer 30.08 Elkjøp... 0109 199,00 0109 221023190 *17975545"
    //   Column 3 (withdrawal) has amount, Column 4 (deposit) is empty, followed by bank date
    // Deposit example: "Pensjon Fra: Nav 1809 26.903,00 1809 99899930000 362599343"
    //   Column 3 (withdrawal) is empty, Column 4 (deposit) has amount, followed by bank date
    private static final Pattern NORWEGIAN_PATTERN = Pattern.compile(
        // Description (everything before ddMM pattern)
        "^(.+?)\\s+" +
        // Buy date in ddMM format (4 digits)
        "(\\d{4})\\s+" +
        // Column 3: Withdrawal amount (with comma decimal) OR empty
        "(?:([\\d .]+,\\d{2})\\s+)?" +
        // Column 4: Deposit amount (with comma decimal) OR empty
        "(?:([\\d .]+,\\d{2})\\s+)?" +
        // Bank date in ddMM format (4 digits) - always present
        "(\\d{4})",
        Pattern.MULTILINE
    );

    // Transaction line patterns - matches typical statement formats
    // Format: Date Amount Description or Date Description Amount
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
        // Date (various formats including Norwegian)
        "(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|\\d{4})" +
        "\\s+" +
        // Amount (optional negative sign, thousands separator, decimal)
        "(?:([+-]?[€$£¥₣kr]?\\s*[\\d,' .]+[,.]\\d{2})\\s+)?" +
        // Description (everything else on line)
        "(.+?)\\s*" +
        // Amount at end (if not at beginning)
        "(?:([+-]?[€$£¥₣kr]?\\s*[\\d,' .]+[,.]\\d{2}))?$",
        Pattern.MULTILINE
    );

    // Alternative pattern for credit/debit columns
    private static final Pattern CREDIT_DEBIT_PATTERN = Pattern.compile(
        "(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4})" +
        "\\s+" +
        "(.+?)\\s+" +  // Description
        "([\\d,' .]+[,.]\\d{2})?\\s*" +  // Debit
        "([\\d,' .]+[,.]\\d{2})?",  // Credit
        Pattern.MULTILINE
    );

    // Currency detection
    private static final Pattern CURRENCY_PATTERN = Pattern.compile(
        "\\b(USD|EUR|GBP|CHF|CAD|AUD|JPY|CNY|NOK|SEK|DKK)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Date formatters for parsing including Norwegian formats
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),  // Norwegian standard
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yy"),
        DateTimeFormatter.ofPattern("dd.MM.yy"),   // Norwegian short format
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

        // Detect currency (check for NOK first for Norwegian statements)
        String currency = detectCurrency(content);
        log.debug("Detected currency: {}", currency);

        // Try Norwegian pattern first (most specific)
        transactions.addAll(extractWithNorwegianPattern(content, currency));
        log.debug("Norwegian pattern extracted {} transactions", transactions.size());

        // Try main transaction pattern if Norwegian pattern didn't find enough
        if (transactions.size() < 3) {
            List<StatementTransaction> mainPatternTransactions = 
                extractWithMainPattern(content, currency);
            log.debug("Main pattern extracted {} transactions", mainPatternTransactions.size());
            if (mainPatternTransactions.size() > transactions.size()) {
                transactions = mainPatternTransactions;
            }
        }

        // Try credit/debit column pattern if main pattern didn't find much
        if (transactions.size() < 3) {
            List<StatementTransaction> creditDebitTransactions = 
                extractWithCreditDebitPattern(content, currency);
            log.debug("Credit/debit pattern extracted {} transactions", creditDebitTransactions.size());
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
        // Check for Norwegian currency first
        if (content.contains("kr") || content.toLowerCase().contains("nok") || 
            content.toLowerCase().contains("kroner")) {
            return "NOK";
        }

        Matcher matcher = CURRENCY_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        // Check for currency symbols
        if (content.contains("€") || content.toLowerCase().contains("eur")) return "EUR";
        if (content.contains("$") || content.toLowerCase().contains("usd")) return "USD";
        if (content.contains("£") || content.toLowerCase().contains("gbp")) return "GBP";
        if (content.contains("₣") || content.toLowerCase().contains("chf")) return "CHF";

        return "NOK"; // Default to NOK for Norwegian statements
    }

    private List<StatementTransaction> extractWithNorwegianPattern(String content, String currency) {
        List<StatementTransaction> transactions = new ArrayList<>();
        Matcher matcher = NORWEGIAN_PATTERN.matcher(content);
        
        // Get current year for ddMM date parsing
        int currentYear = LocalDate.now().getYear();

        while (matcher.find()) {
            try {
                String description = matcher.group(1);
                String buyDateDdmm = matcher.group(2);
                String withdrawalStr = matcher.group(3);  // Column 3: Withdrawal amount (or null)
                String depositStr = matcher.group(4);      // Column 4: Deposit amount (or null)
                matcher.group(5); // Column 5: Bank date (unused for now)

                // Skip lines that look like headers
                if (description != null && isHeaderLine(description)) {
                    continue;
                }

                // Skip if description is too short
                if (description == null || description.trim().length() < 3) {
                    continue;
                }

                // Parse buy date (ddMM) to full date
                LocalDate date = parseDdMmDate(buyDateDdmm, currentYear);
                if (date == null) {
                    continue;
                }

                // Determine amount from withdrawal or deposit column
                Double amount = null;
                boolean isWithdrawal = false;
                
                if (withdrawalStr != null && !withdrawalStr.trim().isEmpty()) {
                    // Withdrawal column has value - this is a withdrawal (negative)
                    amount = parseNorwegianAmount(withdrawalStr);
                    if (amount != null && amount > 0) {
                        amount = -amount;
                    }
                    isWithdrawal = true;
                } else if (depositStr != null && !depositStr.trim().isEmpty()) {
                    // Deposit column has value - this is a deposit (positive)
                    amount = parseNorwegianAmount(depositStr);
                    isWithdrawal = false;
                }

                if (amount == null || amount == 0.0) {
                    continue;
                }

                // Clean up description - remove any date patterns embedded in it
                description = cleanNorwegianDescription(description);
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
                log.debug("Extracted Norwegian {} transaction: {} {} {} - {}", 
                         isWithdrawal ? "withdrawal" : "deposit", date, amount, currency, description);

            } catch (Exception e) {
                log.debug("Could not parse Norwegian transaction line: {}", e.getMessage());
            }
        }

        return transactions;
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
                
                // Sanity check: date should be reasonable (within last 20 years and not future)
                LocalDate now = LocalDate.now();
                LocalDate twentyYearsAgo = now.minusYears(20);
                
                if (date.isAfter(twentyYearsAgo) && !date.isAfter(now.plusDays(1))) {
                    return date;
                }
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        return null;
    }

    private LocalDate parseDdMmDate(String ddmm, int year) {
        if (ddmm == null || ddmm.length() != 4) {
            return null;
        }

        try {
            int day = Integer.parseInt(ddmm.substring(0, 2));
            int month = Integer.parseInt(ddmm.substring(2, 4));
            
            // Create date with provided year
            LocalDate date = LocalDate.of(year, month, day);
            
            // If date is in the future, assume it's from previous year
            LocalDate now = LocalDate.now();
            if (date.isAfter(now)) {
                date = LocalDate.of(year - 1, month, day);
            }
            
            // Sanity check: within last 2 years
            if (date.isBefore(now.minusYears(2))) {
                return null;
            }
            
            return date;
        } catch (Exception e) {
            log.debug("Could not parse ddMM date: {}", ddmm);
            return null;
        }
    }

    private Double parseNorwegianAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }

        try {
            // Remove "kr" and "NOK" suffix/prefix
            amountStr = amountStr.trim()
                .replace("kr", "")
                .replace("Kr", "")
                .replace("KR", "")
                .replace("NOK", "")
                .replace("nok", "");

            // Handle negative sign
            boolean isNegative = amountStr.startsWith("-");
            amountStr = amountStr.replace("-", "").trim();

            // Remove space and dot thousand separators, keep comma as decimal
            // Example: "26.903,00" or "26 903,00" -> "26903.00"
            amountStr = amountStr
                .replace(" ", "")  // Remove space thousand separator
                .replace(".", "")  // Remove dot thousand separator
                .replace(",", "."); // Change comma decimal to dot

            double amount = Double.parseDouble(amountStr);
            return isNegative ? -amount : amount;

        } catch (NumberFormatException e) {
            log.debug("Could not parse Norwegian amount: {}", amountStr);
            return null;
        }
    }

    private Double parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }

        // Try Norwegian format first
        if (amountStr.contains(",")) {
            Double norwegianAmount = parseNorwegianAmount(amountStr);
            if (norwegianAmount != null) {
                return norwegianAmount;
            }
        }

        try {
            // Remove currency symbols and whitespace
            amountStr = amountStr.trim()
                .replace("€", "")
                .replace("$", "")
                .replace("£", "")
                .replace("¥", "")
                .replace("₣", "")
                .replace("kr", "")
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

    private String cleanNorwegianDescription(String description) {
        if (description == null) {
            return "";
        }

        // Remove dd.MM date patterns that might be embedded in description
        description = description.replaceAll("\\d{2}\\.\\d{2}\\b", "");
        
        // Remove multiple spaces
        description = description.replaceAll("\\s+", " ").trim();

        // Remove common Norwegian statement artifacts
        description = description
            .replaceAll("\\s*-?[\\d .]+,\\d{2}\\s*(kr|NOK)?\\s*$", "") // Remove trailing amounts
            .replaceAll("^\\s*(kr|NOK)?\\s*-?[\\d .]+,\\d{2}\\s*", "") // Remove leading amounts
            .trim();

        return description;
    }

    private String cleanDescription(String description) {
        if (description == null) {
            return "";
        }

        // Try Norwegian cleaning first
        String norwegianCleaned = cleanNorwegianDescription(description);
        if (!norwegianCleaned.isEmpty()) {
            return norwegianCleaned;
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
               lower.contains("dato") && lower.contains("tekst") ||  // Norwegian headers
               lower.contains("dato") && lower.contains("beløp") ||  // Norwegian headers
               lower.contains("date") && lower.contains("amount") ||
               lower.contains("debit") && lower.contains("credit") ||
               lower.contains("inn") && lower.contains("ut") ||  // Norwegian in/out
               lower.contains("transaction") && lower.contains("date") ||
               lower.equals("date") || lower.equals("dato") || 
               lower.equals("description") || lower.equals("tekst") ||
               lower.equals("amount") || lower.equals("beløp") || 
               lower.equals("balance") || lower.equals("saldo");
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
            
            // Check for duplicates - skip if exact match already exists
            boolean isDuplicate = false;
            for (StatementTransaction existing : valid) {
                if (isSameTransaction(existing, transaction)) {
                    isDuplicate = true;
                    log.debug("Skipping duplicate transaction: {} {} {} - {}", 
                             transaction.getDate(), transaction.getAmount(), 
                             transaction.getCurrency(), transaction.getDescription());
                    break;
                }
            }
            
            if (!isDuplicate) {
                valid.add(transaction);
            }
        }

        return valid;
    }
    
    private boolean isSameTransaction(StatementTransaction t1, StatementTransaction t2) {
        // Same transaction if date, amount, and description are very similar
        if (!t1.getDate().equals(t2.getDate())) {
            return false;
        }
        
        if (!t1.getAmount().equals(t2.getAmount())) {
            return false;
        }
        
        // Compare descriptions (case-insensitive, normalized)
        String desc1 = t1.getDescription().toLowerCase().replaceAll("\\s+", " ").trim();
        String desc2 = t2.getDescription().toLowerCase().replaceAll("\\s+", " ").trim();
        
        return desc1.equals(desc2);
    }
}
