package com.minko.kafkatraining.api;


import com.minko.kafkatraining.service.kafka.badwords.BadWordsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = APITags.BAD_WORD)
@RequestMapping("api/bad-words")
public class BadWordsRest {

    private final BadWordsService service;


    @GetMapping
    @Operation(summary = "Список запрещенных слов")
    public List<String> getAll() {
        return service.getAll();
    }

    @PostMapping
    @Operation(summary = "Заблокировать слово", description = "вводить БЕЗ КОВЫЧЕК")
    public String block(@RequestBody String word) {
        return service.block(word);
    }

    @PutMapping
    @Operation(summary = "Разблокировать слово")
    public String unblock(@RequestBody String word) {
        return service.unblock(word);
    }

}
