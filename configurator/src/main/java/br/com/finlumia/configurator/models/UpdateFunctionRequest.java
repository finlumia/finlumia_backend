package br.com.finlumia.configurator.models;

public record UpdateFunctionRequest(
        String name,
        SchemaEnum schema,
        LanguageEnum language,
        String returnType,
        String args,
        VolatilityEnum volatility,
        String body,
        String description,
        StatusEnum status) {
}
