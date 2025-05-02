package com.pharmacy.scs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmacy.scs.dto.DeliveryCreateRequest;
import com.pharmacy.scs.dto.DeliveryDTO;
import com.pharmacy.scs.dto.DeliveryResponseDTO;
import com.pharmacy.scs.dto.DeliveryUpdateRequest;
import com.pharmacy.scs.entity.Delivery;
import com.pharmacy.scs.entity.DeliveryStatus;
import com.pharmacy.scs.entity.User;
import com.pharmacy.scs.exception.DeliveryNotFoundException;
import com.pharmacy.scs.mapper.DeliveryMapper;
import com.pharmacy.scs.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для REST API DeliveryController.
 * Используем изолированный подход без загрузки Spring контекста,
 * что позволяет избежать проблем с JPA и зависимостями.
 */
@ExtendWith(MockitoExtension.class)
public class DeliveryControllerTest {

    @Mock
    private DeliveryService deliveryService;

    @Mock
    private DeliveryMapper deliveryMapper;

    @InjectMocks
    private DeliveryController deliveryController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Настраиваем MockMvc без загрузки Spring контекста
        mockMvc = MockMvcBuilders.standaloneSetup(deliveryController)
                .build();

        // Настраиваем ObjectMapper для корректной сериализации/десериализации LocalDateTime
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Создание доставки: 201 Created")
    void testCreateDelivery() throws Exception {
        // Arrange
        DeliveryCreateRequest request = new DeliveryCreateRequest();
        request.setTrackingNumber("TRACK123");
        request.setUserId(1L);
        request.setDeliveryAddress("Test Address");
        request.setExpectedDeliveryTime(LocalDateTime.now().plusDays(1));
        request.setStatus("PENDING");

        Delivery delivery = new Delivery();
        delivery.setId(1L);
        delivery.setTrackingNumber("TRACK123");
        delivery.setStatus(DeliveryStatus.PENDING);

        User user = new User();
        user.setId(1L);
        delivery.setUser(user);

        DeliveryDTO deliveryDTO = new DeliveryDTO();
        deliveryDTO.setId(1L);
        deliveryDTO.setTrackingNumber("TRACK123");
        deliveryDTO.setUserId(1L);
        deliveryDTO.setStatus("PENDING");

        when(deliveryMapper.toEntity(any(DeliveryCreateRequest.class))).thenReturn(delivery);
        when(deliveryService.createDelivery(any(Delivery.class))).thenReturn(delivery);
        when(deliveryMapper.toDto(any(Delivery.class))).thenReturn(deliveryDTO);

        // Act & Assert
        mockMvc.perform(post("/api/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.trackingNumber", is("TRACK123")))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @DisplayName("Получение доставки по номеру отслеживания: 200 OK")
    void testGetDeliveryByTrackingNumber() throws Exception {
        // Arrange
        String trackingNumber = "TRACK123";

        Delivery delivery = new Delivery();
        delivery.setId(1L);
        delivery.setTrackingNumber(trackingNumber);
        delivery.setStatus(DeliveryStatus.IN_TRANSIT);

        User user = new User();
        user.setId(1L);
        delivery.setUser(user);

        DeliveryDTO deliveryDTO = new DeliveryDTO();
        deliveryDTO.setId(1L);
        deliveryDTO.setTrackingNumber(trackingNumber);
        deliveryDTO.setUserId(1L);
        deliveryDTO.setStatus("IN_TRANSIT");

        when(deliveryService.getDeliveryByTrackingNumber(trackingNumber)).thenReturn(Optional.of(delivery));
        when(deliveryMapper.toDto(delivery)).thenReturn(deliveryDTO);

        // Act & Assert
        mockMvc.perform(get("/api/deliveries/{trackingNumber}", trackingNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.trackingNumber", is(trackingNumber)))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.status", is("IN_TRANSIT")));
    }

    @Test
    @DisplayName("Получение доставки по несуществующему номеру: 404 Not Found")
    void testGetDeliveryByTrackingNumberNotFound() throws Exception {
        // Arrange
        String trackingNumber = "NONEXISTENT";

        when(deliveryService.getDeliveryByTrackingNumber(trackingNumber)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/deliveries/{trackingNumber}", trackingNumber))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение доставок пользователя: 200 OK")
    void testGetDeliveriesByUser() throws Exception {
        // Arrange
        Long userId = 1L;

        Delivery delivery1 = new Delivery();
        delivery1.setId(1L);
        delivery1.setTrackingNumber("TRACK1");

        Delivery delivery2 = new Delivery();
        delivery2.setId(2L);
        delivery2.setTrackingNumber("TRACK2");

        List<Delivery> deliveries = Arrays.asList(delivery1, delivery2);

        DeliveryDTO dto1 = new DeliveryDTO();
        dto1.setId(1L);
        dto1.setTrackingNumber("TRACK1");

        DeliveryDTO dto2 = new DeliveryDTO();
        dto2.setId(2L);
        dto2.setTrackingNumber("TRACK2");

        when(deliveryService.getDeliveriesByUserId(userId)).thenReturn(deliveries);
        when(deliveryMapper.toDto(delivery1)).thenReturn(dto1);
        when(deliveryMapper.toDto(delivery2)).thenReturn(dto2);

        // Act & Assert
        mockMvc.perform(get("/api/deliveries")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].trackingNumber", is("TRACK1")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].trackingNumber", is("TRACK2")));
    }

