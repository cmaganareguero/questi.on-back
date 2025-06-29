package com.top.infraestructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.top.application.client.GameServiceFeignClient;
import com.top.application.model.Game;
import com.top.infraestructure.util.SimpleCosineIndex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import com.top.infraestructure.util.SimpleCosineIndex;

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
            String userId,
            String category,
            String difficulty,
            int numberOfQuestions,
            String answerType,
            int embeddingSize
    ) throws JsonProcessingException {
        long tStartTotal = System.currentTimeMillis();
        List<Game.Question> resultado = new ArrayList<>();
        List<float[]> selectedEmbeddings = new ArrayList<>();
        int NUM_FAILED = 2;

        // 1. Hasta 2 preguntas falladas por el usuario
        try {
            List<Game.Question> failed = gameServiceFeignClient.getFailedQuestions(userId, category);
            if (failed != null) {
                int count = 0;
                for (Game.Question q : failed) {
                    float[] embArr = toFloatArray(q.getEmbedding(), embeddingSize);
                    if (!SimpleCosineIndex.hasSimilar(selectedEmbeddings, embArr, UMBRAL_SIMILAR)) {
                        resultado.add(q);
                        selectedEmbeddings.add(embArr);
                        count++;
                        if (count >= NUM_FAILED) break;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Fallo al obtener preguntas falladas: " + ex.getMessage());
            // Continúa, no impide el resto del proceso
        }

        // 2. Preguntas de otros usuarios, diferentes a las ya respondidas (comparando embedding)
        try {
            if (resultado.size() < numberOfQuestions) {
                List<Game.Question> others = gameServiceFeignClient.getOtherUsersQuestions(userId, category, answerType);
                if (others != null) {
                    for (Game.Question q : others) {
                        float[] embArr = toFloatArray(q.getEmbedding(), embeddingSize);
                        if (!SimpleCosineIndex.hasSimilar(selectedEmbeddings, embArr, UMBRAL_SIMILAR)) {
                            resultado.add(q);
                            selectedEmbeddings.add(embArr);
                            if (resultado.size() >= numberOfQuestions) break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Fallo al obtener preguntas de otros usuarios: " + ex.getMessage());
            // Continúa, no impide el resto del proceso
        }

        // 3. Genera nuevas preguntas si siguen faltando (faltan +2 por tu lógica)
        int faltan = (numberOfQuestions - resultado.size());
        if (faltan > 0) {
            // Prepara histórico de embeddings para evitar duplicados
            List<float[]> histEmbeddings = fetchHistoricEmbeddingsFeign(category)
                    .stream()
                    .map(arr -> Arrays.copyOf(arr, Math.min(embeddingSize, arr.length)))
                    .collect(Collectors.toList());
            histEmbeddings.addAll(selectedEmbeddings);
            SimpleCosineIndex indexHistorico = new SimpleCosineIndex(histEmbeddings);

            int batchSize = faltan + 2;
            // Solo una ronda, ya que el batchSize siempre es faltan+2
            List<Game.Question> lote = openAIClient.callOpenAI(
                    category, difficulty, answerType, batchSize, 0.8, 0.9);

            if (lote != null && !lote.isEmpty()) {
                List<String> textosLote = lote.stream().map(Game.Question::getQuestion).collect(Collectors.toList());
                List<float[]> embBatch = embeddingService.getEmbeddings(textosLote);

                List<Pair<Game.Question, float[]>> candidatas = new ArrayList<>();
                for (int i = 0; i < lote.size(); i++) {
                    Game.Question q = lote.get(i);
                    float[] embArr = Arrays.copyOf(embBatch.get(i), Math.min(embeddingSize, embBatch.get(i).length));
                    if (!indexHistorico.hasSimilar(embArr, UMBRAL_SIMILAR)) {
                        candidatas.add(Pair.of(q, embArr));
                    }
                }
                Collections.shuffle(candidatas);

                int tomar = Math.min(faltan, candidatas.size());
                for (int i = 0; i < tomar; i++) {
                    Pair<Game.Question, float[]> par = candidatas.get(i);
                    Game.Question q = par.getFirst();
                    float[] embArr = par.getSecond();
                    q.setEmbedding(toFloatList(embArr));
                    resultado.add(q);
                    indexHistorico.addVector(embArr);
                }
            }
        }

        long tEndTotal = System.currentTimeMillis();
        log.info("[generateQuestions] Tiempo total: {} ms, Embedding size N = {}", (tEndTotal - tStartTotal), embeddingSize);
        return resultado;
    }
    private float[] toFloatArray(List<Float> list, int n) {
        int dim = Math.min(n, list.size());
        float[] arr = new float[dim];
        for (int i = 0; i < dim; i++) arr[i] = list.get(i);
        return arr;
    }

    private List<Float> toFloatList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float f : arr) list.add(f);
        return list;
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
