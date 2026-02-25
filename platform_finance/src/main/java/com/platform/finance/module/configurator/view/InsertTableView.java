package com.platform.finance.module.configurator.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.finance.module.configurator.model.InsertTableModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class InsertTableView {
    @Schema(description = "Código da operação")
    @JsonProperty(value = "cod", required = true)
    private Integer cod;

    @Schema(description = "Lista de objetos da tabela")
    @JsonProperty(value = "finluConfiModelRequestInsertTableList", required = true)
    private List<InsertTableModel> finluConfiModelRequestInsertTableList;

}
