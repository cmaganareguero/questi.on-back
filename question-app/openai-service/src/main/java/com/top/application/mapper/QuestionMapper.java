package com.top.application.mapper;

import com.top.application.dto.GenerateQuestionsDto;
import com.top.application.model.Question;
import com.top.avro.GenerateQuestionResponseValue;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    List<com.top.avro.Question> mapToAvroQuestions(List<Question> questions);

}
