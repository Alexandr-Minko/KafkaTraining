package com.minko.kafkatraining.service.kafka.blockinguser;

import com.minko.kafkatraining.domain.BlockingUserRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.streams.KafkaStreamsInteractiveQueryService;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockingUserStateStoreService {

    @Value("${spring.kafka.topic.user-blocking-commands}")
    private String topic;

    public static final String STORE_NAME = "blocked-users";

    private final KafkaStreamsInteractiveQueryService interactiveQueryService;

    @Bean
    public KStream<String, BlockingUserRequest> userBlockingCommandsStream(StreamsBuilder kStreamBuilder) {

        kStreamBuilder.addStateStore(
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(STORE_NAME),
                        Serdes.String(),
                        Serdes.ListSerde(ArrayList.class, Serdes.String())));

        KStream<String, BlockingUserRequest> inputStream = kStreamBuilder
                .stream(topic, Consumed.with(Serdes.String(), new JsonSerde<>(BlockingUserRequest.class)));

        return inputStream
                .peek((key, value) -> log.info("[{}] New message: {} : {}", topic, key, value))
                .processValues(BlockingUserStateStoreProcessor::new, STORE_NAME);
    }

    // Иногда ловлю InvalidStateStoreException что хранилище не RUNNING. Добавил ретрай, вроде норм
    @Retryable(
            retryFor = {InvalidStateStoreException.class},
            maxAttempts = 20,
            backoff = @Backoff(delay = 1000L))
    public @NotNull List<String> getBlockedForUserName(String userName) {
        ReadOnlyKeyValueStore<String, List<String>> store =
                interactiveQueryService.retrieveQueryableStore(STORE_NAME, QueryableStoreTypes.keyValueStore());
        List<String> blockedUsers = null;
        blockedUsers = store.get(userName);
        if (blockedUsers == null) {
            return List.of();
        }
        return blockedUsers;
    }

    @Recover
    private List<String> recover(InvalidStateStoreException exception) {
        log.error("[{}] 20 repeated attempts. Error: {}", STORE_NAME, exception.getMessage());
        return List.of();
    }
}
