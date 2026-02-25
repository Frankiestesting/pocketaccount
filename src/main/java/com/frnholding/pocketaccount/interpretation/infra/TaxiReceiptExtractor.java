package com.frnholding.pocketaccount.interpretation.infra;

import com.frnholding.pocketaccount.interpretation.domain.InvoiceFieldsDTO;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import com.frnholding.pocketaccount.interpretation.pipeline.InvoiceExtractor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("taxiReceiptExtractor")
public class TaxiReceiptExtractor implements InvoiceExtractor {

    private final RegexInvoiceExtractor baseExtractor;

    public TaxiReceiptExtractor(RegexInvoiceExtractor baseExtractor) {
        this.baseExtractor = baseExtractor;
    }

    @Override
    public InvoiceFieldsDTO extract(InterpretedText text) {
        InvoiceFieldsDTO fields = baseExtractor.extract(text);
        Double amount = extractTaxiAmount(text);
        if (amount != null) {
            fields.setAmount(amount);
        }
        if (fields.getCurrency() == null) {
            fields.setCurrency("NOK");
        }
        return fields;
    }

    private Double extractTaxiAmount(InterpretedText text) {
        if (text == null) {
            return null;
        }
        String filename = text.getMetadata() != null ? String.valueOf(text.getMetadata().get("originalFilename")) : null;
        Double fromFilename = parseAmountFromFilename(filename);
        if (fromFilename != null) {
            return fromFilename;
        }

        String content = text.getRawText();
        Double egenandel = findAmountByLabel(content, "egenandel|egen andel");
        if (egenandel != null) {
            return egenandel;
        }

        Double total = findAmountByLabel(content, "tot(?:al|alt)?|sum|total\\s*pris|totalpris");
        if (total != null) {
            return total;
        }

        return findSmallestAmount(content, 500.0);
    }

    private Double parseAmountFromFilename(String filename) {
        if (filename == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?i)(?:NOK|KR)\\s*(\\d{1,4}(?:[.,]\\d{1,2})?)").matcher(filename);
        if (matcher.find()) {
            return parseAmountValue(matcher.group(1));
        }
        return null;
    }

    private Double findAmountByLabel(String content, String labelRegex) {
        if (content == null) {
            return null;
        }
        Pattern labeledAmount = Pattern.compile(
            "(?:" + labelRegex + ")\\s*:?\\s*(?:kr\\.|kr|NOK)?\\s*([\\d' .]+[,.]\\d{2})",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        Matcher matcher = labeledAmount.matcher(content);
        Double last = null;
        while (matcher.find()) {
            last = parseAmountValue(matcher.group(1));
        }
        return last;
    }

    private Double findSmallestAmount(String content, double max) {
        if (content == null) {
            return null;
        }
        Pattern amountPattern = Pattern.compile("([\\d' .]+[,.]\\d{2})");
        Matcher matcher = amountPattern.matcher(content);
        List<Double> amounts = new ArrayList<>();
        while (matcher.find()) {
            Double parsed = parseAmountValue(matcher.group(1));
            if (parsed != null && parsed > 0 && parsed <= max) {
                amounts.add(parsed);
            }
        }
        return amounts.stream().min(Double::compareTo).orElse(null);
    }

    private Double parseAmountValue(String amountStr) {
        if (amountStr == null) {
            return null;
        }
        try {
            String trimmed = amountStr.trim();
            if (trimmed.contains(",") && trimmed.lastIndexOf(',') > trimmed.lastIndexOf('.')) {
                return parseNorwegianAmount(trimmed);
            }
            trimmed = trimmed.replace(",", "").replace("'", "").replace(" ", "");
            return Double.parseDouble(trimmed);
        } catch (Exception ignored) {
            return null;
        }
    }

    private double parseNorwegianAmount(String amountStr) throws NumberFormatException {
        amountStr = amountStr.trim()
            .replaceAll("(?i)\\s*(kr|NOK)\\s*", "");
        amountStr = amountStr
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".");
        return Double.parseDouble(amountStr);
    }
}
