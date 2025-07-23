package com.minko.kafkatraining.dto;

import com.minko.kafkatraining.domain.BlockingUserRequest;
import lombok.Data;

@Data
public class BlockingUserRequestDto {

    private String userName;

    private String blockedUsername;

    public BlockingUserRequest toDomain() {
        return BlockingUserRequest.builder()
                .userName(userName)
                .blockedUsername(blockedUsername)
                .build();
    }

}
