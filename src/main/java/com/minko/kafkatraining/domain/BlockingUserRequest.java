package com.minko.kafkatraining.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class BlockingUserRequest {

    @NotNull
    private BlockingAction action;

    @NotBlank
    private String userName;

    @NotBlank
    private String blockedUsername;

}
