package com.top.application.dto;

import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuestionsDto {

    @NotNull
    @Size(min = 1, max = 10)
    private String difficulty;

    @NotNull
    @Size(min = 1, max = 10)
    private String answerType;

    @Min(1)
    @Max(10)
    private int numQuestions;

    @Min(2)
    @Max(10)
    private int numAnswers;
}
