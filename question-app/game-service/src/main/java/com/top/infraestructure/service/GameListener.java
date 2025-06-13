package com.top.infraestructure.service;

import com.top.application.interfaces.GameState;
import com.top.application.model.Game;
import com.top.application.model.Question;
import com.top.application.repository.GameRepository;
import com.top.avro.GameValue;
import com.top.avro.GenerateQuestionResponseKey;
import com.top.avro.GenerateQuestionResponseValue;
import com.top.infraestructure.mapper.GameKafkaMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class GameListener {

    @Autowired
    private KafkaTemplate<GenerateQuestionResponseKey, GenerateQuestionResponseKey> kafkaTemplate;

    @Autowired
    GameKafkaMapper gameKafkaMapper;
    @Autowired
    GameRepository gameRepository;

    @KafkaListener(topics = "api-response", groupId = "api-game-response")
    public void consumeFromTopicUser(ConsumerRecord<GenerateQuestionResponseKey, GenerateQuestionResponseValue> record) {
        try {
            GenerateQuestionResponseKey key = record.key();
            GenerateQuestionResponseValue value = record.value();

            System.out.println("Received message from topic api-response with key: " + key + " and value: " + value.toString());

            Game newGame = createGameFromResponse(key, value);
            gameRepository.save(newGame);
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }

    private Game createGameFromResponse(GenerateQuestionResponseKey key, GenerateQuestionResponseValue value) {
        String gameId = UUID.randomUUID().toString();
        List<Question> questions = createQuestions(value.getQuestions());

        return Game.builder()
                .name(gameId)
                .category(value.getCategory())
                .difficulty(value.getDifficulty())
                .answerType(value.getAnswerType())
                .date(LocalDateTime.now())
                .successes(0)
                .failures(0)
                .numQuestions((int) value.getNumQuestions())
                .idUser(key.getIdUser())
                .questions(questions)
                .gameState(String.valueOf(GameState.INPROGRESS))
                .build();
    }

    private List<Question> createQuestions(List<com.top.avro.Question> avroQuestions) {
        List<Question> questions = new ArrayList<>();
        for (com.top.avro.Question avroQuestion : avroQuestions) {
            Question question = new Question(avroQuestion.getQuestion(), avroQuestion.getAnswers(),avroQuestion.getCorrectAnswerIndex(), avroQuestion.getEmbedding());
            questions.add(question);
        }
        return questions;
    }

}