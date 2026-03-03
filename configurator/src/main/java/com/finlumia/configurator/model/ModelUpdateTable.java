package com.finlumia.configurator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModelUpdateTable {
    @JsonProperty(value="KeySystemTable",  required = true)
    @NotNull(message = "Chave da tabela System_Table é obrigatório")
    private String KeySystemTable;

    @JsonProperty(value="DescriptionTable",  required = true)
    @NotNull(message = "Descrição da tabela é obrigatório")
    private String DescriptionTable;
}
