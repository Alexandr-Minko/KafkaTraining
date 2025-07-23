package com.minko.kafkatraining.api;


import com.minko.kafkatraining.domain.BlockingAction;
import com.minko.kafkatraining.domain.BlockingUserRequest;
import com.minko.kafkatraining.dto.BlockingUserRequestDto;
import com.minko.kafkatraining.service.kafka.blockinguser.BlockingUserService;
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
@Tag(name = APITags.BLOCKING_USER)
@RequestMapping("api/blocking-user")
public class BlockingUserRest {

    private final BlockingUserService service;


    @GetMapping
    @Operation(summary = "Список заблокированных для пользователя",
            description = "Я не заморачивался с получением удаленного state store, поэтому при нескольких инстансах" +
                    " будет работать только на том, кому назначена партиция с этим ключом")
    public List<String> get(@RequestParam String userName) {
        return service.getBlockedForUserName(userName);
    }

    @PostMapping
    @Operation(summary = "Заблокировать пользователя")
    public String block(@RequestBody @Valid BlockingUserRequestDto dto) {
        BlockingUserRequest request = dto.toDomain();
        request.setAction(BlockingAction.BLOCKING);
        return service.block(request);
    }

    @PutMapping
    @Operation(summary = "Разблокировать пользователя")
    public String unblock(@RequestBody @Valid BlockingUserRequestDto dto) {
        BlockingUserRequest request = dto.toDomain();
        request.setAction(BlockingAction.UNBLOCKING);
        return service.unblock(request);
    }

}
