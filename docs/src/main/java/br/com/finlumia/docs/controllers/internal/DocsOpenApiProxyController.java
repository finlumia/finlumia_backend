package br.com.finlumia.docs.controllers.internal;

import br.com.finlumia.docs.config.DocsOpenApiPaths;
import br.com.finlumia.docs.services.DocsOpenApiProxyService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(DocsOpenApiPaths.ADMIN_API_DOCS_PREFIX)
public class DocsOpenApiProxyController {

    private final DocsOpenApiProxyService openApiProxyService;

    public DocsOpenApiProxyController(DocsOpenApiProxyService openApiProxyService) {
        this.openApiProxyService = openApiProxyService;
    }

    @GetMapping(value = "/{moduleName}/internal", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> proxyInternalOpenApi(@PathVariable String moduleName) {
        return openApiProxyService.fetchInternalOpenApi(moduleName);
    }
}
