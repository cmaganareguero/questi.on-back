package com.top.infraestructure.mapper;

import com.top.application.dto.RequestAPIDto;
import com.top.avro.GenerateQuestionRequestValue;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RequestAPIKafkaMapper {

    GenerateQuestionRequestValue dtoToGenerateQuestionRequestValue (RequestAPIDto requestAPIDto);
}
