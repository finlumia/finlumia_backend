package br.com.finlumia.configurator.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finlumia.configurator.security.public-api")
public class PublicApiProperties {
    private List<String> paths = new ArrayList<>();
    public List<String> getPaths() { return paths; }
    public void setPaths(List<String> paths) { this.paths = paths; }
    public String[] getPathsArray() { return paths.toArray(String[]::new); }
}
