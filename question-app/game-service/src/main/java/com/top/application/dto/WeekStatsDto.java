package com.top.application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekStatsDto {
    private int totalWeekQuestionsAnswered;
    private int totalWeekSuccesses;
    private int totalWeekFailures;
    private double successWeekRate;
    private double failureWeekRate;
}
