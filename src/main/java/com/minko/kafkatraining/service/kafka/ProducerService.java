package com.minko.kafkatraining.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public <T> Result send(ProducerRecord<String, T> record) {
        String topic = record.topic();
        String key = record.key();
        T value = record.value();
        String jsonValue = null;
        try {
            if (value != null) {
                jsonValue = new ObjectMapper().writeValueAsString(value);
            }

            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, jsonValue);
            kafkaTemplate.send(producerRecord).join();

            log.info("[{}] Sent message: {}", topic, jsonValue);
            return new Result(true, "Message sent successfully");

        } catch (RuntimeException | JsonProcessingException e) {
            log.error("[{}] Error sending message ({}): {}", topic, jsonValue, e.getMessage());
            return new Result(false, e.getMessage());
        }
    }

    public record Result(boolean success, String description) {
    }

}
