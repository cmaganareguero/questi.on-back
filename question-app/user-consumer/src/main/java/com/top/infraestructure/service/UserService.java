package com.top.infraestructure.service;

import com.top.application.dto.UserProfileDto;
import com.top.application.exception.UserNotFoundException;
import com.top.application.model.UserConsumer;
import com.top.application.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public String getUserIdByEmail(String email) {
        UserConsumer user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        if (user != null) {
            return user.getId().toString();
        } else {
            throw new RuntimeException("Usuario no encontrado");
        }
    }

    public UserProfileDto getUserDetailsByEmail(String email) {
        UserConsumer user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return UserProfileDto.builder()
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
