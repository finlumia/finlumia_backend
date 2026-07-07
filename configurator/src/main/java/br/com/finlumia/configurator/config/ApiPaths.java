package br.com.finlumia.configurator.config;

public final class ApiPaths {

    public static final String EXTERNAL_API_PREFIX = "/api";
    public static final String CONFIG_API_PREFIX = "/v1/config";
    public static final String INTERNAL_API_PREFIX = "/internal";
    public static final String INTERNAL_DOCS_PREFIX = "/internal/docs";
    public static final String INTERNAL_DOCS_API_DOCS = INTERNAL_DOCS_PREFIX + "/api-docs";
    public static final String INTERNAL_DOCS_EXTERNAL = INTERNAL_DOCS_API_DOCS + "/external";
    public static final String INTERNAL_DOCS_INTERNAL = INTERNAL_DOCS_API_DOCS + "/internal";

    private ApiPaths() {
    }
}
