package com.example.backend_socialmedia.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StatusController {

    @GetMapping("/check")
    public String checkStatus() {
        return "🚀 Backend de SocialVideo AI funcionando correctamente.";
    }
}