package com.minko.kafkatraining.config;

import com.minko.kafkatraining.domain.Message;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;
import org.springframework.kafka.streams.KafkaStreamsInteractiveQueryService;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.io.File;
import java.util.Properties;

@Configuration
public class KafkaStreamsConfig {

    @Value("${java.io.tmpdir}")
    private String tempDirectory;

    @Value("${server.port}")
    private int serverPort;

    @Value("${spring.kafka.streams.application-id}")
    private String appId;


    private Properties getCustomProperties() {
        Properties props = new Properties();

        // у kafka streams есть только 2 обязательные настройки:
        // bootstrap.servers и application.id (присваивается для consumer group.id, поэтому отдельно group.id не задается)

        // Вот эта интересная настройка. "Гарантии доставки"
        // в stream kafka не нужно настраивать отдельно параметры, чтобы добиться какой-то гарантии доставки,
        // достаточно установить настройку processing.guarantee, а это пресет для всех необходимых настроек consumer/producer
        // в текущей версии доступно 2 значения: at_least_once (по умолчанию) либо exactly_once_v2 (транзакции, идемпотентность)
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.AT_LEAST_ONCE);

        // В кубере такой проблемы не будет. При запуске несколько инастансов на одном компе они занимают одну папку для state store
        String stateDirConfig = tempDirectory + "kafka-streams" + File.separator + appId + serverPort;
        props.put(StreamsConfig.STATE_DIR_CONFIG, stateDirConfig);

        // (де)сериализаторы (Serdes). По умолчанию String
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
//        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, new JsonSerde<>(Message.class).getClass());


        // Если настройка есть одновременно у consumer, producer, то есть префиксы. Задам только для producer
        props.put(StreamsConfig.producerPrefix("metadata.max.age.ms"), 300_001);

        return props;
    }

    @Bean
    // Беру настройки stream kafka из yaml, добавляю свои, и они попадают в defaultKafkaStreamsBuilder
    public StreamsBuilderFactoryBeanConfigurer configurer() {
        return factoryBean -> {
            Properties yamlConfig = factoryBean.getStreamsConfiguration();
            Properties customProperties = getCustomProperties();
            Properties resultProperties = new Properties();
            resultProperties.putAll(yamlConfig);
            resultProperties.putAll(customProperties);
            factoryBean.setStreamsConfiguration(resultProperties);
        };
    }

    @Bean
    public KafkaStreamsInteractiveQueryService kafkaStreamsInteractiveQueryService(StreamsBuilderFactoryBean builder) {
        return new KafkaStreamsInteractiveQueryService(builder);
    }

/*
    Так в доке говорят создавать конфиг
    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "testStreams");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");
        // ...
        return new KafkaStreamsConfiguration(props);
    }
*/

}