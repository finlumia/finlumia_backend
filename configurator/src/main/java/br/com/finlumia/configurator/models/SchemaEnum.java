package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SchemaEnum {
    @JsonProperty("public") PUBLIC,
    @JsonProperty("auth") AUTH,
    @JsonProperty("log") LOG,
    @JsonProperty("cache") CACHE,
    @JsonProperty("billing") BILLING
}
