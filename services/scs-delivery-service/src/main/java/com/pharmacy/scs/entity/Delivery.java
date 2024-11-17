package com.pharmacy.scs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
public class Delivery extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String deliveryAddress;

    @Column(nullable = false)
    private LocalDateTime expectedDeliveryTime;

    private LocalDateTime actualDeliveryTime;


    @Column(nullable = false)
    private DeliveryStatus status;
}
