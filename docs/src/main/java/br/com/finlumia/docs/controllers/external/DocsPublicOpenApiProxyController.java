package br.com.finlumia.docs.controllers.external;

import br.com.finlumia.docs.config.DocsModuleProperties;
import br.com.finlumia.docs.config.DocsOpenApiPaths;
import br.com.finlumia.docs.services.DocsOpenApiProxyService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(DocsOpenApiPaths.PUBLIC_API_DOCS_PREFIX)
public class DocsPublicOpenApiProxyController {

    private final DocsOpenApiProxyService openApiProxyService;

    public DocsPublicOpenApiProxyController(DocsOpenApiProxyService openApiProxyService) {
        this.openApiProxyService = openApiProxyService;
    }

    @GetMapping(value = "/{moduleName}/external", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> proxyExternalOpenApi(@PathVariable String moduleName) {
        return openApiProxyService.fetchExternalOpenApi(moduleName);
    }
}
