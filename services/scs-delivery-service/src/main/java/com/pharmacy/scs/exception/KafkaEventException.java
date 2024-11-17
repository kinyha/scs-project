package com.pharmacy.scs.exception;

public class KafkaEventException extends RuntimeException {
    public KafkaEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
