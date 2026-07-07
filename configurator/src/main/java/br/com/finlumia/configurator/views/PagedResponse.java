package br.com.finlumia.configurator.views;

import java.util.List;

public record PagedResponse<T>(List<T> data, PaginationMeta meta) {
}
