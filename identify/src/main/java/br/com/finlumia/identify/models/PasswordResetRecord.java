package br.com.finlumia.identify.models;

import java.time.Instant;

public record PasswordResetRecord(
        String email,
        String otpHash,
        Instant expiresAt,
        String resetSession) {
}
