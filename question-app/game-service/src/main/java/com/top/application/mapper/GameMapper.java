package com.top.application.mapper;

import com.top.application.dto.GameProducerRequestDto;
import com.top.application.dto.GameUpdateDto;
import com.top.application.model.Game;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GameMapper {

    List<GameProducerRequestDto> modelToDtoList (List<Game> people);

    Game gameDtoToGame(GameProducerRequestDto gameRequestDto);

    Game gameUpdateDtoToGame(GameUpdateDto gameUpdateDto);


}
