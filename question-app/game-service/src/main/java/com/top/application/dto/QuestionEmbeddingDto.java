package com.top.application.dto;

import com.top.application.model.Game;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class QuestionEmbeddingDto {
    private String question;
    private List<Float> embedding;

}