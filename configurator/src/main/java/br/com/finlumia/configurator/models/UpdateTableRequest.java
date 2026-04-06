package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTableRequest {

    @NotNull(message = "Key é obrigatório")
    @JsonProperty("key")
    private Long key;

    @NotNull(message = "Lock é obrigatório")
    @JsonProperty("lock")
    private Boolean lock;

    @NotBlank(message = "SchemaName é obrigatório")
    @Size(min = 1, max = 63, message = "SchemaName deve ter entre 1 e 63 caracteres")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "SchemaName contém caracteres inválidos")
    @JsonProperty("schemaName")
    private String schemaName;

    @NotBlank(message = "Name é obrigatório")
    @Size(min = 1, max = 63, message = "Name deve ter entre 1 e 63 caracteres")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "Name contém caracteres inválidos")
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "DisplayName é obrigatório")
    @Size(min = 1, max = 255, message = "DisplayName deve ter entre 1 e 255 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9 _-]+$", message = "DisplayName contém caracteres inválidos")
    @JsonProperty("displayName")
    private String displayName;

    @Size(max = 255, message = "DisplayDescription deve ter no máximo 255 caracteres")
    @Pattern(regexp = "^[\\p{L}0-9 .,!?_-]*$", message = "DisplayDescription contém caracteres inválidos")
    @JsonProperty("displayDescription")
    private String displayDescription;

}
