package com.top.application.mapper;

import com.top.application.dto.UserDto;
import com.top.application.model.UserConsumer;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    List<UserDto> modelToDtoList (List<UserConsumer> people);
}
