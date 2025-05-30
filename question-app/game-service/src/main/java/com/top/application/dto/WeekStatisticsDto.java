package com.top.application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekStatisticsDto {
    private int totalWeekQuestionsAnswered;
    private int totalWeekSuccesses;
    private int totalWeekFailures;
    private double successWeekRate;
    private double failureWeekRate;
    private List<DayWeekStatsDto> dayStatsList;
}
