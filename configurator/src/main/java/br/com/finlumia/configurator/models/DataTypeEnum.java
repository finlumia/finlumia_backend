package br.com.finlumia.configurator.models;

public enum DataTypeEnum {
    uuid, varchar, integer, bigint, bool, timestamp, decimal, text, jsonb, serial;

    // "boolean" is a Java keyword — the JSON value is mapped via @JsonProperty on deserialization
}
