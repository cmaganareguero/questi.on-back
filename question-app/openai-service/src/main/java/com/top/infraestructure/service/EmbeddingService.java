package com.top.infraestructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.top.application.dto.EmbeddingRequest;
import com.top.application.dto.EmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Este servicio se encarga de generar embeddings para uno o varios textos a la vez.
 */
@Service
@Slf4j
public class EmbeddingService {
    @Value("${chatgpt.api.embeddings}")
    private String embeddingsApiUrl;
    @Value("${chatgpt.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public EmbeddingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Genera embeddings para una lista de textos.
     * Devuelve una lista de float[] (embedding completo por texto).
     */
    public List<float[]> getEmbeddings(List<String> textos) throws JsonProcessingException {
        if (textos == null || textos.isEmpty()) {
            log.warn("Lista de textos vacía.");
            return new ArrayList<>();
        }

        long tStartRequest = System.currentTimeMillis();

        EmbeddingRequest requestBody = EmbeddingRequest.builder()
                .model("text-embedding-3-small")
                .input(textos)
                .build();

        String jsonRequest = objectMapper.writeValueAsString(requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                embeddingsApiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        long tEndRequest = System.currentTimeMillis();
        log.info("[EmbeddingService] Tiempo petición OpenAI: {} ms", (tEndRequest - tStartRequest));

        String responseBody = response.getBody();
        EmbeddingResponse embeddingResponse = objectMapper.readValue(responseBody, EmbeddingResponse.class);

        List<float[]> resultados = new ArrayList<>();
        for (EmbeddingResponse.EmbeddingDatum d : embeddingResponse.getData()) {
            List<Float> vector = d.getEmbedding();
            float[] arr = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) {
                arr[i] = vector.get(i);
            }
            resultados.add(arr);
        }
        return resultados;
    }

    /**
     * Genera y almacena los N primeros valores del embedding para una lista de textos (para pruebas).
     */
    public List<float[]> generateAndStoreEmbeddings(List<String> textos, int n) throws JsonProcessingException {
        List<float[]> embeddings = getEmbeddings(textos);
        List<float[]> resultadosReducidos = new ArrayList<>();

        long tStartStore = System.currentTimeMillis();

        for (float[] vector : embeddings) {
            int dim = Math.min(n, vector.length);
            float[] arr = new float[dim];
            System.arraycopy(vector, 0, arr, 0, dim);
            storeEmbedding(arr); // Simulación de guardado
            resultadosReducidos.add(arr);
        }

        long tEndStore = System.currentTimeMillis();
        log.info("[EmbeddingService] Tiempo almacenamiento embeddings: {} ms", (tEndStore - tStartStore));

        return resultadosReducidos;
    }

    /**
     * Simula el almacenamiento del embedding (sustituye por tu lógica de persistencia real).
     */
    private void storeEmbedding(float[] embedding) {
        // Aquí meterías tu lógica de base de datos.
    }
}
