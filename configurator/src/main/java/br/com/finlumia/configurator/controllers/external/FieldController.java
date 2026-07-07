package br.com.finlumia.configurator.controllers.external;

import br.com.finlumia.configurator.controllers.ExternalApi;
import br.com.finlumia.configurator.models.CreateFieldRequest;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.UpdateFieldRequest;
import br.com.finlumia.configurator.services.FieldService;
import br.com.finlumia.configurator.views.FieldResponse;
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
@RequestMapping("/v1/config/fields")
@Tag(name = "Fields", description = "Gerenciamento de campos de tabelas")
@SecurityRequirement(name = BEARER_AUTH)
public class FieldController {

    private final FieldService fieldService;

    public FieldController(FieldService fieldService) {
        this.fieldService = fieldService;
    }

    @Operation(summary = "Listar campos")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedResponse<FieldResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StatusEnum status,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "asc") String sortOrder,
            @RequestParam(name = "table_id", required = false) UUID tableId) {
        return ResponseEntity.ok(fieldService.list(page, pageSize, search, status, sortBy, sortOrder, tableId));
    }

    @Operation(summary = "Obter campo por ID")
    @ApiResponse(responseCode = "200", description = "Campo retornado",
            content = @Content(schema = @Schema(implementation = FieldResponse.class)))
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FieldResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(fieldService.getById(id));
    }

    @Operation(summary = "Criar campo")
    @ApiResponse(responseCode = "201", description = "Campo criado",
            content = @Content(schema = @Schema(implementation = FieldResponse.class)))
    @ApiResponse(responseCode = "409", description = "Campo ja existe na tabela")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FieldResponse> create(@Valid @RequestBody CreateFieldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fieldService.create(request));
    }

    @Operation(summary = "Atualizar campo")
    @ApiResponse(responseCode = "200", description = "Campo atualizado",
            content = @Content(schema = @Schema(implementation = FieldResponse.class)))
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FieldResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateFieldRequest request) {
        return ResponseEntity.ok(fieldService.update(id, request));
    }

    @Operation(summary = "Excluir campo")
    @ApiResponse(responseCode = "204", description = "Campo excluido")
    @ApiResponse(responseCode = "409", description = "Campo referenciado por indice ou FK ativo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fieldService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
