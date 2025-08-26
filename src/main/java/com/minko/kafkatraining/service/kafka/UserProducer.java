package com.minko.kafkatraining.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minko.kafkatraining.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic}")
    private String topic;

    public String send(User user) {
        try {
            String jsonValue = new ObjectMapper().writeValueAsString(user);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, jsonValue);
            kafkaTemplate.send(producerRecord).join();

            String response = String.format("Message sent to topic %s", topic);
            log.info(response);
            return response;

        } catch (RuntimeException | JsonProcessingException e) {
            String error = String.format("Error sending message to topic %s: %s", topic, e.getMessage());
            log.error(error);
            return error;
        }
    }
}
