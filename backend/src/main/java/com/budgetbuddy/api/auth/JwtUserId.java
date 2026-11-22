package com.budgetbuddy.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

public final class JwtUserId {

    private JwtUserId() { }

    public static Long from(Jwt jwt) {
        Object value = jwt.getClaim("userId");
        if (value instanceof Number number) return number.longValue();
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token does not contain a user id");
    }
}
