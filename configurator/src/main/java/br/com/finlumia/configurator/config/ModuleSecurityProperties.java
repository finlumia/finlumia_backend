package br.com.finlumia.configurator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finlumia.security.module-api")
public class ModuleSecurityProperties {

    private String docsOrigin = "http://localhost:28082";

    public String getDocsOrigin() {
        return docsOrigin;
    }

    public void setDocsOrigin(String docsOrigin) {
        this.docsOrigin = docsOrigin;
    }
}
