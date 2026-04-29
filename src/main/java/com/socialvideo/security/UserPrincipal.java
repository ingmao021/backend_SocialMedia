package com.socialvideo.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

@Getter
public class UserPrincipal extends AbstractAuthenticationToken {

    private final Long userId;
    private final String email;

    public UserPrincipal(Long userId, String email) {
        super(Collections.emptyList());
        this.userId = userId;
        this.email = email;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }
}
