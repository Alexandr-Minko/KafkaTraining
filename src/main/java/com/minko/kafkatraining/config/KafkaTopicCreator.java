package com.minko.kafkatraining.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicCreator {

    private final KafkaAdmin kafkaAdmin;


    @Value("${spring.kafka.topic.user-blocking-commands}")
    private String userBlockingCommandsTopic;

    @Value("${spring.kafka.topic.sent-messages}")
    private String sentMessagesTopic;

    @Value("${spring.kafka.topic.delivered-messages}")
    private String deliveredMessagesTopic;

    @Value("${spring.kafka.topic.blocked-messages}")
    private String blockedMessagesTopic;

    @Value("${spring.kafka.topic.bad-words}")
    private String badWordsTopic;


    @PostConstruct
    public void init() {
        NewTopic topic1 = new NewTopic(userBlockingCommandsTopic, 2, (short) 1);
        NewTopic topic2 = new NewTopic(sentMessagesTopic, 2, (short) 1);
        NewTopic topic3 = new NewTopic(deliveredMessagesTopic, 2, (short) 1);
        NewTopic topic4 = new NewTopic(blockedMessagesTopic, 2, (short) 1);
        NewTopic topic5 = new NewTopic(badWordsTopic, 2, (short) 1);
        kafkaAdmin.createOrModifyTopics(topic1, topic2, topic3, topic4, topic5);
    }

}