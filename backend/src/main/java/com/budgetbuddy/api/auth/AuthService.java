package com.budgetbuddy.api.auth;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserAccountRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw duplicateEmail();
        }

        UserAccount user = new UserAccount(request.getFullName().trim(), email,
                passwordEncoder.encode(request.getPassword()));
        try {
            // Flush here so a concurrent duplicate email is detected before returning success.
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateEmail();
        }
        return createResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return createResponse(user);
    }

    private AuthResponse createResponse(UserAccount user) {
        return new AuthResponse(jwtTokenService.createToken(user), user.getId(), user.getFullName(), user.getEmail());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
    }

    private ResponseStatusException duplicateEmail() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
    }
}
