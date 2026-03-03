package com.finlumia.configurator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ModelDeleteTable {
    @JsonProperty(value="KeySystemTable",  required = true)
    @NotNull(message = "Chave da tabela System Table é obrigatória.")
    private Long KeySystemTable;
}
