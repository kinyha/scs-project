package com.pharmacy.scs.service;

import com.pharmacy.scs.dto.DeliveryEvent;
import com.pharmacy.scs.exception.KafkaEventException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaDeliveryEventService {

    private final KafkaTemplate<String, DeliveryEvent> kafkaTemplate;

    @Value("${kafka.topics.delivery-updates}")
    private String deliveryUpdatesTopic;

    public KafkaDeliveryEventService(KafkaTemplate<String, DeliveryEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void sendDeliveryEvent(DeliveryEvent event) {
        try {
            String key = event.getDeliveryId().toString();

            kafkaTemplate.send(deliveryUpdatesTopic, key, event)
                    .thenAccept(result -> log.info("Delivery event sent successfully: {}", event))
                    .exceptionally(ex -> {
                        log.error("Failed to send delivery event: {}", event, ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Error while sending delivery event: {}", e);
            throw new KafkaEventException("Failed to send event", e);
        }
    }
}
