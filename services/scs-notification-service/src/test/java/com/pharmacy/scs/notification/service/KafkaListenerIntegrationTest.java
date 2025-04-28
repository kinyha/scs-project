package com.pharmacy.scs.notification.service;

import com.pharmacy.scs.notification.dto.DeliveryEvent;
import com.pharmacy.scs.notification.entity.Notification;
import com.pharmacy.scs.notification.repository.NotificationRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Интеграционный тест для KafkaListenerService с использованием EmbeddedKafka.
 * Для полноценных тестов также можно использовать TestContainers с PostgreSQL.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"delivery-updates", "notification-dlq"})
class KafkaListenerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    private KafkaTemplate<String, DeliveryEvent> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Настраиваем producer для отправки тестовых сообщений
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, DeliveryEvent> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);

        kafkaTemplate = new KafkaTemplate<>(producerFactory);

        // Настраиваем mock для UserServiceClient
        KafkaListenerService.UserDTO userDTO = new KafkaListenerService.UserDTO();
        userDTO.setId(123L);
        userDTO.setUsername("Test User");
        userDTO.setEmail("test@example.com");
        userDTO.setPhoneNumber("+11234567890");

        when(userServiceClient.getUserById(anyLong())).thenReturn(userDTO);

        // Очищаем репозиторий перед каждым тестом
        notificationRepository.deleteAll();
    }

    @Test
    void testKafkaListenerProcessesMessage() {
        // Создаем тестовое событие доставки
        DeliveryEvent event = new DeliveryEvent();
        event.setEventType("UPDATED");
        event.setDeliveryId(1L);
        event.setStatus("IN_TRANSIT");
        event.setTimestamp(LocalDateTime.now());

        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("trackingNumber", "TRACK123");
        additionalData.put("userId", 123L);
        additionalData.put("deliveryAddress", "Test Address");
        event.setAdditionalData(additionalData);

        // Отправляем событие в Kafka
        kafkaTemplate.send("delivery-updates", "delivery-1", event);

        // Ждем обработки события (до 10 секунд)
        await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    List<Notification> notifications = notificationRepository.findAll();
                    return !notifications.isEmpty() &&
                            notifications.stream().anyMatch(n -> n.getStatus() == Notification.NotificationStatus.SENT);
                });

        // Проверяем результаты
        List<Notification> notifications = notificationRepository.findAll();
        assertNotNull(notifications);
        assertEquals(1, notifications.size());

        Notification notification = notifications.get(0);
        assertEquals(123L, notification.getUserId());
        assertEquals(1L, notification.getDeliveryId());
        assertEquals("UPDATED", notification.getEventType());
        assertEquals(Notification.NotificationType.EMAIL, notification.getType());
        assertEquals(Notification.NotificationStatus.SENT, notification.getStatus());
        assertEquals("test@example.com", notification.getRecipient());
        assertNotNull(notification.getSentAt());
    }
}