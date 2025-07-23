package com.minko.kafkatraining.service.kafka.trash;

import com.minko.kafkatraining.domain.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;

@Slf4j
//@Component
@RequiredArgsConstructor
public class JoinProcessor {


//    @Bean
    public KStream<String, Message> kStream(StreamsBuilder kStreamBuilder) {
        Serde<Message> userSerde = new JsonSerde<>(Message.class);

        KStream<String, Message> inputStream1 = kStreamBuilder
                .stream("top1", Consumed.with(Serdes.String(), userSerde));

        KStream<String, Message> inputStream2 = kStreamBuilder
                .stream("top2", Consumed.with(Serdes.String(), userSerde));

        KStream<String, Message> joined = inputStream1
                .join(inputStream2,
                        new Joiner(),
                        JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(10000)),
                        StreamJoined.with(
                                Serdes.String(),    // Сериализатор ключей
                                userSerde,          // Сериализатор значений первого стрима
                                userSerde           // Сериализатор значений второго стрима
                        )
                );

        joined
                .peek((key, user) -> log.info("Joined! [key: {}] {}", key, user))
                .to("join", Produced.with(Serdes.String(), userSerde));


        return joined;
    }

}
