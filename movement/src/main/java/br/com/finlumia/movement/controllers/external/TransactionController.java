package br.com.finlumia.movement.controllers.external;

import java.util.List;
import java.util.UUID;

import br.com.finlumia.movement.controllers.ExternalApi;
import br.com.finlumia.movement.models.BatchDeleteRequest;
import br.com.finlumia.movement.models.CategoryId;
import br.com.finlumia.movement.models.DeleteMode;
import br.com.finlumia.movement.models.InstitutionId;
import br.com.finlumia.movement.models.PaymentMethod;
import br.com.finlumia.movement.models.SortBy;
import br.com.finlumia.movement.models.SortOrder;
import br.com.finlumia.movement.models.TransactionCreateRequest;
import br.com.finlumia.movement.models.TransactionFilters;
import br.com.finlumia.movement.models.TransactionPatchRequest;
import br.com.finlumia.movement.models.TransactionType;
import br.com.finlumia.movement.services.TransactionService;
import br.com.finlumia.movement.views.BatchDeleteView;
import br.com.finlumia.movement.views.TransactionListView;
import br.com.finlumia.movement.views.TransactionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@ExternalApi
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Lançamentos financeiros")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "Listar transações", description = "Lista paginada de transações com filtros, busca e totalizadores.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<TransactionListView> list(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) InstitutionId institution,
            @RequestParam(required = false) CategoryId category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SortBy sortBy,
            @RequestParam(required = false) SortOrder sortOrder) {

        TransactionFilters filters = new TransactionFilters(
                page, pageSize, type, method, institution, category,
                dateStart, dateEnd, amountMin, amountMax, search, sortBy, sortOrder);

        return ResponseEntity.ok(transactionService.list(userKey, filters));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar transação por ID")
    @ApiResponse(responseCode = "200", description = "Transação encontrada")
    @ApiResponse(responseCode = "404", description = "Transação não encontrada")
    public ResponseEntity<TransactionView> getById(
            @RequestAttribute("usersKey") UUID userKey,
            @PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getById(userKey, id));
    }

    @PostMapping
    @Operation(summary = "Criar lançamento", description = "Cria um novo lançamento. Se isRecurring=true, gera cópias para os meses seguintes.")
    @ApiResponse(responseCode = "201", description = "Lançamento(s) criado(s)")
    @ApiResponse(responseCode = "422", description = "Dados inválidos")
    public ResponseEntity<List<TransactionView>> create(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestBody @Valid TransactionCreateRequest request) {
        return ResponseEntity.status(201).body(transactionService.createTransaction(userKey, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar lançamento", description = "Atualiza todos os campos de um lançamento.")
    @ApiResponse(responseCode = "200", description = "Lançamento atualizado")
    @ApiResponse(responseCode = "404", description = "Lançamento não encontrado")
    public ResponseEntity<TransactionView> update(
            @RequestAttribute("usersKey") UUID userKey,
            @PathVariable UUID id,
            @RequestBody @Valid TransactionCreateRequest request) {
        return ResponseEntity.ok(transactionService.update(userKey, id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar campos específicos", description = "Atualiza category, description, notes e/ou tags de um lançamento.")
    @ApiResponse(responseCode = "200", description = "Lançamento atualizado")
    @ApiResponse(responseCode = "404", description = "Lançamento não encontrado")
    public ResponseEntity<TransactionView> patch(
            @RequestAttribute("usersKey") UUID userKey,
            @PathVariable UUID id,
            @RequestBody @Valid TransactionPatchRequest request) {
        return ResponseEntity.ok(transactionService.patch(userKey, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir lançamento", description = "Exclui um lançamento. Recorrentes: exclui apenas este, a partir daqui ou todos.")
    @ApiResponse(responseCode = "204", description = "Excluído com sucesso")
    @ApiResponse(responseCode = "404", description = "Lançamento não encontrado")
    public ResponseEntity<Void> delete(
            @RequestAttribute("usersKey") UUID userKey,
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "single") DeleteMode deleteMode) {
        transactionService.delete(userKey, id, deleteMode);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Excluir múltiplos lançamentos", description = "Exclui múltiplos lançamentos em uma única requisição.")
    @ApiResponse(responseCode = "200", description = "Lançamentos excluídos")
    public ResponseEntity<BatchDeleteView> batchDelete(
            @RequestAttribute("usersKey") UUID userKey,
            @RequestBody @Valid BatchDeleteRequest request) {
        return ResponseEntity.ok(transactionService.batchDelete(userKey, request));
    }
}
