package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class CreateTableRequest {
        @NotBlank(message = "Schema é obrigatório")
        @Size(min = 1, max = 63, message = "Schema deve ter entre 1 e 63 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Schema deve conter apenas letras, números e underline")
        @JsonProperty("schema")
        private String schema;

        @NotBlank(message = "Name é obrigatório")
        @Size(min = 1, max = 63, message = "Name deve ter entre 1 e 63 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Name deve conter apenas letras, números e underline")
        @JsonProperty("name")
        private String name;

        @NotBlank(message = "DisplayName é obrigatório")
        @Size(min = 1, max = 100, message = "DisplayName deve ter entre 1 e 100 caracteres")
        @Pattern(regexp = "^[\\p{L}0-9 _-]+$", message = "DisplayName contém caracteres inválidos")
        @JsonProperty("displayName")
        private String displayName;

        @Size(max = 255, message = "DisplayDescription deve ter no máximo 255 caracteres")
        @Pattern(regexp = "^[\\p{L}0-9 .,!?_-]*$", message = "DisplayDescription contém caracteres inválidos")
        @JsonProperty("displayDescription")
        private String displayDescription;
}


