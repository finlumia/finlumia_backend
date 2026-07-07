package br.com.finlumia.identify.views;

import br.com.finlumia.identify.models.UserRole;
import br.com.finlumia.identify.models.UserStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        UserStatus status,
        boolean mfa,
        @JsonProperty("last_login") Instant lastLogin,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {
}
