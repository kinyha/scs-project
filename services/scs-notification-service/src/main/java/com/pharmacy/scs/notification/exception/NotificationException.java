package com.pharmacy.scs.notification.exception;

/**
 * Исключение, бросаемое при ошибках отправки уведомлений
 */
public class NotificationException extends RuntimeException {

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}