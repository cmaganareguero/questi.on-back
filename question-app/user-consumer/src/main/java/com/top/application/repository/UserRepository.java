package com.top.application.repository;

import com.top.application.model.UserConsumer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserConsumer, String> {
    Optional<UserConsumer> findByEmail(String email);

    UserConsumer findUserByEmail(String email);

}
