package com.top.application.mapper;

import com.top.application.dto.UserProducerRequestDto;
import com.top.application.dto.UserReduceRequestDto;
import com.top.application.model.UserProducer;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    List<UserProducerRequestDto> modelToDtoList (List<UserProducer> people);

    UserProducer userDtoToUser(UserReduceRequestDto questionRequestDto);

}
