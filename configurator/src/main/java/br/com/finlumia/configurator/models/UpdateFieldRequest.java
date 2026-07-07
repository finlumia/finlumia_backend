package br.com.finlumia.configurator.models;

import java.util.UUID;

public record UpdateFieldRequest(
        String name,
        UUID tableId,
        DataTypeEnum dataType,
        Integer length,
        Boolean nullable,
        String defaultValue,
        Boolean isPrimary,
        Boolean isForeign,
        String referencesTable,
        String referencesField,
        StatusEnum status) {
}
