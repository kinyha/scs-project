package com.pharmacy.scs.notification.service;

import com.pharmacy.scs.notification.dto.DeliveryEvent;
import com.pharmacy.scs.notification.entity.Notification;
import com.pharmacy.scs.notification.entity.Notification.NotificationStatus;
import com.pharmacy.scs.notification.entity.Notification.NotificationType;
import com.pharmacy.scs.notification.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService implements NotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationRepository notificationRepository;

    @Override
    @Async
    @Transactional
    public CompletableFuture<Notification> sendNotification(
            DeliveryEvent event,
            String recipientEmail,
            Map<String, Object> templateParams) {

        log.debug("Preparing to send email notification for delivery ID: {}", event.getDeliveryId());

        // Создаем запись о уведомлении
        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .deliveryId(event.getDeliveryId())
                .eventType(event.getEventType())
                .type(NotificationType.EMAIL)
                .recipient(recipientEmail)
                .status(NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        try {
            // Формируем содержимое письма с использованием Thymeleaf
            Context context = new Context();
            context.setVariables(templateParams);
            String emailContent = templateEngine.process("email-templates/delivery-status", context);

            // Формируем тему письма
            String subject = getSubjectByEventType(event.getEventType());

            // Отправляем письмо
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(emailContent, true); // true означает HTML-содержимое

            mailSender.send(message);

            // Обновляем статус уведомления
            notification.setContent(emailContent);
            notification.markAsSent();
            log.info("Email notification sent successfully to {} for delivery ID: {}",
                    recipientEmail, event.getDeliveryId());

        } catch (MessagingException e) {
            log.error("Failed to send email notification for delivery ID: {}",
                    event.getDeliveryId(), e);
            notification.markAsFailed(e.getMessage());
        }

        // Сохраняем обновленный статус уведомления
        notification = notificationRepository.save(notification);
        return CompletableFuture.completedFuture(notification);
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<Notification> resendNotification(Notification notification) {
        log.debug("Attempting to resend email notification ID: {}", notification.getId());

        try {
            // Восстанавливаем содержимое письма
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(notification.getRecipient());
            helper.setSubject(getSubjectByEventType(notification.getEventType()));
            helper.setText(notification.getContent(), true);

            mailSender.send(message);

            notification.markAsSent();
            log.info("Email notification resent successfully, ID: {}", notification.getId());

        } catch (MessagingException e) {
            log.error("Failed to resend email notification ID: {}", notification.getId(), e);
            notification.markAsFailed(e.getMessage());
        }

        notification = notificationRepository.save(notification);
        return CompletableFuture.completedFuture(notification);
    }

    @Override
    public boolean supports(NotificationType type) {
        return NotificationType.EMAIL.equals(type);
    }

    // Вспомогательный метод для формирования темы письма
    private String getSubjectByEventType(String eventType) {
        return switch (eventType) {
            case "CREATED" -> "Ваш заказ принят в обработку";
            case "UPDATED" -> "Статус вашего заказа изменен";
            case "COMPLETED" -> "Ваш заказ доставлен";
            default -> "Обновление статуса доставки";
        };
    }
}