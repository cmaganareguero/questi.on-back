package com.top.application.controller.implementations;

import com.top.application.controller.interfaces.UserProducerController;
import com.top.application.dto.UserReduceRequestDto;
import com.top.application.mapper.UserMapper;
import com.top.application.model.UserProducer;
import com.top.infraestructure.service.implementation.UserProducerServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserProducerControllerImpl implements UserProducerController {

    @Autowired
    UserProducerServiceImpl userService;
    @Autowired
    UserMapper userMapper;

    @Override
    @PostMapping(value ="/add")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void addUser(@Valid @RequestBody UserReduceRequestDto userRequestDto) {
        try {
            UserProducer question = userMapper.userDtoToUser(userRequestDto);
            userService.add(question);
        } catch (NullPointerException e ){
            log.error(e.getMessage());
        }
    }

    @Override
    @DeleteMapping(value="/delete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteUser(@RequestParam UUID idUser) {
        try {
            userService.delete(idUser);
        } catch (NullPointerException e ){
            log.error(e.getMessage());
        }
    }
}
