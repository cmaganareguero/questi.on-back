package com.top.application.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequestDto {
    String idUser;
    String difficulty;
    String answerType;
    String category;
    Long numQuestions;
}
