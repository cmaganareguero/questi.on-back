package com.top.application.controller.implementations;

import com.top.application.controller.interfaces.RequestAPIController;
import com.top.application.dto.RequestAPIDto;
import com.top.application.model.UserProducer;
import com.top.infraestructure.service.implementation.RequestAPIServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/questions")
@Slf4j
public class RequestAPIControllerImpl implements RequestAPIController {

    @Autowired
    RequestAPIServiceImpl requestAPIService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<Integer> sendRequestToAPI(@RequestBody RequestAPIDto requestAPIDto) {
        try {
            requestAPIService.sendRequestToAPI(requestAPIDto);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(202);
        } catch (NullPointerException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(500);
        }
    }
}

