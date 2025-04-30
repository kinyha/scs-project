package com.pharmacy.scs.notification.service;

import com.pharmacy.scs.notification.dto.DeliveryEvent;
import com.pharmacy.scs.notification.entity.Notification;
import com.pharmacy.scs.notification.exception.NotificationException;
import com.pharmacy.scs.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaListenerService {

    private final List<NotificationService> notificationServices;
    private final NotificationRepository notificationRepository;
    private final UserServiceClient userServiceClient; // Этот клиент нужно будет определить

    @Value("${notification.retry.max-attempts}")
    private int maxRetryAttempts;

    /**
     * Слушатель сообщений из топика delivery-updates.
     * Ack Mode: MANUAL_IMMEDIATE для гарантии обработки (at-least-once)
     */
    @KafkaListener(
            topics = "${kafka.topics.delivery-updates}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void listenDeliveryUpdates(
            @Payload DeliveryEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received delivery event: type={}, deliveryId={}, from topic={}, partition={}, offset={}",
                event.getEventType(), event.getDeliveryId(), topic, partition, offset);

        try {
            // Получаем информацию о пользователе для отправки уведомления
            Long userId = event.getUserId();
            if (userId == null) {
                log.error("User ID is missing in delivery event: {}", event);
                acknowledgment.acknowledge();
                return;
            }

            // Получаем информацию о пользователе из UserService
            UserDTO user = userServiceClient.getUserById(userId);
            if (user == null) {
                log.error("User with ID {} not found", userId);
                acknowledgment.acknowledge();
                return;
            }

            // Подготавливаем параметры для шаблона уведомления
            Map<String, Object> templateParams = prepareTemplateParams(event, user);

            // Отправляем email уведомление
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                sendNotificationByType(event, user.getEmail(), templateParams, Notification.NotificationType.EMAIL);
            }

            // Отправляем SMS уведомление, если есть номер телефона
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                //sendNotificationByType(event, user.getPhoneNumber(), templateParams, Notification.NotificationType.SMS);
            }

            // Подтверждаем обработку сообщения только после успешной отправки уведомлений
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing delivery event: {}", event, e);
            // Мы не вызываем ack.acknowledge() здесь, что приведет к повторной обработке
            // сообщения после таймаута или перебалансировке
            throw new NotificationException("Failed to process delivery event", e);
        }
    }

    /**
     * Слушатель для Dead Letter Queue (DLQ).
     * Сообщения, которые не удалось обработать, будут помещены сюда.
     */
    @KafkaListener(
            topics = "${kafka.topics.notification-dlq}",
            groupId = "${spring.kafka.consumer.group-id}-dlq",
            containerFactory = "kafkaDltListenerContainerFactory"
    )
    public void listenDeliveryUpdatesDLQ(
            @Payload DeliveryEvent event,
            Acknowledgment acknowledgment) {

        log.warn("Processing message from DLQ: {}", event);

        // Здесь можно реализовать специальную логику для обработки
        // "мертвых" сообщений, например, сохранение их в БД для
        // последующего анализа или специальной обработки

        // Всегда подтверждаем получение из DLQ
        acknowledgment.acknowledge();
    }

    /**
     * Отправляет уведомление выбранного типа
     */
    private void sendNotificationByType(
            DeliveryEvent event,
            String recipient,
            Map<String, Object> templateParams,
            Notification.NotificationType type) {

        // Находим подходящий сервис для отправки уведомления
        NotificationService service = notificationServices.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new NotificationException("No service found for notification type: " + type));

        // Проверяем, не отправляли ли мы уже это уведомление
        // Это важно для идемпотентности при повторной обработке сообщений
        List<Notification> existingNotifications = notificationRepository
                .findByDeliveryAndEventTypeAndNotificationType(
                        event.getDeliveryId(), event.getEventType(), type);

        if (!existingNotifications.isEmpty()) {
            log.info("Notification already sent for delivery ID: {}, event type: {}, notification type: {}",
                    event.getDeliveryId(), event.getEventType(), type);
            return;
        }

        // Асинхронно отправляем уведомление
        CompletableFuture<Notification> future = service.sendNotification(event, recipient, templateParams);

        // Добавляем обработчик для логирования результата
        future.whenComplete((notification, throwable) -> {
            if (throwable != null) {
                log.error("Failed to send notification asynchronously", throwable);
            } else {
                log.info("Notification sent successfully: {}", notification);
            }
        });
    }

    /**
     * Планировщик для повторной отправки неудавшихся уведомлений.
     * Запускается по расписанию, например, каждые 15 минут.
     */
    // @Scheduled(fixedDelayString = "${notification.retry.fixed-delay:900000}")
    @Transactional
    public void retryFailedNotifications() {
        log.info("Starting retry job for failed notifications");

        // Получаем неудавшиеся уведомления за последние 24 часа
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        List<Notification> failedNotifications = notificationRepository
                .findFailedNotificationsForRetry(Notification.NotificationStatus.FAILED, maxRetryAttempts, since);

        log.info("Found {} failed notifications for retry", failedNotifications.size());

        for (Notification notification : failedNotifications) {
            Notification finalNotification = notification;
            NotificationService service = notificationServices.stream()
                    .filter(s -> s.supports(finalNotification.getType()))
                    .findFirst()
                    .orElse(null);

            if (service == null) {
                log.warn("No service found for notification type: {}", notification.getType());
                continue;
            }

            // Обновляем статус перед повторной отправкой
            notification.markAsRetry();
            notification = notificationRepository.save(notification);

            // Повторно отправляем уведомление
            Notification finalNotification1 = notification;
            service.resendNotification(notification)
                    .whenComplete((updatedNotification, throwable) -> {
                        if (throwable != null) {
                            log.error("Failed to resend notification ID: {}", finalNotification1.getId(), throwable);
                        } else {
                            log.info("Successfully resent notification ID: {}", finalNotification1.getId());
                        }
                    });
        }
    }

    /**
     * Подготавливает параметры для шаблонов уведомлений
     */
    private Map<String, Object> prepareTemplateParams(DeliveryEvent event, UserDTO user) {
        Map<String, Object> params = new HashMap<>();

        // Базовые параметры
        params.put("userName", user.getUsername());
        params.put("trackingNumber", event.getTrackingNumber());
        params.put("status", event.getStatus());
        params.put("trackingUrl", "http://tracking.pharmacy.com/" + event.getTrackingNumber());

        // Отображаемый статус
        String statusDescription = switch (event.getStatus()) {
            case "PENDING" -> "Ожидает отправки";
            case "IN_TRANSIT" -> "В пути";
            case "DELIVERED" -> "Доставлено";
            case "CANCELLED" -> "Отменено";
            default -> event.getStatus();
        };
        params.put("statusDescription", statusDescription);

        // Дополнительные данные из события
        event.getAdditionalData().forEach((key, value) -> {
            if (value != null) {
                params.put(key, value);
            }
        });

        return params;
    }

    // Внутренний класс для данных пользователя
    // В реальном проекте лучше вынести в отдельный файл
    public static class UserDTO {
        private Long id;
        private String username;
        private String email;
        private String phoneNumber;

        // Getters and setters...
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }
}