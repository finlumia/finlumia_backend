package br.com.finlumia.configurator.models;

public record UpdateTableRequest(
        String description,
        StatusEnum status) {
}
