package com.top.application.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JwtTokenVerifier {

    @Autowired
    private JwtDecoder jwtDecoder;

    public boolean verifyToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Instant expirationTime = jwt.getExpiresAt();
            Instant now = Instant.now();

            return expirationTime == null || !expirationTime.isBefore(now); // El token ha caducado

        } catch (Exception e) {
            return false;
        }
    }
}
