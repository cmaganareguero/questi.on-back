package com.top.application.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameProducerRequestDto {

    String name;
    String category;
    String difficulty;
    String answerType;
    String date;
    int successes;
    int failures;
    int numQuestions;
    String idUser;
    List<String> idQuestion;
}
