package com.top.application.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "game-service", url = "http://localhost:7785")
public interface GameServiceFeignClient {

    @GetMapping("/game/getEmbeddingsFromRecentGames")
    List<List<Float>> getEmbeddingsFromRecentGames(@RequestParam("category") String category);
}