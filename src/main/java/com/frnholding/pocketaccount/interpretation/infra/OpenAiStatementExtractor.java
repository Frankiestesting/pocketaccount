package com.frnholding.pocketaccount.interpretation.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frnholding.pocketaccount.interpretation.domain.StatementTransaction;
import com.frnholding.pocketaccount.interpretation.pipeline.InterpretedText;
import com.frnholding.pocketaccount.interpretation.pipeline.StatementExtractor;
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
 * AI-based bank statement transaction extraction using OpenAI GPT models.
 * Extracts structured transaction data from interpreted text.
 */
@Slf4j
@Component
public class OpenAiStatementExtractor implements StatementExtractor {

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
    public List<StatementTransaction> extract(InterpretedText text) {
        log.info("Extracting statement transactions using OpenAI");

        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI is not enabled or API key not configured, returning empty list");
            return new ArrayList<>();
        }

        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));

            String prompt = buildStatementPrompt(text);
            
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "You are an expert at extracting structured transaction data from bank statements. " +
                "Extract all transactions and return them as a JSON array. Each transaction should have: " +
                "amount (number, negative for debits), currency (string), date (YYYY-MM-DD), description (string). " +
                "Return only valid JSON array, no additional text."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(0.1)
                    .maxTokens(2000)
                    .build();

            String response = service.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            log.debug("OpenAI response: {}", response);

            List<StatementTransaction> transactions = parseStatementResponse(response);
            service.shutdownExecutor();
            
            log.info("Successfully extracted {} transactions from statement", transactions.size());
            
            return transactions;

        } catch (Exception e) {
            log.error("Failed to extract statement transactions using OpenAI: {}", e.getMessage(), e);
            // Return empty list on error rather than failing
            return new ArrayList<>();
        }
    }

    private String buildStatementPrompt(InterpretedText text) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Extract all transaction data from the following bank statement text:\n\n");
        
        // Limit text length to avoid token limits
        String rawText = text.getRawText();
        if (rawText.length() > 4000) {
            rawText = rawText.substring(0, 4000) + "...";
        }
        
        prompt.append(rawText);
        prompt.append("\n\nFor each transaction, extract:\n");
        prompt.append("- amount: Transaction amount (use negative for debits/withdrawals, positive for credits)\n");
        prompt.append("- currency: Currency code (e.g., USD, EUR, CHF)\n");
        prompt.append("- date: Transaction date in YYYY-MM-DD format\n");
        prompt.append("- description: Transaction description or merchant name\n");
        prompt.append("\nReturn all transactions as a JSON array.\n");
        
        return prompt.toString();
    }

    private List<StatementTransaction> parseStatementResponse(String response) {
        List<StatementTransaction> transactions = new ArrayList<>();
        
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

            List<Map<String, Object>> transactionMaps = objectMapper.readValue(
                jsonStr, 
                new TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> map : transactionMaps) {
                StatementTransaction transaction = new StatementTransaction();
                
                // Extract amount
                if (map.containsKey("amount") && map.get("amount") != null) {
                    Object amountObj = map.get("amount");
                    if (amountObj instanceof Number) {
                        transaction.setAmount(((Number) amountObj).doubleValue());
                    } else if (amountObj instanceof String) {
                        try {
                            transaction.setAmount(Double.parseDouble((String) amountObj));
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse amount: {}", amountObj);
                        }
                    }
                }

                // Extract currency
                if (map.containsKey("currency") && map.get("currency") != null) {
                    transaction.setCurrency(map.get("currency").toString());
                }

                // Extract date
                if (map.containsKey("date") && map.get("date") != null) {
                    String dateStr = map.get("date").toString();
                    transaction.setDate(parseDate(dateStr));
                }

                // Extract description
                if (map.containsKey("description") && map.get("description") != null) {
                    String description = map.get("description").toString();
                    // Limit description length
                    if (description.length() > 1000) {
                        description = description.substring(0, 997) + "...";
                    }
                    transaction.setDescription(description);
                }

                // Only add transaction if it has at least amount and date
                if (transaction.getAmount() != null && transaction.getDate() != null) {
                    transactions.add(transaction);
                } else {
                    log.warn("Skipping incomplete transaction: amount={}, date={}", 
                            transaction.getAmount(), transaction.getDate());
                }
            }

            return transactions;

        } catch (Exception e) {
            log.error("Failed to parse OpenAI statement response: {}", e.getMessage(), e);
            return new ArrayList<>();
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
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),  // 15-01-2024
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
