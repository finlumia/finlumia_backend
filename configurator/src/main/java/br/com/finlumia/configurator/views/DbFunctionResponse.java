package br.com.finlumia.configurator.views;

import br.com.finlumia.configurator.models.LanguageEnum;
import br.com.finlumia.configurator.models.SchemaEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.VolatilityEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record DbFunctionResponse(
        UUID id,
        String name,
        SchemaEnum schema,
        LanguageEnum language,
        @JsonProperty("return_type") String returnType,
        String args,
        VolatilityEnum volatility,
        String body,
        String description,
        StatusEnum status,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {
}
