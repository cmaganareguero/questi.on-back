package com.top.infraestructure.service.implementation;

import com.top.application.dto.RequestAPIDto;
import com.top.avro.GenerateQuestionRequestKey;
import com.top.avro.GenerateQuestionRequestValue;
import com.top.infraestructure.mapper.RequestAPIKafkaMapper;
import com.top.infraestructure.service.interfaces.RequestAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class RequestAPIServiceImpl implements RequestAPIService {

    final String API_REQUEST_TOPIC = "api-request";
    @Autowired
    RequestAPIKafkaMapper requestAPIKafkaMapper;
    @Autowired
    private KafkaTemplate<GenerateQuestionRequestKey, GenerateQuestionRequestValue> kafkaTemplate;
    @Override
    public void sendRequestToAPI(RequestAPIDto requestAPIDto) {

        GenerateQuestionRequestKey generateQuestionRequestKey = new GenerateQuestionRequestKey();
        generateQuestionRequestKey.setId(requestAPIDto.getIdUser());

        GenerateQuestionRequestValue generateQuestionRequestValue = requestAPIKafkaMapper.dtoToGenerateQuestionRequestValue(requestAPIDto);
        log.debug("Send request to generate questions: {}", generateQuestionRequestValue);
        kafkaTemplate.send(API_REQUEST_TOPIC, generateQuestionRequestKey, generateQuestionRequestValue);
    }
}



