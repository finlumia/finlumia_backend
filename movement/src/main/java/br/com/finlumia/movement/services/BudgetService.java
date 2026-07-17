package br.com.finlumia.movement.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.finlumia.movement.models.Budget;
import br.com.finlumia.movement.models.BudgetCreateRequest;
import br.com.finlumia.movement.models.BudgetFilters;
import br.com.finlumia.movement.models.BudgetScope;
import br.com.finlumia.movement.models.CategoryId;
import br.com.finlumia.movement.models.InstitutionId;
import br.com.finlumia.movement.models.PaymentMethod;
import br.com.finlumia.movement.models.Transaction;
import br.com.finlumia.movement.repositorys.BudgetRepository;
import br.com.finlumia.movement.views.BudgetListView;
import br.com.finlumia.movement.views.BudgetView;
import br.com.finlumia.movement.views.PaginationMeta;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);

    private final BudgetRepository budgetRepository;
    private final BudgetEmailService budgetEmailService;

    public BudgetService(BudgetRepository budgetRepository, BudgetEmailService budgetEmailService) {
        this.budgetRepository = budgetRepository;
        this.budgetEmailService = budgetEmailService;
    }

    public BudgetListView list(UUID userKey, BudgetFilters filters) {
        List<Budget> budgets = budgetRepository.findAll(userKey, filters);
        int total = budgetRepository.count(userKey, filters);
        PaginationMeta meta = PaginationMeta.of(filters.page(), filters.pageSize(), total);
        List<BudgetView> data = budgets.stream()
                .map(b -> BudgetView.from(b, budgetRepository.sumForBudget(userKey, b)))
                .toList();
        return new BudgetListView(data, meta);
    }

    public BudgetView getById(UUID userKey, UUID id) {
        Budget b = findOrThrow(userKey, id);
        return BudgetView.from(b, budgetRepository.sumForBudget(userKey, b));
    }

    @Transactional
    public BudgetView create(UUID userKey, BudgetCreateRequest req) {
        validateScopeValue(req.scope(), req.scopeValue());
        if (req.periodEnd().isBefore(req.periodStart())) {
            throw new FinlumiaException(422, "Dados inválidos", "periodEnd não pode ser anterior a periodStart.");
        }

        Budget b = new Budget(
                UUID.randomUUID(), userKey, req.name(), req.type(), req.scope(), req.scopeValue(),
                req.limitAmount(), req.periodStart(), req.periodEnd(), null, null, null);
        Budget saved = budgetRepository.save(b);
        return BudgetView.from(saved, budgetRepository.sumForBudget(userKey, saved));
    }

    @Transactional
    public BudgetView update(UUID userKey, UUID id, BudgetCreateRequest req) {
        findOrThrow(userKey, id);
        validateScopeValue(req.scope(), req.scopeValue());
        if (req.periodEnd().isBefore(req.periodStart())) {
            throw new FinlumiaException(422, "Dados inválidos", "periodEnd não pode ser anterior a periodStart.");
        }

        Budget b = new Budget(
                id, userKey, req.name(), req.type(), req.scope(), req.scopeValue(),
                req.limitAmount(), req.periodStart(), req.periodEnd(), null, null, null);
        Budget updated = budgetRepository.update(id, userKey, b);
        return BudgetView.from(updated, budgetRepository.sumForBudget(userKey, updated));
    }

    @Transactional
    public void delete(UUID userKey, UUID id) {
        findOrThrow(userKey, id);
        budgetRepository.softDelete(id, userKey);
    }

    /**
     * Chamado logo após um lançamento ser criado (TransactionService.create).
     * Nunca propaga exceção — falha aqui não pode derrubar a criação do
     * lançamento que a originou.
     */
    public void checkAndNotify(UUID userKey, Transaction created) {
        try {
            List<Budget> candidates = budgetRepository.findActiveMatchingBudgets(userKey, created.type(), created.date());
            for (Budget budget : candidates) {
                if (!matchesScope(budget, created)) {
                    continue;
                }
                BigDecimal total = budgetRepository.sumForBudget(userKey, budget);
                if (total.compareTo(budget.limitAmount()) >= 0 && budgetRepository.markNotified(budget.id())) {
                    budgetEmailService.sendBudgetAlert(userKey, budget, total);
                }
            }
        } catch (Exception e) {
            log.error("BUDGET_CHECK_FAILURE userKey={} transactionId={} reason={}", userKey, created.id(), e.getMessage());
        }
    }

    private boolean matchesScope(Budget budget, Transaction t) {
        return switch (budget.scope()) {
            case GERAL -> true;
            case CATEGORIA -> budget.scopeValue().equals(t.category().getValue());
            case FORMA_PAGAMENTO -> budget.scopeValue().equals(t.method().getValue());
            case BANCO -> budget.scopeValue().equals(t.institution().getValue());
        };
    }

    private void validateScopeValue(BudgetScope scope, String scopeValue) {
        if (scope == BudgetScope.GERAL) {
            return;
        }
        if (scopeValue == null || scopeValue.isBlank()) {
            throw new FinlumiaException(422, "Dados inválidos", "scopeValue é obrigatório quando scope != geral.");
        }
        try {
            switch (scope) {
                case CATEGORIA -> CategoryId.fromValue(scopeValue);
                case FORMA_PAGAMENTO -> PaymentMethod.fromValue(scopeValue);
                case BANCO -> InstitutionId.fromValue(scopeValue);
                case GERAL -> { }
            }
        } catch (IllegalArgumentException e) {
            throw new FinlumiaException(422, "Dados inválidos", e.getMessage());
        }
    }

    private Budget findOrThrow(UUID userKey, UUID id) {
        return budgetRepository.findById(id, userKey)
                .orElseThrow(() -> new FinlumiaException(404, "Não encontrado", "Orçamento não encontrado."));
    }
}
