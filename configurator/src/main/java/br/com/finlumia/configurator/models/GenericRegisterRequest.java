package br.com.finlumia.configurator.models;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenericRegisterRequest {
    
    @JsonProperty("slugTable")
    @NotBlank(message = "slugTable is required")
    @Size(max = 100, message = "slugTable must have at most 63 characters")
    @Schema(
            description = "Slug da tabela",
            example = "table_example",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String slugTable;

    
    @JsonProperty("fields")
    @NotEmpty(message = "fields is required")
    @Schema(
            description = "Campos da tabela",
            example = "1:value1,2:value2,3:value3",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<Long, String> fields;
    
    
}
