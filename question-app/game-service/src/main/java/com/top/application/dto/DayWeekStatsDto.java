package com.top.application.dto;

import com.top.application.model.Game;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class DayWeekStatsDto {
    private String day;
    private int totalDayQuestionsAnswered;
    private int totalDaySuccesses;
    private int totalDayFailures;
    private double successDayRate;
    private double failureDayRate;

    public DayWeekStatsDto() {
        this.day = LocalDate.now().toString(); // Inicialización automática al día de hoy
        this.totalDayQuestionsAnswered = 0;
        this.totalDaySuccesses = 0;
        this.totalDayFailures = 0;
        this.successDayRate = 0.0;
        this.failureDayRate = 0.0;
    }
}