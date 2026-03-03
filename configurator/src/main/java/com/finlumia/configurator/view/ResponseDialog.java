package com.finlumia.configurator.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class ResponseDialog {

    @JsonProperty("code")
    public int code;

    @JsonProperty("title")
    public String title;

    @JsonProperty("mensage")
    public String mensage;
}
