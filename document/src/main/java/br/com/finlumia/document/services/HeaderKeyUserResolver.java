package br.com.finlumia.document.services;

import java.util.Optional;

import br.com.finlumia.document.config.KeyUserResolutionProperties;
import org.springframework.stereotype.Component;

@Component
public class HeaderKeyUserResolver {

    private final KeyUserResolutionProperties properties;

    public HeaderKeyUserResolver(KeyUserResolutionProperties properties) {
        this.properties = properties;
    }

    public Optional<Long> resolve(String keyUserHeader) {
        if (keyUserHeader == null || keyUserHeader.isBlank()) {
            return Optional.empty();
        }

        String normalized = keyUserHeader.trim();
        Long mapped = properties.getKeyUserMap().get(normalized);
        if (mapped != null && mapped > 0) {
            return Optional.of(mapped);
        }

        try {
            long parsed = Long.parseLong(normalized);
            if (parsed > 0) {
                return Optional.of(parsed);
            }
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }

        return Optional.empty();
    }
}