    @Test
    @DisplayName("Обновление статуса доставки: 200 OK")
    void testUpdateDeliveryStatus() throws Exception {
        // Arrange
        Long deliveryId = 1L;

        DeliveryUpdateRequest request = new DeliveryUpdateRequest();
        request.setStatus(DeliveryStatus.DELIVERED);

        Delivery updatedDelivery = new Delivery();
        updatedDelivery.setId(deliveryId);
        updatedDelivery.setStatus(DeliveryStatus.DELIVERED);

        User user = new User();
        user.setId(1L);
        updatedDelivery.setUser(user);

        DeliveryDTO deliveryDTO = new DeliveryDTO();
        deliveryDTO.setId(deliveryId);
        deliveryDTO.setStatus("DELIVERED");

        when(deliveryService.updateDeliveryStatus(deliveryId, DeliveryStatus.DELIVERED)).thenReturn(updatedDelivery);
        when(deliveryMapper.toDto(updatedDelivery)).thenReturn(deliveryDTO);

        // Act & Assert
        mockMvc.perform(put("/api/deliveries/{deliveryId}/status", deliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("DELIVERED")));
    }

    @Test
    @DisplayName("Подтверждение доставки: 202 Accepted")
    void testConfirmDelivery() throws Exception {
        // Arrange
        Long deliveryId = 1L;

        Delivery completedDelivery = new Delivery();
        completedDelivery.setId(deliveryId);
        completedDelivery.setStatus(DeliveryStatus.COMPLETED);
        completedDelivery.setActualDeliveryTime(LocalDateTime.now());

        User user = new User();
        user.setId(1L);
        completedDelivery.setUser(user);

        DeliveryResponseDTO responseDTO = new DeliveryResponseDTO();
        responseDTO.setId(deliveryId);
        responseDTO.setStatus(DeliveryStatus.COMPLETED);
        //responseDTO.setIsDelivered(true);

        when(deliveryService.completeDelivery(deliveryId)).thenReturn(completedDelivery);

        // Act & Assert
        mockMvc.perform(put("/api/deliveries/{deliveryId}/confirm", deliveryId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
                //.andExpect(jsonPath("$.delivered", is(true)));
    }

    @Test
    @DisplayName("Подтверждение несуществующей доставки: 404 Not Found")
    void testConfirmDeliveryNotFound() throws Exception {
        // Arrange
        Long deliveryId = 999L;

        doThrow(new DeliveryNotFoundException(deliveryId))
                .when(deliveryService).completeDelivery(deliveryId);

        // Act & Assert
        mockMvc.perform(put("/api/deliveries/{deliveryId}/confirm", deliveryId))
                .andExpect(status().isNotFound());
    }
}