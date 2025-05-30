package com.top.application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGamesDto {
    private String name;
    private String category;
    private String difficulty;
    private String answerType;
    private int numQuestions;
    private int successes;
    private int failures;
}

