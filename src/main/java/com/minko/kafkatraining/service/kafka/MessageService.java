package com.minko.kafkatraining.service.kafka;

import com.minko.kafkatraining.domain.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageService {

    private final ProducerService producer;

    private final ConsumerFactory<String, String> consumerFactory;

    @Value("${spring.kafka.topic.sent-messages}")
    private String topicSentMessages;

    @Value("${spring.kafka.topic.delivered-messages}")
    private String topicDeliveredMessages;

    @Value("${spring.kafka.topic.blocked-messages}")
    private String topicBlockedMessages;


    public String send(Message message) {
        String key = message.getFromUserName();
        ProducerRecord<String, Message> record = new ProducerRecord<>(topicSentMessages, key, message);
        ProducerService.Result result = producer.send(record);

        if (!result.success()) {
            return String.format("Error sending message: %s", result.description());
        }
        return "Message has been sent";
    }

    public List<String> getAllSend(String userName) {
        return getAllTopicRecords(topicSentMessages, userName);
    }

    public List<String> getAllDelivered(String userName) {
        return getAllTopicRecords(topicDeliveredMessages, userName);
    }

    public List<String> getAllBlocked(String userName) {
        return getAllTopicRecords(topicBlockedMessages, userName);
    }



    // Прочитать все сообщения, начиная с первого
    // В учебных целях. Так не надо делать =)
    public List<String> getAllTopicRecords(String topic, String userName) {
        List<String> messages = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd:MM:yyyy");
        int counter = 1;

        String groupID = UUID.randomUUID().toString();  // чтобы каждый инстанс читал все партиции
        try (Consumer<String, String> consumer = consumerFactory.createConsumer(null, groupID)) {
            subscribeFromBeginning(consumer, topic); // финт ушами, чтобы читать топик каждый раз с первого сообщения

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                if (records.isEmpty()) {
                    break;

                } else {
                    for (ConsumerRecord<String, String> record : records) {
                        if (!userName.equals(record.key())) continue;

                        long timestampMillis = record.timestamp();
                        LocalDateTime timestamp = LocalDateTime.ofInstant
                                (Instant.ofEpochMilli(timestampMillis), ZoneId.of("Europe/Moscow"));

                        String jsonValue = record.value().replaceAll("\"", "'");
                        String formattedMessage = String.format("%d, %s, %s, %s",
                                counter++,
                                formatter.format(timestamp),
                                record.key(),
                                jsonValue
                        );

                        messages.add(formattedMessage);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return messages;
    }

    private void subscribeFromBeginning(Consumer<String, String> consumer, String topic) {
        consumer.subscribe(Collections.singletonList(topic));
        consumer.poll(Duration.ofMillis(1));
        Set<TopicPartition> partitions = consumer.assignment();
        while (partitions.isEmpty()) {
            consumer.poll(Duration.ofMillis(1));
            partitions = consumer.assignment();
        }
        consumer.seekToBeginning(partitions);
    }

}
