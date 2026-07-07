package br.com.finlumia.configurator.views;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record RebuildIndexResponse(
        @JsonProperty("job_id") UUID jobId,
        @JsonProperty("estimated_seconds") int estimatedSeconds) {
}
