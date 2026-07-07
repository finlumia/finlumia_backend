package br.com.finlumia.identify.models;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID key,
        String email,
        String passwordHash,
        boolean active,
        Instant createdAt,
        int failedAttempts,
        Instant lockedUntil,
        boolean emailVerified) {
}
