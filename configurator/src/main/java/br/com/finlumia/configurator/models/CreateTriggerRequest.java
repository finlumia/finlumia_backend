package br.com.finlumia.configurator.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTriggerRequest(
        @NotBlank @Size(max = 63) String name,
        @NotNull UUID tableId,
        @NotNull SchemaEnum schema,
        @NotNull TriggerEventEnum event,
        @NotNull TriggerTimingEnum timing,
        @NotBlank String function,
        Boolean enabled,
        String description,
        StatusEnum status) {
}
