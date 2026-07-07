package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum VolatilityEnum {
    @JsonProperty("volatile") VOLATILE,
    @JsonProperty("stable") STABLE,
    @JsonProperty("immutable") IMMUTABLE
}
