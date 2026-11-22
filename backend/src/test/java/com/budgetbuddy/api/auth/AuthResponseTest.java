package com.budgetbuddy.api.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuthResponseTest {

    @Test
    void stringRepresentationDoesNotExposeTokenOrPersonalDetails() {
        String token = "signed.jwt.value";
        String email = "private@example.com";
        String fullName = "Private User";

        String safeRepresentation = new AuthResponse(token, 42L, fullName, email).toString();

        assertFalse(safeRepresentation.contains(token));
        assertFalse(safeRepresentation.contains(email));
        assertFalse(safeRepresentation.contains(fullName));
        assertTrue(safeRepresentation.contains("[REDACTED]"));
    }

    @Test
    void redactedStringRepresentationDoesNotChangeJsonResponse() throws Exception {
        String token = "signed.jwt.value";
        String json = new tools.jackson.databind.ObjectMapper()
                .writeValueAsString(new AuthResponse(token, 42L, "Private User", "private@example.com"));

        assertTrue(json.contains("\"token\":\"" + token + "\""));
    }
}
