package com.top.application.model;

import java.util.List;

public class Game {
    private String category;
    private long numQuestions;
    private String difficulty;
    private String answerType;
    private List<Question> questions;

    // Constructor
    public Game(String category, long numQuestions, String difficulty, String answerType, List<Question> questions) {
        this.category = category;
        this.numQuestions = numQuestions;
        this.difficulty = difficulty;
        this.answerType = answerType;
        this.questions = questions;
    }

    // Getters and Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getNumQuestions() {
        return numQuestions;
    }

    public void setNumQuestions(long numQuestions) {
        this.numQuestions = numQuestions;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getAnswerType() {
        return answerType;
    }

    public void setAnswerType(String answerType) {
        this.answerType = answerType;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    // Clase anidada Question
    public static class Question {
        private String question;
        private List<String> answers;
        private int correctAnswerIndex;

        // Constructor
        public Question(String question, List<String> answers, int correctAnswerIndex) {
            this.question = question;
            this.answers = answers;
            this.correctAnswerIndex = correctAnswerIndex;
        }

        // Getters and Setters
        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public List<String> getAnswers() {
            return answers;
        }

        public void setAnswers(List<String> answers) {
            this.answers = answers;
        }

        public int getCorrectAnswerIndex() {
            return correctAnswerIndex;
        }

        public void setCorrectAnswerIndex(int correctAnswerIndex) {
            this.correctAnswerIndex = correctAnswerIndex;
        }
    }
}
