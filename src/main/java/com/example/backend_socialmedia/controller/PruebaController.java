package com.example.backend_socialmedia.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PruebaController {

    @GetMapping("/api/status")
    public String verificarEstado() {
        return "Servidor de SocialVideo AI: ONLINE y conectado a la base de datos.";
    }
}