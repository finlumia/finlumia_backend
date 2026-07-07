package br.com.finlumia.document.views;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InsightsView(
        List<InsightView> data,
        @JsonProperty("generatedAt") Instant generatedAt
) {
}
