package com.top.application.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingRequest {
    private String model;
    private List<String> input;
}
