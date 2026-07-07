package br.com.finlumia.configurator.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTableRequest(
        @NotBlank @Size(max = 63) @Pattern(regexp = "^[a-z_][a-z0-9_]*$") String name,
        @NotNull SchemaEnum schema,
        String description,
        StatusEnum status) {
}
