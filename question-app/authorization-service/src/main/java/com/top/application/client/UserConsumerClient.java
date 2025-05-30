package com.top.application.client;

import com.top.application.dto.AuthenticationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-consumer", url = "http://localhost:7789")
public interface  UserConsumerClient {

    @PostMapping("/users/validate")
    boolean validateUser(@RequestBody AuthenticationRequest loginRequest);
}
