package br.com.finlumia.document.views;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import br.com.finlumia.document.models.InsightType;
import com.fasterxml.jackson.annotation.JsonProperty;

public record InsightView(
        UUID id,
        InsightType type,
        String title,
        String description,
        String icon,
        String relatedCategory,
        BigDecimal delta,
        @JsonProperty("generatedAt") Instant generatedAt
) {
}
