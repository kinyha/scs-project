package com.pharmacy.scs.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO для событий доставки, принимаемых из Kafka.
 * Должен соответствовать формату, отправляемому из delivery-service.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryEvent {
    private String eventType; // CREATED, UPDATED, COMPLETED
    private Long deliveryId;
    private String status;
    private LocalDateTime timestamp;
    private Map<String, Object> additionalData = new HashMap<>();

    // Вспомогательные методы
    public String getTrackingNumber() {
        return additionalData.containsKey("trackingNumber")
                ? (String) additionalData.get("trackingNumber")
                : null;
    }

    public Long getUserId() {
        if (additionalData.containsKey("userId")) {
            Object value = additionalData.get("userId");
            if (value instanceof Integer) {
                return ((Integer) value).longValue();
            } else if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAdditionalData(String key, Class<T> clazz) {
        if (additionalData.containsKey(key)) {
            Object value = additionalData.get(key);
            if (clazz.isInstance(value)) {
                return (T) value;
            }
        }
        return null;
    }
}