package com.pharmacy.scs.notification.service;

import com.pharmacy.scs.notification.dto.DeliveryEvent;
import com.pharmacy.scs.notification.entity.Notification;
import com.pharmacy.scs.notification.entity.Notification.NotificationStatus;
import com.pharmacy.scs.notification.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailNotificationService emailService;

    private DeliveryEvent event;
    private Map<String, Object> templateParams;

    @BeforeEach
    void setUp() {
        // Подготавливаем тестовое событие доставки
        event = new DeliveryEvent();
        event.setEventType("UPDATED");
        event.setDeliveryId(1L);
        event.setStatus("IN_TRANSIT");
        event.setTimestamp(LocalDateTime.now());

        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("trackingNumber", "TRACK123");
        additionalData.put("userId", 123L);
        event.setAdditionalData(additionalData);

        // Подготавливаем параметры шаблона
        templateParams = new HashMap<>();
        templateParams.put("userName", "Test User");
        templateParams.put("trackingNumber", "TRACK123");
    }

    @Test
    void sendNotification_Success() throws Exception {
        // Arrange
        String recipientEmail = "test@example.com";
        String emailContent = "<html><body>Test email content</body></html>";

        // Mock поведение templateEngine
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn(emailContent);

        // Mock поведение mailSender
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Mock поведение repository
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setStatus(NotificationStatus.PENDING);

        Notification updatedNotification = new Notification();
        updatedNotification.setId(1L);
        updatedNotification.setStatus(NotificationStatus.SENT);

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(savedNotification)
                .thenReturn(updatedNotification);

        // Act
        CompletableFuture<Notification> future = emailService.sendNotification(
                event, recipientEmail, templateParams);
        Notification result = future.get(); // Дожидаемся завершения CompletableFuture

        // Assert
        assertNotNull(result);
        assertEquals(NotificationStatus.SENT, result.getStatus());

        // Verify
        verify(templateEngine).process(eq("email-templates/delivery-status"), any(IContext.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void sendNotification_Failure() throws Exception {
        // Arrange
        String recipientEmail = "test@example.com";

        // Mock поведение templateEngine
        when(templateEngine.process(anyString(), any(IContext.class)))
                .thenReturn("Test email content");

        // Mock поведение mailSender
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP server error"))
                .when(mailSender).send(any(MimeMessage.class));

        // Mock поведение repository
        Notification savedNotification = new Notification();
        savedNotification.setId(1L);
        savedNotification.setStatus(NotificationStatus.PENDING);

        Notification updatedNotification = new Notification();
        updatedNotification.setId(1L);
        updatedNotification.setStatus(NotificationStatus.FAILED);

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(savedNotification)
                .thenReturn(updatedNotification);

        // Act
        CompletableFuture<Notification> future = emailService.sendNotification(
                event, recipientEmail, templateParams);
        Notification result = future.get(); // Дожидаемся завершения CompletableFuture

        // Assert
        assertNotNull(result);
        assertEquals(NotificationStatus.FAILED, result.getStatus());

        // Verify
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void supportsEmailType() {
        // Act & Assert
        assertTrue(emailService.supports(Notification.NotificationType.EMAIL));
        assertFalse(emailService.supports(Notification.NotificationType.SMS));
    }
}