package br.com.finlumia.docs.support.config;

public final class ApiPaths {

    public static final String EXTERNAL_API_PREFIX = "/api";
    public static final String INTERNAL_API_PREFIX = "/internal";
    public static final String INTERNAL_DOCS_PREFIX = "/internal/docs";
    public static final String INTERNAL_DOCS_API_DOCS = INTERNAL_DOCS_PREFIX + "/api-docs";
    public static final String INTERNAL_DOCS_EXTERNAL = INTERNAL_DOCS_API_DOCS + "/external";
    public static final String INTERNAL_DOCS_INTERNAL = INTERNAL_DOCS_API_DOCS + "/internal";

    public static final String SUPPORT_PREFIX = "/api/v1/support";
    public static final String TICKETS_PREFIX = SUPPORT_PREFIX + "/tickets";

    private ApiPaths() {
    }
}
