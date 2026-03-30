package com.example.backend_socialmedia.auth.infrastructure.persistence;

import com.example.backend_socialmedia.auth.domain.User;
import com.example.backend_socialmedia.auth.domain.UserRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryImpl(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByGoogleId(String googleId) {
        return jpa.findByGoogleId(googleId).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    private User toDomain(UserEntity e) {
        return new User(e.getId(), e.getName(), e.getEmail(), e.getPicture(), e.getGoogleId());
    }

    private UserEntity toEntity(User u) {
        UserEntity e = new UserEntity();
        if (u.getId() != null) e.setId(u.getId());
        e.setName(u.getName());
        e.setEmail(u.getEmail());
        e.setPicture(u.getPicture());
        e.setGoogleId(u.getGoogleId());
        return e;
    }
}