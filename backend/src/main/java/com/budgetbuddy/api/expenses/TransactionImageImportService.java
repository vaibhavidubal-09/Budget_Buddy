package com.budgetbuddy.api.expenses;

import java.io.IOException;
import java.time.Clock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TransactionImageImportService {

    private static final List<String> CATEGORIES = List.of(
            "Food & Dining", "Transportation", "Shopping", "Education",
            "Entertainment", "Bills & Subscriptions", "Healthcare", "Other");
    private static final List<String> PAYMENT_METHODS = List.of("UPI", "Cash", "Card", "Other");

    private final GeminiApiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final Clock clock;

    public TransactionImageImportService(
            GeminiApiClient geminiClient,
            @Value("${budgetbuddy.gemini.api-key:}") String apiKey,
            @Value("${budgetbuddy.gemini.model:gemini-3.8-flash}") String model,
            Clock applicationClock) {
        this.geminiClient = geminiClient;
        this.apiKey = apiKey;
        this.model = model;
        this.clock = applicationClock;
    }

    public List<ExpenseUnderstandingResponse> importDocument(MultipartFile file) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI is not configured.");
        }
        if (file.isEmpty() || !List.of("image/png", "image/jpeg", "image/webp", "application/pdf").contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a PDF, PNG, JPG, or WEBP document.");
        }

        Map<String, Object> transactionSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "amount", Map.of("type", "number"),
                        "description", Map.of("type", "string"),
                        "category", Map.of("type", "string", "enum", CATEGORIES),
                        "paymentMethod", Map.of("type", "string", "enum", PAYMENT_METHODS),
                        "date", Map.of("type", "string", "description", "Transaction date as YYYY-MM-DD")),
                "required", List.of("amount", "description", "category", "paymentMethod", "date"));
        Map<String, Object> responseSchema = Map.of(
                "type", "object",
                "properties", Map.of("transactions", Map.of("type", "array", "items", transactionSchema)),
                "required", List.of("transactions"));

        try {
            Map<String, Object> imagePart = Map.of("inlineData", Map.of(
                    "mimeType", file.getContentType(), "data", Base64.getEncoder().encodeToString(file.getBytes())));
            Map<String, Object> textPart = Map.of("text",
                    "Read this Google Pay or bank transaction screenshot or PDF statement. Extract only completed outgoing payments, not balances, incoming payments, or promotional text. "
                    + "For each transaction, return amount in rupees, a short merchant/item description, category from the schema, payment method, and date as YYYY-MM-DD. "
                    + "Use today's date " + LocalDate.now(clock) + " only when the screenshot clearly says today; otherwise infer the displayed date. "
                    + "If a field cannot be read reliably, omit that transaction. Return an empty transactions array if none can be read.");
            Map<String, Object> content = Map.of("parts", List.of(textPart, imagePart));
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(content),
                    "generationConfig", Map.of("responseMimeType", "application/json", "responseSchema", responseSchema));

            JsonNode response = geminiClient.generateContent(model, apiKey, requestBody);
            return parseTransactions(getOutputText(response));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded image.");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini could not read that image. Try a clearer screenshot.");
        }
    }

    private List<ExpenseUnderstandingResponse> parseTransactions(String outputText) {
        try {
            JsonNode transactions = objectMapper.readTree(outputText).path("transactions");
            java.util.ArrayList<ExpenseUnderstandingResponse> result = new java.util.ArrayList<>();
            for (JsonNode transaction : transactions) {
                String description = transaction.path("description").asText("").trim();
                String category = transaction.path("category").asText("Other");
                String paymentMethod = transaction.path("paymentMethod").asText("Other");
                if (description.isBlank() || !CATEGORIES.contains(category)) continue;
                if (!PAYMENT_METHODS.contains(paymentMethod)) paymentMethod = "Other";
                BigDecimal amount = new BigDecimal(transaction.path("amount").asText("0"));
                LocalDate date = LocalDate.parse(transaction.path("date").asText());
                if (amount.signum() <= 0) continue;
                result.add(new ExpenseUnderstandingResponse(amount, description, category, paymentMethod, date, ""));
            }
            if (result.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "No readable completed transactions were found in that image.");
            }
            return result;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini could not extract checked transaction details from that image.");
        }
    }

    private String getOutputText(JsonNode response) {
        if (response != null) {
            for (JsonNode part : response.path("candidates").path(0).path("content").path("parts")) {
                if (part.has("text")) return part.path("text").asText("");
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned no transaction details.");
    }
}
