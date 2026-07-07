package br.com.finlumia.movement.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.finlumia.movement.models.BatchDeleteRequest;
import br.com.finlumia.movement.models.DeleteMode;
import br.com.finlumia.movement.models.Transaction;
import br.com.finlumia.movement.models.TransactionCreateRequest;
import br.com.finlumia.movement.models.TransactionFilters;
import br.com.finlumia.movement.models.TransactionPatchRequest;
import br.com.finlumia.movement.repositorys.TransactionRepository;
import br.com.finlumia.movement.views.BatchDeleteView;
import br.com.finlumia.movement.views.PaginationMeta;
import br.com.finlumia.movement.views.TotalsView;
import br.com.finlumia.movement.views.TransactionListView;
import br.com.finlumia.movement.views.TransactionView;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionListView list(UUID userKey, TransactionFilters filters) {
        List<Transaction> transactions = transactionRepository.findAll(userKey, filters);
        int total = transactionRepository.count(userKey, filters);
        TransactionRepository.TotalsData totalsData = transactionRepository.totals(userKey, filters);

        BigDecimal netBalance = totalsData.totalIncome().subtract(totalsData.totalExpenses());
        TotalsView totals = new TotalsView(totalsData.totalIncome(), totalsData.totalExpenses(), netBalance);
        PaginationMeta meta = PaginationMeta.of(filters.page(), filters.pageSize(), total);

        return new TransactionListView(transactions.stream().map(TransactionView::from).toList(), meta, totals);
    }

    public TransactionView getById(UUID userKey, UUID id) {
        return transactionRepository.findById(id, userKey)
                .map(TransactionView::from)
                .orElseThrow(() -> new FinlumiaException(404, "Não encontrado", "Lançamento não encontrado."));
    }

    @Transactional
    public List<TransactionView> create(UUID userKey, TransactionCreateRequest req) {
        boolean recurring = Boolean.TRUE.equals(req.isRecurring());
        int months = recurring && req.recurringMonths() != null ? req.recurringMonths() : 1;

        if (recurring && (req.recurringMonths() == null || req.recurringMonths() < 1)) {
            throw new FinlumiaException(422, "Dados inválidos", "recurringMonths é obrigatório quando isRecurring=true.");
        }

        UUID recurringId = recurring ? UUID.randomUUID() : null;
        List<TransactionView> created = new ArrayList<>();

        for (int i = 0; i < months; i++) {
            Transaction t = new Transaction(
                    UUID.randomUUID(),
                    userKey,
                    req.type(),
                    req.method(),
                    req.institution(),
                    req.date().plusMonths(i),
                    req.category(),
                    req.description(),
                    req.subDescription(),
                    req.amount(),
                    req.notes(),
                    req.tags(),
                    recurring,
                    recurringId,
                    null,
                    null
            );
            created.add(TransactionView.from(transactionRepository.save(t)));
        }

        return created;
    }

    @Transactional
    public TransactionView update(UUID userKey, UUID id, TransactionCreateRequest req) {
        transactionRepository.findById(id, userKey)
                .orElseThrow(() -> new FinlumiaException(404, "Não encontrado", "Lançamento não encontrado."));

        Transaction t = new Transaction(
                id,
                userKey,
                req.type(),
                req.method(),
                req.institution(),
                req.date(),
                req.category(),
                req.description(),
                req.subDescription(),
                req.amount(),
                req.notes(),
                req.tags(),
                Boolean.TRUE.equals(req.isRecurring()),
                null,
                null,
                null
        );

        return TransactionView.from(transactionRepository.update(id, userKey, t));
    }

    @Transactional
    public TransactionView patch(UUID userKey, UUID id, TransactionPatchRequest req) {
        transactionRepository.findById(id, userKey)
                .orElseThrow(() -> new FinlumiaException(404, "Não encontrado", "Lançamento não encontrado."));

        return TransactionView.from(
                transactionRepository.patch(id, userKey, req.category(), req.description(), req.notes(), req.tags()));
    }

    @Transactional
    public void delete(UUID userKey, UUID id, DeleteMode mode) {
        transactionRepository.findById(id, userKey)
                .orElseThrow(() -> new FinlumiaException(404, "Não encontrado", "Lançamento não encontrado."));

        int affected = switch (mode) {
            case SINGLE -> transactionRepository.softDelete(id, userKey);
            case FROM_HERE -> transactionRepository.softDeleteFromHere(id, userKey);
            case ALL -> transactionRepository.softDeleteAll(id, userKey);
        };

        if (affected == 0) {
            throw new FinlumiaException(404, "Não encontrado", "Lançamento não encontrado.");
        }
    }

    @Transactional
    public BatchDeleteView batchDelete(UUID userKey, BatchDeleteRequest req) {
        int deleted = transactionRepository.batchSoftDelete(req.ids(), userKey);
        return new BatchDeleteView(deleted);
    }
}
