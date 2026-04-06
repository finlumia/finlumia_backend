package br.com.finlumia.configurator.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguratorFieldRow {

    private String fieldName;
    private String dataType;
    private Integer fieldLength;
    private Integer fieldPrecision;
    private Integer fieldScale;
    private boolean required;
    private boolean primaryKey;
    private boolean foreignKey;
    private String fkReferenceTable;
    private String fkReferenceColumn;
    private String defaultValue;
    private boolean unique;
    private boolean indexed;
}
