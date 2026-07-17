package br.com.finlumia.movement.controllers.external;

import java.util.UUID;

import br.com.finlumia.movement.controllers.ExternalApi;
import br.com.finlumia.movement.models.BudgetCreateRequest;
import br.com.finlumia.movement.models.BudgetFilters;
import br.com.finlumia.movement.models.TransactionType;
import br.com.finlumia.movement.services.BudgetService;
import br.com.finlumia.movement.views.BudgetListView;
import br.com.finlumia.movement.views.BudgetView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ExternalApi
@RestController
@RequestMapping("/api/v1/budgets")
@Tag(name = "Budgets", description = "Orçamentos (limite de despesas ou meta de receita) com alerta por e-mail")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(summary = "Listar orçamentos", description = "Lista paginada de orçamentos com o total já apurado no período.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<BudgetListView> list(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) TransactionType type) {
        return ResponseEntity.ok(budgetService.list(userKey, new BudgetFilters(page, pageSize, type)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar orçamento por ID")
    @ApiResponse(responseCode = "200", description = "Orçamento encontrado")
    @ApiResponse(responseCode = "404", description = "Orçamento não encontrado")
    public ResponseEntity<BudgetView> getById(
            @RequestAttribute("usersKey") UUID userKey,
            @PathVariable UUID id) {
        return ResponseEntity.ok(budgetService.getById(userKey, id));
    }

    @PostMapping
    @Operation(summary = "Criar orçamento", description = "Cria um limite de despesa ou meta de receita para um período fixo.")
    @ApiResponse(responseCode = "201", description = "Orçamento criado")
    @ApiResponse(responseCode = "422", description = "Dados inválidos")
    public ResponseEntity<BudgetView> create(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestBody @Valid BudgetCreateRequest request) {
        return ResponseEntity.status(201).body(budgetService.create(userKey, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar orçamento")
    @ApiResponse(responseCode = "200", description = "Orçamento atualizado")
    @ApiResponse(responseCode = "404", description = "Orçamento não encontrado")
    public ResponseEntity<BudgetView> update(
            @RequestAttribute("usersKey") UUID userKey,
            @PathVariable UUID id,
            @RequestBody @Valid BudgetCreateRequest request) {
        return ResponseEntity.ok(budgetService.update(userKey, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir orçamento")
    @ApiResponse(responseCode = "204", description = "Excluído com sucesso")
    @ApiResponse(responseCode = "404", description = "Orçamento não encontrado")
    public ResponseEntity<Void> delete(
            @RequestAttribute("usersKey") UUID userKey,
            @PathVariable UUID id) {
        budgetService.delete(userKey, id);
        return ResponseEntity.noContent().build();
    }
}
