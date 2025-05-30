package com.top.infraestructure.service.interfaces;

import com.top.application.model.UserProducer;

import java.util.UUID;

public interface UserProducerService {

    public void add(UserProducer user);

    public void delete(UUID idUser);
}
