package com.frnholding.pocketaccount.interpretation.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frnholding.pocketaccount.interpretation.domain.InvoiceFieldsDTO;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import com.frnholding.pocketaccount.interpretation.pipeline.InvoiceExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * AI-based invoice field extraction using OpenAI GPT models.
 * Extracts structured invoice data from interpreted text using direct REST API.
 */
@Slf4j
@Component
public class OpenAiInvoiceExtractor implements InvoiceExtractor {

    @Value("${openai.api.key:#{null}}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public InvoiceFieldsDTO extract(InterpretedText text) {
        log.info(
            "Extracting invoice fields using OpenAI REST API (enabled={}, apiKeyPresent={}, model={}, textLength={})",
            enabled,
            apiKey != null && !apiKey.isEmpty(),
            model,
            text != null && text.getRawText() != null ? text.getRawText().length() : 0
        );

        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI is not enabled or API key not configured, returning empty fields");
            return new InvoiceFieldsDTO();
        }

        try {
            String prompt = buildInvoicePrompt(text);
            String response = callOpenAiApi(prompt);

            log.debug("OpenAI response: {}", response);

            InvoiceFieldsDTO fields = parseInvoiceResponse(response);
            
            log.info("Successfully extracted invoice fields: amount={}, date={}, sender={}", 
                    fields.getAmount(), fields.getDate(), fields.getSender());
            
            return fields;

        } catch (OpenAiAuthenticationException e) {
            log.error("OpenAI authentication failed while extracting invoice fields: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract invoice fields using OpenAI: {}", e.getMessage(), e);
            return new InvoiceFieldsDTO();
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
        requestBody.put("max_tokens", 500);
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", 
            "You are an expert at extracting structured data from invoice documents. " +
            "Extract the invoice fields and return them as JSON with these exact keys: " +
            "amount (number), currency (string), date (YYYY-MM-DD), description (string), sender (string). " +
            "If a field cannot be determined, use null. Return only valid JSON, no additional text."));
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
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new OpenAiAuthenticationException(
                    "OpenAI authentication failed. Check OPENAI_API_KEY and OPENAI_ENABLED.",
                    e
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to call OpenAI API", e);
        }
    }

    private String buildInvoicePrompt(InterpretedText text) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Extract invoice information from the following document text:\n\n");
        
        String rawText = text.getRawText();
        if (rawText.length() > 3000) {
            rawText = rawText.substring(0, 3000) + "...";
        }
        
        prompt.append(rawText);
        prompt.append("\n\nExtract the following fields:\n");
        prompt.append("- amount: Total invoice amount (number only, no currency symbol)\n");
        prompt.append("- currency: Currency code (e.g., USD, EUR, CHF)\n");
        prompt.append("- date: Invoice date in YYYY-MM-DD format\n");
        prompt.append("- description: Brief description of the invoice\n");
        prompt.append("- sender: Company or person who issued the invoice\n");
        
        return prompt.toString();
    }

    private InvoiceFieldsDTO parseInvoiceResponse(String response) {
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

            @SuppressWarnings("unchecked")
            Map<String, Object> raw = objectMapper.readValue(jsonStr, Map.class);

            InvoiceFieldsDTO fields = new InvoiceFieldsDTO();

            if (raw.get("amount") != null) {
                fields.setAmount(parseAmount(raw.get("amount")));
            }

            if (raw.get("currency") != null) {
                fields.setCurrency(raw.get("currency").toString());
            }

            if (raw.get("date") != null) {
                fields.setDate(parseDate(raw.get("date").toString()));
            }

            if (raw.get("description") != null) {
                fields.setDescription(raw.get("description").toString());
            }

            if (raw.get("sender") != null) {
                fields.setSender(raw.get("sender").toString());
            }

            return fields;

        } catch (Exception e) {
            log.error("Failed to parse OpenAI response as JSON: {}", response, e);
            return new InvoiceFieldsDTO();
        }
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
