package com.example.backend_socialmedia.auth.infrastructure.web;

import com.example.backend_socialmedia.auth.domain.User;
import java.util.Map;

public class AuthMapper {

    public static Map<String, Object> toResponse(User user) {
        return Map.of(
                "id",      user.getId(),
                "name",    user.getName(),
                "email",   user.getEmail(),
                "picture", user.getPicture()
        );
    }
}