package com.top.application.client;

import com.top.application.model.Game;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "game-service", url = "http://localhost:7785")
public interface GameServiceFeignClient {

    @GetMapping("/game/getEmbeddingsFromRecentGames")
    List<List<Float>> getEmbeddingsFromRecentGames(@RequestParam("category") String category);
    @GetMapping("/game/getEmbeddingsFromRecentGames")
    List<Game.Question> getFailedQuestions(@RequestParam("userId") String idUser, @RequestParam("category") String category);
    @GetMapping("/game/getOtherUsersQuestions")
    List<Game.Question> getOtherUsersQuestions(@RequestParam("userId") String idUser, @RequestParam("category") String category);
}