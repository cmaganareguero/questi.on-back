package com.top.application.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.top.infraestructure.service.APIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/question")
@Slf4j
public class APIControllerImpl {

    @Autowired
    APIService chatGPTService;

    @GetMapping(value ="/getMoreInfo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String getMoreInfo(@RequestParam String question, @RequestParam String correctAnswer) throws JsonProcessingException {
        try {
            return chatGPTService.getMoreInfo(question,correctAnswer);
        } catch (NullPointerException e) {
            log.error(e.getMessage());
            return "No se pudo obtener respuesta";
        }
    }

}
