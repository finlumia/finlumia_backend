package br.com.finlumia.docs.support.views;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaginationMeta(
        int page,
        int limit,
        @JsonProperty("total") long total,
        @JsonProperty("total_pages") int totalPages
) {
}
