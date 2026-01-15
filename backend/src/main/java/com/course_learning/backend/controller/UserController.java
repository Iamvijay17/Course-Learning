package com.course_learning.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.course_learning.backend.dto.LoginRequest;
import com.course_learning.backend.dto.LoginResponse;
import com.course_learning.backend.model.User;
import com.course_learning.backend.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String token = userService.login(loginRequest.getUserName(), loginRequest.getPassword());
        if (token != null) {
            return ResponseEntity.ok(new LoginResponse(token));
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User userData) {
        User createdUser = userService.createUser(userData);
        return ResponseEntity.ok(createdUser);
    }


    // delete user by userid
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUserById(@PathVariable String userId) {
        boolean deleted = userService.deleteUserById(userId);
        if (deleted) {
            return ResponseEntity.ok("User deleted successfully.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/health")
    String healthCheck() {
        return "Hello";
    }
}
