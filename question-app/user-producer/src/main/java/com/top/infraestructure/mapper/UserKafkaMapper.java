package com.top.infraestructure.mapper;


import com.top.application.model.UserProducer;
import org.mapstruct.Mapper;
import com.top.avro.UserValue;

@Mapper(componentModel = "spring")
public interface UserKafkaMapper {
    UserValue userToUserValue(UserProducer user);
}
