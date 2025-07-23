-- Работаю с топиком sent-messages из основного задания. Там все отправленные сообщения

-- 1. Анализ сообщений в реальном времени
-- 1.1 Создать поток из топика sent-messages:
CREATE STREAM message_stream (fromUserName VARCHAR, toUserName VARCHAR, text VARCHAR)
       WITH (KAFKA_TOPIC = 'sent-messages', VALUE_FORMAT = 'JSON');

-- Результат
SHOW STREAMS;


-- 1.2.1 Общее количество отправленных сообщений:
-- 1.2.1.1. Запросом:
SELECT COUNT(*) AS total_messages
FROM message_stream
EMIT CHANGES;

-- 1.2.1.2 Таблицей. При создании таблицы надо группировать, поэтому сгруппируем на константу
CREATE TABLE message_count AS
    SELECT 'constant' AS group_key, COUNT(*) AS total_messages
    FROM message_stream
    GROUP BY 'constant'
    EMIT CHANGES;

-- Результат
SELECT * FROM message_count  EMIT CHANGES;


-- 1.2.2 Количество уникальных получателей сообщений
CREATE TABLE unique_recipients AS
SELECT toUserName, COUNT(*) AS recipient_count
FROM message_stream
GROUP BY toUserName;

-- Результат
SELECT * FROM unique_recipients  EMIT CHANGES;


-- 2. Сбор статистики по пользователям
-- Кол-во сообщений отправленные пользователем и кол-во уникальных получателей
CREATE TABLE user_statistic AS
SELECT
    fromUserName AS user,
    COUNT(*) AS messages_sent,
    -- собираем уникальных получателей в массив
    ARRAY_LENGTH(COLLECT_SET(toUserName)) AS unique_recipients
FROM message_stream
GROUP BY fromUserName
EMIT CHANGES;

-- Результат
SELECT * FROM user_statistic  EMIT CHANGES;


-- Полезные запросы:
-- Посмотреть все таблицы:
SHOW TABLES;

-- Удалить таблицу:
DROP TABLE unique_recipients;

-- Удалить стрим
DROP STREAM message_stream;