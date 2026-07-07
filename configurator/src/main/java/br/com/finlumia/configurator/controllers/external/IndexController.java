package br.com.finlumia.configurator.controllers.external;

import br.com.finlumia.configurator.controllers.ExternalApi;
import br.com.finlumia.configurator.models.CreateIndexRequest;
import br.com.finlumia.configurator.models.IndexTypeEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.UpdateIndexRequest;
import br.com.finlumia.configurator.services.IndexService;
import br.com.finlumia.configurator.views.DbIndexResponse;
import br.com.finlumia.configurator.views.IndexStatsResponse;
import br.com.finlumia.configurator.views.PagedResponse;
import br.com.finlumia.configurator.views.RebuildIndexResponse;
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
@RequestMapping("/v1/config/indexes")
@Tag(name = "Indexes", description = "Gerenciamento de indices do banco de dados")
@SecurityRequirement(name = BEARER_AUTH)
public class IndexController {

    private final IndexService indexService;

    public IndexController(IndexService indexService) {
        this.indexService = indexService;
    }

    @Operation(summary = "Listar indices")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedResponse<DbIndexResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StatusEnum status,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "asc") String sortOrder,
            @RequestParam(name = "table_id", required = false) UUID tableId,
            @RequestParam(required = false) IndexTypeEnum type) {
        return ResponseEntity.ok(indexService.list(page, pageSize, search, status, sortBy, sortOrder, tableId, type));
    }

    @Operation(summary = "Obter indice por ID")
    @ApiResponse(responseCode = "200", description = "Indice retornado",
            content = @Content(schema = @Schema(implementation = DbIndexResponse.class)))
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DbIndexResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(indexService.getById(id));
    }

    @Operation(summary = "Criar indice")
    @ApiResponse(responseCode = "201", description = "Indice criado",
            content = @Content(schema = @Schema(implementation = DbIndexResponse.class)))
    @ApiResponse(responseCode = "409", description = "Indice com este nome ja existe")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DbIndexResponse> create(@Valid @RequestBody CreateIndexRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(indexService.create(request));
    }

    @Operation(summary = "Atualizar indice")
    @ApiResponse(responseCode = "200", description = "Indice atualizado",
            content = @Content(schema = @Schema(implementation = DbIndexResponse.class)))
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DbIndexResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateIndexRequest request) {
        return ResponseEntity.ok(indexService.update(id, request));
    }

    @Operation(summary = "Excluir indice")
    @ApiResponse(responseCode = "204", description = "Indice excluido")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        indexService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reconstruir indice", description = "Reconstroi o indice (REINDEX CONCURRENTLY) — resposta assincrona.")
    @ApiResponse(responseCode = "202", description = "Reconstrucao iniciada",
            content = @Content(schema = @Schema(implementation = RebuildIndexResponse.class)))
    @PostMapping(path = "/{id}/rebuild", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RebuildIndexResponse> rebuild(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(indexService.rebuild(id));
    }

    @Operation(summary = "Estatisticas do indice", description = "Retorna estatisticas de uso do indice.")
    @ApiResponse(responseCode = "200", description = "Estatisticas retornadas",
            content = @Content(schema = @Schema(implementation = IndexStatsResponse.class)))
    @GetMapping(path = "/{id}/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IndexStatsResponse> getStats(@PathVariable UUID id) {
        return ResponseEntity.ok(indexService.getStats(id));
    }
}
