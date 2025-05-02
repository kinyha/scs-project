package com.pharmacy.scs.service.impl;

import com.pharmacy.scs.dto.DeliveryEvent;
import com.pharmacy.scs.entity.Delivery;
import com.pharmacy.scs.entity.DeliveryStatus;
import com.pharmacy.scs.exception.DeliveryNotFoundException;
import com.pharmacy.scs.repository.DeliveryRepository;
import com.pharmacy.scs.service.DeliveryService;
import com.pharmacy.scs.service.KafkaDeliveryEventService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final KafkaDeliveryEventService eventService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = {"userDeliveries", "deliveryByTracking"}, allEntries = true)
    public Delivery createDelivery(Delivery request) {
        Delivery delivery = deliveryRepository.save(request);

        // Отправляем событие о создании
        eventService.sendDeliveryEvent(
                DeliveryEvent.from(delivery, "CREATED")
        );

        return delivery;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    @Cacheable(value = "deliveryByTracking", key = "#trackingNumber")
    public Optional<Delivery> getDeliveryByTrackingNumber(String trackingNumber) {
        return deliveryRepository.findByTrackingNumber(trackingNumber);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    @Cacheable(value = "userDeliveries", key = "#userId")
    public List<Delivery> getDeliveriesByUserId(Long userId) {
        return (List<Delivery>) deliveryRepository.findByUserId(userId);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = {"userDeliveries", "deliveryByTracking"}, allEntries = true)
    public Delivery updateDeliveryStatus(Long deliveryId, DeliveryStatus status) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
        delivery.setStatus(status);
        return deliveryRepository.save(delivery);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value = {"userDeliveries", "deliveryByTracking"}, allEntries = true)
    public Delivery completeDelivery(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        delivery.setStatus(DeliveryStatus.COMPLETED);
        delivery.setActualDeliveryTime(LocalDateTime.now());
        delivery = deliveryRepository.save(delivery);

        // Отправляем событие о завершении
        DeliveryEvent event = DeliveryEvent.from(delivery, "COMPLETED");
        event.getAdditionalData().put("completedAt", delivery.getActualDeliveryTime());
        eventService.sendDeliveryEvent(event);

        return delivery;
    }
}