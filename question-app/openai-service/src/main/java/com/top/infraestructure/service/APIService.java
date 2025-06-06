package com.top.infraestructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.top.application.model.APIRequest;
import com.top.application.model.APIResponse;
import com.top.application.model.Game;
import com.top.infraestructure.util.CosineSimilarityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 1. generateQuestionsWithEmbeddings
 * 2. callChatGPTWithRandomizedPrompt
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

    @Value("${chatgpt.api.url}")
    private String apiUrl;

    @Value("${chatgpt.api.key}")
    private String apiKey;

    @Value("${game.service.url}")
    private String gameServiceUrl;  // Ejemplo: "http://localhost:7785/game"

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddingService embeddingService;

    private static final double UMBRAL_SIMILAR = 0.85;

    /**
     * (1) Genera preguntas completas con embedding:
     *     - Obtiene embeddings históricos (últimas 5 partidas) en un solo paso (lista de vectores).
     *     - Pide lotes a ChatGPT.
     *     - Calcula todos los embeddings nuevos de golpe (batch).
     *     - Filtra preguntas semánticamente similares con un índice simple (O(N)).
     *     - Devuelve List<Game.Question> con embedding calculado, y anota tiempos parciales.
     */
    public List<Game.Question> generateQuestionsWithEmbeddings(
            String category,
            String difficulty,
            int numberOfQuestions,
            String answerType
    ) throws JsonProcessingException {

        long tStartTotal = System.currentTimeMillis();

        // --------------------------------------------------------
        // 1.a) PETICIÓN A “game-service” para obtener embeddings históricos
        long tStartHist = System.currentTimeMillis();
        String url = String.format("%s/getEmbeddingsFromRecentGames?category=%s", gameServiceUrl, category);
        log.info("[generateQuestions] 1.a) URL para recientes: {}", url);

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        String body = response.getBody();
        log.info("[generateQuestions] 1.b) Body recibido de game-service: {}", body);

        // 1.b) Deserializar directamente a List<List<Float>>
        List<List<Float>> rawHistEmbeddings = objectMapper.readValue(
                body,
                new TypeReference<List<List<Float>>>() {}
        );

        // 1.c) Convertimos List<List<Float>> a List<float[]> para facilitar comparaciones
        List<float[]> histEmbeddings = new ArrayList<>();
        for (List<Float> vec : rawHistEmbeddings) {
            float[] arr = new float[vec.size()];
            for (int i = 0; i < vec.size(); i++) {
                arr[i] = vec.get(i);
            }
            histEmbeddings.add(arr);
        }
        log.info("[generateQuestions] 1.c) Total de vectores históricos recibidos: {}", histEmbeddings.size());

        // 1.d) Si hay más de 50 vectores, barajar y recortar a 50
        int sampleSize = 50;
        if (histEmbeddings.size() > sampleSize) {
            Collections.shuffle(histEmbeddings);
            histEmbeddings = histEmbeddings.subList(0, sampleSize);
            log.info("[generateQuestions] 1.d) HistEmbeddings recortado a sampleSize={} vectores", sampleSize);
        }
        long tEndHist = System.currentTimeMillis();
        log.info("[generateQuestions] → Duración de “game-service” + conversión: {} ms",
                (tEndHist - tStartHist));

        // Construimos un índice simple basado en array;
        // si quieres optimizarlo, aquí podrías usar HNSW u otra estructura.
        SimpleCosineIndex indexHistórico = new SimpleCosineIndex(histEmbeddings);

        // --------------------------------------------------------
        // 2.a) Ronda 1: pedir lote grande a ChatGPT
        int batch1Size = numberOfQuestions + 2;
        log.info("[generateQuestions] 2.a) Solicitando batch1Size={} preguntas a ChatGPT", batch1Size);
        long tStartChat1 = System.currentTimeMillis();
        List<Game.Question> lote1 = callChatGPTWithRandomizedPrompt(
                category, difficulty, answerType,
                batch1Size,
                0.8, 0.9
        );
        long tEndChat1 = System.currentTimeMillis();
        log.info("[generateQuestions] 2.a) ChatGPT devolvió {} preguntas en lote1", lote1.size());
        log.info("[generateQuestions] → Duración llamada ChatGPT (ronda1): {} ms",
                (tEndChat1 - tStartChat1));

        // 2.b) Cálculo batch de embeddings para las preguntas de lote1
        long tStartEmbBatch1 = System.currentTimeMillis();
        List<String> textosLote1 = new ArrayList<>();
        for (Game.Question q : lote1) {
            textosLote1.add(q.getQuestion());
        }
        List<float[]> embBatch1 = embeddingService.getEmbeddings(textosLote1);
        long tEndEmbBatch1 = System.currentTimeMillis();
        log.info("[generateQuestions] 2.b) Calculados {} embeddings en batch para lote1", embBatch1.size());
        log.info("[generateQuestions] → Duración embeddings en batch (ronda1): {} ms",
                (tEndEmbBatch1 - tStartEmbBatch1));

        // 2.c) Filtrar usando el índice simple: O(N)
        long tStartFiltrado1 = System.currentTimeMillis();
        List<Pair<Game.Question, float[]>> candidatas = new ArrayList<>();
        for (int i = 0; i < lote1.size(); i++) {
            Game.Question q = lote1.get(i);
            float[] embArr = embBatch1.get(i);

            // Si NO existe ningún vector histórico con coseno ≥ UMBRAL_SIMILAR, la guardamos
            if (!indexHistórico.hasSimilar(embArr, UMBRAL_SIMILAR)) {
                candidatas.add(Pair.of(q, embArr));
            }
        }
        Collections.shuffle(candidatas);
        long tEndFiltrado1 = System.currentTimeMillis();
        log.info("[generateQuestions] 2.c) Candidatas tras filtrar+shuffle: {}", candidatas.size());
        log.info("[generateQuestions] → Duración filtrado (ronda1): {} ms",
                (tEndFiltrado1 - tStartFiltrado1));

        // 2.d) Tomar las primeras ‘numberOfQuestions’
        List<Game.Question> resultado = new ArrayList<>();
        int nTomar = Math.min(numberOfQuestions, candidatas.size());
        log.info("[generateQuestions] 2.d) Tomando nTomar={} preguntas finales de lote1", nTomar);
        for (int i = 0; i < nTomar; i++) {
            Pair<Game.Question, float[]> par = candidatas.get(i);
            Game.Question q         = par.getFirst();
            float[] embArr          = par.getSecond();

            // Convertir float[] a List<Float> y asignar al objeto pregunta
            List<Float> embList = new ArrayList<>(embArr.length);
            for (float f : embArr) embList.add(f);
            q.setEmbedding(embList);

            resultado.add(q);
            // Actualizamos el índice “histórico” (en memoria)
            indexHistórico.addVector(embArr);
        }
        log.info("[generateQuestions] 2.d) Resultado.size() tras ronda1 = {}", resultado.size());

        // --------------------------------------------------------
        // 3.a) Si faltan, segunda ronda
        if (resultado.size() < numberOfQuestions) {
            int faltan = numberOfQuestions - resultado.size();
            int batch2Size = faltan + 2;
            log.info("[generateQuestions] 3.a) Faltan {} preguntas, solicitando batch2Size={} a ChatGPT",
                    faltan, batch2Size);

            long tStartChat2 = System.currentTimeMillis();
            List<Game.Question> lote2 = callChatGPTWithRandomizedPrompt(
                    category, difficulty, answerType,
                    batch2Size,
                    0.9, 0.85
            );
            long tEndChat2 = System.currentTimeMillis();
            log.info("[generateQuestions] 3.a) ChatGPT devolvió {} preguntas en lote2", lote2.size());
            log.info("[generateQuestions] → Duración llamada ChatGPT (ronda2): {} ms",
                    (tEndChat2 - tStartChat2));

            // 3.b) Embeddings batch para lote2
            long tStartEmbBatch2 = System.currentTimeMillis();
            List<String> textosLote2 = new ArrayList<>();
            for (Game.Question q : lote2) {
                textosLote2.add(q.getQuestion());
            }
            List<float[]> embBatch2 = embeddingService.getEmbeddings(textosLote2);
            long tEndEmbBatch2 = System.currentTimeMillis();
            log.info("[generateQuestions] 3.b) Calculados {} embeddings en batch para lote2", embBatch2.size());
            log.info("[generateQuestions] → Duración embeddings en batch (ronda2): {} ms",
                    (tEndEmbBatch2 - tStartEmbBatch2));

            // 3.c) Filtrar lote2 contra índice histórico (ahora actualizado)
            long tStartFiltrado2 = System.currentTimeMillis();
            List<Pair<Game.Question, float[]>> candidatas2 = new ArrayList<>();
            for (int i = 0; i < lote2.size(); i++) {
                Game.Question q    = lote2.get(i);
                float[] embArr2    = embBatch2.get(i);
                if (!indexHistórico.hasSimilar(embArr2, UMBRAL_SIMILAR)) {
                    candidatas2.add(Pair.of(q, embArr2));
                }
            }
            Collections.shuffle(candidatas2);
            long tEndFiltrado2 = System.currentTimeMillis();
            log.info("[generateQuestions] 3.c) candidatas2 tras shuffle: {}", candidatas2.size());
            log.info("[generateQuestions] → Duración filtrado (ronda2): {} ms",
                    (tEndFiltrado2 - tStartFiltrado2));

            // 3.d) Tomar hasta ‘faltan’ candidatas2
            int toTake2 = Math.min(faltan, candidatas2.size());
            log.info("[generateQuestions] 3.d) Tomando toTake2={} preguntas finales de lote2", toTake2);
            for (int i = 0; i < toTake2; i++) {
                Pair<Game.Question, float[]> par = candidatas2.get(i);
                Game.Question q      = par.getFirst();
                float[] embArr2      = par.getSecond();

                List<Float> embList2 = new ArrayList<>(embArr2.length);
                for (float f : embArr2) embList2.add(f);
                q.setEmbedding(embList2);

                resultado.add(q);
                indexHistórico.addVector(embArr2);
            }
            log.info("[generateQuestions] 3.d) Resultado.size() tras ronda2 = {}", resultado.size());
        }

        long tEndTotal = System.currentTimeMillis();
        log.info("[generateQuestions] → Tiempo total de método: {} ms",
                (tEndTotal - tStartTotal));

        return resultado;
    }

    /**
     * (2) Pide a ChatGPT un lote de preguntas usando una plantilla aleatoria y parámetros dados.
     */
    private List<Game.Question> callChatGPTWithRandomizedPrompt(
            String category,
            String difficulty,
            String answerType,
            int batchSize,
            double temperature,
            double topP
    ) throws JsonProcessingException {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String prompt = buildPromptTemplate(category, difficulty, answerType, batchSize, runId);
        log.info("[callChatGPT] Usando prompt:\n{}", prompt);

        APIRequest req = buildChatGPTRequest(prompt, temperature, topP);
        String jsonReq = convertToJson(req);
        log.info("[callChatGPT] JSON de request a ChatGPT (primeros 200 chars): {}...",
                jsonReq.length() > 200 ? jsonReq.substring(0, 200) : jsonReq);

        HttpHeaders headers = createHttpHeaders(apiKey);
        HttpEntity<String> request = new HttpEntity<>(jsonReq, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, request, String.class
        );
        String content = extractContentFromResponse(response.getBody());
        log.info("[callChatGPT] Contenido crudo recibido de ChatGPT (primeros 300 chars): {}...",
                content.length() > 300 ? content.substring(0, 300) : content);

        // Parsear el array JSON puro de preguntas
        List<Game.Question> preguntas = new ArrayList<>();
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        log.info("[callChatGPT] Índices delimitadores JSON: start={}, end={}", start, end);
        if (start < 0 || end < 0 || end <= start) {
            log.error("[callChatGPT] No se encontró array JSON válido.");
            return preguntas;
        }
        String jsonArray = content.substring(start, end + 1);
        log.info("[callChatGPT] jsonArray extraído (primeros 200 chars): {}...",
                jsonArray.length() > 200 ? jsonArray.substring(0, 200) : jsonArray);

        JsonNode root = objectMapper.readTree(jsonArray);
        if (!root.isArray()) {
            log.error("[callChatGPT] El contenido extraído no es un array JSON.");
            return preguntas;
        }
        for (JsonNode node : root) {
            JsonNode preguntaNode = node.get("pregunta");
            JsonNode opcionesNode = node.get("opciones");
            JsonNode indiceNode   = node.get("indice_correcto");
            if (preguntaNode == null || !preguntaNode.isTextual() ||
                    opcionesNode == null || !opcionesNode.isArray() ||
                    indiceNode == null || !indiceNode.canConvertToInt()) {
                log.warn("[callChatGPT] Nodo omitido por formato incorrecto: {}", node);
                continue;
            }
            String texto = preguntaNode.asText();
            List<String> opciones = new ArrayList<>();
            for (JsonNode opt : opcionesNode) {
                opciones.add(opt.asText());
            }
            int idx = indiceNode.asInt();
            Game.Question q = Game.Question.builder()
                    .question(texto)
                    .answers(opciones)
                    .correctAnswerIndex(idx)
                    .embedding(null) // se llenará más adelante
                    .build();
            preguntas.add(q);
        }
        log.info("[callChatGPT] Número de preguntas parseadas: {}", preguntas.size());
        return preguntas;
    }

    /**
     * (3) Construye un prompt que garantice devolver siempre un array JSON
     *     en el formato correcto, según answerType ("CUATRO_RESPUESTAS" o "VERDADERO_FALSO").
     */
    private String buildPromptTemplate(
            String category,
            String difficulty,
            String answerType,
            int numQuestions,
            String runId
    ) {
        if ("CUATRO_RESPUESTAS".equals(answerType)) {
            String[] templates = new String[] {
                    "/* RUN: %s */ Genera exactamente %d preguntas de opción múltiple sobre \"%s\" "
                            + "(dificultad \"%s\"). Devuélvelas ÚNICAMENTE como un array JSON puro, "
                            + "sin ningún texto antes ni después, con este formato:\n"
                            + "[\n"
                            + "  {\"pregunta\": \"\", \"opciones\": [\"\",\"\",\"\",\"\"], \"indice_correcto\": 0},\n"
                            + "  {\"pregunta\": \"\", \"opciones\": [\"\",\"\",\"\",\"\"], \"indice_correcto\": 0},\n"
                            + "  …\n"
                            + "]",
                    "/* ID: %s */ Por favor, crea exactamente %d preguntas de opción múltiple en la categoría \"%s\" "
                            + "(dificultad \"%s\"). El resultado debe ser sólo un array JSON puro, con los objetos en este formato:\n"
                            + "[ {\"pregunta\":\"\",\"opciones\":[\"\",\"\",\"\",\"\"],\"indice_correcto\":0}, … ] "
                            + "No incluyas ningún texto ni explicación extra.",
                    "/* TAG: %s */ Necesito un array JSON puro que contenga %d preguntas de opción múltiple "
                            + "sobre \"%s\" (nivel \"%s\"). Cada elemento debe ser un objeto con los campos exactos:\n"
                            + "  \"pregunta\" (String), \"opciones\" (Array de 4 strings), \"indice_correcto\" (entero 0–3).\n"
                            + "Devuélvelo SIN texto adicional ni envoltura; sólo el array JSON cerrado con corchetes."
            };
            String tpl = templates[new Random().nextInt(templates.length)];
            return String.format(tpl, runId, numQuestions, category, difficulty);

        } else if ("VERDADERO_FALSO".equals(answerType)) {
            String[] templates = new String[] {
                    "/* RUN: %s */ Genera exactamente %d preguntas de Verdadero/Falso sobre \"%s\" "
                            + "(dificultad \"%s\"). Devuélvelas ÚNICAMENTE como un array JSON puro, "
                            + "sin ningún texto antes ni después, con este formato:\n"
                            + "[\n"
                            + "  {\"pregunta\": \"\", \"opciones\": [\"Verdadero\",\"Falso\"], \"indice_correcto\": 0},\n"
                            + "  {\"pregunta\": \"\", \"opciones\": [\"Verdadero\",\"Falso\"], \"indice_correcto\": 1},\n"
                            + "  …\n"
                            + "]",
                    "/* ID: %s */ Por favor, crea exactamente %d preguntas de Verdadero/Falso en la categoría \"%s\" "
                            + "(dificultad \"%s\"). El resultado debe ser sólo un array JSON puro, con los objetos en este formato:\n"
                            + "[ {\"pregunta\":\"\",\"opciones\":[\"Verdadero\",\"Falso\"],\"indice_correcto\":0}, … ] "
                            + "No incluyas ningún texto ni explicación extra.",
                    "/* TAG: %s */ Necesito un array JSON puro que contenga %d preguntas de Verdadero/Falso "
                            + "sobre \"%s\" (nivel \"%s\"). Cada elemento debe ser un objeto con los campos exactos:\n"
                            + "  \"pregunta\" (String), \"opciones\" (Array [\"Verdadero\",\"Falso\"]), \"indice_correcto\" (0 o 1).\n"
                            + "Devuélvelo SIN texto adicional ni envoltura; sólo el array JSON cerrado con corchetes."
            };
            String tpl = templates[new Random().nextInt(templates.length)];
            return String.format(tpl, runId, numQuestions, category, difficulty);

        } else {
            throw new IllegalArgumentException("Tipo de respuesta no soportado: " + answerType);
        }
    }

    /**
     * (4) Construye la petición a ChatGPT con parámetros temperature y top_p variables.
     */
    private APIRequest buildChatGPTRequest(String content, double temperature, double topP) {
        return APIRequest.builder()
                .model("gpt-4o-mini")
                .messages(Arrays.asList(
                        new APIRequest.Message("system", content),
                        new APIRequest.Message("user", "Question game")
                ))
                .temperature(temperature)
                .top_p(topP)
                .max_tokens(1200)
                .build();
    }

    /**
     * (5) Convierte el objeto APIRequest a JSON.
     */
    private String convertToJson(APIRequest requestBody) {
        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting request body to JSON", e);
        }
    }

    /**
     * (6) Crea cabeceras HTTP con la API key.
     */
    private HttpHeaders createHttpHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    /**
     * (7) Extrae el contenido de texto de la respuesta de ChatGPT.
     */
    private String extractContentFromResponse(String responseBody) throws JsonProcessingException {
        APIResponse chatGPTResponse = objectMapper.readValue(responseBody, APIResponse.class);
        return chatGPTResponse.getChoices().stream()
                .findFirst()
                .map(APIResponse.Choice::getMessage)
                .map(APIResponse.Choice.Message::getContent)
                .orElseThrow(() -> new RuntimeException("No content found in response"));
    }

    /**
     * (8) Dado un texto y su respuesta correcta, pide detalles a ChatGPT.
     */
    public String getMoreInfo(String question, String correctAnswer) throws JsonProcessingException {
        String prompt = String.format(
                "Te he preguntado %s y he obtenido la respuesta %s. Dame más información en hasta 80 palabras.",
                question, correctAnswer
        );
        APIRequest requestBody = buildChatGPTRequest(prompt, 0.7, 1.0);
        String jsonReq = convertToJson(requestBody);
        log.info("[getMoreInfo] JSON request a ChatGPT: {}", jsonReq);
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonReq, createHttpHeaders(apiKey));
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, requestEntity, String.class
        );
        String content = extractContentFromResponse(response.getBody());
        log.info("[getMoreInfo] Respuesta de ChatGPT: {}", content);
        return content;
    }

    // -------------------------------------------------------------------
    // Clase interna para índice simple: O(N) por consulta, pero desacoplada
    // para que puedas reemplazarla fácilmente por HNSW si lo deseas.
    // -------------------------------------------------------------------
    private static class SimpleCosineIndex {
        private final List<float[]> vectors;

        public SimpleCosineIndex(List<float[]> initialVectors) {
            this.vectors = new ArrayList<>(initialVectors);
        }

        /**
         * Agrega un nuevo vector al índice (por ejemplo, embedding de una pregunta aceptada).
         */
        public void addVector(float[] vec) {
            this.vectors.add(vec);
        }

        /**
         * Devuelve true si existe al menos un vector en el índice cuya similitud
         * coseno con 'query' sea ≥ threshold.
         */
        public boolean hasSimilar(float[] query, double threshold) {
            for (float[] hist : vectors) {
                double sim = CosineSimilarityUtil.cosineSimilarity(query, hist);
                if (sim >= threshold) {
                    return true;
                }
            }
            return false;
        }
    }
}
