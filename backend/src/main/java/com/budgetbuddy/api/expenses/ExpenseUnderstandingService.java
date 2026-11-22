package com.budgetbuddy.api.expenses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExpenseUnderstandingService {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(ExpenseUnderstandingService.class);

    private static final List<String> CATEGORIES = List.of(
            "Food & Dining", "Transportation", "Shopping", "Education",
            "Entertainment", "Bills & Subscriptions", "Healthcare", "Other");
    private static final List<String> PAYMENT_METHODS = List.of("UPI", "Cash", "Card", "Other");

    private final GeminiApiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public ExpenseUnderstandingService(
            GeminiApiClient geminiClient,
            @Value("${budgetbuddy.gemini.api-key:}") String apiKey,
            @Value("${budgetbuddy.gemini.model:gemini-3.8-flash}") String model) {
        this.geminiClient = geminiClient;
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.model = model;
    }

    public ExpenseUnderstandingResponse understand(String description, LocalDate today) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI is not configured. You can enter the expense manually.");
        }

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "amount", Map.of("type", "number", "description", "Amount in Indian rupees; use 0 when missing"),
                        "description", Map.of("type", "string", "description", "Short cleaned merchant or item description"),
                        "category", Map.of("type", "string", "enum", CATEGORIES),
                        "paymentMethod", Map.of("type", "string", "enum", PAYMENT_METHODS),
                        "date", Map.of("type", "string", "description", "Date in YYYY-MM-DD format"),
                        "spellingSuggestion", Map.of("type", "string", "description", "Corrected phrase, or empty if no correction is needed")),
                "required", List.of("amount", "description", "category", "paymentMethod", "date", "spellingSuggestion"),
                "additionalProperties", false);

        Map<String, Object> requestSchema = new java.util.HashMap<>(schema);
        requestSchema.remove("additionalProperties");
        String prompt = "Understand one personal expense written in natural language. Be tolerant of spelling mistakes and informal student language. "
                + "Correct the item description when the intended meaning is clear; put the corrected phrase in spellingSuggestion, otherwise use an empty string. "
                + "Examples: 'sanitaruy pads' means sanitary pads and belongs to Healthcare; bus fare belongs to Transportation; lunch belongs to Food & Dining. "
                + "Choose a category only from the provided list. Do not invent an amount. Use amount 0 if none is stated. "
                + "Use the provided current date unless the user clearly gives another date. Use payment method Other unless the user clearly says UPI, cash, or card.\n"
                + "Return JSON matching the requested schema. Current date: " + today + ". Expense text: " + description;
        Map<String, Object> promptPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(promptPart));
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(content),
                "generationConfig", Map.of("responseMimeType", "application/json", "responseSchema", requestSchema));

        try {
            JsonNode response = geminiClient.generateContent(model, apiKey, requestBody);

            return readExpense(getOutputText(response), today);
        } catch (RestClientResponseException exception) {
            logger.warn("Gemini request returned HTTP status {}", exception.getStatusCode().value());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI could not understand the expense right now. Please try again or enter it manually.");
        } catch (RestClientException exception) {
            logger.warn("Gemini request failed before receiving a response ({})",
                    exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI could not understand the expense right now. Please try again or enter it manually.");
        }
    }

    private ExpenseUnderstandingResponse readExpense(String outputText, LocalDate today) {
        try {
            JsonNode result = objectMapper.readTree(outputText);
            BigDecimal amount = new BigDecimal(result.path("amount").asText("0"));
            String description = result.path("description").asText("").trim();
            String category = result.path("category").asText("Other");
            String paymentMethod = result.path("paymentMethod").asText("Other");
            LocalDate date = LocalDate.parse(result.path("date").asText());
            String spellingSuggestion = result.path("spellingSuggestion").asText("").trim();

            if (!CATEGORIES.contains(category) || description.isBlank()) {
                throw new IllegalArgumentException("Invalid expense fields returned by AI");
            }
            if (!PAYMENT_METHODS.contains(paymentMethod)) paymentMethod = "Other";
            if (amount.signum() < 0) amount = BigDecimal.ZERO;

            return new ExpenseUnderstandingResponse(amount, description, category, paymentMethod, date, spellingSuggestion);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI returned expense details that could not be checked. Please enter them manually.");
        }
    }

    private String getOutputText(JsonNode response) {
        if (response == null) throw invalidOutput();
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        for (JsonNode part : parts) {
            if (part.has("text")) return part.path("text").asText("");
        }
        throw invalidOutput();
    }

    private ResponseStatusException invalidOutput() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "AI did not return expense details. Please enter them manually.");
    }
}
