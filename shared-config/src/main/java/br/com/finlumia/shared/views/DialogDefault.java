package br.com.finlumia.shared.views;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DialogDefault {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("title")
    private String title;

    @JsonProperty("mensage")
    private String mensage;
}
