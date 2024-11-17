package com.pharmacy.scs.service;

import com.pharmacy.scs.entity.Delivery;
import com.pharmacy.scs.entity.DeliveryStatus;
import com.pharmacy.scs.exception.DeliveryException;

import java.util.List;
import java.util.Optional;

public interface DeliveryService {
    Delivery createDelivery(Delivery delivery);
    Optional<Delivery> getDeliveryByTrackingNumber(String trackingNumber);
    List<Delivery> getDeliveriesByUserId(Long userId);
    Delivery updateDeliveryStatus(Long deliveryId, DeliveryStatus status);
    Delivery completeDelivery(Long deliveryId);
}