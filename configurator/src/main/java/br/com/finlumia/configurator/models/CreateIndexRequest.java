package br.com.finlumia.configurator.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateIndexRequest(
        @NotBlank @Size(max = 63) String name,
        @NotNull UUID tableId,
        @NotNull SchemaEnum schema,
        @NotBlank String fields,
        @NotNull IndexTypeEnum type,
        Boolean unique,
        Boolean partial,
        String whereClause,
        StatusEnum status) {
}
