package br.com.finlumia.identify.models;

import java.time.Instant;

public record EmailVerificationRecord(
        String email,
        String codeHash,
        Instant expiresAt) {
}
