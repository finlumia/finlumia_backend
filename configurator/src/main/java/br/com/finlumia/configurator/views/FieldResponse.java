package br.com.finlumia.configurator.views;

import br.com.finlumia.configurator.models.DataTypeEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record FieldResponse(
        UUID id,
        String name,
        @JsonProperty("table_id") UUID tableId,
        String table,
        @JsonProperty("data_type") DataTypeEnum dataType,
        Integer length,
        boolean nullable,
        @JsonProperty("default_value") String defaultValue,
        @JsonProperty("is_primary") boolean isPrimary,
        @JsonProperty("is_foreign") boolean isForeign,
        @JsonProperty("references_table") String referencesTable,
        @JsonProperty("references_field") String referencesField,
        StatusEnum status,
        @JsonProperty("created_at") Instant createdAt) {
}
