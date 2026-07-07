package br.com.finlumia.configurator.models;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PermissionUpdateItem(
        @NotNull UUID id,
        @NotNull Boolean canRead,
        @NotNull Boolean canWrite,
        @NotNull Boolean canDelete,
        @NotNull Boolean canAdmin) {
}
