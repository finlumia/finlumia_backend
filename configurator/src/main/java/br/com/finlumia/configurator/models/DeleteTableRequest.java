package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DeleteTableRequest {
    
    @JsonProperty("keyTable")
    private Long keyTable;

}
