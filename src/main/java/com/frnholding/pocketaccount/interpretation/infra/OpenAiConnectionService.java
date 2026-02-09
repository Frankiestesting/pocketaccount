package com.frnholding.pocketaccount.interpretation.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class OpenAiConnectionService {

    @Value("${openai.api.key:#{null}}")
    private String apiKey;

    @Value("${openai.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> checkConnection() {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("OpenAI is not enabled or API key not configured.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = "https://api.openai.com/v1/models";

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
