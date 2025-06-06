package com.top.application.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private String question;
    private List<String> answers;
    private int correctAnswerIndex;
    private List<Float> embedding;

}
