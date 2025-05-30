package com.top.infraestructure.service.implementation;

import com.top.application.mapper.UserMapper;
import com.top.application.model.UserProducer;
import com.top.application.model.UserProducer;
import com.top.avro.UserKey;
import com.top.avro.UserValue;
import com.top.infraestructure.mapper.UserKafkaMapper;
import com.top.infraestructure.service.interfaces.UserProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
@Slf4j
public class UserProducerServiceImpl implements UserProducerService {
    final String USERS_TOPIC = "users";
    @Autowired
    UserKafkaMapper userKafkaMapper;
    @Autowired
    UserKafkaMapper userMapper;

    @Autowired
    private KafkaTemplate<UserKey, UserValue> kafkaTemplate;
    public void add(UserProducer user) {

        UserKey userKey = new UserKey();
        userKey.setId(UUID.randomUUID().toString());

        UserValue userValue = new UserValue();
        userValue.setName(user.getName());
        userValue.setEmail(user.getEmail());
        userValue.setPassword(user.getPassword());
        userValue.setGender("");
        userValue.setPlayerType("");
        userValue.setBirthDate("");
        userValue.setGames(new ArrayList<>());

        log.debug("Añadir nuevo usuario: {}", userValue);
        kafkaTemplate.send(USERS_TOPIC, userKey, userValue);
    }

    public void delete(UUID idUser) {
        UserKey userKey = new UserKey(idUser.toString());
        log.debug("Borrar usuario");
        kafkaTemplate.send(USERS_TOPIC, userKey, null);
    }

}
