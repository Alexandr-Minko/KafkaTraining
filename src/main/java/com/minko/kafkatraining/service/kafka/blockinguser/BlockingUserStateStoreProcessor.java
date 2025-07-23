package com.minko.kafkatraining.service.kafka.blockinguser;

import com.minko.kafkatraining.domain.BlockingAction;
import com.minko.kafkatraining.domain.BlockingUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockingUserStateStoreProcessor implements FixedKeyProcessor<String, BlockingUserRequest, BlockingUserRequest> {

    private KeyValueStore<String, List<String>> store;

    @Override
    public void init(FixedKeyProcessorContext<String, BlockingUserRequest> context) {
        this.store = context.getStateStore(BlockingUserStateStoreService.STORE_NAME);
    }

    @Override
    public void process(FixedKeyRecord<String, BlockingUserRequest> record) {
        String key = record.key();
        BlockingUserRequest value = record.value();
        List<String> storedBlockedUsers = store.get(key);
        ArrayList<String> newBlockedUsers = new ArrayList<>();
        if (storedBlockedUsers != null && !storedBlockedUsers.isEmpty()) {
            newBlockedUsers.addAll(storedBlockedUsers);
        }

        BlockingAction action = value.getAction();
        String blockedUserName = value.getBlockedUsername();
        if (BlockingAction.BLOCKING.equals(action)) {
            newBlockedUsers.add(blockedUserName);
        } else if (BlockingAction.UNBLOCKING.equals(action)) {
            newBlockedUsers.remove(blockedUserName);
        }

        store.put(key, newBlockedUsers);
    }

}