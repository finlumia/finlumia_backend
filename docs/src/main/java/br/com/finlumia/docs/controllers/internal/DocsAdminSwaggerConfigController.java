package br.com.finlumia.docs.controllers.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.finlumia.docs.config.DocsModuleProperties;
import br.com.finlumia.docs.config.DocsOpenApiPaths;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/docs/admin")
public class DocsAdminSwaggerConfigController {

    private final DocsModuleProperties docsModuleProperties;

    public DocsAdminSwaggerConfigController(DocsModuleProperties docsModuleProperties) {
        this.docsModuleProperties = docsModuleProperties;
    }

    @GetMapping(value = "/swagger-config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> adminSwaggerConfig() {
        List<Map<String, String>> urls = new ArrayList<>();

        docsModuleProperties.getBaseUrl().keySet().stream().sorted().forEach(moduleName -> {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("name", moduleName + " (internal)");
            entry.put("url", DocsOpenApiPaths.ADMIN_API_DOCS_PREFIX + "/" + moduleName + "/internal");
            urls.add(entry);
        });

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("urls", urls);
        config.put("urls.primaryName", urls.isEmpty() ? null : urls.get(0).get("name"));
        return config;
    }
}
