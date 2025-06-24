package com.minko.kafkatraining.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    // Нужен для errorHandler как отправитель в DLQ. Можно в errorHandler создать, но так красивее
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate);
    }

    // Логика обработки эксепшенов в Listener
    @Bean
    public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer DeadLetterRecoverer) {
        // при exception в listener: кол-во повторных попыток обработки и интервал между ними
        // сейчас при первом exception кидаем в DLQ
        BackOff fixedBackOff = new FixedBackOff(0, 0);
        return new DefaultErrorHandler(DeadLetterRecoverer, fixedBackOff);

//      а можно определить какие exception будут retryable, а какие нет. Например все NotRetryable
//      var errorHandler = new DefaultErrorHandler(DeadLetterRecoverer);
//      errorHandler.addNotRetryableExceptions(RuntimeException.class);
//      return errorHandler;
//
    }

    // итоговые настройки для SingleKafkaListener (Consumer)
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> SingleFactoryDeadLetter(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // добавляем errorHandler
        factory.setCommonErrorHandler(errorHandler);
        // по моему тут можно и по дефолту оставить BATCH, ну да ладно
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    // итоговые настройки для BatchKafkaListener (Consumer)
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> batchFactory(
            ConsumerFactory<String, String> commonConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // берем общие настройки и устанавливаем уникальные настройки для Batch Listener-а
        Map<String, Object> properties = new HashMap<>(commonConsumerFactory.getConfigurationProperties());
        properties.put("fetch.min.bytes", 1000);
        properties.put("fetch.max.wait.ms", 10000);
        properties.put("enable.auto.commit", false);
        ConsumerFactory<String, String> customConsumerFactory = new DefaultKafkaConsumerFactory<>(properties);
        factory.setConsumerFactory(customConsumerFactory);

        // читаем пачками
        factory.setBatchListener(true);
        // Ask отправляем вручную в любой момент.
        // Есть еще MANUAL. Как его понял: это фиксация ask ручная, а отправка всех ask-ов пачки после последнего
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

}
