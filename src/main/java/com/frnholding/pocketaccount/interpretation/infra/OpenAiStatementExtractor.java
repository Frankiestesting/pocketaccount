package com.frnholding.pocketaccount.interpretation.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import com.frnholding.pocketaccount.interpretation.pipeline.StatementExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * AI-based bank statement transaction extraction using OpenAI GPT models.
 * Extracts structured transaction data from interpreted text using direct REST API.
 */
@Slf4j
@Component
public class OpenAiStatementExtractor implements StatementExtractor {

    @Value("${openai.api.key:#{null}}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<StatementTransaction> extract(InterpretedText text) {
        log.info("Extracting statement transactions using OpenAI REST API");

        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI is not enabled or API key not configured, returning empty list");
            return new ArrayList<>();
        }

        try {
            String prompt = buildStatementPrompt(text);
            String response = callOpenAiApi(prompt);

            log.debug("OpenAI response: {}", response);

            List<StatementTransaction> transactions = parseStatementResponse(response);
            
            log.info("Successfully extracted {} transactions from statement", transactions.size());
            
            return transactions;

        } catch (Exception e) {
            log.error("Failed to extract statement transactions using OpenAI: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String callOpenAiApi(String userPrompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 2000);
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", 
            "You are an expert at extracting structured transaction data from bank statements. " +
            "Extract all transactions and return them as a JSON array. Each transaction should have: " +
            "amount (number, negative for debits/withdrawals, positive for credits/deposits), " +
            "currency (string), date (YYYY-MM-DD), description (string). " +
            "Return only valid JSON array, no additional text."));
        messages.add(Map.of("role", "user", "content", userPrompt));
        requestBody.put("messages", messages);
        
        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            
            return (String) message.get("content");
        } catch (Exception e) {
            throw new RuntimeException("Failed to call OpenAI API", e);
        }
    }

    private String buildStatementPrompt(InterpretedText text) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Extract all transaction data from the following bank statement text:\n\n");
        
        String rawText = text.getRawText();
        if (rawText.length() > 4000) {
            rawText = rawText.substring(0, 4000) + "...";
        }
        
        prompt.append(rawText);
        prompt.append("\n\nFor each transaction, extract:\n");
        prompt.append("- amount: Transaction amount (use negative for debits/withdrawals, positive for credits)\n");
        prompt.append("- currency: Currency code (e.g., USD, EUR, CHF, NOK)\n");
        prompt.append("- date: Transaction date in YYYY-MM-DD format\n");
        prompt.append("- description: Transaction description or merchant name\n");
        prompt.append("\nReturn all transactions as a JSON array.\n");
        
        return prompt.toString();
    }

    private List<StatementTransaction> parseStatementResponse(String response) {
        List<StatementTransaction> transactions = new ArrayList<>();
        
        try {
            String jsonStr = response.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();

            List<Map<String, Object>> rawTransactions = objectMapper.readValue(
                    jsonStr, new TypeReference<List<Map<String, Object>>>() {});

            for (Map<String, Object> raw : rawTransactions) {
                try {
                    StatementTransaction transaction = new StatementTransaction();
                    
                    if (raw.get("amount") != null) {
                        transaction.setAmount(parseAmount(raw.get("amount")));
                    }
                    
                    if (raw.get("currency") != null) {
                        transaction.setCurrency(raw.get("currency").toString());
                    }
                    
                    if (raw.get("date") != null) {
                        transaction.setDate(parseDate(raw.get("date").toString()));
                    }
                    
                    if (raw.get("description") != null) {
                        transaction.setDescription(raw.get("description").toString());
                    }
                    
                    transactions.add(transaction);
                    
                } catch (Exception e) {
                    log.warn("Failed to parse transaction: {}", raw, e);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse OpenAI response as JSON: {}", response, e);
        }

        return transactions;
    }

    private Double parseAmount(Object amountObj) {
        if (amountObj instanceof Number) {
            return ((Number) amountObj).doubleValue();
        }
        if (amountObj instanceof String) {
            return Double.parseDouble(((String) amountObj).replaceAll("[^0-9.-]", ""));
        }
        return 0.0;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse date: {}", dateStr);
            return LocalDate.now();
        }
    }
}
