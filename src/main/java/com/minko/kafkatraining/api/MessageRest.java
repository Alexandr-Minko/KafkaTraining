package com.minko.kafkatraining.api;


import com.minko.kafkatraining.domain.Message;
import com.minko.kafkatraining.service.kafka.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/message")
@Tag(name = APITags.MESSAGE)
public class MessageRest {

    private final MessageService service;

    @PostMapping
    @Operation(summary = "Отправить сообщение")
    public String send(@RequestBody @Valid Message message) {
        return service.send(message);
    }

    @GetMapping("/send")
    @Operation(summary = "Отправленные сообщения")
    public List<String> getAllSend(String userName) {
        return service.getAllSend(userName);
    }

    @GetMapping("/delivered")
    @Operation(summary = "Доставленные сообщения")
    public List<String> getAllDelivered(String userName) {
        return service.getAllDelivered(userName);
    }

    @GetMapping("/blocked")
    @Operation(summary = "Заблокированные сообщения")
    public List<String> getAllBlocked(String userName) {
        return service.getAllBlocked(userName);
    }

}
