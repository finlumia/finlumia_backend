package br.com.finlumia.identify.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finlumia.identify.security.public-api")
public class PublicApiProperties {

    private List<String> paths = new ArrayList<>(List.of(
            "/api/identify/token",
            "/api/identify/token/**"));

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public String[] getPathsArray() {
        return paths.toArray(String[]::new);
    }
}
