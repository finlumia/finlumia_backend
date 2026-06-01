package br.com.finlumia.docs.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docs.modules")
public class DocsModuleProperties {

    private Map<String, String> baseUrl = new HashMap<>();

    public Map<String, String> getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(Map<String, String> baseUrl) {
        this.baseUrl = baseUrl;
    }
}
