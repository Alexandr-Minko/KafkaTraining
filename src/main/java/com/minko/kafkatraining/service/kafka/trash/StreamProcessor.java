package com.minko.kafkatraining.service.kafka.trash;

import com.minko.kafkatraining.domain.Message;
import com.minko.kafkatraining.service.kafka.blockinguser.BlockingUserStateStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.Stores;
import org.springframework.kafka.support.serializer.JsonSerde;

@Slf4j
//@Component
@RequiredArgsConstructor
public class StreamProcessor {

//    @Value("${spring.kafka.topic1}")
    private String topicIn = "sdsd";

//    public static final String STORE_NAME = "store1";


//    @Bean
    public KStream<String, Message> kStream(StreamsBuilder kStreamBuilder) {
        // добавить локальный state store
        kStreamBuilder.addStateStore(
                Stores.keyValueStoreBuilder(
                        // с хранением на hdd
                        Stores.persistentKeyValueStore(BlockingUserStateStoreService.STORE_NAME), Serdes.String(), Serdes.Integer()

                        // c хранение только в RAM
                        // Stores.inMemoryKeyValueStore(STORE_NAME), Serdes.String(), Serdes.Integer()
                )// настроить сохраннее store в topic .withLoggingEnabled() или отключить .withLoggingDisabled()
                );

        KStream<String, Message> inputStream = kStreamBuilder
                .stream(topicIn, Consumed.with(Serdes.String(), new JsonSerde<>(Message.class)));

        KStream<String, Message> processedStream = inputStream
                // сделать действие, не меняя поток
                .peek((key, user) -> log.info("New message! [key: {}] {}", key, user))

                // оставить в потоке удовлетворяющие предикату (filterNot() - НЕ удовлетворяющие)
//                .filter((key, user) -> user.getDescription() != null && !user.getDescription().isBlank())

                // Преобразовать только value. Есть еще map: преобразовать и key и value
                .mapValues(user -> user.toBuilder()
//                        .name(user.getName() + " - added kafka streams!")
                        .build())

                .peek((key, user) -> log.info("Filtered, change value: [key: {}] {}", key, user))

                // Поменять key. Произойдет автоматическое репартиционирование перед командами где важна партиция (join, marge..)
//                .selectKey((key, user) -> user.getKey())


                // .repartition() Изменить партицию. 2 варианта: 1. без параметров (если ранее изменили ключ. но смена партиции все равно позже произойдет само) и 2. с логикой выбора партиции
                // Как работает: создается служебный topic, туда перекидываются данные в нужные партиции и далее по флоу используется новый topic
                // Важно задавать правильный key, иначе будут не ожидаемо работать:
                // 1. State store: флоу kafka stream выполняет stream task -> 1 партицию обрабатывает 1 task -> у каждой task свой state store -> если данные попали в разные партиции, то и state store у них разный
                // 2. join, marge. Надо чтобы все записи с одним ключом обрабатывались 1 инстансом

                // Для работы со State store есть 2 метода. У обоих есть доступ к state store:
                // transform(): выполнить преобразование и вернуть KStream или KTable
                // process(Values): выполнить побочные действия, вернуть void (но можно вернуть через context.forward()))
//                .processValues(Processor2::new, BlockingUserStateStoreService.STORE_NAME)

                .peek((key, user) -> log.info("After Processor1 [key: {}] {}", key, user));

        return processedStream;
    }

}
