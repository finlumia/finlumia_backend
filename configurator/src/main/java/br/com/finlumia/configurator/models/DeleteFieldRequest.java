package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeleteFieldRequest {

    @NotNull(message = "Key é obrigatório")
    @JsonProperty("key")
    private Long key;
}
