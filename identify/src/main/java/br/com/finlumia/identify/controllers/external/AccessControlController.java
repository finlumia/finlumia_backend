package br.com.finlumia.identify.controllers.external;

import br.com.finlumia.identify.controllers.ExternalApi;
import br.com.finlumia.identify.models.AccessControlSelfCheckRequest;
import br.com.finlumia.identify.services.AccessControlService;
import br.com.finlumia.identify.services.JwtAuthentication;
import br.com.finlumia.identify.views.AccessControlCheckResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static br.com.finlumia.identify.config.IdentifyOpenApiConfig.BEARER_AUTH;

@ExternalApi
@RestController
@RequestMapping("/api/identify/access-control")
@Tag(name = "Access Control", description = "Verificacao de permissao do usuario autenticado")
@SecurityRequirement(name = BEARER_AUTH)
public class AccessControlController {

    private final AccessControlService accessControlService;

    public AccessControlController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Operation(
            summary = "Verificar permissao do usuario autenticado",
            description = "Usa o userKey do JWT para validar a operacao no recurso informado.")
    @ApiResponse(
            responseCode = "200",
            description = "Acesso permitido",
            content = @Content(schema = @Schema(implementation = AccessControlCheckResponse.class)))
    @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
    @ApiResponse(responseCode = "403", description = "Acesso negado")
    @ApiResponse(responseCode = "404", description = "Recurso nao encontrado")
    @PostMapping(
            path = "/check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccessControlCheckResponse> check(@Valid @RequestBody AccessControlSelfCheckRequest request) {
        JwtAuthentication authentication = currentAuthentication();
        AccessControlCheckResponse response = accessControlService.authorize(
                authentication.getUserKey(),
                request.resourceName(),
                request.operation());
        return ResponseEntity.ok(response);
    }

    private JwtAuthentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthentication jwtAuthentication) {
            return jwtAuthentication;
        }

        throw new FinlumiaException(401, "Nao autenticado", "Bearer token e obrigatorio.");
    }
}
