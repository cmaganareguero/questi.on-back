package com.top.application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthStatisticsDto {
    private String month;
    private int totalQuestionsAnswered;
    private int totalSuccesses;
    private int totalFailures;
    private double successRate;
    private double failureRate;
}
