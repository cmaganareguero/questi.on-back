package com.top.application.model;

import lombok.*;

import java.util.Collections;
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

    @Override
    public String toString() {
        return "QuestionModel{" +
                "text='" + question + '\'' +
                ", answers=" + answers +
                ", correctIndex=" + correctAnswerIndex +
                '}';
    }

    public void shuffleAnswers() {
        String correctAnswer = answers.get(correctAnswerIndex);
        Collections.shuffle(answers);
        correctAnswerIndex = answers.indexOf(correctAnswer);
    }
}
