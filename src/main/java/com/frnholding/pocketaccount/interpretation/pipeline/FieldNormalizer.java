package com.frnholding.pocketaccount.interpretation.pipeline;

import java.time.LocalDate;

/**
 * Normalizes extracted field values to ensure consistency.
 * Handles date formatting, currency symbols, decimal separators (comma/period),
 * negative signs, and other field formatting.
 */
public interface FieldNormalizer {
    
    /**
     * Normalizes a date string to LocalDate
     * @param dateString raw date string (e.g., "02.01.2026", "2026-01-02")
     * @return normalized LocalDate
     */
    LocalDate normalizeDate(String dateString);
    
    /**
     * Normalizes a currency string to standard currency code
     * @param currencyString raw currency (e.g., "kr", "NOK", ",-")
     * @return normalized currency code (e.g., "NOK")
     */
    String normalizeCurrency(String currencyString);
    
    /**
     * Normalizes an amount string to Double
     * Handles comma/period decimal separators, negative signs, currency symbols
     * @param amountString raw amount (e.g., "1.234,56", "-399,00", "12450.00")
     * @return normalized amount as Double
     */
    Double normalizeAmount(String amountString);
}
