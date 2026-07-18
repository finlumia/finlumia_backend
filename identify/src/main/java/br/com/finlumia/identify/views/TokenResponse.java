package br.com.finlumia.identify.views;

import java.time.Instant;
import java.util.UUID;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserInfo user) {

    public record UserInfo(
            UUID id,
            String name,
            String email,
            String role,
            String status,
            boolean mfa,
            Instant lastLogin,
            Instant createdAt,
            Instant updatedAt) {}
}
