package com.examly.springapp.service;

import com.examly.springapp.model.User;

public interface UserService {
    User findByUsername(String username);
    User save(User user);
}