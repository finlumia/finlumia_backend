package br.com.finlumia.movement.views;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CategoryView(
        String id,
        String label,
        String color,
        @JsonProperty("bgColor") String bgColor,
        String icon
) {}
