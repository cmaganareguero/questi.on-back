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

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthorizationControllerImp {

    @Autowired
    private UserConsumerClient userConsumerClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthenticationRequest loginRequest) {
        return Optional
                .ofNullable(userConsumerClient.validateUser(loginRequest))               // Optional<String> con el id de usuario (o vacío si es null)
                .map(idUser -> {                                                         // si existe idUser, creamos el token y el ResponseEntity
                    String token = jwtTokenProvider.createToken(idUser);
                    return ResponseEntity.ok(new AuthResponse(token));
                })
                .orElse(null);                                                           // si no hay idUser, devolvemos null
    }

}
