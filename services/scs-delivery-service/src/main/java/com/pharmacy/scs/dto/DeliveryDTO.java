package com.pharmacy.scs.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;


@Data
public class DeliveryDTO {
    private Long id;
    private String trackingNumber;
    private Long userId;
    private String deliveryAddress;
    private LocalDateTime expectedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
