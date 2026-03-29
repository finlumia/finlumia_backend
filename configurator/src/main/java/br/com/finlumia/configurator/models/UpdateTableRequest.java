package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTableRequest {
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
