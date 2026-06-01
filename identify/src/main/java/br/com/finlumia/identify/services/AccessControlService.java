package br.com.finlumia.identify.services;

import java.util.UUID;

import br.com.finlumia.identify.models.AccessGrant;
import br.com.finlumia.identify.models.AccessOperation;
import br.com.finlumia.identify.repositorys.AccessControlRepository;
import br.com.finlumia.identify.views.AccessControlCheckResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.stereotype.Service;

@Service
public class AccessControlService {

    private final AccessControlRepository accessControlRepository;

    public AccessControlService(AccessControlRepository accessControlRepository) {
        this.accessControlRepository = accessControlRepository;
    }

    public AccessControlCheckResponse authorize(UUID userKey, String resourceName, AccessOperation operation) {
        if (!accessControlRepository.isActiveUser(userKey)) {
            throw new FinlumiaException(404, "Usuario nao encontrado", "Usuario nao encontrado ou inativo.");
        }

        String normalizedResource = normalizeResourceName(resourceName);
        if (!accessControlRepository.resourceExists(normalizedResource)) {
            throw new FinlumiaException(404, "Recurso nao encontrado", "Recurso nao cadastrado: " + normalizedResource);
        }

        AccessGrant grant = accessControlRepository.findGrant(userKey, normalizedResource, operation)
                .orElseThrow(() -> new FinlumiaException(
                        403,
                        "Acesso negado",
                        "Usuario nao possui permissao de "
                                + operation.name().toLowerCase()
                                + " no recurso "
                                + normalizedResource));

        return new AccessControlCheckResponse(
                userKey,
                normalizedResource,
                operation,
                grant.resourceKey(),
                grant.roles());
    }

    private String normalizeResourceName(String resourceName) {
        return resourceName == null ? "" : resourceName.trim().toLowerCase();
    }
}
