package com.top.application.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
public class Game {
    private String category;
    private long numQuestions;
    private String difficulty;
    private String answerType;
    private List<Question> questions;

    public Game(String category, long numQuestions, String difficulty, String answerType, List<Question> questions) {
        this.category = category;
        this.numQuestions = numQuestions;
        this.difficulty = difficulty;
        this.answerType = answerType;
        this.questions = questions;
    }

    @Getter
    @Setter
    @Builder
    @Data
    public static class Question {
        private String id;
        private String question;
        private List<String> answers;
        private int correctAnswerIndex;
        private Integer selectedAnswerIndex;
        private List<Float> embedding;

        @Override
        public String toString() {
            return "Question{" +
                    "question='" + question + '\'' +
                    ", answers=" + answers +
                    ", correctAnswerIndex=" + correctAnswerIndex +
                    '}';
        }

        public void shuffleAnswers() {
            String correct = answers.get(correctAnswerIndex);
            Collections.shuffle(answers);
            correctAnswerIndex = answers.indexOf(correct);
        }
    }
}
