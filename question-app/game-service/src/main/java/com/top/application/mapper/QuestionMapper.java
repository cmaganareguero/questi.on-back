package com.top.application.mapper;

import com.top.application.dto.GameProducerRequestDto;
import com.top.application.dto.GameUpdateDto;
import com.top.application.dto.QuestionEmbeddingDto;
import com.top.application.model.Game;
import com.top.application.model.Question;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    List<QuestionEmbeddingDto> modelToDtoList (List<Question> people);

}
