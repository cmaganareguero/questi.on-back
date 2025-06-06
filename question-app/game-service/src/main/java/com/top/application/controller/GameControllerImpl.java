package com.top.application.controller;

import com.top.application.dto.*;
import com.top.application.mapper.GameMapper;
import com.top.application.model.Game;
import com.top.infraestructure.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    public Game getLastGame(@RequestParam String idUser, @RequestParam String category) {
        try {
            return gameService.getLastGame(idUser, category);
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
    /*
    @GetMapping(value = "/getMonthlyStatistics")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<MonthStatsDto> getMonthlyStatistics(@RequestParam String idUser) {
        try {
            return gameService.getMonthlyStatistics(idUser);
        } catch (NullPointerException e) {
            log.error(e.getMessage());
        }
        return null;
    }
    */
    /*
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
    */

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
    public Game deleteLastGame(@PathVariable String userId, @PathVariable String category) {
        try {
            return gameService.deleteGameInProgress(userId, category);
        } catch (NullPointerException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @GetMapping(value = "/getEmbeddingsFromRecentGames")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<List<Float>> getEmbeddingsFromRecentGames(@RequestParam String category) {
        try {
            return gameService.getEmbeddingsFromRecentGames(category, 5);
        } catch (NullPointerException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @GetMapping("/gamesPlayedStats")
    public ResponseEntity<List<MonthGamesDto>> getMonthlyGamesCount(@RequestParam String idUser) {
        List<MonthGamesDto> stats = gameService.getGamesCountPerMonthCurrentYear(idUser);
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }

    @GetMapping("/getTotalsStatistics")
    public ResponseEntity<StatsGeneralesDto> getTotalsStatistics(@RequestParam String idUser) {
        try {
            StatsGeneralesDto totals = gameService.getTotalsStatistics(idUser);
            return ResponseEntity.ok(totals);
        } catch (Exception e) {
            log.error("Error en getTotalsStatistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/getMonthlySuccessFailure")
    public ResponseEntity<List<MonthStatsDto>> getMonthlySuccessFailure(@RequestParam String idUser) {
        try {
            List<MonthStatsDto> stats = gameService.getSuccessFailureByMonthCurrentYear(idUser);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error en getMonthlySuccessFailure: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

    