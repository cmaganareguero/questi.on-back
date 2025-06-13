package com.top.infraestructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.top.application.client.GameServiceFeignClient;
import com.top.application.model.Game;
import com.top.infraestructure.util.SimpleCosineIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 1. generateQuestionsWithEmbeddings
 * 2. callOpenAI
 * 3. buildPromptTemplate
 * 4. buildChatGPTRequest
 * 5. convertToJson
 * 6. createHttpHeaders
 * 7. extractContentFromResponse
 * 8. getMoreInfo
 *
 * <p>
 * Ahora usa BatchEmbeddings para reducir llamadas a la API y un índice simple en memoria
 * para filtrar rápidamente vectores similares (puede sustituirse por un HNSW real).
 */
@Service
@Slf4j
public class APIService {

    private static final double UMBRAL_SIMILAR = 0.85;
    private static final int SAMPLE_LIMIT = 50;

    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private GameServiceFeignClient gameServiceFeignClient;
    @Autowired
    private OpenAIClient openAIClient;

    /**
     * Genera preguntas completas con embedding, filtrando semánticamente duplicados.
     */
    public List<Game.Question> generateQuestionsWithEmbeddings(
            String category,
            String difficulty,
            int numberOfQuestions,
            String answerType,
            int n // Nuevo parámetro para los índices
    ) throws JsonProcessingException {
        long tStartTotal = System.currentTimeMillis();

        List<float[]> histEmbeddings = fetchHistoricEmbeddingsFeign(category)
                .stream()
                .map(arr -> Arrays.copyOf(arr, Math.min(n, arr.length))) // recorta también los históricos
                .collect(Collectors.toList());

        SimpleCosineIndex indexHistorico = new SimpleCosineIndex(histEmbeddings);

        List<Game.Question> resultado = new ArrayList<>();
        int preguntasPendientes = numberOfQuestions;

        for (int ronda = 0; ronda < 2 && preguntasPendientes > 0; ronda++) {
            int batchSize = preguntasPendientes + 2;

            long tGptStart = System.currentTimeMillis();
            List<Game.Question> lote = openAIClient.callOpenAI(
                    category, difficulty, answerType, batchSize,
                    ronda == 0 ? 0.8 : 0.9, ronda == 0 ? 0.9 : 0.85);
            if (lote.isEmpty()) break;
            long tGptEnd = System.currentTimeMillis();
            log.info("[GPT-4o][INTENTO {}] Tiempo petición generación preguntas: {} ms ({} preguntas solicitadas)", ronda+1, (tGptEnd-tGptStart), batchSize);
            List<String> textosLote = new ArrayList<>();
            for (Game.Question q : lote) textosLote.add(q.getQuestion());

            long tEmbeddingStart = System.currentTimeMillis();
            List<float[]> embBatch = embeddingService.getEmbeddings(textosLote);
            long tEmbeddingEnd = System.currentTimeMillis();
            log.info("[EmbeddingService] Tiempo generación embeddings batch ({} textos): {} ms", textosLote.size(), tEmbeddingEnd - tEmbeddingStart);

            List<Pair<Game.Question, float[]>> candidatas = new ArrayList<>();
            for (int i = 0; i < lote.size(); i++) {
                Game.Question q = lote.get(i);
                float[] embArr = embBatch.get(i);
                float[] reduced = Arrays.copyOf(embArr, Math.min(n, embArr.length)); // recorta aquí
                if (!indexHistorico.hasSimilar(reduced, UMBRAL_SIMILAR)) {
                    candidatas.add(Pair.of(q, reduced));
                }
            }
            Collections.shuffle(candidatas);

            int tomar = Math.min(preguntasPendientes, candidatas.size());
            for (int i = 0; i < tomar; i++) {
                Pair<Game.Question, float[]> par = candidatas.get(i);
                Game.Question q = par.getFirst();
                float[] embArr = par.getSecond();

                List<Float> embList = new ArrayList<>(embArr.length);
                for (float f : embArr) embList.add(f);
                q.setEmbedding(embList);

                resultado.add(q);
                indexHistorico.addVector(embArr);
                log.info("[EmbeddingStorage] Pregunta '{}' almacenada con embedding de {} dimensiones", q.getQuestion(), embArr.length);
            }
            preguntasPendientes -= tomar;
        }

        long tEndTotal = System.currentTimeMillis();
        log.info("[generateQuestions] Tiempo total: {} ms, Embedding size N = {}", (tEndTotal - tStartTotal), n);
        return resultado;
    }

    /**
     * Obtiene embeddings históricos recientes usando Feign Client.
     */
    private List<float[]> fetchHistoricEmbeddingsFeign(String category) {
        List<List<Float>> rawHistEmbeddings = gameServiceFeignClient.getEmbeddingsFromRecentGames(category);
        List<float[]> histEmbeddings = new ArrayList<>();
        for (List<Float> vec : rawHistEmbeddings) {
            float[] arr = new float[vec.size()];
            for (int i = 0; i < vec.size(); i++) arr[i] = vec.get(i);
            histEmbeddings.add(arr);
        }
        // Limitar muestra para eficiencia
        if (histEmbeddings.size() > SAMPLE_LIMIT) {
            Collections.shuffle(histEmbeddings);
            histEmbeddings = histEmbeddings.subList(0, SAMPLE_LIMIT);
        }
        log.info("[fetchHistoricEmbeddingsFeign] Embeddings históricos obtenidos: {}", histEmbeddings.size());
        return histEmbeddings;
    }

    /**
     * Pide detalles extra a ChatGPT sobre una pregunta y su respuesta.
     */
    public String getMoreInfo(String question, String correctAnswer) throws JsonProcessingException {
        return openAIClient.getMoreInfo(question, correctAnswer);
    }
}
