package com.pharmacy.scs.notification.repository;

import com.pharmacy.scs.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndDeliveryId(Long userId, Long deliveryId);

    List<Notification> findByStatus(Notification.NotificationStatus status);

    List<Notification> findByUserId(Long userId);

    List<Notification> findByDeliveryId(Long deliveryId);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < :maxRetries AND n.createdAt > :since")
    List<Notification> findFailedNotificationsForRetry(
            Notification.NotificationStatus status,
            int maxRetries,
            LocalDateTime since);

    // Индексированный запрос для быстрого поиска (будет использоваться часто)
    @Query(value = "SELECT n FROM Notification n WHERE n.deliveryId = :deliveryId AND n.eventType = :eventType AND n.type = :type")
    List<Notification> findByDeliveryAndEventTypeAndNotificationType(
            Long deliveryId,
            String eventType,
            Notification.NotificationType type);
}