package br.com.finlumia.configurator.models;

public record UpdateTriggerRequest(
        String function,
        Boolean enabled,
        String description,
        StatusEnum status) {
}
