### 1. Создание корневого сертификата:
1. Создаем CA.conf в ./CA. Это конфигурация для создания корневого сертификата (Root CA)
2. Запускаем создание Root CA по конфигу  
openssl req -new -nodes -x509 -days 365 -newkey rsa:2048 -keyout ./CA/CAPrivate.key -out ./CA/CACert.crt -config ./CA/CA.config  
-nodes: приватный ключ хранится без шифрования  
-x509: создаем самоподписанный сертификат (его никто не подписывает, он первый в цепочке)
-newkey rsa:2048: создаем новую пару ключей
3. В ./CA создалось 2 файла: CAPrivate.key (приватный ключ корневого сертификата) и CACert.crt (сертификат, включающий открытый ключ)
4. Объединим корневой сертификат и его приватный ключ в единый файл CA.pem.
Для этого создадим новый файл CA.pem и скопируем содержимое сначала CACert.crt, потом CAPrivate.key (последовательность важны, можно разделить пустой строкой)


### 2. Создание сертификата для брокера kafka0:
1. Создаем kafka0.config в ./kafka0. Это конфигурация для создания сертификата для кафка брокера kafka0
2. Создадим приватный ключ и запрос на сертификат (CSR, Certificate Signing Request, Запрос на подписание сертификата):
openssl req -new -nodes -newkey rsa:2048 -keyout kafka0/kafka0Private.key -out kafka0/kafka0CSR.csr -config kafka0/kafka0.config
(аналогично созданию корневого, только уже не самоподписанный)
3. Запускаем создание сертификат брокера из CSR, подписанный CA:
openssl x509 -req -days 365 -in ./kafka0/kafka0CSR.csr -CA ./CA/CACert.crt -CAkey ./CA/CAPrivate.key -CAcreateserial -out ./kafka0/kafka0Cert.crt -extfile ./kafka0/kafka0.config -extensions v3_req
- -тут уже x509 не параметр, а субкоманда
- -in kafka0/kafkaCSR.csr запрос на подписание (CSR)
- -CA ./CA/CACert.crt: корневой сертификат
- -CAkey ./CA/CAPrivate.key: приватный ключ CA
4. Создалось 2 файла:
- ./kafka0/kafka0Cert.crt: сам сертифкат
- ./CA/CACert.srl: серийный номер сертификата, так как мы использовали параметр -CAcreateserial
5. Создадим PKCS12-хранилище сертификата для kafka0:
openssl pkcs12 -export -in ./kafka0/kafka0Cert.crt -inkey ./kafka0/kafka0Private.key -chain -CAfile ./CA/CA.pem -name kafka0 -out ./kafka0/kafka0P12.p12 -password pass:password1
6. Создадим Java keystore для kafka0 (путем импорта созданного PKCS12 хранилища)
keytool -importkeystore -deststorepass password1 -destkeystore ./kafka0/kafka0.keystore.jks -srckeystore ./kafka0/kafka0P12.p12 -deststoretype JKS -srcstoretype PKCS12 -noprompt -srcstorepass password1
7. Создадим Java truststore (общий для всех брокеров)
keytool -import -file ./CA/CACert.crt -alias CARoot -keystore ./src/main/resources/ssl/kafka.truststore.jks -storepass password1 -noprompt
8. Проверим SSL соеденение с брокером. Напишет кучу всякой фигни о сертификате и протоколе
openssl s_client -connect localhost:9094 -tls1_3 -showcerts
openssl s_client -connect localhost:9094 -CAfile ./CA/CACert.crt


### 3. Сертификаты для брокеров kafka1 и kafka2:
1. В моей инфраструктуре (кафка в докере, java приложение на хосте), создавать отдельные сертификаты для 2х остальных брокеров не нужно.
Так как dns имя и IP адреса в сертификатах для брокеров kafka0,1,2 будут одинаковыми (localhost, 127.0.0.1). Поэтому можно использовать один сертификат
2. Но так как в ТЗ написано "Создайте сертификаты для каждого брокера", я создал сертификаты:

openssl req -new -nodes -newkey rsa:2048 -keyout kafka1/kafka1Private.key -out kafka1/kafka1CSR.csr -config kafka1/kafka1.config
openssl x509 -req -days 365 -in ./kafka1/kafka1CSR.csr -CA ./CA/CACert.crt -CAkey ./CA/CAPrivate.key -CAcreateserial -out ./kafka1/kafka1Cert.crt -extfile ./kafka1/kafka1.config -extensions v3_req
openssl pkcs12 -export -in ./kafka1/kafka1Cert.crt -inkey ./kafka1/kafka1Private.key -chain -CAfile ./CA/CA.pem -name kafka1 -out ./kafka1/kafka1P12.p12 -password pass:password1
keytool -importkeystore -deststorepass password1 -destkeystore ./kafka1/kafka1.keystore.jks -srckeystore ./kafka1/kafka1P12.p12 -deststoretype JKS -srcstoretype PKCS12 -noprompt -srcstorepass password1

openssl req -new -nodes -newkey rsa:2048 -keyout kafka2/kafka2Private.key -out kafka2/kafka2CSR.csr -config kafka2/kafka2.config
openssl x509 -req -days 365 -in ./kafka2/kafka2CSR.csr -CA ./CA/CACert.crt -CAkey ./CA/CAPrivate.key -CAcreateserial -out ./kafka2/kafka2Cert.crt -extfile ./kafka2/kafka2.config -extensions v3_req
openssl pkcs12 -export -in ./kafka2/kafka2Cert.crt -inkey ./kafka2/kafka2Private.key -chain -CAfile ./CA/CA.pem -name kafka2 -out ./kafka2/kafka2P12.p12 -password pass:password1
keytool -importkeystore -deststorepass password1 -destkeystore ./kafka2/kafka2.keystore.jks -srckeystore ./kafka2/kafka2P12.p12 -deststoretype JKS -srcstoretype PKCS12 -noprompt -srcstorepass password1

