package com.budgetbuddy.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

class SecurityConfigJwtValidationTest {

    private final SecretKey key = new SecretKeySpec(new byte[32], "HmacSHA256");
    private JwtEncoder encoder;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig();
        encoder = config.jwtEncoder(key);
        decoder = config.jwtDecoder(key);
    }

    @Test
    void acceptsValidBudgetBuddyToken() {
        var decoded = decoder.decode(token("budgetbuddy", Instant.now().plusSeconds(60)));

        assertEquals("budgetbuddy", decoded.getClaimAsString("iss"));
    }

    @Test
    void rejectsTokenWithUnexpectedIssuer() {
        assertThrows(JwtException.class,
                () -> decoder.decode(token("another-issuer", Instant.now().plusSeconds(60))));
    }

    @Test
    void rejectsExpiredToken() {
        assertThrows(JwtException.class,
                () -> decoder.decode(token("budgetbuddy", Instant.now().minusSeconds(60))));
    }

    private String token(String issuer, Instant expiresAt) {
        Instant now = Instant.now();
        Instant issuedAt = expiresAt.isBefore(now) ? expiresAt.minusSeconds(60) : now.minusSeconds(1);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("user@example.com")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("userId", 1L)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
