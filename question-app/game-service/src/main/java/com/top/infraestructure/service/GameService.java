package com.top.infraestructure.service;

import com.top.application.dto.*;
import com.top.application.mapper.GameMapper;
import com.top.application.model.Game;
import com.top.application.repository.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GameService {

    @Autowired
    GameRepository gameRepository;

    @Autowired
    GameMapper gameMapper;

    public Game getLastGame(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID de usuario no puede ser nulo");
        }

        Optional<Game> optionalGame = Optional.ofNullable(gameRepository.findFirstByIdUserOrderByDateDesc(userId));

        if (optionalGame.isPresent()) {
            return optionalGame.get();
        } else {
            return new Game();
        }
    }

    public Game deleteLastGame(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID de usuario no puede ser nulo");
        }

        Optional<Game> optionalGame = Optional.ofNullable(gameRepository.findFirstByIdUserOrderByDateDesc(userId));

        if (optionalGame.isPresent()) {
            Game deletedGame = optionalGame.get();
            gameRepository.delete(deletedGame);
            return deletedGame;
        } else {
            return new Game();
        }
    }



    public void updateGame(GameUpdateDto gameUpdateDto) {
        if (gameUpdateDto == null) {
            throw new IllegalArgumentException("El game no puede ser nulo");
        }

        Optional<Game> optionalGame = gameRepository.findById(gameUpdateDto.getIdGame());

        if (optionalGame.isPresent()) {
            Game gameExistent = optionalGame.get();
            gameExistent.setSuccesses(gameUpdateDto.getSuccesses());
            gameExistent.setFailures(gameExistent.getNumQuestions()-gameUpdateDto.getSuccesses());
            gameRepository.save(gameExistent);
        }
    }

    public MonthStatisticsDto calculateMonthlyStatistics(String idUser) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minus(1, ChronoUnit.MONTHS);

        // Obtener todos los juegos del último mes para el usuario
        List<Game> games = gameRepository.findByIdUserAndDateGreaterThanEqual(idUser, oneMonthAgo);
        System.out.println("iduser: " + idUser);  // Verifica si se están obteniendo juegos
        System.out.println("Juegos obtenidos: " + games);  // Verifica si se están obteniendo juegos

        for (Game game : games) {
            LocalDateTime gameDate = game.getDate();
            int weekOfYear = gameDate.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
            int year = gameDate.getYear();
            System.out.println("Fecha del juego: " + gameDate + ", Semana del año: " + weekOfYear + ", Año: " + year);
        }

        // Estructura para almacenar estadísticas semanales
        Map<String, WeekStatisticsDto> weekStatsMap = new TreeMap<>();

        // Calcular estadísticas por semana y día
        for (Game game : games) {
            LocalDateTime gameDate = game.getDate();
            int weekOfYear = gameDate.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
            int year = gameDate.getYear();

            // Formato "año-semana" para la clave del mapa
            String weekKey = String.format("%d-%02d", year, weekOfYear);

            // Obtener o inicializar las estadísticas de la semana
            WeekStatisticsDto weekStats = weekStatsMap.getOrDefault(weekKey, WeekStatisticsDto.builder().build());

            // Actualizar las estadísticas de la semana
            weekStats.setTotalWeekQuestionsAnswered(weekStats.getTotalWeekQuestionsAnswered() + game.getSuccesses() + game.getFailures());
            weekStats.setTotalWeekSuccesses(weekStats.getTotalWeekSuccesses() + game.getSuccesses());
            weekStats.setTotalWeekFailures(weekStats.getTotalWeekFailures() + game.getFailures());

            // Obtener o inicializar la lista de días para la semana actual
            List<DayWeekStatsDto> dayStatsList = weekStats.getDayStatsList();
            if (dayStatsList == null) {
                dayStatsList = new ArrayList<>();
                weekStats.setDayStatsList(dayStatsList);
            }

            // Formato "año-mes-día" para la clave del mapa
            String dayKey = gameDate.toLocalDate().toString();

            // Obtener o inicializar las estadísticas del día
            DayWeekStatsDto dayStats = dayStatsList.stream()
                    .filter(day -> day.getDay().equals(dayKey))
                    .findFirst()
                    .orElse(DayWeekStatsDto.builder().day(dayKey).build());

            // Actualizar las estadísticas del día
            dayStats.setTotalDayQuestionsAnswered(dayStats.getTotalDayQuestionsAnswered() + game.getSuccesses() + game.getFailures());
            dayStats.setTotalDaySuccesses(dayStats.getTotalDaySuccesses() + game.getSuccesses());
            dayStats.setTotalDayFailures(dayStats.getTotalDayFailures() + game.getFailures());

            // Agregar el día al listado de días de la semana
            if (!dayStatsList.contains(dayStats)) {
                dayStatsList.add(dayStats);
            }

            // Actualizar la semana en el mapa
            weekStatsMap.put(weekKey, weekStats);
        }

        // Calcular estadísticas mensuales
        int totalMonthQuestionsAnswered = 0;
        int totalMonthSuccesses = 0;
        int totalMonthFailures = 0;

        for (WeekStatisticsDto weekStats : weekStatsMap.values()) {
            totalMonthQuestionsAnswered += weekStats.getTotalWeekQuestionsAnswered();
            totalMonthSuccesses += weekStats.getTotalWeekSuccesses();
            totalMonthFailures += weekStats.getTotalWeekFailures();
        }

        // Calcular tasas de éxito y fallo mensual
        double successRate = calculateRate(totalMonthSuccesses, totalMonthQuestionsAnswered);
        double failureRate = calculateRate(totalMonthFailures, totalMonthQuestionsAnswered);

        // Construir y devolver el objeto MonthStatisticsDto
        return MonthStatisticsDto.builder()
                .totalQuestionsAnswered(totalMonthQuestionsAnswered)
                .totalSuccesses(totalMonthSuccesses)
                .totalFailures(totalMonthFailures)
                .successRate(successRate)
                .failureRate(failureRate)
                .build();

    }
    public List<MonthStatisticsDto> getMonthlyStatistics(String idUser) {
        // Mapa para almacenar los juegos agrupados por mes (clave será el mes en formato "yyyy-MM")
        Map<String, List<Game>> gamesByMonth = new TreeMap<>();

        // Formateador para obtener "yyyy-MM" a partir de una fecha
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        List<Game> games = gameRepository.findGamesByIdUser(idUser);
        System.out.println("iduser: " + idUser);  // Verifica si se están obteniendo juegos
        System.out.println("Juegos obtenidos: " + games);  // Verifica si se están obteniendo juegos

        // Agrupar los juegos por mes
        for (Game game : games) {
            String monthKey = game.getDate().format(monthFormatter);
            gamesByMonth.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(game);
        }

        // Lista para almacenar las estadísticas mensuales
        List<MonthStatisticsDto> monthlyStatistics = new ArrayList<>();

        // Calcular las estadísticas por cada mes
        for (Map.Entry<String, List<Game>> entry : gamesByMonth.entrySet()) {
            String month = entry.getKey();
            List<Game> gamesInMonth = entry.getValue();

            int totalMonthQuestionsAnswered = 0;
            int totalMonthSuccesses = 0;
            int totalMonthFailures = 0;

            for (Game game : gamesInMonth) {
                totalMonthQuestionsAnswered += game.getSuccesses() + game.getFailures();
                totalMonthSuccesses += game.getSuccesses();
                totalMonthFailures += game.getFailures();
            }

            double successRate = calculateRate(totalMonthSuccesses, totalMonthQuestionsAnswered);
            double failureRate = calculateRate(totalMonthFailures, totalMonthQuestionsAnswered);

            // Crear el DTO para el mes y agregarlo a la lista
            MonthStatisticsDto monthStats = MonthStatisticsDto.builder()
                    .month(month)  // Establecer el mes
                    .totalQuestionsAnswered(totalMonthQuestionsAnswered)
                    .totalSuccesses(totalMonthSuccesses)
                    .totalFailures(totalMonthFailures)
                    .successRate(successRate)
                    .failureRate(failureRate)
                    .build();

            monthlyStatistics.add(monthStats);
        }

        // Retornar la lista de estadísticas mensuales
        return monthlyStatistics;
    }


    public List<WeekStatisticsDto> getWeeklyStatistics(String idUser) {
        Map<String, WeekStatisticsDto> weekStatsMap = new TreeMap<>();

        List<Game> games = gameRepository.findGamesByIdUser(idUser);
        System.out.println("iduser: " + idUser);  // Verifica si se están obteniendo juegos
        System.out.println("Juegos obtenidos: " + games);  // Verifica si se están obteniendo juegos

        for (Game game : games) {
            LocalDateTime gameDate = game.getDate();
            int weekOfYear = gameDate.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
            int year = gameDate.getYear();
            String weekKey = String.format("%d-%02d", year, weekOfYear);

            WeekStatisticsDto weekStats = weekStatsMap.getOrDefault(weekKey, WeekStatisticsDto.builder().build());

            weekStats.setTotalWeekQuestionsAnswered(weekStats.getTotalWeekQuestionsAnswered() + game.getSuccesses() + game.getFailures());
            weekStats.setTotalWeekSuccesses(weekStats.getTotalWeekSuccesses() + game.getSuccesses());
            weekStats.setTotalWeekFailures(weekStats.getTotalWeekFailures() + game.getFailures());

            // Actualizar estadísticas por día
            List<DayWeekStatsDto> dayStatsList = weekStats.getDayStatsList();
            if (dayStatsList == null) {
                dayStatsList = new ArrayList<>();
                weekStats.setDayStatsList(dayStatsList);
            }

            String dayKey = gameDate.toLocalDate().toString();
            DayWeekStatsDto dayStats = dayStatsList.stream()
                    .filter(day -> day.getDay().equals(dayKey))
                    .findFirst()
                    .orElse(DayWeekStatsDto.builder().day(dayKey).build());

            dayStats.setTotalDayQuestionsAnswered(dayStats.getTotalDayQuestionsAnswered() + game.getSuccesses() + game.getFailures());
            dayStats.setTotalDaySuccesses(dayStats.getTotalDaySuccesses() + game.getSuccesses());
            dayStats.setTotalDayFailures(dayStats.getTotalDayFailures() + game.getFailures());

            if (!dayStatsList.contains(dayStats)) {
                dayStatsList.add(dayStats);
            }

            weekStatsMap.put(weekKey, weekStats);
        }

        // Calcular la tasa de éxito/fallo semanal
        for (WeekStatisticsDto weekStats : weekStatsMap.values()) {
            int totalQuestions = weekStats.getTotalWeekQuestionsAnswered();
            if (totalQuestions > 0) {
                double successRate = calculateRate(weekStats.getTotalWeekSuccesses(), totalQuestions);
                double failureRate = calculateRate(weekStats.getTotalWeekFailures(), totalQuestions);
                weekStats.setSuccessWeekRate(successRate);
                weekStats.setFailureWeekRate(failureRate);
            }
        }

        return new ArrayList<>(weekStatsMap.values());
    }

    public Map<String, DifficultyStatisticsDto> getStatisticsByDifficulty(String idUser) {

        List<Game> games = gameRepository.findGamesByIdUser(idUser);
        System.out.println("iduser: " + idUser);
        System.out.println("Juegos obtenidos: " + games);

        Map<String, DifficultyStatisticsDto> difficultyStatsMap = new HashMap<>();

        for (Game game : games) {
            String difficulty = game.getDifficulty();

            DifficultyStatisticsDto difficultyStats = difficultyStatsMap.getOrDefault(difficulty, new DifficultyStatisticsDto());

            difficultyStats.setTotalSuccesses(difficultyStats.getTotalSuccesses() + game.getSuccesses());
            difficultyStats.setTotalFailures(difficultyStats.getTotalFailures() + game.getFailures());
            difficultyStats.setTotalQuestionsAnswered(difficultyStats.getTotalQuestionsAnswered() + game.getSuccesses() + game.getFailures());

            difficultyStatsMap.put(difficulty, difficultyStats);
        }

        return difficultyStatsMap;
    }



    public List<UserGamesDto> getGamesByIdUser(String userEmail) {
        List<Game> games = gameRepository.findGamesByIdUser(userEmail);
        return games.stream()
                .map(game -> UserGamesDto.builder()
                        .name(game.getName())
                        .category(game.getCategory())
                        .difficulty(game.getDifficulty())
                        .answerType(game.getAnswerType())
                        .numQuestions(game.getNumQuestions())
                        .successes(game.getSuccesses())
                        .failures(game.getFailures())
                        .build())
                .collect(Collectors.toList());
    }

    public WeekStatisticsDto calculateWeeklyStatistics(String idUser) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minus(1, ChronoUnit.WEEKS);

        // Obtener todos los juegos de la última semana para el usuario
        List<Game> games = gameRepository.findByIdUserAndDateGreaterThanEqual(idUser, oneWeekAgo);

        // Estructura para almacenar estadísticas diarias
        Map<String, DayStatsDto> dayStatsMap = new TreeMap<>();

        // Calcular estadísticas por día
        for (Game game : games) {
            LocalDateTime gameDate = game.getDate();
            String dayKey = gameDate.toLocalDate().toString();

            // Obtener o inicializar las estadísticas del día
            DayStatsDto dayStats = dayStatsMap.getOrDefault(dayKey, new DayStatsDto());

            // Actualizar las estadísticas del día
            dayStats.setTotalDayQuestionsAnswered(dayStats.getTotalDayQuestionsAnswered() + game.getSuccesses() + game.getFailures());
            dayStats.setTotalDaySuccesses(dayStats.getTotalDaySuccesses() + game.getSuccesses());
            dayStats.setTotalDayFailures(dayStats.getTotalDayFailures() + game.getFailures());

            dayStatsMap.put(dayKey, dayStats); // Actualizar el mapa
        }

        // Calcular estadísticas semanales
        int totalWeekQuestionsAnswered = 0;
        int totalWeekSuccesses = 0;
        int totalWeekFailures = 0;

        for (DayStatsDto dayStats : dayStatsMap.values()) {
            totalWeekQuestionsAnswered += dayStats.getTotalDayQuestionsAnswered();
            totalWeekSuccesses += dayStats.getTotalDaySuccesses();
            totalWeekFailures += dayStats.getTotalDayFailures();
        }

        // Calcular tasas de éxito y fallo semanal
        double successRate = calculateRate(totalWeekSuccesses, totalWeekQuestionsAnswered);
        double failureRate = calculateRate(totalWeekFailures, totalWeekQuestionsAnswered);

        // Construir y devolver el objeto WeekStatisticsDto
        return WeekStatisticsDto.builder()
                .totalWeekQuestionsAnswered(totalWeekQuestionsAnswered)
                .totalWeekSuccesses(totalWeekSuccesses)
                .totalWeekFailures(totalWeekFailures)
                .successWeekRate(successRate)
                .failureWeekRate(failureRate)
                .build();
    }

    public DayStatsDto calculateDailyStatistics(String idUser) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();

        // Obtener todos los juegos del día para el usuario
        List<Game> games = gameRepository.findByIdUserAndDateGreaterThanEqual(idUser, startOfDay);

        // Calcular estadísticas diarias
        int totalDayQuestionsAnswered = 0;
        int totalDaySuccesses = 0;
        int totalDayFailures = 0;

        for (Game game : games) {
            totalDayQuestionsAnswered += game.getSuccesses() + game.getFailures();
            totalDaySuccesses += game.getSuccesses();
            totalDayFailures += game.getFailures();
        }

        // Calcular tasas de éxito y fallo diario
        double successRate = calculateRate(totalDaySuccesses, totalDayQuestionsAnswered);
        double failureRate = calculateRate(totalDayFailures, totalDayQuestionsAnswered);

        // Construir y devolver el objeto DayStatsDto con la lista de partidas (games) dentro
        return DayStatsDto.builder()
                .totalDayQuestionsAnswered(totalDayQuestionsAnswered)
                .totalDaySuccesses(totalDaySuccesses)
                .totalDayFailures(totalDayFailures)
                .successDayRate(successRate)
                .failureDayRate(failureRate)
                .games(games)
                .build();
    }

    // Método auxiliar para calcular la tasa de éxito o fallo
    private double calculateRate(int count, int total) {
        return total > 0 ? (double) count / total * 100 : 0;
    }

}
