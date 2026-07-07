package br.com.finlumia.movement.models;

import java.util.List;

import jakarta.validation.constraints.Size;

public record TransactionPatchRequest(
        CategoryId category,
        @Size(max = 500) String description,
        @Size(max = 2000) String notes,
        @Size(max = 20) List<@Size(max = 50) String> tags
) {}
