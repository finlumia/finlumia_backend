package br.com.finlumia.configurator.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;


@Data
public class CreateTableRequest {
 
    @JsonProperty("schemaName")
    private String schemaName;

    @JsonProperty("tableName")
    private String tableName;

    @JsonProperty("displayName")
    private String displayName;
    
    @JsonProperty("description")
    private String description;
    
}
