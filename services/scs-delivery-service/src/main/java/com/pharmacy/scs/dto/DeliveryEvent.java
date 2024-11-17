package com.pharmacy.scs.dto;

import com.pharmacy.scs.entity.Delivery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryEvent {
    private String eventType; // CREATED, UPDATED, COMPLETED
    private Long deliveryId;
    private String status;
    private LocalDateTime timestamp;
    private Map<String, Object> additionalData;

    public static DeliveryEvent from(Delivery delivery, String eventType) {
        return new DeliveryEvent(
                eventType,
                delivery.getId(),
                delivery.getStatus().toString(),
                LocalDateTime.now(),
                new HashMap<>()
        );
    }
}
