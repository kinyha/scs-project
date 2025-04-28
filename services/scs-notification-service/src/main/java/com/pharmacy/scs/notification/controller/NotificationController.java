package com.pharmacy.scs.notification.controller;

import com.pharmacy.scs.notification.entity.Notification;
import com.pharmacy.scs.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Контроллер для мониторинга и управления уведомлениями.
 * В реальном проекте следует добавить проверку авторизации.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationRepository notificationRepository;

    /**
     * Получает список всех уведомлений для пользователя
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getNotificationsByUser(@PathVariable Long userId) {
        log.debug("Getting notifications for user ID: {}", userId);
        List<Notification> notifications = notificationRepository.findByUserIdAndDeliveryId(userId, null);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Получает список уведомлений для конкретной доставки
     */
    @GetMapping("/delivery/{deliveryId}")
    public ResponseEntity<List<Notification>> getNotificationsByDelivery(@PathVariable Long deliveryId) {
        log.debug("Getting notifications for delivery ID: {}", deliveryId);
        List<Notification> notifications = notificationRepository.findByUserIdAndDeliveryId(null, deliveryId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Получает статистику по уведомлениям
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        log.debug("Getting notification statistics");

        long totalCount = notificationRepository.count();
        long sentCount = notificationRepository.findByStatus(Notification.NotificationStatus.SENT).size();
        long failedCount = notificationRepository.findByStatus(Notification.NotificationStatus.FAILED).size();
        long pendingCount = notificationRepository.findByStatus(Notification.NotificationStatus.PENDING).size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("sentCount", sentCount);
        stats.put("failedCount", failedCount);
        stats.put("pendingCount", pendingCount);
        stats.put("successRate", totalCount > 0 ? (double) sentCount / totalCount : 0);

        return ResponseEntity.ok(stats);
    }

    /**
     * Получает информацию о конкретном уведомлении
     */
    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotification(@PathVariable Long id) {
        log.debug("Getting notification with ID: {}", id);
        return notificationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}