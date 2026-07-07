package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TriggerTimingEnum {
    @JsonProperty("BEFORE") BEFORE,
    @JsonProperty("AFTER") AFTER,
    @JsonProperty("INSTEAD OF") INSTEAD_OF
}
