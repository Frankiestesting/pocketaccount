package com.frnholding.pocketaccount.interpretation.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frnholding.pocketaccount.interpretation.domain.InvoiceFieldsDTO;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import com.frnholding.pocketaccount.interpretation.pipeline.InvoiceExtractor;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI-based invoice field extraction using OpenAI GPT models.
 * Extracts structured invoice data from interpreted text.
 */
@Slf4j
@Component
public class OpenAiInvoiceExtractor implements InvoiceExtractor {

    @Value("${openai.api.key:#{null}}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.timeout:30}")
    private int timeoutSeconds;

    @Value("${openai.enabled:false}")
    private boolean enabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public InvoiceFieldsDTO extract(InterpretedText text) {
        log.info("Extracting invoice fields using OpenAI");

        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI is not enabled or API key not configured, returning empty fields");
            return new InvoiceFieldsDTO();
        }

        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));

            String prompt = buildInvoicePrompt(text);
            
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are an expert at extracting structured data from invoice documents. " +
                "Extract the invoice fields and return them as JSON with these exact keys: " +
                "amount (number), currency (string), date (YYYY-MM-DD), description (string), sender (string). " +
                "If a field cannot be determined, use null. Return only valid JSON, no additional text."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(0.1)
                    .maxTokens(500)
                    .build();

            String response = service.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            log.debug("OpenAI response: {}", response);

            InvoiceFieldsDTO fields = parseInvoiceResponse(response);
            service.shutdownExecutor();
            
            log.info("Successfully extracted invoice fields: amount={}, date={}, sender={}", 
                    fields.getAmount(), fields.getDate(), fields.getSender());
            
            return fields;

        } catch (Exception e) {
            log.error("Failed to extract invoice fields using OpenAI: {}", e.getMessage(), e);
            // Return empty fields on error rather than failing
            return new InvoiceFieldsDTO();
        }
    }

    private String buildInvoicePrompt(InterpretedText text) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Extract invoice information from the following document text:\n\n");
        
        // Limit text length to avoid token limits
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
            // Clean response - remove markdown code blocks if present
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
            Map<String, Object> map = objectMapper.readValue(jsonStr, Map.class);

            InvoiceFieldsDTO fields = new InvoiceFieldsDTO();
            
            // Extract amount
            if (map.containsKey("amount") && map.get("amount") != null) {
                Object amountObj = map.get("amount");
                if (amountObj instanceof Number) {
                    fields.setAmount(((Number) amountObj).doubleValue());
                } else if (amountObj instanceof String) {
                    try {
                        fields.setAmount(Double.parseDouble((String) amountObj));
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse amount: {}", amountObj);
                    }
                }
            }

            // Extract currency
            if (map.containsKey("currency") && map.get("currency") != null) {
                fields.setCurrency(map.get("currency").toString());
            }

            // Extract date
            if (map.containsKey("date") && map.get("date") != null) {
                String dateStr = map.get("date").toString();
                fields.setDate(parseDate(dateStr));
            }

            // Extract description
            if (map.containsKey("description") && map.get("description") != null) {
                fields.setDescription(map.get("description").toString());
            }

            // Extract sender
            if (map.containsKey("sender") && map.get("sender") != null) {
                fields.setSender(map.get("sender").toString());
            }

            return fields;

        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", e.getMessage(), e);
            return new InvoiceFieldsDTO();
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        // Try various date formats
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ISO_LOCAL_DATE,           // 2024-01-15
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),  // 15.01.2024
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),  // 15/01/2024
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),  // 01/15/2024
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),  // 2024-01-15
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }

        log.warn("Could not parse date: {}", dateStr);
        return null;
    }
}
