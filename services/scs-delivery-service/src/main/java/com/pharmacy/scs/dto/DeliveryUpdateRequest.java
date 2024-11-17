package com.pharmacy.scs.dto;

import com.pharmacy.scs.entity.DeliveryStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliveryUpdateRequest {
    private String deliveryAddress;
    private LocalDateTime expectedDeliveryTime;
    private DeliveryStatus status;
}
