package com.minko.kafkatraining.service.kafka.blockinguser;

import com.minko.kafkatraining.domain.BlockingUserRequest;
import com.minko.kafkatraining.service.kafka.ProducerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockingUserService {

    private final ProducerService producer;

    private final BlockingUserStateStoreService storeService;

    @Value("${spring.kafka.topic.user-blocking-commands}")
    private String topic;


    public List<String> getBlockedForUserName(String userName) {
        return storeService.getBlockedForUserName(userName);
    }

    public String block(BlockingUserRequest request) {
        String user = request.getUserName();
        ProducerRecord<String, BlockingUserRequest> record = new ProducerRecord<>(topic, user, request);
        ProducerService.Result result = producer.send(record);
        if (!result.success()) {
            return String.format("Error Blocking user: %s", result.description());
        }
        return "User successfully Blocked";
    }

    public String unblock(@Valid BlockingUserRequest request) {
        String user = request.getUserName();
        ProducerRecord<String, BlockingUserRequest> record = new ProducerRecord<>(topic, user, request);
        ProducerService.Result result = producer.send(record);
        if (!result.success()) {
            return String.format("Error Unblocking user: %s", result.description());
        }
        return "User successfully Unblocked";
    }

}
