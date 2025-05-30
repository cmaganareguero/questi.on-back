package com.top.application.controller;

import com.top.application.dto.AuthResponse;
import com.top.application.dto.AuthenticationRequest;
import com.top.application.client.UserConsumerClient;
import com.top.application.filter.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthorizationControllerImp {

    @Autowired
    private UserConsumerClient userConsumerClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest loginRequest) {
        boolean isAuthenticated = userConsumerClient.validateUser(loginRequest);
        if (isAuthenticated) {
            String token = jwtTokenProvider.createToken(loginRequest.getName());
            return ResponseEntity.ok(new AuthResponse(token));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }
}
