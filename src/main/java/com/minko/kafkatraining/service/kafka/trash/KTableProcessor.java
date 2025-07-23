package com.minko.kafkatraining.service.kafka.trash;

import com.minko.kafkatraining.domain.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KTable;
import org.springframework.kafka.support.serializer.JsonSerde;

@Slf4j
//@Component
@RequiredArgsConstructor
public class KTableProcessor {


//    @Bean
    public KTable<String, Message> kStream(StreamsBuilder kStreamBuilder) {
        Serde<Message> userSerde = new JsonSerde<>(Message.class);

/*        return kStreamBuilder
                .stream("top1", Consumed.with(Serdes.String(), userSerde))
                // Группируем по полю name
                .groupBy(
                        (key, user) -> user.getName(),
                        Grouped.with(Serdes.String(), userSerde)
                )
                // Агрегация: создаем новый User с суммированным count
                .aggregate(
                        Message::new,
                        (key, newValue, aggregate) -> {
                            aggregate.setName(newValue.getName());
                            aggregate.setTopic(newValue.getTopic());
                            aggregate.setKey(newValue.getKey());
                            aggregate.setDescription(newValue.getDescription());
                            // Суммируем count
                            aggregate.setCount(
                                    (aggregate.getCount() == null ? 0 : aggregate.getCount())
                                            + (newValue.getCount() == null ? 0 : newValue.getCount())
                            );
                            return aggregate;
                        },
                        Materialized.as("store1")
                );*/
        return null;
    }


}
