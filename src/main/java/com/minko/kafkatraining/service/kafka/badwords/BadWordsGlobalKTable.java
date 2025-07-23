package com.minko.kafkatraining.service.kafka.badwords;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.streams.KafkaStreamsInteractiveQueryService;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadWordsGlobalKTable {

    private final KafkaStreamsInteractiveQueryService interactiveQueryService;

    @Value("${spring.kafka.topic.bad-words}")
    private String topic;

    private final String BAD_WORDS_STORE = "bad-words";


    @Bean
    public GlobalKTable<String, String> buildBadWordsGlobalKTable(StreamsBuilder kStreamBuilder) {
        return kStreamBuilder.globalTable(
                topic,
                Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as(BAD_WORDS_STORE)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.String())
        );
    }

    // Иногда ловлю InvalidStateStoreException что хранилище не RUNNING. Добавил ретрай, вроде норм
    @Retryable(
            retryFor = {InvalidStateStoreException.class},
            maxAttempts = 20,
            backoff = @Backoff(delay = 1000L))
    public List<String> getAllWords() {
        ReadOnlyKeyValueStore<String, String> store =
                interactiveQueryService.retrieveQueryableStore(BAD_WORDS_STORE, QueryableStoreTypes.keyValueStore());
        return getAllKeys(store);
    }

    @Recover
    private List<String> recover(InvalidStateStoreException exception) {
        log.error("[{}] 20 repeated attempts. Error: {}", BAD_WORDS_STORE, exception.getMessage());
        return List.of();
    }

    private List<String> getAllKeys(ReadOnlyKeyValueStore<String, String> store) {
        List<String> keys = new ArrayList<>();
        try (KeyValueIterator<String, String> iterator = store.all()) {
            while (iterator.hasNext()) {
                KeyValue<String, String> entry = iterator.next();
                keys.add(entry.key);
            }
        } catch (InvalidStateStoreException e) {
            throw new RuntimeException("Store is not initialized", e);
        }
        return keys;
    }

}
