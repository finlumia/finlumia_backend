package br.com.finlumia.configurator.models;

import jakarta.validation.constraints.NotNull;

public record ToggleTriggerRequest(@NotNull Boolean enabled) {
}
