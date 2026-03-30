package com.example.backend_socialmedia.shared.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class TestController {
    @GetMapping("/hola")
    public String saludo() {
        return "¡Conexión exitosa! El backend de SocialVideo AI está funcionando.";
    }
}
