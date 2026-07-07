package br.com.finlumia.docs.support.views;

import java.util.List;

public record PagedResponse<T>(List<T> data, PaginationMeta meta) {
}
