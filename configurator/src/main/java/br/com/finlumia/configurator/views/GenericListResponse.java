package br.com.finlumia.configurator.views;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GenericListResponse {

    @JsonProperty("tableSlug")
    private String tableSlug;

    @JsonProperty("linhas")
    private List<Linha> linhas;

    @JsonProperty("totalElements")
    private long totalElements;

    @JsonProperty("page")
    private int page;

    @JsonProperty("pageSize")
    private int pageSize;

    @Data
    public class Linha {
        @JsonProperty("identifier")
        private Long identifier;

        @JsonProperty("column")
        private List<Column> column;
    }

    @Data
    public class Column {
        @JsonProperty("fieldName")
        private String fieldName;

        @JsonProperty("fieldValue")
        private String fieldValue;
    }



}
