package br.com.finlumia.document.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finlumia.security.module-api")
public class ModuleSecurityProperties {

    private String docsOrigin = "http://localhost:28082";
    private List<String> allowedOrigins = List.of("http://localhost:3000");

    public String getDocsOrigin() {
        return docsOrigin;
    }

    public void setDocsOrigin(String docsOrigin) {
        this.docsOrigin = docsOrigin;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
