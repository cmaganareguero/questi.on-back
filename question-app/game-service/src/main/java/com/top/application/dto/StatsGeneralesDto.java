package com.top.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsGeneralesDto {
    private int totalGames;
    private int totalSuccesses;
    private int totalFailures;
}
