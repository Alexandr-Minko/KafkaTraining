package com.minko.kafkatraining.service.kafka;

import com.minko.kafkatraining.domain.Message;
import com.minko.kafkatraining.service.kafka.badwords.BadWordsGlobalKTable;
import com.minko.kafkatraining.service.kafka.blockinguser.BlockingUserStateStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaStreamBrancher;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainStreamService {

    private final BlockingUserStateStoreService storeService;

    private final BadWordsGlobalKTable badWordsGlobalKTable;

    @Value("${spring.kafka.topic.sent-messages}")
    private String topicSentMessages;

    @Value("${spring.kafka.topic.delivered-messages}")
    private String topicDeliveredMessages;

    @Value("${spring.kafka.topic.blocked-messages}")
    private String topicBlockedMessages;


    @Bean
    public KStream<String, Message> blockUserCensorBrunchStream(StreamsBuilder kStreamBuilder) {

        KStream<String, Message> inputStream = kStreamBuilder
                .stream(topicSentMessages, Consumed.with(Serdes.String(), new JsonSerde<>(Message.class)));

        inputStream
                .peek((key, value) -> log.info("[{}] New message: {} : {}", topicSentMessages, key, value));

        // Разделить поток на потоки. Аналог .split() только от Spring. Обратная операция - merge()
        new KafkaStreamBrancher<String, Message>()
                .branch((key, mes) -> isBlockedForUser(mes.getFromUserName(), mes.getToUserName()),
                        (stream) -> stream.to(topicBlockedMessages))
                .defaultBranch(deliveredStream -> {

                    // маскируем запрещённые слова
                    var censoredStream = deliveredStream.mapValues(message -> {
                        String originalText = message.getText();
                        String censoredText = getCensorText(originalText);
                        return message.toBuilder()
                                .text(censoredText)
                                .build();
                    });
                    censoredStream.to(topicDeliveredMessages);
                })
                .onTopOf(inputStream); // тут указываем stream который делим

        return inputStream;

        /*
            А это родной метод деления от kafka stream
            .split()
                .branch((key, user) -> "branch1".equals(user.getSurname()), Branched.withConsumer((ks) -> ks.to( "branch1")))
                .branch((key, user) -> "branch2".equals(user.getSurname()), Branched.withConsumer((ks) -> ks.to( "branch2")))
                .defaultBranch(Branched.withConsumer((ks) -> ks.to( "unknown")));
        */
    }

    public String getCensorText (String text) {
        List<String> badWords = badWordsGlobalKTable.getAllWords();

        StringBuilder result = new StringBuilder();
        String[] words = text.split("\\s+");

        for (String word : words) {
            if (badWords.contains(word)) {
                result.append("<censored>");
            } else {
                result.append(word);
            }
            result.append(" ");
        }

        if (!result.isEmpty()) result.setLength(result.length() - 1);
        return result.toString();
    }

    public boolean isBlockedForUser(String userName, String blockedUser) {
        List<String> blockedList = storeService.getBlockedForUserName(userName);
        if (blockedList.isEmpty()) {
            return false;
        }
        return blockedList.contains(blockedUser);
    }

}
