package com.minko.kafkatraining.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Message {

    @NotBlank
    private String fromUserName;

    @NotBlank
    private String toUserName;

    @NotBlank
    private String text;

}
