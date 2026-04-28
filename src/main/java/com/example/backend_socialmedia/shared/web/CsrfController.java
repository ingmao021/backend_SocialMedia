package com.example.backend_socialmedia.shared.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class CsrfController {
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(CsrfToken token) {
        // El token también se envía como cookie por Spring Security
        return ResponseEntity.ok(Map.of("csrfToken", token.getToken()));
    }
}

