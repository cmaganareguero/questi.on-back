package com.top.application.dto;

import com.top.application.enumeration.AnswerType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAPIDto {
    String idUser;
    String difficulty;
    AnswerType answerType;
    String category;
    Long numQuestions;
}
