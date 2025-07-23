package com.minko.kafkatraining.service.kafka.trash;

import com.minko.kafkatraining.domain.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.kstream.ValueJoiner;

@Slf4j
@RequiredArgsConstructor
public class Joiner implements ValueJoiner<Message, Message, Message> {

    @Override
    public Message apply(Message leftMessage, Message rightMessage) {
        return new Message();
//        return Message.builder()
//                .name(leftMessage.getName() + " + " + rightMessage.getName())
//                .description(leftMessage.getDescription() + " + " + rightMessage.getDescription())
//                .build();
    }
}
