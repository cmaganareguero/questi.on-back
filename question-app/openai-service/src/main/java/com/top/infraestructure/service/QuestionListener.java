package com.top.infraestructure.service;

import com.top.application.model.Question;
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

@Service
@Slf4j
public class QuestionListener {

    final String API_RESPONSE_TOPIC = "api-response";

    @Autowired
    private KafkaTemplate<GenerateQuestionResponseKey, GenerateQuestionResponseValue> kafkaTemplateResponse;

    @Autowired
    private ChatGPTService chatGPTService;

    @KafkaListener(topics = "api-request", groupId = "api-request")
    public void consumeFromTopicUser(ConsumerRecord<GenerateQuestionRequestKey, GenerateQuestionRequestValue> record) {
        GenerateQuestionRequestKey key = record.key();
        GenerateQuestionRequestValue value = record.value();

        log.info("Received message from topic users with key: {} and value: {}", key, value);

        try {
            List<Question> questions = chatGPTService.generateVariedQuestionsFromSubcategories(
                    value.getCategory(), value.getDifficulty(), value.getNumQuestions(), value.getAnswerType()
            );

            sendResponseToKafka(key, value, questions);

        } catch (Exception e) {
            log.error("Error generating questions", e);
        }
    }

    private void sendResponseToKafka(GenerateQuestionRequestKey key, GenerateQuestionRequestValue value, List<Question> questions) {
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

    private List<com.top.avro.Question> mapToAvroQuestions(List<Question> questions) {
        List<com.top.avro.Question> avroQuestions = new ArrayList<>();
        for (Question question : questions) {
            com.top.avro.Question avroQuestion = new com.top.avro.Question();
            avroQuestion.setQuestion(question.getQuestion());
            avroQuestion.setAnswers(question.getAnswers());
            avroQuestion.setCorrectAnswerIndex(question.getCorrectAnswerIndex());
            avroQuestions.add(avroQuestion);
        }
        return avroQuestions;
    }
}