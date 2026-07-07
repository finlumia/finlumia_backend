package br.com.finlumia.document.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finlumia.security")
public class KeyUserResolutionProperties {

    private Map<String, Long> keyUserMap = new HashMap<>();

    public Map<String, Long> getKeyUserMap() {
        return keyUserMap;
    }

    public void setKeyUserMap(Map<String, Long> keyUserMap) {
        this.keyUserMap = keyUserMap;
    }
}
