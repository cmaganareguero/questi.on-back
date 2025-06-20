package com.top.application.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextModelResponseDTO {
    private String content;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
}