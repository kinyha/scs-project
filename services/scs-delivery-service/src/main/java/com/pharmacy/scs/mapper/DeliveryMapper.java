package com.pharmacy.scs.mapper;

import com.pharmacy.scs.dto.DeliveryCreateRequest;
import com.pharmacy.scs.dto.DeliveryDTO;
import com.pharmacy.scs.entity.Delivery;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DeliveryMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "actualDeliveryTime", ignore = true)  // Add this line
    @Mapping(source = "userId", target = "user.id")
    Delivery toEntity(DeliveryCreateRequest request);

    @Mapping(source = "user.id", target = "userId")
    DeliveryDTO toDto(Delivery delivery);
}