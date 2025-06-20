package com.top.infraestructure.service;

import com.top.application.model.UserConsumer;
import com.top.application.repository.UserRepository;
import com.top.avro.UserKey;
import com.top.avro.UserValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class UsersListener {

    @Autowired
    UserRepository userRepository;

    @KafkaListener(topics = "userscript", groupId = "userscript-service")
    public void consumeFromTopic2(ConsumerRecord<UserKey, UserValue> record) {
        try {
            UserKey key = record.key();
            UserValue value = record.value();

            if (value != null) {
                UserConsumer newUser = UserConsumer.builder()
                        .id(key.getId())
                        .name(value.getName())
                        .email(value.getEmail())
                        .password(value.getPassword())
                        .build();

                userRepository.save(newUser);
            } else {
                userRepository.deleteById(key.getId());
            }

            System.out.println("Received message from topic2 with key: " + key + " and value: " + value);
        } catch (Exception e) {
            log.debug(e.getMessage());
        }

    }
}
