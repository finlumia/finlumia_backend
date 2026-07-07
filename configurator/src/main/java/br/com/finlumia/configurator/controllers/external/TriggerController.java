package br.com.finlumia.configurator.controllers.external;

import br.com.finlumia.configurator.controllers.ExternalApi;
import br.com.finlumia.configurator.models.CreateTriggerRequest;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.ToggleTriggerRequest;
import br.com.finlumia.configurator.models.TriggerEventEnum;
import br.com.finlumia.configurator.models.UpdateTriggerRequest;
import br.com.finlumia.configurator.services.TriggerService;
import br.com.finlumia.configurator.views.DbTriggerResponse;
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
@RequestMapping("/v1/config/triggers")
@Tag(name = "Triggers", description = "Gerenciamento de triggers do banco de dados")
@SecurityRequirement(name = BEARER_AUTH)
public class TriggerController {

    private final TriggerService triggerService;

    public TriggerController(TriggerService triggerService) {
        this.triggerService = triggerService;
    }

    @Operation(summary = "Listar triggers")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedResponse<DbTriggerResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StatusEnum status,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "asc") String sortOrder,
            @RequestParam(name = "table_id", required = false) UUID tableId,
            @RequestParam(required = false) TriggerEventEnum event,
            @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(triggerService.list(page, pageSize, search, status, sortBy, sortOrder, tableId, event, enabled));
    }

    @Operation(summary = "Obter trigger por ID")
    @ApiResponse(responseCode = "200", description = "Trigger retornado",
            content = @Content(schema = @Schema(implementation = DbTriggerResponse.class)))
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DbTriggerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(triggerService.getById(id));
    }

    @Operation(summary = "Criar trigger")
    @ApiResponse(responseCode = "201", description = "Trigger criado",
            content = @Content(schema = @Schema(implementation = DbTriggerResponse.class)))
    @ApiResponse(responseCode = "409", description = "Trigger com este nome ja existe na tabela")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DbTriggerResponse> create(@Valid @RequestBody CreateTriggerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(triggerService.create(request));
    }

    @Operation(summary = "Atualizar trigger")
    @ApiResponse(responseCode = "200", description = "Trigger atualizado",
            content = @Content(schema = @Schema(implementation = DbTriggerResponse.class)))
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DbTriggerResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateTriggerRequest request) {
        return ResponseEntity.ok(triggerService.update(id, request));
    }

    @Operation(summary = "Habilitar ou desabilitar trigger", description = "Habilita ou desabilita o trigger sem excluir.")
    @ApiResponse(responseCode = "200", description = "Trigger atualizado",
            content = @Content(schema = @Schema(implementation = DbTriggerResponse.class)))
    @PatchMapping(path = "/{id}/toggle", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DbTriggerResponse> toggle(@PathVariable UUID id,
                                                    @Valid @RequestBody ToggleTriggerRequest request) {
        return ResponseEntity.ok(triggerService.toggle(id, request));
    }

    @Operation(summary = "Excluir trigger")
    @ApiResponse(responseCode = "204", description = "Trigger excluido")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        triggerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
