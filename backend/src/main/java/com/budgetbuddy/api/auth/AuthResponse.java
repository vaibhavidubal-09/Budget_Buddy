package com.budgetbuddy.api.auth;

public record AuthResponse(String token, Long id, String fullName, String email) {

    @Override
    public String toString() {
        return "AuthResponse[token=[REDACTED], id=[REDACTED], fullName=[REDACTED], email=[REDACTED]]";
    }
}
