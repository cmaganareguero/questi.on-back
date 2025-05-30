package com.top.infraestructure.mapper;


import com.top.application.model.Game;
import org.mapstruct.Mapper;
import com.top.avro.GameValue;

@Mapper(componentModel = "spring")
public interface GameKafkaMapper {
    GameValue gameToGameValue(Game game);

    Game gameValueToGame(GameValue game);
}
