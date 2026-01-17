package com.frnholding.pocketaccount.interpretation.infra;

import com.frnholding.pocketaccount.interpretation.domain.InvoiceFieldsDTO;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import com.frnholding.pocketaccount.interpretation.pipeline.InvoiceExtractor;
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
 * Rule-based invoice field extraction using regular expressions and heuristics.
 * Fast, deterministic extraction without external dependencies.
 * Works well for standardized invoice formats.
 */
@Slf4j
@Component
@Primary
public class RegexInvoiceExtractor implements InvoiceExtractor {

    // Currency patterns
    private static final Pattern CURRENCY_PATTERN = Pattern.compile(
        "\\b(USD|EUR|GBP|CHF|CAD|AUD|JPY|CNY)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Amount patterns - matches various currency formats
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(?:total|amount|sum|balance|grand\\s+total|invoice\\s+total|due)\\s*:?\\s*" +
        "(?:[A-Z]{3}\\s*)?" +  // Optional currency code
        "([€$£¥₣])?\\s*" +  // Optional currency symbol
        "([\\d,']+\\.\\d{2})" +  // Amount with 2 decimal places
        "(?:\\s*([A-Z]{3}))?",  // Optional currency code after
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    // Simple amount pattern as fallback
    private static final Pattern SIMPLE_AMOUNT_PATTERN = Pattern.compile(
        "([€$£¥₣])?\\s*([\\d,']+\\.\\d{2})(?:\\s*([A-Z]{3}))?\\b"
    );

    // Date patterns - supports multiple formats
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(?:invoice\\s+date|date|issued|dated)\\s*:?\\s*" +
        "(?:(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})|" +  // YYYY-MM-DD or YYYY/MM/DD
        "(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})|" +  // DD-MM-YYYY or DD.MM.YYYY
        "(\\d{1,2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+(\\d{4}))",  // DD Month YYYY
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    // Invoice number pattern (useful for identifying sender context)
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile(
        "(?:invoice|inv|bill)\\s*#?\\s*:?\\s*([A-Z0-9-]+)",
        Pattern.CASE_INSENSITIVE
    );

    // Company/sender patterns - look for common invoice sender indicators
    private static final Pattern SENDER_PATTERN = Pattern.compile(
        "^(?:from|issued\\s+by|billed\\s+by|seller|vendor|company|corporation)\\s*:?\\s*([A-Z][A-Za-z0-9\\s&.,'-]+?)(?:\\n|$)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    private static final Pattern COMPANY_NAME_PATTERN = Pattern.compile(
        "^([A-Z][A-Za-z0-9\\s&.,'-]+(?:Inc|LLC|Ltd|GmbH|AG|SA|Corp|Corporation|Company))\\s*$",
        Pattern.MULTILINE
    );

    @Override
    public InvoiceFieldsDTO extract(InterpretedText text) {
        log.info("Extracting invoice fields using regex-based rules");

        if (text == null || text.getRawText() == null || text.getRawText().isEmpty()) {
            log.warn("No text provided for invoice extraction");
            return new InvoiceFieldsDTO();
        }

        String content = text.getRawText();
        InvoiceFieldsDTO fields = new InvoiceFieldsDTO();

        // Extract amount
        Double amount = extractAmount(content);
        if (amount != null) {
            fields.setAmount(amount);
            log.debug("Extracted amount: {}", amount);
        }

        // Extract currency
        String currency = extractCurrency(content);
        if (currency != null) {
            fields.setCurrency(currency);
            log.debug("Extracted currency: {}", currency);
        }

        // Extract date
        LocalDate date = extractDate(content);
        if (date != null) {
            fields.setDate(date);
            log.debug("Extracted date: {}", date);
        }

        // Extract sender
        String sender = extractSender(content, text.getLines());
        if (sender != null) {
            fields.setSender(sender);
            log.debug("Extracted sender: {}", sender);
        }

        // Extract description
        String description = extractDescription(content);
        if (description != null) {
            fields.setDescription(description);
            log.debug("Extracted description: {}", description);
        }

        log.info("Regex extraction complete: amount={}, currency={}, date={}, sender={}",
                fields.getAmount(), fields.getCurrency(), fields.getDate(), fields.getSender());

        return fields;
    }

    private Double extractAmount(String content) {
        // Try to find amount with context (total, amount, etc.)
        Matcher matcher = AMOUNT_PATTERN.matcher(content);
        List<Double> amounts = new ArrayList<>();
        
        while (matcher.find()) {
            String amountStr = matcher.group(2);
            if (amountStr != null) {
                try {
                    // Remove thousand separators
                    amountStr = amountStr.replace(",", "").replace("'", "");
                    double amount = Double.parseDouble(amountStr);
                    amounts.add(amount);
                } catch (NumberFormatException e) {
                    log.debug("Could not parse amount: {}", amountStr);
                }
            }
        }

        // Return the largest amount found (usually the total)
        if (!amounts.isEmpty()) {
            return amounts.stream().max(Double::compareTo).orElse(null);
        }

        // Fallback: find any amount-like pattern
        matcher = SIMPLE_AMOUNT_PATTERN.matcher(content);
        while (matcher.find()) {
            String amountStr = matcher.group(2);
            if (amountStr != null) {
                try {
                    amountStr = amountStr.replace(",", "").replace("'", "");
                    double amount = Double.parseDouble(amountStr);
                    if (amount > 0) {
                        amounts.add(amount);
                    }
                } catch (NumberFormatException e) {
                    log.debug("Could not parse fallback amount: {}", amountStr);
                }
            }
        }

        // Return largest amount from fallback
        return amounts.stream().max(Double::compareTo).orElse(null);
    }

    private String extractCurrency(String content) {
        Matcher matcher = CURRENCY_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        // Check for currency symbols near amounts
        if (content.contains("€")) return "EUR";
        if (content.contains("$")) return "USD";
        if (content.contains("£")) return "GBP";
        if (content.contains("¥")) return "JPY";
        if (content.contains("₣") || content.contains("CHF")) return "CHF";

        return null;
    }

    private LocalDate extractDate(String content) {
        Matcher matcher = DATE_PATTERN.matcher(content);
        
        while (matcher.find()) {
            try {
                // YYYY-MM-DD or YYYY/MM/DD format
                if (matcher.group(1) != null) {
                    String year = matcher.group(1);
                    String month = matcher.group(2);
                    String day = matcher.group(3);
                    return LocalDate.of(Integer.parseInt(year), 
                                       Integer.parseInt(month), 
                                       Integer.parseInt(day));
                }
                
                // DD-MM-YYYY or DD.MM.YYYY format
                if (matcher.group(4) != null) {
                    String day = matcher.group(4);
                    String month = matcher.group(5);
                    String year = matcher.group(6);
                    return LocalDate.of(Integer.parseInt(year), 
                                       Integer.parseInt(month), 
                                       Integer.parseInt(day));
                }
                
                // DD Month YYYY format
                if (matcher.group(7) != null) {
                    String day = matcher.group(7);
                    String monthName = matcher.group(8);
                    String year = matcher.group(9);
                    
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy");
                    return LocalDate.parse(day + " " + monthName + " " + year, formatter);
                }
            } catch (Exception e) {
                log.debug("Could not parse date from match: {}", matcher.group());
            }
        }

        // Fallback: try to find any date-like pattern
        Pattern fallbackPattern = Pattern.compile(
            "(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})|(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2,4})"
        );
        matcher = fallbackPattern.matcher(content);
        
        while (matcher.find()) {
            try {
                if (matcher.group(1) != null) {
                    // YYYY-MM-DD
                    return LocalDate.of(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                    );
                } else if (matcher.group(4) != null) {
                    // DD-MM-YYYY
                    String yearStr = matcher.group(6);
                    int year = Integer.parseInt(yearStr);
                    if (year < 100) year += 2000; // Convert 2-digit year
                    
                    return LocalDate.of(
                        year,
                        Integer.parseInt(matcher.group(5)),
                        Integer.parseInt(matcher.group(4))
                    );
                }
            } catch (Exception e) {
                log.debug("Could not parse fallback date");
            }
        }

        return null;
    }

    private String extractSender(String content, List<String> lines) {
        // Try explicit sender pattern
        Matcher matcher = SENDER_PATTERN.matcher(content);
        if (matcher.find()) {
            String sender = matcher.group(1).trim();
            if (sender.length() > 3 && sender.length() < 100) {
                return sender;
            }
        }

        // Try company name pattern in first 5 lines
        if (lines != null && !lines.isEmpty()) {
            for (int i = 0; i < Math.min(5, lines.size()); i++) {
                String line = lines.get(i).trim();
                matcher = COMPANY_NAME_PATTERN.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }
        }

        // Fallback: look for company-like words in first few lines
        if (lines != null && !lines.isEmpty()) {
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                String line = lines.get(i).trim();
                if (line.length() > 3 && line.length() < 100 && 
                    Character.isUpperCase(line.charAt(0)) &&
                    !line.toLowerCase().contains("invoice") &&
                    !line.toLowerCase().contains("bill") &&
                    !line.toLowerCase().contains("receipt")) {
                    return line;
                }
            }
        }

        return null;
    }

    private String extractDescription(String content) {
        // Look for description or service lines
        Pattern descPattern = Pattern.compile(
            "(?:description|services?|items?|details?)\\s*:?\\s*([^\\n]{10,200})",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = descPattern.matcher(content);
        if (matcher.find()) {
            String desc = matcher.group(1).trim();
            // Clean up the description
            desc = desc.replaceAll("\\s+", " ");
            if (desc.length() > 3) {
                return desc;
            }
        }

        // Fallback: use invoice number or generic description
        matcher = INVOICE_NUMBER_PATTERN.matcher(content);
        if (matcher.find()) {
            return "Invoice " + matcher.group(1);
        }

        return "Invoice";
    }
}