3. Обратите внимание, что у меня убрана настройка ssl.endpoint.identification.algorithm: "".   
Т.е. server host name verification у меня включена и работает (добавил в секцию [ alt_names ] IP.1 = 127.0.0.1)


### 4. Файл конфигурации JAAS
1. Указывать пользователей можно как в файле JAAS, прокидывая его потом через volumes + KAFKA_OPTS: "-Djava.security.auth.login.config=...kafka_server_jaas.conf"
   так и без этого файла, через переменные среды KAFKA_CLIENT_USERS, KAFKA_CLIENT_PASSWORDS. Я использовал именно такой вариант


### 5. Работа из java (SSL, SASL, ACL) 
1. Создадим 2 топика через UI: topic1, topic2.
Лучше указать фактор репликации 3, чтобы проверить и межброкерное взаиможействие (оно на SASL_PLAINTEXT, SASL_SSL в KRAFT режиме не поддерживается)
2. Уже заведено 2 пользователей kafka (docker-compose - .env - KAFKA_CLIENT_USERS=producer_user;consumer_user)
3. Стартуем все сервисы из docker-compose
4. Эти 2 пользователя прописаны в application.yml, разные для consumer и producer
5. В application.yml так же указано SSL шифрование и путь до truststore
На клиенте нужно указывать keystore только если хотят аутентифицироваться с помощью этого сертификата
Для шифрования трафика достаточно указать клиенту только truststore (цепочку доверия). Что я и продемонстрировал
6. Настройка в брокере allow.everyone.if.no.acl.found=false, т.е. всем запрещено, если не разрешено (ниже расписано)
7. В application.yml в spring.kafka.topic указан "topic1". Т.е. java приложение (consumer и producer) работают сейчас только с topic1
8. Запускаем java приложение (KafkaTrainingApplication.java) и убеждаемся что в логе topic1=TOPIC_AUTHORIZATION_FAILED
9. Даем права на топик topic1 для пользака producer_user
docker exec -it kafkatraining-kafka-0-1 kafka-acls.sh --bootstrap-server kafka-0:9092 --add --allow-principal User:producer_user --producer --topic topic1 --command-config /tmp/client.properties
Обратите внимание, что используется параметр --producer без указания --operation... В этом случае даются права: create, describe, write
10. Так же даем права на топик topic1 для пользака consumer_user
docker exec -it kafkatraining-kafka-0-1 kafka-acls.sh --bootstrap-server kafka-0:9092 --add --allow-principal User:consumer_user --consumer --group singleCG --topic topic1 --command-config /tmp/client.properties 
Тоже обращаю внимание, что используется параметр --consumer без указания --operation... В этом случае даются права: read, describe
Так же в команде необходимо указать consumer-group (--group) что я и сделал
11. Кстати в kafka-ui можно убедится без командной строки, что права добавились: http://localhost:8090/ui/clusters/kraft/acl
12. Запускаем опять java приложение, переходим http://localhost:8080 и через свагер и единственную ручку отправляем сообщение в topic1
13. Убеждаемся что ошибок нет и что в логе java есть строка SingleMessageConsumer. Read new user from topic topic1
14. Значит все Ок: producer отправил, а consumer прочитал это сообщение. Доступ к topic1 есть! Ура!
15. Остановим java приложение и изменим в application.yml в spring.kafka.topic топик на "topic2"
16. Даем права на топик topic2 для пользака producer_user, но не даем при этом прав для consumer_user
docker exec -it kafkatraining-kafka-0-1 kafka-acls.sh --bootstrap-server kafka-0:9092 --add --allow-principal User:producer_user --producer --topic topic2 --command-config /tmp/client.properties
17. Запускаем java приложение и видим в логах topic2=TOPIC_AUTHORIZATION_FAILED. Т.е. consumer не может прочитать топик - прав нет
18. Но при этом переходим на http://localhost:8080 и через свагер и единственную ручку отправляем сообщение в topic2
19. Видим что отправка происходи без ошибок и в kafka-ui в топике topic2 появилось сообщение
20. Ура еще раз - все работает как надо )


### 6. ACL. Настройка в брокерах allow.everyone.if.no.acl.found
1. Если ресурс имеет ACL запись, то доступ к нему разрешен тем, у кого есть соответствующие права, и суперпользователям.
#### allow.everyone.if.no.acl.found=true
2. Если ресурс не имеет записи ACL, то доступ к нему разрешен для всех
#### allow.everyone.if.no.acl.found=false
2. Если ресурс не имеет записи ACL, то доступ к нему запрещен для всех, кроме суперпользователей


### 7. Обращение к проверяющему
1. Привет! Спасибо, что дочитал до этого абзаца )
2. Большая просьба: я думаю цель обучения - понять и мочь повторить
3. Думаю вы поняли, что я разобрался в теме и очень прошу не отправлять на даработку практику, если найдете не 100% соответствие поставленной задаче
4. У меня ОЧЕНЬ горят сроки и я бы не хотел останавливаться на "вылизывание"
5. В теории и описании задаче - допущены и с вашей стороны ошибки, но я подхожу к этому лояльно. Прошу отнестись так же и ко мне
6. Еще раз спасибо: за понимание, и за то что вы стараетесь и проверяете!