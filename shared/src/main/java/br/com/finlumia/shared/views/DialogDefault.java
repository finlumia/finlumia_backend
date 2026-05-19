package br.com.finlumia.shared.views;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class DialogDefault {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("title")
    private String title;

    @JsonProperty("mensage")
    private String mensage;
}
