package br.com.finlumia.configurator.views;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaginationMeta(
        @JsonProperty("page") int page,
        @JsonProperty("page_size") int pageSize,
        @JsonProperty("total_items") long totalItems,
        @JsonProperty("total_pages") int totalPages) {
}
