package com.minko.kafkatraining.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class APITags {

    public static final String MESSAGE = "Отправка / получение сообщений";

    public static final String BLOCKING_USER = "Блокировка пользователей";

    public static final String BAD_WORD = "Запрещенные слова";

}
