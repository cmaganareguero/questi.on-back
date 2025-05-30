package com.top.application.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Game")
public class Game {
    @Id
    private String id;
    private String name;
    private String category;
    private String difficulty;
    private String answerType;
    private LocalDateTime date;
    private int successes;
    private int failures;
    private int numQuestions;
    private String idUser;
    private List<Question> questions;


    public void validate(String value) {
        if (value == null || value.isEmpty()) throw new NullPointerException("La pregunta no puede ser vacia");
    }

    public void validateList(List<Long> questions) {
        if (questions == null || questions.isEmpty() || questions.size() == 1) throw new NullPointerException("Las preguntas proporcionadas no tienen el formato adecuado");
    }
}
