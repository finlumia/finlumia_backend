package br.com.finlumia.identify.models;

import java.time.Instant;
import java.util.UUID;

public record UserProfileRecord(
        UUID key,
        String name,
        String email,
        String passwordHash,
        UserRole role,
        UserStatus status,
        boolean mfa,
        String locale,
        String theme,
        Instant lastLogin,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
