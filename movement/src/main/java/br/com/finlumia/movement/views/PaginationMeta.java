package br.com.finlumia.movement.views;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaginationMeta(
        int page,
        @JsonProperty("pageSize") int pageSize,
        int total,
        @JsonProperty("totalPages") int totalPages
) {
    public static PaginationMeta of(int page, int pageSize, int total) {
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PaginationMeta(page, pageSize, total, Math.max(totalPages, 1));
    }
}
