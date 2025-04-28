package com.pharmacy.scs.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long deliveryId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(length = 512)
    private String recipient; // email или номер телефона

    @Column(length = 1024)
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private String errorMessage;

    @Column
    private Integer retryCount;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    public enum NotificationType {
        EMAIL, SMS
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED, RETRY
    }

    public boolean canRetry(int maxRetries) {
        return status == NotificationStatus.FAILED &&
                retryCount < maxRetries;
    }

    public void markAsRetry() {
        status = NotificationStatus.RETRY;
        retryCount++;
    }

    public void markAsSent() {
        status = NotificationStatus.SENT;
        sentAt = LocalDateTime.now();
    }

    public void markAsFailed(String error) {
        status = NotificationStatus.FAILED;
        errorMessage = error;
    }
}