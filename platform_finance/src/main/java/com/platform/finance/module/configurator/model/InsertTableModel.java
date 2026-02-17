package com.platform.finance.module.configurator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class InsertTableModel {
    @Schema(description = "Código da tabela")
    @JsonProperty(value = "tableCode", required = true)
    private String tableCode;

    @Schema(description = "Nome da tabela")
    @JsonProperty(value = "tableName", required = true)
    private String tableName;

    @Schema(description = "Descrição da tabela")
    @JsonProperty(value = "tableDescription", required = true)
    private String tableDescription;

    @Schema(description = "Módulo da tabela")
    @JsonProperty(value = "tableModule", required = true)
    private String tableModule;

}


