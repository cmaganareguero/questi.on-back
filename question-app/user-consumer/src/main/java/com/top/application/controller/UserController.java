package com.top.application.controller;

import com.top.application.dto.UserAuthorization;
import com.top.application.dto.UserProfileDto;
import com.top.application.model.UserConsumer;
import com.top.application.repository.UserRepository;
import com.top.infraestructure.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    @GetMapping("/getUserIdByEmail")
    public String getUserIdByEmail(@RequestParam String userEmail) {
        String idUser = userService.getUserIdByEmail(userEmail);
        if (idUser != null) {
            return idUser;
        } else {
            throw new RuntimeException("Usuario no encontrado");
        }
    }

    @GetMapping("/getUserDetailsByEmail")
    public UserProfileDto getUserDetailsByEmail(@RequestParam String userEmail) {
        return userService.getUserDetailsByEmail(userEmail);
    }


}
