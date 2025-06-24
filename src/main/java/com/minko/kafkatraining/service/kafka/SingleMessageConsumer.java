package com.minko.kafkatraining.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minko.kafkatraining.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.InvalidRecordException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class SingleMessageConsumer {

    @Value("${spring.kafka.topic}")
    private String topic;

    // Consumer, который считывает по 1 сообщению и отправляет ask после обработки автоматически (по завершению метода)
    // Использую kafka-spring и @KafkaListener

    @KafkaListener(
            topics = "${spring.kafka.topic}",
            groupId = "${spring.kafka.consumer-group-single}",
            containerFactory = "SingleFactoryDeadLetter",
            concurrency = "1" // кол-во потоков Listener-ов
    )
    public void readRecord(ConsumerRecord<String, String> kafkaRecord) {
        log.info("SingleMessageConsumer. New record in topic {}: {}", topic, kafkaRecord);

        try {
            String value = kafkaRecord.value();
            // имитирую ошибку десериализации
            String breakingValue = breakingJson(value);

            ObjectMapper objectMapper = new ObjectMapper();
            User user = objectMapper.readValue(breakingValue, new TypeReference<>() {});

            // все успешно - логируем объект
            log.info("SingleMessageConsumer. Read new user from topic {}: {}", topic, user);

        } catch (JsonProcessingException e) {
            String errorMessage = String.format("SingleMessageConsumer. Unable to parse kafka record: %s", e.getMessage());
            log.error(errorMessage, e);
            // кидаем эксепшен, чтобы сработал errorHandler
            throw new InvalidRecordException(errorMessage);
        }
    }

    public static String breakingJson(String value) {
        return value.substring(0, value.length() - 1);
    }

}
