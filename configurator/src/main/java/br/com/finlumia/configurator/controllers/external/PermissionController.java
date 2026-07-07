package br.com.finlumia.configurator.controllers.external;

import br.com.finlumia.configurator.controllers.ExternalApi;
import br.com.finlumia.configurator.models.BatchUpdatePermissionsRequest;
import br.com.finlumia.configurator.models.CreatePermissionRequest;
import br.com.finlumia.configurator.models.RoleEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.UpdatePermissionRequest;
import br.com.finlumia.configurator.services.PermissionService;
import br.com.finlumia.configurator.views.BatchUpdateResponse;
import br.com.finlumia.configurator.views.PagedResponse;
import br.com.finlumia.configurator.views.PermissionMatrixResponse;
import br.com.finlumia.configurator.views.PermissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static br.com.finlumia.configurator.config.ConfiguratorOpenApiConfig.BEARER_AUTH;

@ExternalApi
@RestController
@RequestMapping("/v1/config/permissions")
@Tag(name = "Permissions", description = "Gerenciamento de permissoes por modulo e funcao")
@SecurityRequirement(name = BEARER_AUTH)
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Operation(summary = "Listar permissoes")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedResponse<PermissionResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StatusEnum status,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) RoleEnum role) {
        return ResponseEntity.ok(permissionService.list(page, pageSize, search, status, sortBy, sortOrder, module, role));
    }

    @Operation(summary = "Matriz de permissoes", description = "Retorna a matriz completa agrupada por modulo e funcao.")
    @ApiResponse(responseCode = "200", description = "Matriz retornada",
            content = @Content(schema = @Schema(implementation = PermissionMatrixResponse.class)))
    @GetMapping(path = "/matrix", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PermissionMatrixResponse> getMatrix() {
        return ResponseEntity.ok(permissionService.getMatrix());
    }

    @Operation(summary = "Criar permissao")
    @ApiResponse(responseCode = "201", description = "Permissao criada",
            content = @Content(schema = @Schema(implementation = PermissionResponse.class)))
    @ApiResponse(responseCode = "409", description = "Permissao ja existe para este modulo/role")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PermissionResponse> create(@Valid @RequestBody CreatePermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.create(request));
    }

    @Operation(summary = "Atualizar permissao")
    @ApiResponse(responseCode = "200", description = "Permissao atualizada",
            content = @Content(schema = @Schema(implementation = PermissionResponse.class)))
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PermissionResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdatePermissionRequest request) {
        return ResponseEntity.ok(permissionService.update(id, request));
    }

    @Operation(summary = "Atualizar multiplas permissoes", description = "Atualiza multiplas permissoes de uma vez.")
    @ApiResponse(responseCode = "200", description = "Permissoes atualizadas",
            content = @Content(schema = @Schema(implementation = BatchUpdateResponse.class)))
    @PostMapping(path = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchUpdateResponse> batchUpdate(@Valid @RequestBody BatchUpdatePermissionsRequest request) {
        return ResponseEntity.ok(permissionService.batchUpdate(request));
    }

    @Operation(summary = "Excluir permissao")
    @ApiResponse(responseCode = "204", description = "Permissao excluida")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
