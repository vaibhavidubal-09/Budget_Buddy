package com.budgetbuddy.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void unexpectedErrorsUseSafeApiErrorWithoutInternalDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/example");
        var response = handler.handleUnexpectedException(
                new IllegalStateException("SQL password=secret; stack details"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("The request could not be completed", response.getBody().message());
        assertFalse(response.getBody().message().contains("secret"));
        assertEquals("/api/example", response.getBody().path());
    }
}
