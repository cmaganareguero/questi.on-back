package com.top.infraestructure.service.implementation;

import com.top.avro.GenerateQuestionResponseKey;
import com.top.avro.GenerateQuestionResponseValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QuestionListenerServiceImpl {

    @Autowired
    private KafkaTemplate<GenerateQuestionResponseKey, GenerateQuestionResponseKey> kafkaTemplate;

    @KafkaListener(topics = "api-response", groupId = "user-vote-video")
    public void consumeFromTopicUser(ConsumerRecord<GenerateQuestionResponseKey, GenerateQuestionResponseValue> record) {

        try {
        GenerateQuestionResponseKey key = record.key();
        GenerateQuestionResponseValue value = record.value();

            System.out.println("Received message from topic users with key: " + key + " and value: " + value);
        } catch (Exception e) {
            log.debug(e.getMessage());
        }

    }
}

