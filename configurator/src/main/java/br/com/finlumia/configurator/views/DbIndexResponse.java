package br.com.finlumia.configurator.views;

import br.com.finlumia.configurator.models.IndexTypeEnum;
import br.com.finlumia.configurator.models.SchemaEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record DbIndexResponse(
        UUID id,
        String name,
        @JsonProperty("table_id") UUID tableId,
        String table,
        SchemaEnum schema,
        String fields,
        IndexTypeEnum type,
        boolean unique,
        boolean partial,
        @JsonProperty("where_clause") String whereClause,
        @JsonProperty("size_kb") long sizeKb,
        StatusEnum status,
        @JsonProperty("created_at") Instant createdAt) {
}
