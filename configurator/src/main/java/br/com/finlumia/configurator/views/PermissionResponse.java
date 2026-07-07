package br.com.finlumia.configurator.views;

import br.com.finlumia.configurator.models.RoleEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String module,
        String subsystem,
        RoleEnum role,
        @JsonProperty("can_read") boolean canRead,
        @JsonProperty("can_write") boolean canWrite,
        @JsonProperty("can_delete") boolean canDelete,
        @JsonProperty("can_admin") boolean canAdmin) {
}
