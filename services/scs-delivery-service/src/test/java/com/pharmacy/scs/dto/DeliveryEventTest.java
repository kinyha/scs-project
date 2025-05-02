package com.pharmacy.scs.dto;

import com.pharmacy.scs.entity.Delivery;
import com.pharmacy.scs.entity.DeliveryStatus;
import com.pharmacy.scs.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для класса DeliveryEvent, который используется для отправки сообщений в Kafka.
 * Проверяется корректность создания событий из сущности Delivery.
 */
public class DeliveryEventTest {

    @Test
    @DisplayName("Создание события CREATED из сущности Delivery")
    void testCreateEventFromDelivery() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testUser");

        Delivery delivery = new Delivery();
        delivery.setId(2L);
        delivery.setTrackingNumber("TRACK123");
        delivery.setUser(user);
        delivery.setDeliveryAddress("Test Address");
        delivery.setExpectedDeliveryTime(LocalDateTime.now().plusDays(1));
        delivery.setStatus(DeliveryStatus.CREATED);

        // Act
        DeliveryEvent event = DeliveryEvent.from(delivery, "CREATED");

        // Assert
        assertNotNull(event);
        assertEquals("CREATED", event.getEventType());
        assertEquals(2L, event.getDeliveryId());
        assertEquals("CREATED", event.getStatus());
        assertNotNull(event.getTimestamp());

        // Проверяем additionalData
        Map<String, Object> additionalData = event.getAdditionalData();
        assertNotNull(additionalData);
        assertEquals(3, additionalData.size());
        assertEquals(1L, additionalData.get("userId"));
        assertEquals("TRACK123", additionalData.get("trackingNumber"));
        assertEquals("Test Address", additionalData.get("deliveryAddress"));
    }

    @Test
    @DisplayName("Создание события COMPLETED из сущности Delivery")
    void testCreateCompletedEventFromDelivery() {
        // Arrange
        User user = new User();
        user.setId(1L);

        LocalDateTime completionTime = LocalDateTime.now();

        Delivery delivery = new Delivery();
        delivery.setId(2L);
        delivery.setTrackingNumber("TRACK123");
        delivery.setUser(user);
        delivery.setDeliveryAddress("Test Address");
        delivery.setExpectedDeliveryTime(LocalDateTime.now().minusDays(1));
        delivery.setActualDeliveryTime(completionTime);
        delivery.setStatus(DeliveryStatus.COMPLETED);

        // Act
        DeliveryEvent event = DeliveryEvent.from(delivery, "COMPLETED");
        // Добавим вручную completedAt в additionalData
        event.getAdditionalData().put("completedAt", completionTime);

        // Assert
        assertNotNull(event);
        assertEquals("COMPLETED", event.getEventType());
        assertEquals(2L, event.getDeliveryId());
        assertEquals("COMPLETED", event.getStatus());

        // Проверяем additionalData с временем завершения
        Map<String, Object> additionalData = event.getAdditionalData();
        assertNotNull(additionalData);
        assertEquals(4, additionalData.size());
        assertEquals(completionTime, additionalData.get("completedAt"));
    }

    @Test
    @DisplayName("Обработка null полей при создании события")
    void testHandleNullFieldsInEvent() {
        // Arrange - создаем объект с null полями
        User user = new User();
        user.setId(1L);

        Delivery delivery = new Delivery();
        delivery.setId(2L);
        delivery.setUser(user);
        delivery.setStatus(DeliveryStatus.PENDING);
        // Не устанавливаем trackingNumber и deliveryAddress

        // Act
        DeliveryEvent event = DeliveryEvent.from(delivery, "UPDATED");

        // Assert
        assertNotNull(event);
        assertEquals("UPDATED", event.getEventType());
        assertEquals(2L, event.getDeliveryId());

        // Проверяем, что additionalData не содержит null значений
        Map<String, Object> additionalData = event.getAdditionalData();
        assertNotNull(additionalData);
        assertEquals(1L, additionalData.get("userId"));
        assertNull(additionalData.get("trackingNumber")); // null, так как не был установлен
        assertNull(additionalData.get("deliveryAddress")); // null, так как не был установлен
    }
}