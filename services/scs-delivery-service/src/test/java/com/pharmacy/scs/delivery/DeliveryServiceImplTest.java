package com.pharmacy.scs.delivery;

import com.pharmacy.scs.dto.DeliveryEvent;
import com.pharmacy.scs.entity.Delivery;
import com.pharmacy.scs.entity.DeliveryStatus;
import com.pharmacy.scs.entity.User;
import com.pharmacy.scs.exception.DeliveryNotFoundException;
import com.pharmacy.scs.repository.DeliveryRepository;
import com.pharmacy.scs.service.KafkaDeliveryEventService;
import com.pharmacy.scs.service.impl.DeliveryServiceImpl;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Модульные тесты для DeliveryServiceImpl.
 * Тесты структурированы по методам сервиса, чтобы обеспечить полное покрытие
 * и служить документацией по бизнес-логике.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private KafkaDeliveryEventService eventService;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    // Общие тестовые данные
    private User testUser;
    private Delivery testDelivery;

    @Captor
    private ArgumentCaptor<DeliveryEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        // Подготавливаем тестовые данные для всех тестов
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNumber("+71234567890");

        testDelivery = new Delivery();
        testDelivery.setId(1L);
        testDelivery.setTrackingNumber("TRACK123");
        testDelivery.setUser(testUser);
        testDelivery.setDeliveryAddress("Test Address");
        testDelivery.setExpectedDeliveryTime(LocalDateTime.now().plusDays(1));
        testDelivery.setStatus(DeliveryStatus.PENDING);
    }

    /**
     * Тесты для метода createDelivery
     */
    @Nested
    @DisplayName("Тесты для createDelivery")
    class CreateDeliveryTests {

        @Test
        @DisplayName("Должен успешно создать доставку и отправить событие")
        void shouldCreateDeliveryAndSendEvent() {
            // Arrange - настраиваем поведение моков
            when(deliveryRepository.save(any(Delivery.class))).thenReturn(testDelivery);
            doNothing().when(eventService).sendDeliveryEvent(any(DeliveryEvent.class));

            // Act - вызываем тестируемый метод
            Delivery result = deliveryService.createDelivery(testDelivery);

            // Assert - проверяем результаты
            assertNotNull(result);
            assertEquals(testDelivery.getId(), result.getId());
            assertEquals(testDelivery.getTrackingNumber(), result.getTrackingNumber());

            // Проверяем, что был вызван метод сохранения в репозитории
            verify(deliveryRepository).save(testDelivery);

            // Проверяем, что было отправлено событие с правильными данными
            verify(eventService).sendDeliveryEvent(eventCaptor.capture());
            DeliveryEvent capturedEvent = eventCaptor.getValue();
            assertEquals("CREATED", capturedEvent.getEventType());
            assertEquals(testDelivery.getId(), capturedEvent.getDeliveryId());
            assertEquals(testDelivery.getStatus().toString(), capturedEvent.getStatus());
        }
    }

    /**
     * Тесты для метода getDeliveryByTrackingNumber
     */
    @Nested
    @DisplayName("Тесты для getDeliveryByTrackingNumber")
    class GetDeliveryByTrackingNumberTests {

        @Test
        @DisplayName("Должен вернуть доставку по номеру отслеживания")
        void shouldReturnDeliveryByTrackingNumber() {
            // Arrange
            String trackingNumber = "TRACK123";
            when(deliveryRepository.findByTrackingNumber(trackingNumber))
                    .thenReturn(Optional.of(testDelivery));

            // Act
            Optional<Delivery> result = deliveryService.getDeliveryByTrackingNumber(trackingNumber);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(testDelivery, result.get());
            verify(deliveryRepository).findByTrackingNumber(trackingNumber);
        }

        @Test
        @DisplayName("Должен вернуть пустой Optional, если доставка не найдена")
        void shouldReturnEmptyOptionalWhenDeliveryNotFound() {
            // Arrange
            String trackingNumber = "NONEXISTENT";
            when(deliveryRepository.findByTrackingNumber(trackingNumber))
                    .thenReturn(Optional.empty());

            // Act
            Optional<Delivery> result = deliveryService.getDeliveryByTrackingNumber(trackingNumber);

            // Assert
            assertFalse(result.isPresent());
            verify(deliveryRepository).findByTrackingNumber(trackingNumber);
        }
    }

    /**
     * Тесты для метода getDeliveriesByUserId
     */
    @Nested
    @DisplayName("Тесты для getDeliveriesByUserId")
    class GetDeliveriesByUserIdTests {

        @Test
        @DisplayName("Должен вернуть список доставок пользователя")
        void shouldReturnDeliveriesByUserId() {
            // Arrange
            Long userId = 1L;
            Delivery delivery1 = new Delivery();
            delivery1.setId(1L);
            delivery1.setUser(testUser);

            Delivery delivery2 = new Delivery();
            delivery2.setId(2L);
            delivery2.setUser(testUser);

            List<Delivery> expectedDeliveries = Arrays.asList(delivery1, delivery2);

            when(deliveryRepository.findByUserId(userId)).thenReturn(expectedDeliveries);

            // Act
            List<Delivery> result = deliveryService.getDeliveriesByUserId(userId);

            // Assert
            assertEquals(expectedDeliveries.size(), result.size());
            assertEquals(expectedDeliveries, result);
            verify(deliveryRepository).findByUserId(userId);
        }

        @Test
        @DisplayName("Должен вернуть пустой список, если доставки не найдены")
        void shouldReturnEmptyListWhenNoDeliveriesFound() {
            // Arrange
            Long userId = 999L;
            when(deliveryRepository.findByUserId(userId)).thenReturn(List.of());

            // Act
            List<Delivery> result = deliveryService.getDeliveriesByUserId(userId);

            // Assert
            assertTrue(result.isEmpty());
            verify(deliveryRepository).findByUserId(userId);
        }
    }

    /**
     * Тесты для метода updateDeliveryStatus
     */
    @Nested
    @DisplayName("Тесты для updateDeliveryStatus")
    class UpdateDeliveryStatusTests {

        @Test
        @DisplayName("Должен обновить статус доставки")
        void shouldUpdateDeliveryStatus() {
            // Arrange
            Long deliveryId = 1L;
            DeliveryStatus newStatus = DeliveryStatus.IN_TRANSIT;

            Delivery deliveryToUpdate = new Delivery();
            deliveryToUpdate.setId(deliveryId);
            deliveryToUpdate.setStatus(DeliveryStatus.PENDING);

            Delivery updatedDelivery = new Delivery();
            updatedDelivery.setId(deliveryId);
            updatedDelivery.setStatus(newStatus);

            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(deliveryToUpdate));
            when(deliveryRepository.save(any(Delivery.class))).thenReturn(updatedDelivery);

            // Act
            Delivery result = deliveryService.updateDeliveryStatus(deliveryId, newStatus);

            // Assert
            assertNotNull(result);
            assertEquals(newStatus, result.getStatus());
            verify(deliveryRepository).findById(deliveryId);
            verify(deliveryRepository).save(deliveryToUpdate);
        }

        @Test
        @DisplayName("Должен выбросить исключение, если доставка не найдена")
        void shouldThrowExceptionWhenDeliveryNotFound() {
            // Arrange
            Long deliveryId = 999L;
            DeliveryStatus newStatus = DeliveryStatus.IN_TRANSIT;

            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, () ->
                    deliveryService.updateDeliveryStatus(deliveryId, newStatus));

            verify(deliveryRepository).findById(deliveryId);
            verify(deliveryRepository, never()).save(any(Delivery.class));
        }
    }

    /**
     * Тесты для метода completeDelivery
     */
    @Nested
    @DisplayName("Тесты для completeDelivery")
    class CompleteDeliveryTests {

        @Test
        @DisplayName("Должен завершить доставку и отправить событие")
        void shouldCompleteDeliveryAndSendEvent() {
            // Arrange
            Long deliveryId = 1L;

            // Клонируем тестовую доставку для дальнейшей модификации
            Delivery deliveryToComplete = new Delivery();
            deliveryToComplete.setId(deliveryId);
            deliveryToComplete.setUser(testUser);
            deliveryToComplete.setStatus(DeliveryStatus.IN_TRANSIT);
            deliveryToComplete.setTrackingNumber("TRACK123");
            deliveryToComplete.setDeliveryAddress("Test Address");
            deliveryToComplete.setExpectedDeliveryTime(LocalDateTime.now().plusDays(1));

            // Создаем объект завершенной доставки
            Delivery completedDelivery = new Delivery();
            completedDelivery.setId(deliveryId);
            completedDelivery.setUser(testUser);
            completedDelivery.setStatus(DeliveryStatus.COMPLETED);
            completedDelivery.setTrackingNumber("TRACK123");
            completedDelivery.setDeliveryAddress("Test Address");
            completedDelivery.setExpectedDeliveryTime(LocalDateTime.now().plusDays(1));
            completedDelivery.setActualDeliveryTime(LocalDateTime.now());

            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(deliveryToComplete));
            when(deliveryRepository.save(any(Delivery.class))).thenReturn(completedDelivery);
            doNothing().when(eventService).sendDeliveryEvent(any(DeliveryEvent.class));

            // Act
            Delivery result = deliveryService.completeDelivery(deliveryId);

            // Assert
            assertNotNull(result);
            assertEquals(DeliveryStatus.COMPLETED, result.getStatus());
            assertNotNull(result.getActualDeliveryTime());

            verify(deliveryRepository).findById(deliveryId);
            verify(deliveryRepository).save(any(Delivery.class));

            // Проверяем отправку события
            verify(eventService).sendDeliveryEvent(eventCaptor.capture());
            DeliveryEvent capturedEvent = eventCaptor.getValue();
            assertEquals("COMPLETED", capturedEvent.getEventType());
            assertEquals(deliveryId, capturedEvent.getDeliveryId());
            assertEquals(DeliveryStatus.COMPLETED.toString(), capturedEvent.getStatus());
            assertTrue(capturedEvent.getAdditionalData().containsKey("completedAt"));
        }

        @Test
        @DisplayName("Должен выбросить исключение, если доставка не найдена")
        void shouldThrowExceptionWhenDeliveryNotFound() {
            // Arrange
            Long deliveryId = 999L;
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(DeliveryNotFoundException.class, () ->
                    deliveryService.completeDelivery(deliveryId));

            verify(deliveryRepository).findById(deliveryId);
            verify(deliveryRepository, never()).save(any(Delivery.class));
            verify(eventService, never()).sendDeliveryEvent(any(DeliveryEvent.class));
        }
    }
}
