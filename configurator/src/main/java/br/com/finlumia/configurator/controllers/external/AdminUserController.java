package br.com.finlumia.configurator.controllers.external;

import br.com.finlumia.configurator.controllers.ExternalApi;
import br.com.finlumia.configurator.models.CreateAdminUserRequest;
import br.com.finlumia.configurator.models.RoleEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.ToggleUserStatusRequest;
import br.com.finlumia.configurator.models.UpdateAdminUserRequest;
import br.com.finlumia.configurator.services.AdminUserService;
import br.com.finlumia.configurator.views.AdminResetPasswordResponse;
import br.com.finlumia.configurator.views.AdminUserResponse;
import br.com.finlumia.configurator.views.PagedResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/v1/config/users")
@Tag(name = "Admin Users", description = "Gerenciamento de usuarios administrativos")
@SecurityRequirement(name = BEARER_AUTH)
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "Listar usuarios")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedResponse<AdminUserResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StatusEnum status,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) RoleEnum role) {
        return ResponseEntity.ok(adminUserService.list(page, pageSize, search, status, sortBy, sortOrder, role));
    }

    @Operation(summary = "Obter usuario por ID")
    @ApiResponse(responseCode = "200", description = "Usuario retornado",
            content = @Content(schema = @Schema(implementation = AdminUserResponse.class)))
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminUserResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.getById(id));
    }

    @Operation(summary = "Criar usuario")
    @ApiResponse(responseCode = "201", description = "Usuario criado",
            content = @Content(schema = @Schema(implementation = AdminUserResponse.class)))
    @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody CreateAdminUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.create(request));
    }

    @Operation(summary = "Atualizar usuario")
    @ApiResponse(responseCode = "200", description = "Usuario atualizado",
            content = @Content(schema = @Schema(implementation = AdminUserResponse.class)))
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminUserResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateAdminUserRequest request) {
        return ResponseEntity.ok(adminUserService.update(id, request));
    }

    @Operation(summary = "Excluir usuario")
    @ApiResponse(responseCode = "204", description = "Usuario excluido")
    @ApiResponse(responseCode = "403", description = "Nao e possivel excluir o proprio usuario")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ativar ou desativar usuario", description = "Ativa ou desativa um usuario sem exclui-lo.")
    @ApiResponse(responseCode = "200", description = "Status atualizado",
            content = @Content(schema = @Schema(implementation = AdminUserResponse.class)))
    @PatchMapping(path = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminUserResponse> toggleStatus(@PathVariable UUID id,
                                                          @Valid @RequestBody ToggleUserStatusRequest request) {
        return ResponseEntity.ok(adminUserService.toggleStatus(id, request));
    }

    @Operation(summary = "Resetar senha (admin)", description = "Admin forca redefnicao de senha e envia link para o e-mail do usuario.")
    @ApiResponse(responseCode = "200", description = "Link de redefinicao enviado",
            content = @Content(schema = @Schema(implementation = AdminResetPasswordResponse.class)))
    @PostMapping(path = "/{id}/reset-password", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminResetPasswordResponse> adminResetPassword(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.adminResetPassword(id));
    }
}
