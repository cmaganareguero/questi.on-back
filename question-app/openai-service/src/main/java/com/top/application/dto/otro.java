package com.top.application.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class otro {

    private String model;
    private double temperature;
    private int maxTokens;
    private double topP;
    // Otros campos si los hay

}
