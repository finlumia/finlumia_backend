package com.finlumia.configurator.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ModelInsertTable {

    @JsonProperty(value="NameSchema",  required = true)
    @NotNull(message = "Nome do schema é obrigatório")
    private String NameSchema;

    @JsonProperty(value="NameTable",  required = true)
    @NotNull(message = "Nome da tabela é obrigatório")
    private String NameTable;

    @JsonProperty(value="DescriptionTable",  required = true)
    @NotNull(message = "Descrição da tabela é obrigatório")
    private String DescriptionTable;
}
