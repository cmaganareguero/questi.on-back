package com.top;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class OpenAIApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenAIApplication.class, args);
    }
}