package com.pharmacy.scs.notification.service;

import com.pharmacy.scs.notification.dto.DeliveryEvent;
import com.pharmacy.scs.notification.entity.Notification;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Интерфейс для сервисов отправки уведомлений.
 * Каждый тип уведомления (Email, SMS) будет иметь свою реализацию.
 */
public interface NotificationService {

    /**
     * Отправляет уведомление на основе события доставки
     *
     * @param event Событие доставки из Kafka
     * @param recipientContact Контактная информация получателя (email или телефон)
     * @param templateParams Параметры шаблона уведомления
     * @return CompletableFuture с результатом отправки
     */
    CompletableFuture<Notification> sendNotification(
            DeliveryEvent event,
            String recipientContact,
            Map<String, Object> templateParams);

    /**
     * Повторно отправляет уведомление, которое ранее не удалось отправить
     *
     * @param notification Сущность уведомления для повторной отправки
     * @return CompletableFuture с результатом отправки
     */
    CompletableFuture<Notification> resendNotification(Notification notification);

    /**
     * Определяет, может ли данный сервис обработать указанный тип уведомления
     *
     * @param type Тип уведомления
     * @return true, если сервис может обработать указанный тип
     */
    boolean supports(Notification.NotificationType type);
}