package br.com.finlumia.configurator.views;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericListColumnView {

    @JsonProperty("fieldName")
    private String fieldName;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("dataType")
    private String dataType;

    /** Quando {@code false}, o campo não entra na montagem de filtros dinâmicos (JSON {@code listFilterable}). */
    @JsonProperty("filterable")
    private boolean filterable;

    /** Indica se a listagem já aplica {@code fie_sql_script_depara} no SQL. */
    @JsonProperty("usesDepara")
    private boolean usesDepara;

    @JsonProperty("sqlMask")
    private String sqlMask;

    @JsonProperty("validationRegex")
    private String validationRegex;
}
