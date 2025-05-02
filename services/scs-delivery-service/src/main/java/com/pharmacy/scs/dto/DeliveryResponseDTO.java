package com.pharmacy.scs.dto;

import com.pharmacy.scs.entity.Delivery;
import com.pharmacy.scs.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponseDTO {
    private Long id;
    private String trackingNumber;
    private Long userId;  // Instead of entire User object
    private String deliveryAddress;
    private LocalDateTime expectedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
    private DeliveryStatus status;

    private String statusDescription;
    private boolean isDelivered;
    private Long deliveryDuration;  // в минутах

    // Статический метод для конвертации Delivery в DTO
    public static DeliveryResponseDTO fromEntity(Delivery delivery) {
        return DeliveryResponseDTO.builder()
                .id(delivery.getId())
                .trackingNumber(delivery.getTrackingNumber())
                .userId(delivery.getUser().getId())
                .deliveryAddress(delivery.getDeliveryAddress())
                .expectedDeliveryTime(delivery.getExpectedDeliveryTime())
                .actualDeliveryTime(delivery.getActualDeliveryTime())
                .status(delivery.getStatus())
                .statusDescription(getStatusDescription(delivery.getStatus()))
                .isDelivered(delivery.getStatus() == DeliveryStatus.DELIVERED)
                .deliveryDuration(calculateDeliveryDuration(
                        delivery.getExpectedDeliveryTime(),
                        delivery.getActualDeliveryTime()))
                .build();
    }

    private static String getStatusDescription(DeliveryStatus status) {
        return switch (status) {
            case PENDING -> "Ожидает отправки";
            case IN_TRANSIT -> "В пути";
            case DELIVERED -> "Доставлено";
            case CANCELLED -> "Отменено";
            default -> "Неизвестный статус";
        };
    }

    private static Long calculateDeliveryDuration(
            LocalDateTime expectedTime,
            LocalDateTime actualTime) {
        if (expectedTime == null || actualTime == null) {
            return null;
        }
        return ChronoUnit.MINUTES.between(expectedTime, actualTime);
    }
}
