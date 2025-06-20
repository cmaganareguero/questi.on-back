package com.top.infraestructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.top.application.dto.TextModelResponseDTO;
import com.top.application.model.APIRequest;
import com.top.application.model.APIResponse;
import com.top.application.model.Game;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
@Slf4j
public class OpenAIClient {
    @Value("${chatgpt.api.url}")
    private String apiUrl;
    @Value("${chatgpt.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    public OpenAIClient(RestTemplate restTemplate, ObjectMapper objectMapper, PromptService promptService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.promptService = promptService;
    }

    public List<Game.Question> callOpenAI(
            String category,
            String difficulty,
            String answerType,
            int batchSize,
            double temperature,
            double topP
    ) throws JsonProcessingException {
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // 1. Construcción del prompt
        String prompt = promptService.buildPromptTemplate(category, difficulty, answerType, batchSize, runId);
        log.info("[callOpenAI] ===== Generación de preguntas =====");
        log.info("[callOpenAI] Categoría: {}, Dificultad: {}, Tipo respuesta: {}, Nº preguntas: {}, RunID: {}",
                category, difficulty, answerType, batchSize, runId);
        log.info("[callOpenAI] Prompt generado:\n{}", prompt);

        // 2. Construcción del request JSON
        APIRequest req = buildModelGpt4o(prompt, temperature, topP);
        String jsonReq = objectMapper.writeValueAsString(req);
        log.debug("[callOpenAI] JSON de request enviado a ChatGPT (primeros 200 chars): {}...",
                jsonReq.length() > 200 ? jsonReq.substring(0, 200) : jsonReq);

        // 3. Preparación y envío de la petición HTTP
        log.info("[callOpenAI] Enviando petición POST a la API de OpenAI...");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");
        HttpEntity<String> request = new HttpEntity<>(jsonReq, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, request, String.class
        );

        // 4. Procesamiento de la respuesta
        TextModelResponseDTO result = extractContentFromResponse(response.getBody());
        String content = result.getContent();
        int promptTokens = result.getPromptTokens();
        int completionTokens = result.getCompletionTokens();
        int totalTokens = result.getTotalTokens();

        log.info("[callOpenAI] ----- Resumen de tokens consumidos -----");
        log.info("[callOpenAI] Prompt tokens (entrada): {}", promptTokens);
        log.info("[callOpenAI] Completion tokens (salida): {}", completionTokens);
        log.info("[callOpenAI] Total tokens usados: {}", totalTokens);

        log.info("[callOpenAI] Contenido crudo recibido de ChatGPT (primeros 300 chars):\n{}...",
                content.length() > 300 ? content.substring(0, 300) : content);

        // 5. Parseo de las preguntas generadas
        List<Game.Question> questions = parseQuestionsFromContent(content);
        log.info("[callOpenAI] Preguntas generadas y parseadas: {}", questions.size());
        log.info("[callOpenAI] ===== Fin de la generación =====");

        return questions;
    }

    private APIRequest buildModelGpt4o(String content, double temperature, double topP) {
        return APIRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(Arrays.asList(
                        new APIRequest.Message("system", content),
                        new APIRequest.Message("user", "Juego de preguntas")
                ))
                .temperature(temperature)
                .top_p(topP)
                .max_tokens(1000)
                .build();
    }

    private TextModelResponseDTO extractContentFromResponse(String responseBody) throws JsonProcessingException {
        APIResponse chatGPTResponse = objectMapper.readValue(responseBody, APIResponse.class);

        String content = chatGPTResponse.getChoices().stream()
                .findFirst()
                .map(APIResponse.Choice::getMessage)
                .map(APIResponse.Choice.Message::getContent)
                .orElseThrow(() -> new RuntimeException("No content found in response"));

        APIResponse.Usage usage = chatGPTResponse.getUsage();
        int promptTokens = usage != null ? usage.getPrompt_tokens() : 0;
        int completionTokens = usage != null ? usage.getCompletion_tokens() : 0;
        int totalTokens = usage != null ? usage.getTotal_tokens() : 0;

        return new TextModelResponseDTO(content, promptTokens, completionTokens, totalTokens);
    }

    /**
     * Parsear el array JSON puro de preguntas devuelto por ChatGPT.
     */
    private List<Game.Question> parseQuestionsFromContent(String content) throws JsonProcessingException {
        List<Game.Question> preguntas = new ArrayList<>();
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        log.info("[callOpenAI] Índices delimitadores JSON: start={}, end={}", start, end);
        if (start < 0 || end < 0 || end <= start) {
            log.error("[callOpenAI] No se encontró array JSON válido.");
            return preguntas;
        }
        String jsonArray = content.substring(start, end + 1);
        log.info("[callOpenAI] jsonArray extraído (primeros 200 chars): {}...",
                jsonArray.length() > 200 ? jsonArray.substring(0, 200) : jsonArray);

        JsonNode root = objectMapper.readTree(jsonArray);
        if (!root.isArray()) {
            log.error("[callOpenAI] El contenido extraído no es un array JSON.");
            return preguntas;
        }
        for (JsonNode node : root) {
            JsonNode preguntaNode = node.get("pregunta");
            JsonNode opcionesNode = node.get("opciones");
            JsonNode indiceNode   = node.get("indice_correcto");
            if (preguntaNode == null || !preguntaNode.isTextual() ||
                    opcionesNode == null || !opcionesNode.isArray() ||
                    indiceNode == null || !indiceNode.canConvertToInt()) {
                log.warn("[callOpenAI] Nodo omitido por formato incorrecto: {}", node);
                continue;
            }
            String texto = preguntaNode.asText();
            List<String> opciones = new ArrayList<>();
            for (JsonNode opt : opcionesNode) {
                opciones.add(opt.asText());
            }
            int idx = indiceNode.asInt();
            Game.Question q = Game.Question.builder()
                    .id(UUID.randomUUID().toString())
                    .question(texto)
                    .answers(opciones)
                    .correctAnswerIndex(idx)
                    .selectedAnswerIndex(null)
                    .embedding(null)
                    .build();
            preguntas.add(q);
        }
        log.info("[callOpenAI] Número de preguntas parseadas: {}", preguntas.size());
        return preguntas;
    }

    /**
     * Pide detalles extra a ChatGPT sobre una pregunta y su respuesta.
     */
    public String getMoreInfo(String question, String correctAnswer) throws JsonProcessingException {
        String prompt = String.format(
                "Te he preguntado %s y he obtenido la respuesta %s. Dame más información en hasta 80 palabras.",
                question, correctAnswer
        );
        APIRequest requestBody = buildModelGpt4o(prompt, 0.7, 1.0);
        String jsonReq = objectMapper.writeValueAsString(requestBody);
        log.info("[getMoreInfo] JSON request a ChatGPT: {}", jsonReq);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonReq, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, requestEntity, String.class
        );
        TextModelResponseDTO result = extractContentFromResponse(response.getBody());
        String content = result.getContent();
        int promptTokens = result.getPromptTokens();
        int completionTokens = result.getCompletionTokens();
        int totalTokens = result.getTotalTokens();
        log.info("[getMoreInfo] Respuesta de ChatGPT: {}", content);
        return content;
    }

}
