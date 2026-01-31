package com.nqt.identity_service.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/identity")
public class AuthController {
    @PostMapping("/login")
    public String login() {
        return "Login successful";
    }
}
