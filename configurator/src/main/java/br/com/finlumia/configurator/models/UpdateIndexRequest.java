package br.com.finlumia.configurator.models;

public record UpdateIndexRequest(
        String whereClause,
        StatusEnum status) {
}
