package com.top.application.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifficultyStatisticsDto {
    private int totalQuestionsAnswered;
    private int totalSuccesses;
    private int totalFailures;
    private double successRate;
    private double failureRate;

    public void calculateRates() {
        if (totalQuestionsAnswered > 0) {
            this.successRate = ((double) totalSuccesses / totalQuestionsAnswered) * 100;
            this.failureRate = ((double) totalFailures / totalQuestionsAnswered) * 100;
        } else {
            this.successRate = 0;
            this.failureRate = 0;
        }
    }
}
