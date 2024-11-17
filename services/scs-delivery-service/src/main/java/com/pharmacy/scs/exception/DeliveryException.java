package com.pharmacy.scs.exception;

public class DeliveryException extends Throwable {
    public DeliveryException(String deliveryAlreadyConfirmed) {
        super(deliveryAlreadyConfirmed);
    }
}
