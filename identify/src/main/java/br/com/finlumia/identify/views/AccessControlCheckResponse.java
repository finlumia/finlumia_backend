package br.com.finlumia.identify.views;

import java.util.List;
import java.util.UUID;

import br.com.finlumia.identify.models.AccessOperation;

public record AccessControlCheckResponse(
        UUID userKey,
        String resourceName,
        AccessOperation operation,
        UUID resourceKey,
        List<String> roles) {
}
