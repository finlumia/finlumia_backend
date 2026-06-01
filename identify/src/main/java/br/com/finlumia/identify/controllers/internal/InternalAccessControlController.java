package br.com.finlumia.identify.controllers.internal;

import br.com.finlumia.identify.controllers.InternalApi;
import br.com.finlumia.identify.models.AccessControlCheckRequest;
import br.com.finlumia.identify.services.AccessControlService;
import br.com.finlumia.identify.views.AccessControlCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@InternalApi
@RestController
@RequestMapping("/internal/identify/access-control")
@Tag(name = "Access Control (internal)", description = "Autorizacao service-to-service por recurso e operacao CRUD")
public class InternalAccessControlController {

    private final AccessControlService accessControlService;

    public InternalAccessControlController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @Operation(
            summary = "Verificar permissao",
            description = "Valida se o usuario possui permissao para a operacao no recurso informado.")
    @ApiResponse(
            responseCode = "200",
            description = "Acesso permitido",
            content = @Content(schema = @Schema(implementation = AccessControlCheckResponse.class)))
    @ApiResponse(responseCode = "403", description = "Acesso negado")
    @ApiResponse(responseCode = "404", description = "Usuario ou recurso nao encontrado")
    @PostMapping(
            path = "/check",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccessControlCheckResponse> check(@Valid @RequestBody AccessControlCheckRequest request) {
        AccessControlCheckResponse response = accessControlService.authorize(
                request.userKey(),
                request.resourceName(),
                request.operation());
        return ResponseEntity.ok(response);
    }
}
