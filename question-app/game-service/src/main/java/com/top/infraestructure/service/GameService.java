package com.top.infraestructure.service;

import com.top.application.dto.*;
import com.top.application.interfaces.GameState;
import com.top.application.mapper.GameMapper;
import com.top.application.model.Game;
import com.top.application.model.Question;
import com.top.application.repository.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    public Game getLastGame(String userId, String category) {

        System.out.println("Recibido isUser: " + userId + " y categoria: " + category);


        Game inProgressGame = gameRepository.findFirstByIdUserAndCategoryAndGameState(userId, category, String.valueOf(GameState.INPROGRESS));

        System.out.println("Partida en progreso: " + inProgressGame);

        return (inProgressGame != null) ? inProgressGame : new Game();

    }

    public Game deleteGameInProgress(String userId, String category) {


        Game inProgressGame = getLastGame(userId, category);

        System.out.println("Partida en progreso: " + inProgressGame);

        if (inProgressGame != null && inProgressGame.getId() != null) {
            gameRepository.delete(inProgressGame);
            return inProgressGame;
        }

        return new Game();
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
            gameExistent.setGameState(String.valueOf(GameState.COMPLETED));
            gameRepository.save(gameExistent);
        }
    }

    /*
    public MonthStatsDto calculateMonthlyStatistics(String idUser) {
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
        return MonthStatsDto.builder()
                .totalQuestionsAnswered(totalMonthQuestionsAnswered)
                .totalSuccesses(totalMonthSuccesses)
                .totalFailures(totalMonthFailures)
                .successRate(successRate)
                .failureRate(failureRate)
                .build();

    }
    /*
    public List<MonthStatsDto> getMonthlyStatistics(String idUser) {
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
        List<MonthStatsDto> monthlyStatistics = new ArrayList<>();

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
            MonthStatsDto monthStats = MonthStatsDto.builder()
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
    */

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

    public List<List<Float>> getEmbeddingsFromRecentGames(String category, int sampleSize) {

        List<Game> recentGames = gameRepository.findLastFiveGamesByCategoryOrderByDateDesc(category);
        // 2. Extraer todas las preguntas (embeddings) de esas partidas
        List<List<Float>> allEmbeddings = new ArrayList<>();
        for (Game g : recentGames) {
            for (Question q : g.getQuestions()) {
                if (q.getEmbedding() != null && !q.getEmbedding().isEmpty()) {
                    allEmbeddings.add(q.getEmbedding());
                }
            }
        }
        // 3. Si hay menos vectores que sampleSize, devolvemos todo; si no, barajamos y tomamos sampleSize
        if (allEmbeddings.size() <= sampleSize) {
            return allEmbeddings;
        }
        Collections.shuffle(allEmbeddings);
        return allEmbeddings.subList(0, sampleSize);
    }

    public List<MonthGamesDto> getGamesCountPerMonthCurrentYear(String idUser) {
        List<Game> allGamesOfUser = gameRepository.findGamesByIdUser(idUser);
        System.out.println("Total games for user " + idUser + ": " + allGamesOfUser.size());

        int currentYear = LocalDateTime.now().getYear();
        System.out.println("Current year: " + currentYear);

        List<Game> gamesThisYear = allGamesOfUser.stream()
                .filter(g -> {
                    LocalDateTime date = g.getDate();
                    boolean match = date != null && date.getYear() == currentYear;
                    if (!match) {
                        System.out.println("Excluding game with date " + date);
                    }
                    return match;
                })
                .collect(Collectors.toList());
        System.out.println("Games in year " + currentYear + ": " + gamesThisYear.size());

        Map<String, Long> gamesByMonth = gamesThisYear.stream()
                .collect(Collectors.groupingBy(
                        g -> {
                            String monthKey = g.getDate().toLocalDate().toString().substring(0, 7);
                            System.out.println("Mapping game dated " + g.getDate() + " to month " + monthKey);
                            return monthKey;
                        },
                        TreeMap::new,
                        Collectors.counting()
                ));
        System.out.println("Grouped games by month:");
        gamesByMonth.forEach((month, count) ->
                System.out.println("  Month: " + month + " -> Count: " + count)
        );

        List<MonthGamesDto> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : gamesByMonth.entrySet()) {
            String month = entry.getKey();
            int count = entry.getValue().intValue();
            System.out.println("Adding DTO - month: " + month + ", gamesCount: " + count);
            result.add(
                    MonthGamesDto.builder()
                            .month(month)
                            .gamesCount(count)
                            .build()
            );
        }

        System.out.println("Final result size: " + result.size());
        return result;
    }

    public StatsGeneralesDto getTotalsStatistics(String idUser) {
        List<Game> allGames = gameRepository.findGamesByIdUser(idUser);

        int totalGames = allGames.size();
        int totalSuccesses = allGames.stream().mapToInt(Game::getSuccesses).sum();
        int totalFailures  = allGames.stream().mapToInt(Game::getFailures).sum();

        return StatsGeneralesDto.builder()
                .totalGames(totalGames)
                .totalSuccesses(totalSuccesses)
                .totalFailures(totalFailures)
                .build();
    }

    public List<MonthStatsDto> getSuccessFailureByMonthCurrentYear(String idUser) {
        int currentYear = LocalDateTime.now().getYear();

        // 1) Traer todas las partidas del usuario
        List<Game> allGames = gameRepository.findGamesByIdUser(idUser);

        // 2) Filtrar solo las del año actual
        List<Game> gamesThisYear = allGames.stream()
                .filter(g -> {
                    LocalDateTime d = g.getDate();
                    return d != null && d.getYear() == currentYear;
                })
                .collect(Collectors.toList());

        // 3) Formateador para "yyyy-MM"
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // 4) Agrupar por mes y acumular éxitos/fallos/preguntas
        Map<String, List<Game>> gamesGroupedByMonth = gamesThisYear.stream()
                .collect(Collectors.groupingBy(
                        g -> g.getDate().toLocalDate().format(monthFormatter),
                        TreeMap::new,
                        Collectors.toList()
                ));

        List<MonthStatsDto> result = new ArrayList<>();
        for (Map.Entry<String, List<Game>> entry : gamesGroupedByMonth.entrySet()) {
            String monthKey = entry.getKey();
            List<Game> gamesInMonth = entry.getValue();

            int totalQuestionsAnswered = gamesInMonth.stream()
                    .mapToInt(g -> g.getSuccesses() + g.getFailures())
                    .sum();
            int totalSuccesses = gamesInMonth.stream()
                    .mapToInt(Game::getSuccesses)
                    .sum();
            int totalFailures = gamesInMonth.stream()
                    .mapToInt(Game::getFailures)
                    .sum();

            double successRate = calculateRate(totalSuccesses, totalQuestionsAnswered);
            double failureRate = calculateRate(totalFailures, totalQuestionsAnswered);

            MonthStatsDto monthDto = MonthStatsDto.builder()
                    .month(monthKey)
                    .totalMonthQuestionsAnswered(totalQuestionsAnswered)
                    .totalMonthSuccesses(totalSuccesses)
                    .totalMonthFailures(totalFailures)
                    .successMonthRate(successRate)
                    .failureMonthRate(failureRate)
                    .build();

            result.add(monthDto);
        }

        return result;
    }


}

