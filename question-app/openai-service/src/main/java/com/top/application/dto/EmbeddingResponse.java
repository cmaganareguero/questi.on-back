package com.top.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Data
public class EmbeddingResponse {

    // Lista de elementos, cada uno con su índice y su vector embedding
    private List<EmbeddingDatum> data;

    @Data
    public static class EmbeddingDatum {
        private int index;
        private List<Float> embedding;
    }
}
