package com.top.infraestructure.service;

import com.top.application.model.Game;
import com.top.avro.GenerateQuestionRequestKey;
import com.top.avro.GenerateQuestionRequestValue;
import com.top.avro.GenerateQuestionResponseKey;
import com.top.avro.GenerateQuestionResponseValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class QuestionListener {

    private static final String API_RESPONSE_TOPIC = "api-response";

    @Autowired
    private KafkaTemplate<GenerateQuestionResponseKey, GenerateQuestionResponseValue> kafkaTemplateResponse;

    @Autowired
    private APIService apiService;

    /**
     * Escucha en el topic "api-request". Cuando recibe un mensaje que contiene:
     *   - key: GenerateQuestionRequestKey (con userId en getId())
     *   - value: GenerateQuestionRequestValue (con categoría, dificultad, numQuestions, answerType)
     *
     * Llama a APIService.generateQuestionsWithEmbeddings(...) para:
     *   1. Solicitar X preguntas a ChatGPT.
     *   2. Calcular el embedding de cada pregunta.
     *   3. Devolver List<Game.Question> con cada pregunta + su vector de embedding.
     *
     * A continuación envía esa lista por Kafka al topic "api-response", en un GenerateQuestionResponseValue
     * que contiene:
     *   - category, numQuestions, difficulty, answerType
     *   - questions: array de registros Avro, cada uno con campos: question, answers, correctAnswerIndex, embedding.
     */
    @KafkaListener(topics = "api-request", groupId = "api-request")
    public void consumeFromTopicUser(ConsumerRecord<GenerateQuestionRequestKey, GenerateQuestionRequestValue> record) {
        GenerateQuestionRequestKey key = record.key();
        GenerateQuestionRequestValue value = record.value();

        log.info("Received message from topic api-request with key: {} and value: {}", key, value);

        try {
            // Extraemos userId directamente del GenerateQuestionRequestKey
            String userId = key.getId();

            // Llamada al nuevo método: genera preguntas + embeddings
            List<Game.Question> questions = apiService.generateQuestionsWithEmbeddings(
                    value.getCategory(),
                    value.getDifficulty(),
                    value.getNumQuestions(),
                    value.getAnswerType()
            );

            sendResponseToKafka(key, value, questions);

        } catch (Exception e) {
            log.error("Error generating questions with embeddings", e);
        }
    }

    /**
     * Construye el mensaje Avro de salida (GenerateQuestionResponseValue) y lo envía
     * al topic "api-response". Mapea cada Game.Question a com.top.avro.Question,
     * incluyendo el campo embedding (array de floats).
     */
    private void sendResponseToKafka(
            GenerateQuestionRequestKey key,
            GenerateQuestionRequestValue value,
            List<Game.Question> questions
    ) {
        GenerateQuestionResponseKey responseKey = new GenerateQuestionResponseKey();
        responseKey.setIdUser(key.getId());

        GenerateQuestionResponseValue responseValue = new GenerateQuestionResponseValue();
        responseValue.setNumQuestions(value.getNumQuestions());
        responseValue.setDifficulty(value.getDifficulty());
        responseValue.setCategory(value.getCategory());
        responseValue.setAnswerType(value.getAnswerType());

        responseValue.setQuestions(mapToAvroQuestions(questions));

        kafkaTemplateResponse.send(API_RESPONSE_TOPIC, responseKey, responseValue);
    }

    /**
     * Mapea cada Game.Question (Java) a com.top.avro.Question (Avro),
     * copiando: question, answers, correctAnswerIndex y embedding (lista de floats).
     */
    private List<com.top.avro.Question> mapToAvroQuestions(List<Game.Question> questions) {
        List<com.top.avro.Question> avroQuestions = new ArrayList<>();
        for (Game.Question question : questions) {
            com.top.avro.Question avroQuestion = new com.top.avro.Question();

            avroQuestion.setQuestion(question.getQuestion());
            avroQuestion.setAnswers(question.getAnswers());
            avroQuestion.setCorrectAnswerIndex(question.getCorrectAnswerIndex());

            // Aquí asignamos el campo embedding (List<Float>) al array<float> de Avro
            // Asegúrate de que tu esquema Avro para Question tenga:
            //   { "name": "embedding", "type": { "type": "array", "items": "float" } }
            avroQuestion.setEmbedding(question.getEmbedding());

            avroQuestions.add(avroQuestion);
        }
        return avroQuestions;
    }
}
