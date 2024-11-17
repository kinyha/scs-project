package com.pharmacy.scs.mapper;


import org.mapstruct.*;
import com.pharmacy.scs.dto.UserDTO;
import com.pharmacy.scs.dto.UserCreateRequest;
import com.pharmacy.scs.entity.User;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deliveries", ignore = true)
    User toEntity(UserCreateRequest request);

    UserDTO toDto(User user);
}
