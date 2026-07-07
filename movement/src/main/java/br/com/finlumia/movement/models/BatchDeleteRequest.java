package br.com.finlumia.movement.models;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BatchDeleteRequest(
        @NotNull @NotEmpty List<UUID> ids
) {}
