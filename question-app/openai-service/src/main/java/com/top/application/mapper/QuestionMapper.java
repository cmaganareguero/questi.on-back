package com.top.application.mapper;

import com.top.application.model.Game;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    List<com.top.avro.Question> mapToAvroQuestions(List<Game.Question> questions);

}
