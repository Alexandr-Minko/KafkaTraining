package com.minko.kafkatraining.service.kafka.badwords;

import com.minko.kafkatraining.service.kafka.ProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadWordsService {

    private final ProducerService producer;

    private final BadWordsGlobalKTable globalKTable;

    @Value("${spring.kafka.topic.bad-words}")
    private String topic;


    public List<String> getAll() {
        return globalKTable.getAllWords();
    }

    public String block(String word) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, word, word);
        ProducerService.Result result = producer.send(record);
        if (!result.success()) {
            return String.format("Error Blocking word: %s", result.description());
        }
        return "Word Blocking successfully";
    }

    public String unblock(String word) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, word, null);
        ProducerService.Result result = producer.send(record);
        if (!result.success()) {
            return String.format("Error Unlocking word: %s", result.description());
        }
        return "Word Unlocking successfully";
    }

}
