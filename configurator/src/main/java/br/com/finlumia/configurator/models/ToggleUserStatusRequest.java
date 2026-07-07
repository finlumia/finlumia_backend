package br.com.finlumia.configurator.models;

import jakarta.validation.constraints.NotNull;

public record ToggleUserStatusRequest(@NotNull StatusEnum status) {
}
