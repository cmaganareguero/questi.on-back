package com.top.application.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthStatsDto {
    private String month;
    private int totalMonthQuestionsAnswered;
    private int totalMonthSuccesses;
    private int totalMonthFailures;
    private double successMonthRate;
    private double failureMonthRate;
}
