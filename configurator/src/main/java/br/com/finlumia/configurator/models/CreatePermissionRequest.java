package br.com.finlumia.configurator.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePermissionRequest(
        @NotBlank String module,
        @NotBlank String subsystem,
        @NotNull RoleEnum role,
        @NotNull Boolean canRead,
        @NotNull Boolean canWrite,
        @NotNull Boolean canDelete,
        @NotNull Boolean canAdmin) {
}
