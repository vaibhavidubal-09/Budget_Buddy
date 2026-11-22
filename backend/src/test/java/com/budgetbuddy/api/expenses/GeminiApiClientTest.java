package com.budgetbuddy.api.expenses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class GeminiApiClientTest {

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent";

    @Test
    void retriesRateLimitOnceAndReturnsNextValidResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(ENDPOINT)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));
        GeminiApiClient client = new GeminiApiClient(builder);

        var response = client.generateContent("test-model", "test-key", java.util.Map.of("prompt", "hello"));

        assertEquals("ok", response.path("result").asText());
        server.verify();
    }

    @Test
    void doesNotRetryNonTransientClientErrors() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT)).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));
        GeminiApiClient client = new GeminiApiClient(builder);

        assertThrows(RestClientResponseException.class,
                () -> client.generateContent("test-model", "test-key", java.util.Map.of("prompt", "hello")));
        server.verify();
    }

    @Test
    void malformedModelJsonProducesSafeManualEntryError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess("""
                {"candidates":[{"content":{"parts":[{"text":"not json"}]}}]}
                """, MediaType.APPLICATION_JSON));
        GeminiApiClient client = new GeminiApiClient(builder);
        ExpenseUnderstandingService service = new ExpenseUnderstandingService(client, "test-key", "test-model");

        org.springframework.web.server.ResponseStatusException error = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.understand("spent 50 on lunch", java.time.LocalDate.of(2026, 10, 15)));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        server.verify();
    }
}
