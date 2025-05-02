package com.pharmacy.scs.service;

import com.pharmacy.scs.entity.Delivery;
import com.pharmacy.scs.entity.DeliveryStatus;
import com.pharmacy.scs.entity.User;
import com.pharmacy.scs.exception.DeliveryNotFoundException;
import com.pharmacy.scs.repository.DeliveryRepository;
import com.pharmacy.scs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Интеграционные тесты для DeliveryService с использованием H2 вместо TestContainers
 * для упрощения и ускорения тестирования.
 */
@SpringBootTest
@ActiveProfiles("test-h2")
public class DeliveryServiceIntegrationTest {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private KafkaDeliveryEventService kafkaEventService;

    private User testUser;
    private Delivery testDelivery;

    @BeforeEach
    void setUp() {
        // Очищаем базу перед каждым тестом
        deliveryRepository.deleteAll();
        userRepository.deleteAll();

        // Создаем тестового пользователя
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNumber("+71234567890");
        testUser.setPassword("password");
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);

        // Создаем тестовую доставку
        testDelivery = new Delivery();
        testDelivery.setTrackingNumber("TRACK" + System.currentTimeMillis());
        testDelivery.setUser(testUser);
        testDelivery.setDeliveryAddress("Test Address");
        testDelivery.setExpectedDeliveryTime(LocalDateTime.now().plusDays(1));
        testDelivery.setStatus(DeliveryStatus.PENDING);

        // Мокируем Kafka, чтобы не отправлять реальные сообщения
        doNothing().when(kafkaEventService).sendDeliveryEvent(any());
    }

    @Test
    @DisplayName("Интеграционный тест: Создание и получение доставки")
    void testCreateAndGetDelivery() {
        // Создаем доставку
        Delivery createdDelivery = deliveryService.createDelivery(testDelivery);
        assertNotNull(createdDelivery.getId());

        // Получаем доставку по номеру отслеживания
        Optional<Delivery> foundDelivery = deliveryService.getDeliveryByTrackingNumber(createdDelivery.getTrackingNumber());

        assertTrue(foundDelivery.isPresent());
        assertEquals(createdDelivery.getId(), foundDelivery.get().getId());
        assertEquals(testUser.getId(), foundDelivery.get().getUser().getId());
        assertEquals(DeliveryStatus.PENDING, foundDelivery.get().getStatus());
    }

    @Test
    @DisplayName("Интеграционный тест: Получение доставок пользователя")
    void testGetDeliveriesByUserId() {
        // Создаем две доставки для одного пользователя
        deliveryService.createDelivery(testDelivery);

        Delivery secondDelivery = new Delivery();
        secondDelivery.setTrackingNumber("TRACK2" + System.currentTimeMillis());
        secondDelivery.setUser(testUser);
        secondDelivery.setDeliveryAddress("Another Address");
        secondDelivery.setExpectedDeliveryTime(LocalDateTime.now().plusDays(2));
        secondDelivery.setStatus(DeliveryStatus.PENDING);

        deliveryService.createDelivery(secondDelivery);

        // Получаем доставки пользователя
        List<Delivery> userDeliveries = deliveryService.getDeliveriesByUserId(testUser.getId());

        assertEquals(2, userDeliveries.size());
        assertTrue(userDeliveries.stream()
                .anyMatch(d -> d.getTrackingNumber().equals(testDelivery.getTrackingNumber())));
        assertTrue(userDeliveries.stream()
                .anyMatch(d -> d.getTrackingNumber().equals(secondDelivery.getTrackingNumber())));
    }

    @Test
    @DisplayName("Интеграционный тест: Обновление статуса доставки")
    void testUpdateDeliveryStatus() {
        // Создаем доставку
        Delivery createdDelivery = deliveryService.createDelivery(testDelivery);

        // Обновляем статус
        Delivery updatedDelivery = deliveryService.updateDeliveryStatus(
                createdDelivery.getId(), DeliveryStatus.IN_TRANSIT);

        assertEquals(DeliveryStatus.IN_TRANSIT, updatedDelivery.getStatus());

        // Проверяем, что статус обновлен в базе
        Optional<Delivery> foundDelivery = deliveryRepository.findById(createdDelivery.getId());
        assertTrue(foundDelivery.isPresent());
        assertEquals(DeliveryStatus.IN_TRANSIT, foundDelivery.get().getStatus());
    }

    @Test
    @DisplayName("Интеграционный тест: Завершение доставки")
    void testCompleteDelivery() {
        // Создаем доставку
        Delivery createdDelivery = deliveryService.createDelivery(testDelivery);

        // Завершаем доставку
        Delivery completedDelivery = deliveryService.completeDelivery(createdDelivery.getId());

        assertEquals(DeliveryStatus.COMPLETED, completedDelivery.getStatus());
        assertNotNull(completedDelivery.getActualDeliveryTime());

        // Проверяем, что доставка завершена в базе
        Optional<Delivery> foundDelivery = deliveryRepository.findById(createdDelivery.getId());
        assertTrue(foundDelivery.isPresent());
        assertEquals(DeliveryStatus.COMPLETED, foundDelivery.get().getStatus());
    }

    @Test
    @DisplayName("Интеграционный тест: Исключение при несуществующей доставке")
    void testDeliveryNotFoundException() {
        // Пытаемся завершить несуществующую доставку
        assertThrows(DeliveryNotFoundException.class, () ->
                deliveryService.completeDelivery(999L));
    }
}