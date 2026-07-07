package br.com.finlumia.configurator.controllers.external;

import br.com.finlumia.configurator.controllers.ExternalApi;
import br.com.finlumia.configurator.models.CreateTableRequest;
import br.com.finlumia.configurator.models.SchemaEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.UpdateTableRequest;
import br.com.finlumia.configurator.services.TableService;
import br.com.finlumia.configurator.views.PagedResponse;
import br.com.finlumia.configurator.views.TableResponse;
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
@RequestMapping("/v1/config/tables")
@Tag(name = "Tables", description = "Gerenciamento de tabelas do banco de dados")
@SecurityRequirement(name = BEARER_AUTH)
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @Operation(summary = "Listar tabelas", description = "Lista todas as tabelas com paginacao, filtro e busca.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedResponse<TableResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StatusEnum status,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) SchemaEnum schema) {
        return ResponseEntity.ok(tableService.list(page, pageSize, search, status, sortBy, sortOrder, schema));
    }

    @Operation(summary = "Obter tabela por ID")
    @ApiResponse(responseCode = "200", description = "Tabela retornada",
            content = @Content(schema = @Schema(implementation = TableResponse.class)))
    @ApiResponse(responseCode = "404", description = "Tabela nao encontrada")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TableResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(tableService.getById(id));
    }

    @Operation(summary = "Criar tabela")
    @ApiResponse(responseCode = "201", description = "Tabela criada",
            content = @Content(schema = @Schema(implementation = TableResponse.class)))
    @ApiResponse(responseCode = "409", description = "Tabela ja existe neste schema")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TableResponse> create(@Valid @RequestBody CreateTableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableService.create(request));
    }

    @Operation(summary = "Atualizar tabela")
    @ApiResponse(responseCode = "200", description = "Tabela atualizada",
            content = @Content(schema = @Schema(implementation = TableResponse.class)))
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TableResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateTableRequest request) {
        return ResponseEntity.ok(tableService.update(id, request));
    }

    @Operation(summary = "Excluir tabela")
    @ApiResponse(responseCode = "204", description = "Tabela excluida")
    @ApiResponse(responseCode = "409", description = "Tabela possui campos ou indices ativos")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
