package br.com.finlumia.docs.support.views;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record TicketStatsView(
        @JsonProperty("by_status")   Map<String, Long> byStatus,
        @JsonProperty("by_category") Map<String, Long> byCategory,
        @JsonProperty("by_priority") Map<String, Long> byPriority,
        long total,
        @JsonProperty("avg_resolution_hours") Double avgResolutionHours
) {
}
