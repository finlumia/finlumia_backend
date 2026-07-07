package br.com.finlumia.configurator.views;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record IndexStatsResponse(
        @JsonProperty("size_kb") long sizeKb,
        long scans,
        @JsonProperty("tuple_reads") long tupleReads,
        @JsonProperty("blks_read") long blksRead,
        @JsonProperty("last_used") Instant lastUsed) {
}
