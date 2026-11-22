package com.budgetbuddy.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.mock;

class AuthServiceTest {

    @Test
    void concurrentDuplicateEmailIsReturnedAsConflict() {
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtTokenService);
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Student");
        request.setEmail("student@example.com");
        request.setPassword("StrongPass9!");

        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass9!")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(UserAccount.class)))
                .thenThrow(new DataIntegrityViolationException("Unique email constraint"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("An account with this email already exists", exception.getReason());
        verify(userRepository).saveAndFlush(any(UserAccount.class));
    }
}
