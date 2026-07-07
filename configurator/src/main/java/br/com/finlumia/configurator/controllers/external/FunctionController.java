package br.com.finlumia.configurator.controllers.external;

import br.com.finlumia.configurator.controllers.ExternalApi;
import br.com.finlumia.configurator.models.CreateFunctionRequest;
import br.com.finlumia.configurator.models.LanguageEnum;
import br.com.finlumia.configurator.models.SchemaEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.TestFunctionRequest;
import br.com.finlumia.configurator.models.UpdateFunctionRequest;
import br.com.finlumia.configurator.services.FunctionService;
import br.com.finlumia.configurator.views.DbFunctionResponse;
import br.com.finlumia.configurator.views.PagedResponse;
import br.com.finlumia.configurator.views.TestFunctionResponse;
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
@RequestMapping("/v1/config/functions")
@Tag(name = "Functions", description = "Gerenciamento de funcoes do banco de dados")
@SecurityRequirement(name = BEARER_AUTH)
public class FunctionController {

    private final FunctionService functionService;

    public FunctionController(FunctionService functionService) {
        this.functionService = functionService;
    }

    @Operation(summary = "Listar funcoes")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedResponse<DbFunctionResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StatusEnum status,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) SchemaEnum schema,
            @RequestParam(required = false) LanguageEnum language) {
        return ResponseEntity.ok(functionService.list(page, pageSize, search, status, sortBy, sortOrder, schema, language));
    }

    @Operation(summary = "Obter funcao por ID")
    @ApiResponse(responseCode = "200", description = "Funcao retornada",
            content = @Content(schema = @Schema(implementation = DbFunctionResponse.class)))
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DbFunctionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(functionService.getById(id));
    }

    @Operation(summary = "Criar funcao")
    @ApiResponse(responseCode = "201", description = "Funcao criada",
            content = @Content(schema = @Schema(implementation = DbFunctionResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DbFunctionResponse> create(@Valid @RequestBody CreateFunctionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(functionService.create(request));
    }

    @Operation(summary = "Atualizar funcao")
    @ApiResponse(responseCode = "200", description = "Funcao atualizada",
            content = @Content(schema = @Schema(implementation = DbFunctionResponse.class)))
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DbFunctionResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateFunctionRequest request) {
        return ResponseEntity.ok(functionService.update(id, request));
    }

    @Operation(summary = "Excluir funcao")
    @ApiResponse(responseCode = "204", description = "Funcao excluida")
    @ApiResponse(responseCode = "409", description = "Funcao referenciada por trigger ativo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        functionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Testar funcao", description = "Executa a funcao em sandbox com argumentos de teste.")
    @ApiResponse(responseCode = "200", description = "Funcao executada com sucesso",
            content = @Content(schema = @Schema(implementation = TestFunctionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Erro na execucao da funcao")
    @PostMapping(path = "/{id}/test", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestFunctionResponse> test(@PathVariable UUID id,
                                                     @RequestBody(required = false) TestFunctionRequest request) {
        return ResponseEntity.ok(functionService.test(id, request != null ? request : new TestFunctionRequest(null)));
    }
}
