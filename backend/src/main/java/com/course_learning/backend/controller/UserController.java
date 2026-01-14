package com.course_learning.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.course_learning.backend.service.UserService;

@RestController
public class UserController {

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List users = UserService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/health")
    String healthCheck() {
        return "Hello";
    }
}
