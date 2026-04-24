package br.com.finlumia.shared.security;

import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.finlumia.shared.config.KeyUserResolutionProperties;

@Component
public class HeaderKeyUserResolver {

    private final KeyUserResolutionProperties properties;

    public HeaderKeyUserResolver(KeyUserResolutionProperties properties) {
        this.properties = properties;
    }

    public Optional<Long> resolve(String headerValue) {
        if (headerValue == null) {
            return Optional.empty();
        }

        String normalized = headerValue.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        Long mapped = properties.getKeyUserMap().get(normalized);
        if (mapped != null && mapped > 0) {
            return Optional.of(mapped);
        }

        try {
            Long numeric = Long.valueOf(normalized);
            return numeric > 0 ? Optional.of(numeric) : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
