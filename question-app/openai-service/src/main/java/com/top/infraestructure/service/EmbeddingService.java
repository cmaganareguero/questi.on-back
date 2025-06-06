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
     * Genera un embedding para un solo texto (método antiguo, se conserva para compatibilidad).
     */
    public float[] getEmbedding(String texto) throws JsonProcessingException {
        List<String> inputs = new ArrayList<>();
        inputs.add(texto);
        List<float[]> lista = getEmbeddings(inputs);
        return lista.isEmpty() ? new float[0] : lista.get(0);
    }

    /**
     * (Nuevo) Genera embeddings para una lista de textos en una sola petición.
     *
     * @param textos Lista de cadenas para las que se quiere el embedding.
     * @return Lista de arrays float[], cada uno correspondiente al embedding de cada texto en el mismo orden.
     */
    public List<float[]> getEmbeddings(List<String> textos) throws JsonProcessingException {
        if (textos == null || textos.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Construir el cuerpo de la petición: modelo + lista de inputs
        EmbeddingRequest requestBody = EmbeddingRequest.builder()
                .model("text-embedding-3-small")
                .input(textos)
                .build();

        // 2. Convertir a JSON
        String jsonRequest = objectMapper.writeValueAsString(requestBody);

        // 3. Cabeceras HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);

        // 4. Llamada al endpoint de embeddings de OpenAI
        ResponseEntity<String> response = restTemplate.exchange(
                embeddingsApiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        // 5. Leer la respuesta como cadena y deserializar a EmbeddingResponse
        String responseBody = response.getBody();
        EmbeddingResponse embeddingResponse = objectMapper.readValue(responseBody, EmbeddingResponse.class);

        // 6. Extraer y convertir cada embedding de Float a float[]
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
}
