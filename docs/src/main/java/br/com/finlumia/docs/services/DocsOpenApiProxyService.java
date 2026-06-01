package br.com.finlumia.docs.services;

import br.com.finlumia.docs.config.DocsModuleProperties;
import br.com.finlumia.docs.config.DocsOpenApiPaths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class DocsOpenApiProxyService {

    private final DocsModuleProperties docsModuleProperties;
    private final RestClient restClient;
    private final String internalServiceHeader;
    private final String internalServiceToken;

    public DocsOpenApiProxyService(
            DocsModuleProperties docsModuleProperties,
            RestClient.Builder restClientBuilder,
            @Value("${finlumia.security.internal.header-name:X-Internal-Service-Token}") String internalServiceHeader,
            @Value("${finlumia.security.internal.service-token:}") String internalServiceToken) {
        this.docsModuleProperties = docsModuleProperties;
        this.internalServiceHeader = internalServiceHeader;
        this.internalServiceToken = internalServiceToken;
        this.restClient = restClientBuilder.build();
    }

    public ResponseEntity<String> fetchExternalOpenApi(String moduleName) {
        return fetchOpenApi(moduleName, DocsOpenApiPaths.MODULE_EXTERNAL_SUFFIX, false);
    }

    public ResponseEntity<String> fetchInternalOpenApi(String moduleName) {
        return fetchOpenApi(moduleName, DocsOpenApiPaths.MODULE_INTERNAL_SUFFIX, true);
    }

    private ResponseEntity<String> fetchOpenApi(String moduleName, String suffix, boolean useInternalToken) {
        String moduleBaseUrl = docsModuleProperties.getBaseUrl().get(moduleName);
        if (moduleBaseUrl == null || moduleBaseUrl.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri(moduleBaseUrl + suffix)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            if (useInternalToken && internalServiceToken != null && !internalServiceToken.isBlank()) {
                request = request.header(internalServiceHeader, internalServiceToken);
            }

            String openApiDocument = request.retrieve().body(String.class);
            return ResponseEntity.ok(openApiDocument);
        } catch (RestClientResponseException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getResponseBodyAsString());
        }
    }
}
