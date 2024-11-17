package com.pharmacy.scs.service;

import com.pharmacy.scs.entity.User;

import java.util.Optional;

public interface UserService {
    User createUser(User user);
    Optional<User> getUserByEmail(String email);
}

