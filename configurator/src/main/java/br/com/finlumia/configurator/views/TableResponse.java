package br.com.finlumia.configurator.views;

import br.com.finlumia.configurator.models.SchemaEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record TableResponse(
        UUID id,
        String name,
        SchemaEnum schema,
        @JsonProperty("row_count") long rowCount,
        @JsonProperty("size_kb") long sizeKb,
        StatusEnum status,
        String description,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {
}
