package br.com.finlumia.identify.models;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenRecord(
        UUID key,
        UUID userKey,
        String tokenHash,
        Instant expiresAt,
        boolean revoked,
        Instant createdAt) {
}
