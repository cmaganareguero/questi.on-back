package com.top.infraestructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.top.application.model.ChatGPTRequest;
import com.top.application.model.ChatGPTResponse;
import com.top.application.model.Question;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatGPTService {

    @Value("${chatgpt.api.url}")
    private String apiUrl;

    @Value("${chatgpt.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public List<Question> generateQuestionsFromCategoryAndDifficulty(String category, String difficulty, int numberOfQuestions, String answerType) {
        List<Question> questions = new ArrayList<>();
        try {
            String prompt = buildPrompt(category, difficulty, numberOfQuestions, answerType);

            ChatGPTRequest requestBody = buildChatGPTRequest(prompt);
            String jsonRequestBody = convertToJson(requestBody);

            HttpHeaders headers = createHttpHeaders(apiKey);
            HttpEntity<String> request = new HttpEntity<>(jsonRequestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);
            String content = extractContentFromResponse(response.getBody());

            questions = parseQuestions(content, answerType);

        } catch (Exception e) {
            log.error("Error generating questions", e);
        }
        return questions;
    }

    private String buildPrompt(String category, String difficulty, int numQuestions, String answerType) {
        if ("CUATRO_RESPUESTAS".equals(answerType)) {
            return String.format("Proporcionarme %d preguntas de dificultad %s relacionadas con la categoría %s indicándome la respuesta correcta con el formato: 1. ¿Pregunta? (RespuestaCorrecta) (RespuestaIncorrecta) (RespuestaIncorrecta) (RespuestaIncorrecta). Es importante que todas las respuestas estén en la misma línea de respuesta.",
                    numQuestions, difficulty, category);
        } else if ("VERDADERO_FALSO".equals(answerType)) {
            return String.format("Proporcionarme %d preguntas de verdadero o falso de dificultad %s relacionadas con la categoría %s indicándome la respuesta correcta con el formato: 1. ¿Pregunta? (RespuestaCorrecta) (RespuestaIncorrecta). Es importante que todas las respuestas estén en la misma línea de respuesta.",
                    numQuestions, difficulty, category);
        } else {
            throw new IllegalArgumentException("Tipo de respuesta no soportado: " + answerType);
        }
    }

    private List<Question> parseQuestions(String content, String answerType) {
        if ("CUATRO_RESPUESTAS".equals(answerType)) {
            return formatQuestions(content);
        } else if ("VERDADERO_FALSO".equals(answerType)) {
            return formatQuestionsWithTwoAnswers(content);
        } else {
            throw new IllegalArgumentException("Tipo de respuesta no soportado: " + answerType);
        }
    }

    private List<Question> formatQuestions(String text) {
        List<Question> questions = new ArrayList<>();
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.matches("^\\d+\\.\\s.*")) {
                int qStart = line.indexOf('.') + 2;
                int qEnd = line.indexOf('?') + 1;
                String question = line.substring(qStart, qEnd).trim();

                String[] answerTokens = line.substring(qEnd + 1).trim().split("\\)\\s*\\(");
                List<String> answers = new ArrayList<>();
                for (String ans : answerTokens) {
                    ans = ans.replace("(", "").replace(")", "").trim();
                    answers.add(ans);
                }
                Question q = new Question(question, answers, 0);
                q.shuffleAnswers();
                questions.add(q);
            }
        }
        return questions;
    }

    private List<Question> formatQuestionsWithTwoAnswers(String text) {
        List<Question> questions = new ArrayList<>();
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.matches("^\\d+\\.\\s.*")) {
                int qStart = line.indexOf('.') + 2;
                int qEnd = line.indexOf('?') + 1;
                String question = line.substring(qStart, qEnd).trim();

                String[] answerTokens = line.substring(qEnd + 1).trim().split("\\)\\s*\\(");
                List<String> answers = new ArrayList<>();
                for (String ans : answerTokens) {
                    ans = ans.replace("(", "").replace(")", "").trim();
                    answers.add(ans);
                }
                if (answers.size() == 2) {
                    Question q = new Question(question, answers, 0);
                    q.shuffleAnswers();
                    questions.add(q);
                }
            }
        }
        return questions;
    }

    private ChatGPTRequest buildChatGPTRequest(String content) {
        return ChatGPTRequest.builder()
                .model("gpt-4o")
                .messages(Arrays.asList(
                        new ChatGPTRequest.Message("system", content),
                        new ChatGPTRequest.Message("user", "Question game")
                ))
                .temperature(0.7)
                .max_tokens(400)
                .top_p(1.0)
                .build();
    }

    private String convertToJson(ChatGPTRequest requestBody) {
        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting request body to JSON", e);
        }
    }

    private HttpHeaders createHttpHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private String extractContentFromResponse(String responseBody) throws JsonProcessingException {
        ChatGPTResponse chatGPTResponse = objectMapper.readValue(responseBody, ChatGPTResponse.class);
        return chatGPTResponse.getChoices().stream()
                .findFirst()
                .map(ChatGPTResponse.Choice::getMessage)
                .map(ChatGPTResponse.Choice.Message::getContent)
                .orElseThrow(() -> new RuntimeException("No content found in response"));
    }

    public String getMoreInfo(String question, String correctAnswer) throws JsonProcessingException {
        String prompt = String.format(
                "Te he preguntado %s y he obtenido la respuesta %s. Dame más información sobre por qué esta es la respuesta correcta en un máximo de 80 palabras. La explicación debe ajustarse a esas 80, sin que quede una frase final coherente.",
                question, correctAnswer
        );

        ChatGPTRequest requestBody = buildChatGPTRequest(prompt);
        String jsonRequestBody = convertToJson(requestBody);
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequestBody, createHttpHeaders(apiKey));

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);
        return extractContentFromResponse(response.getBody());
    }

    //Genera las subcategorias para tener una selección de prguntas variadas
    public List<String> generateSubcategories(String category) throws JsonProcessingException {
        String prompt = String.format(
                "Genera una lista de 10 subcategorías relacionadas con la categoría '%s'. Estas subcategorías deben ser específicas y variadas. Devuélveme solo la lista en el formato: [subcategoria1, subcategoria2, ...]",
                category
        );

        ChatGPTRequest request = buildChatGPTRequest(prompt);
        String jsonRequest = convertToJson(request);

        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, createHttpHeaders(apiKey));
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
        String content = extractContentFromResponse(response.getBody());

        return parseListFromBracketedString(content);
    }

    private List<String> parseListFromBracketedString(String rawList) {
        return Arrays.stream(rawList.replace("[", "")
                        .replace("]", "")
                        .split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public List<Question> generateVariedQuestionsFromSubcategories(String mainCategory, String difficulty, int numQuestions, String answerType) throws JsonProcessingException {
        List<String> subcategories = generateSubcategories(mainCategory);
        Collections.shuffle(subcategories);

        int numSubcats = Math.min(subcategories.size(), numQuestions);
        List<String> selectedSubcats = subcategories.subList(0, numSubcats);

        List<Question> result = new ArrayList<>();
        for (String subcat : selectedSubcats) {
            result.addAll(generateQuestionsFromCategoryAndDifficulty(subcat, difficulty, 1, answerType));
        }
        return result;
    }


}