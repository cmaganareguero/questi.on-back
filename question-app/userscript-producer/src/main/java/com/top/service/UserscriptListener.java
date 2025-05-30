package com.top.service;

import com.top.avro.UserKey;
import com.top.avro.UserValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Log4j2
public class UserscriptListener {
    final String USERSCRIPT_TOPIC = "userscript";

    @Autowired
    private KafkaTemplate<UserKey, UserValue> kafkaTemplate;

    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserscriptListener(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @KafkaListener(topics = "users", groupId = "producer-userscript")
    public void consumeFromTopicUsers(ConsumerRecord<UserKey, UserValue> record) {

        try {
            UserValue userValue = record.value();

            String encryptedPassword = passwordEncoder.encode(userValue.getPassword());

            userValue.setPassword(encryptedPassword);

            log.info("Received message from topic 'users': {}", userValue);

            log.debug("Sending movie to users-script topic on Kafka where UserKey: {} | UserValue : {}", record.key(), userValue);
            kafkaTemplate.send(USERSCRIPT_TOPIC, record.key(), userValue);

        } catch (Exception e) {
            log.error("Error processing message from topic 'users'", e);
        }
    }

}
