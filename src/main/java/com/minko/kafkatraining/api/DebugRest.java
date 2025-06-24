package com.minko.kafkatraining.api;


import com.minko.kafkatraining.domain.User;
import com.minko.kafkatraining.service.kafka.UserProducer;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/user")
@RequiredArgsConstructor
public class DebugRest {

    private final UserProducer userProducer;

    @PostMapping
    @Operation(summary = "Отправить kafka")
    public String send(User user) {
        return userProducer.send(user);
    }
}
