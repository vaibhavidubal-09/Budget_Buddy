package com.budgetbuddy.api.assistant;

import com.budgetbuddy.api.auth.JwtUserId;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantChatController {
    private final AssistantChatService assistantChatService;

    public AssistantChatController(AssistantChatService assistantChatService) {
        this.assistantChatService = assistantChatService;
    }

    @PostMapping("/chat")
    public AssistantChatResponse chat(@Valid @RequestBody AssistantChatRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return assistantChatService.reply(request, JwtUserId.from(jwt));
    }
}
