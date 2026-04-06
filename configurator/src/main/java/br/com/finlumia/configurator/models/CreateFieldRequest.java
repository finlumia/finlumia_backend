package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFieldRequest {

    @NotNull(message = "TableKey é obrigatório")
    @JsonProperty("tableKey")
    private Long tableKey;

    @NotBlank(message = "FieldName é obrigatório")
    @Size(min = 1, max = 63, message = "FieldName deve ter entre 1 e 63 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "FieldName deve conter apenas letras, números e underline")
    @JsonProperty("fieldName")
    private String fieldName;

    @NotBlank(message = "DisplayName é obrigatório")
    @Size(min = 1, max = 255, message = "DisplayName deve ter entre 1 e 255 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9 _-]+$", message = "DisplayName contém caracteres inválidos")
    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("description")
    private String description;

    @NotBlank(message = "DataType é obrigatório")
    @Size(min = 1, max = 50, message = "DataType deve ter entre 1 e 50 caracteres")
    @JsonProperty("dataType")
    private String dataType;

    @JsonProperty("fieldLength")
    private Integer fieldLength;

    @JsonProperty("fieldPrecision")
    private Integer fieldPrecision;

    @JsonProperty("fieldScale")
    private Integer fieldScale;

    @NotNull(message = "IsRequired é obrigatório")
    @JsonProperty("isRequired")
    private Boolean isRequired;

    @NotNull(message = "IsPrimaryKey é obrigatório")
    @JsonProperty("isPrimaryKey")
    private Boolean isPrimaryKey;

    @NotNull(message = "IsForeignKey é obrigatório")
    @JsonProperty("isForeignKey")
    private Boolean isForeignKey;

    @Size(max = 128, message = "FkReferenceTable deve ter no máximo 128 caracteres")
    @JsonProperty("fkReferenceTable")
    private String fkReferenceTable;

    @Size(max = 63, message = "FkReferenceColumn deve ter no máximo 63 caracteres")
    @JsonProperty("fkReferenceColumn")
    private String fkReferenceColumn;

    @JsonProperty("defaultValue")
    private String defaultValue;

    @NotNull(message = "IsUnique é obrigatório")
    @JsonProperty("isUnique")
    private Boolean isUnique;

    @NotNull(message = "IsIndexed é obrigatório")
    @JsonProperty("isIndexed")
    private Boolean isIndexed;

    @NotNull(message = "DisplayOrder é obrigatório")
    @JsonProperty("displayOrder")
    private Integer displayOrder;

    @NotNull(message = "IsVisible é obrigatório")
    @JsonProperty("isVisible")
    private Boolean isVisible;

    @NotNull(message = "IsEditable é obrigatório")
    @JsonProperty("isEditable")
    private Boolean isEditable;

    @JsonProperty("sqlScriptDepara")
    private String sqlScriptDepara;

    @JsonProperty("sqlMask")
    private String sqlMask;

    @JsonProperty("validationRegex")
    private String validationRegex;

    /** JSON bruto para coluna JSONB (ex.: objeto JSON como string). */
    @JsonProperty("metadata")
    private String metadata;

    @JsonProperty("viewHabilit")
    private Boolean viewHabilit;
}
