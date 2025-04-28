package com.pharmacy.scs.notification.config;

import com.pharmacy.scs.notification.dto.DeliveryEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, DeliveryEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Настройки для гарантии at-least-once (отключаем автокоммит)
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Настройки для отказоустойчивости
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 минут
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);        // Не более 100 записей за раз
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);    // 15 секунд

        // Десериализаторы
        JsonDeserializer<DeliveryEvent> jsonDeserializer = new JsonDeserializer<>(DeliveryEvent.class);
        jsonDeserializer.setRemoveTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("com.pharmacy.scs.notification.dto", "com.pharmacy.scs.dto");
        jsonDeserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeliveryEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Ручное подтверждение получения сообщений (для exactly-once)
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Настройка обработки ошибок с повторными попытками
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(1000L, 3)); // 3 повторные попытки с интервалом 1 секунда
        factory.setCommonErrorHandler(errorHandler);

        // Количество потоков для обработки сообщений
        factory.setConcurrency(3);

        return factory;
    }

    // Можно добавить еще один factory для Dead Letter Queue
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryEvent> kafkaDltListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeliveryEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // DLQ не требует дополнительных повторных попыток
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}