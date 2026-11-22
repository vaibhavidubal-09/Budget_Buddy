package com.budgetbuddy.api.assistant;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantChatRequest(
        @NotBlank @Size(max = 1200)
        String message,
        @Size(max = 16)
        List<AssistantChatTurn> history) { }
