package br.com.finlumia.identify.models;

import java.util.List;
import java.util.UUID;

public record AccessGrant(
        UUID resourceKey,
        String resourceName,
        List<String> roles) {
}
