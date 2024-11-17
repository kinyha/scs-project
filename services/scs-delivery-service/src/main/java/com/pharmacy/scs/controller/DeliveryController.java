package com.pharmacy.scs.controller;

import com.pharmacy.scs.dto.DeliveryCreateRequest;
import com.pharmacy.scs.dto.DeliveryDTO;
import com.pharmacy.scs.dto.DeliveryResponseDTO;
import com.pharmacy.scs.dto.DeliveryUpdateRequest;
import com.pharmacy.scs.entity.Delivery;
import com.pharmacy.scs.exception.DeliveryNotFoundException;
import com.pharmacy.scs.mapper.DeliveryMapper;
import com.pharmacy.scs.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.kafka.common.requests.FetchMetadata.log;


@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService deliveryService;
    private final DeliveryMapper deliveryMapper;

    @PostMapping
    public ResponseEntity<DeliveryDTO> createDelivery(@RequestBody DeliveryCreateRequest request) {
        Delivery delivery = deliveryMapper.toEntity(request);
        Delivery savedDelivery = deliveryService.createDelivery(delivery);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deliveryMapper.toDto(savedDelivery));
    }

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<DeliveryDTO> getDeliveryByTrackingNumber(@PathVariable String trackingNumber) {
        return deliveryService.getDeliveryByTrackingNumber(trackingNumber)
                .map(deliveryMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<DeliveryDTO>> getDeliveriesByUser(@RequestParam Long userId) {
        List<Delivery> deliveries = (List<Delivery>) deliveryService.getDeliveriesByUserId(userId);
        List<DeliveryDTO> deliveryDTOs = deliveries.stream()
                .map(deliveryMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(deliveryDTOs);
    }

    @PutMapping("/{deliveryId}/status")
    public ResponseEntity<DeliveryDTO> updateDeliveryStatus(@PathVariable Long deliveryId, @RequestBody DeliveryUpdateRequest request) {
        Delivery updatedDelivery = deliveryService.updateDeliveryStatus(deliveryId, request.getStatus());
        return ResponseEntity.ok(deliveryMapper.toDto(updatedDelivery));
    }

    @PutMapping("/{deliveryId}/confirm")
    public ResponseEntity<DeliveryResponseDTO> confirmDelivery(@PathVariable Long deliveryId) {
        try {
            Delivery delivery = deliveryService.completeDelivery(deliveryId);
            DeliveryResponseDTO result = DeliveryResponseDTO.fromEntity(delivery);
            return ResponseEntity.accepted().body(result);
        } catch (DeliveryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error while confirming delivery {}", deliveryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}