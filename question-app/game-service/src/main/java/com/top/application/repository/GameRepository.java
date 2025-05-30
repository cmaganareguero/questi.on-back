package com.top.application.repository;

import com.top.application.model.Game;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface GameRepository extends MongoRepository<Game, String> {

    Game findFirstByIdUserOrderByDateDesc(String idUser);

    List<Game> findByIdUserAndDateGreaterThanEqual(String idUser, LocalDateTime date);

    List<Game> findGamesByIdUser(String idUser);


}
