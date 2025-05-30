package com.top.infraestructure.service.interfaces;

import com.top.application.dto.RequestAPIDto;
import com.top.application.model.UserProducer;

import java.util.UUID;

public interface RequestAPIService {

    public void sendRequestToAPI(RequestAPIDto requestAPIDto);
}
