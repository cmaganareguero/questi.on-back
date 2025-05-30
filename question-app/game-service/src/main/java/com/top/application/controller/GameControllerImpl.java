package com.top.application.controller;

import com.top.application.dto.*;
import com.top.application.mapper.GameMapper;
import com.top.application.model.Game;
import com.top.infraestructure.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.repository.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/game")
@Slf4j
public class GameControllerImpl implements GameController {

    @Autowired
    GameService gameService;
    @Autowired
    GameMapper gameMapper;

    @GetMapping(value = "/getLastGame")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Game getLastGame(@RequestParam String idUser) {
        try {
            return gameService.getLastGame(idUser);
        } catch (NullPointerException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @PostMapping(value = "/updateGame")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void updateGame(@RequestBody GameUpdateDto gameUpdateDto) {
        try {
            gameService.updateGame(gameUpdateDto);
        } catch (NullPointerException e) {
            log.error(e.getMessage());
        }
    }

    @GetMapping(value = "/getMonthlyStatistics")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<MonthStatisticsDto> getMonthlyStatistics(@RequestParam String idUser) {
        try {
            return gameService.getMonthlyStatistics(idUser);
        } catch (NullPointerException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @GetMapping(value = "/getWeeklyStats")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WeekStatisticsDto getWeeklyStats(@RequestParam String idUser) {
        try {
            return gameService.calculateWeeklyStatistics(idUser);
        } catch (NullPointerException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @GetMapping(value = "/getStatisticsByDifficulty")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, DifficultyStatisticsDto> getStatisticsByDifficulty(@RequestParam String idUser) {
        try {
            // Obtener las estadísticas agrupadas por dificultad
            Map<String, DifficultyStatisticsDto> statsByDifficulty = gameService.getStatisticsByDifficulty(idUser);

            // Calcular las tasas para cada dificultad
            statsByDifficulty.values().forEach(DifficultyStatisticsDto::calculateRates);

            return statsByDifficulty;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return Collections.emptyMap();
    }

    @GetMapping(value = "/getDailyStats")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DayStatsDto getDailyStats(@RequestParam String idUser) {
        try {
            return gameService.calculateDailyStatistics(idUser);
        } catch (NullPointerException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @DeleteMapping("/deleteLastGame/{userId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Game deleteLastGame(@PathVariable String userId) {
        try {
            return gameService.deleteLastGame(userId);
        } catch (NullPointerException e) {
            log.error(e.getMessage());
        }
        return null;
    }

}

    