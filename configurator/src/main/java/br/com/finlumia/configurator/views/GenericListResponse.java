package br.com.finlumia.configurator.views;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericListResponse {

    @JsonProperty("tableKey")
    private long tableKey;

    @JsonProperty("tableDisplayName")
    private String tableDisplayName;

    @JsonProperty("columns")
    private List<GenericListColumnView> columns;

    /**
     * Botões por linha, lidos de {@code tab_description} em JSON (chave {@code rowActions}) quando presente.
     */
    @JsonProperty("rowActions")
    private List<GenericListRowActionView> rowActions;

    @JsonProperty("rows")
    private List<Map<String, Object>> rows;

    @JsonProperty("totalElements")
    private long totalElements;

    @JsonProperty("page")
    private int page;

    @JsonProperty("pageSize")
    private int pageSize;
}
