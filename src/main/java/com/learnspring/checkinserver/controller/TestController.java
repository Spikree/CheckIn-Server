package com.learnspring.checkinserver.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
public class TestController {
    @GetMapping("/health")
    public String ceckHealth() {
        return "OK";
    }
}
