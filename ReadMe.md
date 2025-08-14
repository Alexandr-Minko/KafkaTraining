### 1. Идея проекта:
1. Вставляем строки в таблички postgres
2. Debezium подхватывает изменения и генерит сообщения в kafka
3. Сообщения kafka ловит написанный sink connector и выводит в лог 

### 2. Запуск проекта 
1. Клонируем проект с git в папку KafkaTraining.
2. Выполняем mvn clean package. В паке target появится CustomSinkConnector-0.0.1.jar. Копируем его в ./connectors
3. Запускаем все из docker-compose.yml  
Поднимется инфраструктура: broker x 3, kafka UI, schema registry, postgres, kafka connect, prometheus, grafana
4. Убеждаемся что в kafka connect появились наши коннекторы из ./connector: Debezium и CustomSinkConnector   
Делаем GET http://localhost:8083/connector-plugins
5. Подключаемся к БД, создаем таблички и наполняем их: файл SQL_Requests.sql пункты 1 и 2
6. Запускаем debezium коннектор: файл ConnectorRequests.txt пункт 1
7. Проверяем что debezium коннектор запустился: GET http://localhost:8083/connectors/debezium/status
8. Через Kafka UI проверяем, что появились сообщения в топиках: debezium.public.orders и debezium.public.users
9. Запускаем кастомный sink connector: файл ConnectorRequests.txt пункт 2
10. Проверяем что коннектор запустился: GET http://localhost:8083/connectors/custom/status
11. Убеждаемся что коннектор обрабатывает сообщения по логу kafka-connect. Ищем по префиксу: "CUSTOM CONNECTOR: Received record"
12. Можно еще проверить коннектор через debug. Об этом ниже

### 3. DEBUG коннектора kafka connect
Я научился дебажить через IntelliJ IDEA созданный коннектор!!! Для этого:
1. В docker-compose.yml в kafka-connect добавлен параметр JVM для включения дебага:   
JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,address=*:5005,server=y,suspend=n"
2. Прокидываем этот порт (5005) наружу: ports: - "5005:5005" 
3. Создаем конфигурацию запуска в Идеи типа "Remote JVM Debug" и настройки по умолчанию: localhost:5005   
и запускаем эту конфигурацию при запущенном kafka-connect1
4. Должно появится: Connected to the target VM, address: 'localhost:5005', transport: 'socket'
5. Ставит брейк-поинт в методе CustomSinkTask.put() и наслаждаемся! =)

### 4. Мониторинг
Мониторинг через Kafka connect -> JMX -> Prometheus  -> Grafana тоже сделал. Проверяем:
1. Kafka connect (как и сама kafka), тоже не умеет отдавать метрики Prometheus по http.
Но как и kafka отдает метрики по JMX. Поэтому используем JMX экспортер, но в отличие от урока с kafka,
тут мы подкладываем jmx экспортер в образ kafka connect, а не запускаем рядом:  
build: context: ./kafka-connect, а там Dockerfile копирующий JAR экспортера и его настройки
2. Включаем JMX метрики на 9876 портус настройками из файла и прокидываем этот порт наружу:
   KAFKA_OPTS: "-javaagent:/opt/jmx_prometheus_javaagent-0.15.0.jar=9876:/opt/kafka-connect.yml"
3. Проверяем что http метрики доступны по http://localhost:9876/
4. Prometheus их оттуда забирает как указано в его настройках: .prometheus/prometheus.yml -> scrape_configs: targets: ['kafka-connect:9876']
   В этом можно убедиться перейдя по http://localhost:9090/targets. State будет UP
5. Grafana берет данные с Prometheus (это указано в ее настройках, которые подкидываются в Dockerfile из .grafana/. Там же подкидывается дашборд).
6. Убедиться что данные визуализируются можно тут: http://localhost:3000/d/kafka-connect-overview-0/kafka-connect-overview-0?orgId=1&from=now-1m&to=now 
