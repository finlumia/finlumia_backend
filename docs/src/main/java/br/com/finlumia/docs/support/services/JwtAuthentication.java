package br.com.finlumia.docs.support.services;

import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtAuthentication extends AbstractAuthenticationToken {

    public static final String ROLE_USER = "ROLE_USER";

    private final UUID userKey;
    private final String email;

    public JwtAuthentication(UUID userKey, String email) {
        super(List.of(new SimpleGrantedAuthority(ROLE_USER)));
        this.userKey = userKey;
        this.email = email;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userKey;
    }

    public UUID getUserKey() {
        return userKey;
    }

    public String getEmail() {
        return email;
    }
}
