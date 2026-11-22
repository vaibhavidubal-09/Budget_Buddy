package com.budgetbuddy.api.expenses;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class CategorySuggestionService {

    private static final List<String> ALLOWED_CATEGORIES = List.of(
            "Food & Dining",
            "Transportation",
            "Shopping",
            "Education",
            "Entertainment",
            "Bills & Subscriptions",
            "Healthcare",
            "Other"
    );

    private final GeminiApiClient geminiClient;
    private final String apiKey;
    private final String model;

    public CategorySuggestionService(
            GeminiApiClient geminiClient,
            @Value("${budgetbuddy.gemini.api-key:}") String apiKey,
            @Value("${budgetbuddy.gemini.model:gemini-3.8-flash}") String model) {
        this.geminiClient = geminiClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    public CategorySuggestion suggest(String description) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI is not configured yet. Please select a category manually.");
        }

        Map<String, Object> schema = Map.of("type", "string", "enum", ALLOWED_CATEGORIES);
        String prompt = "You categorize personal finance expenses. Infer the intended item when the description has a minor spelling mistake. "
                + "Examples: sanitary pads or menstrual products -> Healthcare; medicine or a clinic visit -> Healthcare; bus fare or an Uber ride -> Transportation; groceries or a restaurant meal -> Food & Dining. "
                + "Choose exactly one category from this list: "
                + String.join(", ", ALLOWED_CATEGORIES)
                + ". Return only the exact category name. If the description is unclear, return Other.\nExpense: " + description;
        Map<String, Object> promptPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(promptPart));
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(content),
                "generationConfig", Map.of("responseMimeType", "application/json", "responseSchema", schema));

        try {
            JsonNode response = geminiClient.generateContent(model, apiKey, requestBody);

            String suggestedCategory = getOutputText(response).trim();
            if (!ALLOWED_CATEGORIES.contains(suggestedCategory)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "The AI returned an unexpected category. Please choose one manually.");
            }

            return new CategorySuggestion(suggestedCategory,
                    "AI suggestion. Please review it before saving.", "Gemini");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI category suggestions are temporarily unavailable. Please choose a category manually.");
        }
    }

    public CategorySuggestion suggestBudgetCategory(String period, Map<String, BigDecimal> spendingByCategory,
                                                     List<String> requestedCategories) {
        List<String> candidates = requestedCategories.stream().distinct()
                .filter(ALLOWED_CATEGORIES::contains).toList();
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There are no available budget categories.");
        }
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI is not configured yet. Please choose a category manually.");
        }

        String spending = spendingByCategory.entrySet().stream()
                .filter(entry -> ALLOWED_CATEGORIES.contains(entry.getKey()) && entry.getValue() != null)
                .map(entry -> entry.getKey() + ": ₹" + entry.getValue())
                .reduce((first, second) -> first + "; " + second)
                .orElse("No recorded spending yet");
        String prompt = "Recommend one category for a " + period + " spending budget, based on the user's recorded spending. "
                + "Prefer a category with higher spending, and choose only from the available categories. "
                + "If there is no history, choose the first available category. Return only the exact category name.\n"
                + "Spending totals: " + spending + "\nAvailable categories: " + String.join(", ", candidates);

        Map<String, Object> schema = Map.of("type", "string", "enum", candidates);
        Map<String, Object> content = Map.of("parts", List.of(Map.of("text", prompt)));
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(content),
                "generationConfig", Map.of("responseMimeType", "application/json", "responseSchema", schema));
        try {
            JsonNode response = geminiClient.generateContent(model, apiKey, requestBody);
            String category = getOutputText(response).trim();
            if (!candidates.contains(category)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "The AI returned an unexpected category. Please choose one manually.");
            }
            return new CategorySuggestion(category, "AI suggestion based on your spending. Review it before saving.", "Gemini");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI category suggestions are temporarily unavailable. Please choose one manually.");
        }
    }

    private String getOutputText(JsonNode response) {
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The AI did not return a category. Please choose one manually.");
        }

        for (JsonNode part : response.path("candidates").path(0).path("content").path("parts")) {
            if (part.has("text")) return part.path("text").asText("");
        }

        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "The AI did not return a category. Please choose one manually.");
    }
}
