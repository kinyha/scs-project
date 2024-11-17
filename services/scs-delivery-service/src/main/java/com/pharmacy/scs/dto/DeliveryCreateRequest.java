package com.pharmacy.scs.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeliveryCreateRequest {
    private String trackingNumber;
    private Long userId;
    private String deliveryAddress;
    private LocalDateTime expectedDeliveryTime;
    private String status;
}
