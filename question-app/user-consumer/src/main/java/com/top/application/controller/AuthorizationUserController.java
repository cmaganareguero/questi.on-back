package com.top.application.controller;


import com.top.application.dto.UserAuthorization;
import com.top.application.model.UserValidation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Slf4j
public class AuthorizationUserController {

    @Autowired
    UserValidation userValidation;

    @PostMapping("/validate")
    public ResponseEntity<String> validateUser(@RequestBody UserAuthorization loginRequest) {
        String userId = userValidation.authenticate(loginRequest);
        return ResponseEntity.ok(userId);
    }


}


