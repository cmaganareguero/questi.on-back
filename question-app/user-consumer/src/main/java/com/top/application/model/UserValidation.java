package com.top.application.model;

import com.top.application.dto.UserAuthorization;
import com.top.application.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class   UserValidation {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public String authenticate(UserAuthorization loginRequest) {
        UserConsumer user = userRepository.findUserByEmail(loginRequest.getName());
        if (user != null && bCryptPasswordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return user.getId();
        } else
        { return "";}
    }
}
