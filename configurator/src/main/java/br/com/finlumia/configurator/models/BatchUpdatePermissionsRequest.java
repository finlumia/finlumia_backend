package br.com.finlumia.configurator.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BatchUpdatePermissionsRequest(
        @NotNull @NotEmpty List<@Valid PermissionUpdateItem> permissions) {
}
