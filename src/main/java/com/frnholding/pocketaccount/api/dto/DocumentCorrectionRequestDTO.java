package com.frnholding.pocketaccount.api.dto;

import com.frnholding.pocketaccount.interpretation.api.dto.SaveCorrectionRequestDTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocumentCorrectionRequestDTO {
    private String documentType;
    private Map<String, Object> fields;
    private String note;

    public DocumentCorrectionRequestDTO() {
    }

    public DocumentCorrectionRequestDTO(String documentType, Map<String, Object> fields, String note) {
        this.documentType = documentType;
        this.fields = fields;
        this.note = note;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public SaveCorrectionRequestDTO toInterpretationRequest() {
        SaveCorrectionRequestDTO request = new SaveCorrectionRequestDTO();
        String resolvedType = documentType;
        if ((resolvedType == null || resolvedType.isBlank()) && fields != null && fields.containsKey("transactions")) {
            resolvedType = "STATEMENT";
        }
        if (resolvedType == null || resolvedType.isBlank()) {
            resolvedType = "INVOICE";
        }
        request.setDocumentType(resolvedType);
        request.setNote(note);

        Map<String, Object> safeFields = fields;
        if (safeFields == null) {
            return request;
        }

        if ("STATEMENT".equalsIgnoreCase(resolvedType) || safeFields.containsKey("transactions")) {
            List<SaveCorrectionRequestDTO.TransactionDto> transactions = toTransactions(safeFields.get("transactions"));
            if (!transactions.isEmpty()) {
                request.setTransactions(transactions);
            }
            return request;
        }

        SaveCorrectionRequestDTO.InvoiceFieldsDto invoiceFields = toInvoiceFields(safeFields);
        request.setInvoiceFields(invoiceFields);
        return request;
    }

    private List<SaveCorrectionRequestDTO.TransactionDto> toTransactions(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }
        List<?> list = (List<?>) value;
        List<SaveCorrectionRequestDTO.TransactionDto> transactions = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> map = (Map<?, ?>) item;
            Double amount = toDouble(map.get("amount"));
            String currency = toStringValue(map.get("currency"));
            LocalDate date = toDate(map.get("date"));
            String description = toStringValue(map.get("description"));
                String accountNo = toStringValue(map.get("accountNo"));
            Boolean approved = toBoolean(map.get("approved"));
            transactions.add(new SaveCorrectionRequestDTO.TransactionDto(
                    amount,
                    currency,
                    date,
                    description,
                    accountNo,
                    approved
            ));
        }
        return transactions;
    }

    private SaveCorrectionRequestDTO.InvoiceFieldsDto toInvoiceFields(Map<String, Object> map) {
        Double amount = toDouble(map.get("amount"));
        String currency = toStringValue(map.get("currency"));
        LocalDate date = toDate(map.get("date"));
        String description = toStringValue(map.get("description"));
        String sender = toStringValue(map.get("sender"));
        return new SaveCorrectionRequestDTO.InvoiceFieldsDto(amount, currency, date, description, sender);
    }

    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            String text = ((String) value);
            if (text.isBlank()) {
                return null;
            }
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private LocalDate toDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof String) {
            String text = ((String) value);
            if (text.isBlank()) {
                return null;
            }
            try {
                return LocalDate.parse(text);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty()) {
                return null;
            }
            return Boolean.parseBoolean(text);
        }
        return null;
    }
}