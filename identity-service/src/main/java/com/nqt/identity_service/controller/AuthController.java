package com.nqt.identity_service.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @PostMapping("/login")
    public String login() {
        return "Login successful";
    }

    @PostMapping("/internal/register")
    public String register() {
        return "Register successful";
    }
}
