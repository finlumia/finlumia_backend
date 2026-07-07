package br.com.finlumia.configurator.views;

import br.com.finlumia.configurator.models.RoleEnum;
import br.com.finlumia.configurator.models.UserAdminStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String name,
        String email,
        RoleEnum role,
        UserAdminStatus status,
        boolean mfa,
        @JsonProperty("last_login") Instant lastLogin,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {
}
