package com.minko.kafkatraining.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minko.kafkatraining.domain.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.InvalidRecordException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchMessageConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate; // для отправки в DLQ

    @Value("${spring.kafka.topic}")
    private String topic;

    // чтобы продемонстрировать что я научился вручную управлять отправкой ask, сделал следующую локику:
    // читаю из kafka пачками, а отправляю ask после обработки каждого сообщения

    // условие читать минимум по 10 записей выполнено за счет fetch.min.bytes и fetch.max.wait.ms
    // Число достигается за счет: размер 1 сообщения х 10


    @KafkaListener(
            topics = "${spring.kafka.topic}",
            groupId = "${spring.kafka.consumer-group-batch}",
            containerFactory = "batchFactory",
            concurrency = "1"
    )
    public void readRecords(List<ConsumerRecord<String, String>> kafkaRecords, Acknowledgment acknowledgment) {
        log.info("BatchMessageConsumer. New records in topic {}: {}", topic, kafkaRecords);

        for (int i = 0; i < kafkaRecords.size(); i++) {
            ConsumerRecord<String, String> record = kafkaRecords.get(i);
            String value = record.value();
            try {
                // имитируем ошибку десериализации
                value = breakingJson(value);

                User user = objectMapper.readValue(value, new TypeReference<>() {});
                log.info("BatchMessageConsumer. Read new user from topic {}: {}", topic, user);

            } catch (JsonProcessingException e) {
                String errorMessage = String.format("SingleMessageConsumer. Unable to parse kafka record: %s", e.getMessage());
                log.error(errorMessage, e);

                // Отправляем в DLQ. Наверняка можно красивее через объект spring, но пофик
                String dlqTopic = topic + "-dlt";
                kafkaTemplate.send(dlqTopic, value);
            }
            // отправка ask с указанием индекса из полученной пачки
            acknowledgment.acknowledge(i);
        }
    }

    public static String breakingJson(String value) {
        return value.substring(0, value.length() - 1);
    }
}
