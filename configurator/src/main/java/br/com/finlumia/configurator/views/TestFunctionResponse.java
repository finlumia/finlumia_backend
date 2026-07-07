package br.com.finlumia.configurator.views;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TestFunctionResponse(
        Object result,
        @JsonProperty("execution_ms") long executionMs) {
}
