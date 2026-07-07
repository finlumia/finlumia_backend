package br.com.finlumia.configurator.views;

import br.com.finlumia.configurator.models.SchemaEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.TriggerEventEnum;
import br.com.finlumia.configurator.models.TriggerTimingEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record DbTriggerResponse(
        UUID id,
        String name,
        @JsonProperty("table_id") UUID tableId,
        String table,
        SchemaEnum schema,
        TriggerEventEnum event,
        TriggerTimingEnum timing,
        String function,
        boolean enabled,
        String description,
        StatusEnum status,
        @JsonProperty("created_at") Instant createdAt) {
}
