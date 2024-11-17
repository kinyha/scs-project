package com.pharmacy.scs.dto;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
    private String role;
}
