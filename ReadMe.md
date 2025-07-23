### 1. Как запустить проект:
1. Скачай проект в папку KafkaTraining
2. Запусти все сервисы из docker-compose.yml (3 брокера kafka, kafka-ui, kSQLdb, kSQLdb-cli)
3. Через IntelliJ IDEA запусти 2 экземпляра spring boot приложения на разных портах, например 8081 и 8082. Сохранил конфигурации запуска
4. При запуске будут созданы нужные топики. Есть вероятность, что stream запуститься быстрей (будет ошибка, что топика нет) - просто перезапусти приложение
5. Убедится что открывается swagger UI: (http://localhost:8081 для инстанса на порту 8081)

### 2. Как работает приложение
1. Отправляем сообщение (POST /api/message): новое сообщение в топик sent-messages (key: fromUserName, value: JSON (fromUserName, toUserName, text))
2. Его подхватывает основной kafka stream флоу (MainStreamService)
   1. Делит поток на 2: delivered-messages и blocked-messages (в зависимости от того, находится ли получатель сообщения в блоке у отправителя или нет)
   2. Выполняет цензуру сообщений в потоке delivered-messages (Маскирование запрещенных слов словом <censored>. Маскируются только слова окруженные пробелами)
3. Список заблокированных пользователей сохраняется в state store (key: userName, value: List<String> blockedUsername) из топика user-blocking-commands. Да, с сохранением на HDD.
Это делает флоу kafka stream №2 (BlockingUserStateStoreService -> BlockingUserStateStoreProcessor).
Если пришла команда BLOCKING, то в state store добавляем в список заблокированных этого пользователя, если UNBLOCKING - удаляем.
Так как ключ в сообщении и в команде управления блокировкой одинаков (имя отправителя), то state store заблокированных будет всегда на том же инстансе, где обрабатывается сообщение.
4. Список запрещенных слов это GlobalKTable из топика bad-words с сохранением в state store (key: word, value: word) (класс BadWordsGlobalKTable)
Чтобы убрать слово из блокировки, отправляю в топик bad-words сообщение с key=word, value=null

### 3. Возможные ошибки
1. Несколько раз ловил InvalidStateStoreException при получении значения из state store (что оно еще не RUNNING).
Добавил 20 ретраев на него, а после возвращаю пустой список. Лень было долго разбираться
2. Конечно в проде надо перед получением ключа из state store проверять где по мнению кафки лежит этот ключ,
а то можно попасть на, то что владелец ключа упал, а ребалансировка еще не закончилась. Такой метод есть:
HostInfo kafkaStreamsApplicationHostInfo = this.interactiveQueryService.getKafkaStreamsApplicationHostInfo("app-store", 12345, new IntegerSerializer());
3. На пакет com.minko.kafkatraining.service.kafka.trash не обращать внимание - тут эксперименты во время обучения.
Пробовал JOIN, фильтрацию, агрегацию, repartition

### 4. kSQLdb. Как проверить мои запросы
1. Как говорил ранее, нужно было запустить все сервисы их docker-compose.yml (3 брокера kafka, kafka-ui, kSQLdb, kSQLdb-cli)
2. Выполняем в терминале: docker-compose exec ksqldb-cli ksql http://ksqldb:8088 (http://ksqldb:8088 - url ksqldb)
3. И проверяем запросы из файла ksqldb-queries.sql
4. Пробовал в качестве UI для kSQLdb https://github.com/deniskrumko/ksqldb-ui. Работает. Но при SELECT * FROM ... EMIT CHANGES падает на таймауте
(именно при наличии EMIT CHANGES. Если статично - работает). В итоге сделал все через CLI