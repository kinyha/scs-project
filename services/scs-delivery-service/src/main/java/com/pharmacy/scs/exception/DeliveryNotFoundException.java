package com.pharmacy.scs.exception;

public class DeliveryNotFoundException extends RuntimeException {
    public DeliveryNotFoundException(Long message) {
        super(String.valueOf(message));
    }
}
