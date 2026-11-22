package com.budgetbuddy.api.expenses;

import tools.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GeminiApiClient {

    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MILLIS = 200;
    private final RestClient client;

    public GeminiApiClient(RestClient.Builder builder) {
        this.client = builder.build();
    }

    public JsonNode generateContent(String model, String apiKey, Object requestBody) {
        for (int attempt = 1; ; attempt++) {
            try {
                return client.post()
                        .uri("/models/{model}:generateContent", model)
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (RestClientResponseException exception) {
                int status = exception.getStatusCode().value();
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt >= MAX_ATTEMPTS) throw exception;
                pauseBeforeRetry();
            } catch (ResourceAccessException exception) {
                if (attempt >= MAX_ATTEMPTS) throw exception;
                pauseBeforeRetry();
            } catch (RestClientException exception) {
                throw exception;
            }
        }
    }

    private void pauseBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RestClientException("Interrupted while waiting to retry Gemini request", exception);
        }
    }
}
