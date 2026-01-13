package com.course_learning.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class UserController {

    @GetMapping("path")
    public String getAllUsers(@RequestParam String param) {
        return new String();
    }
    

    @GetMapping
    String healthCheck() {
        return "Hello";
    }
}
