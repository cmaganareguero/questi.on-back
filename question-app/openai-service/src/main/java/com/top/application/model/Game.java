package com.top.application.model;

import lombok.Builder;
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
    public static class Question {
        private String question;
        private List<String> answers;
        private int correctAnswerIndex;
        private List<Float> embedding;

        public Question(String question, List<String> answers, int correctAnswerIndex, List<Float> embedding) {
            //this.id = id;
            this.question = question;
            this.answers = answers;
            this.correctAnswerIndex = correctAnswerIndex;
            this.embedding = embedding;
        }

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
