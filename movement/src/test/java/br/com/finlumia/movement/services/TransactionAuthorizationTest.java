package br.com.finlumia.movement.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.finlumia.movement.models.BatchDeleteRequest;
import br.com.finlumia.movement.models.CategoryId;
import br.com.finlumia.movement.models.DeleteMode;
import br.com.finlumia.movement.models.InstitutionId;
import br.com.finlumia.movement.models.PaymentMethod;
import br.com.finlumia.movement.models.Transaction;
import br.com.finlumia.movement.models.TransactionCreateRequest;
import br.com.finlumia.movement.models.TransactionPatchRequest;
import br.com.finlumia.movement.models.TransactionType;
import br.com.finlumia.movement.repositorys.TransactionRepository;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Garante que nenhuma operação financeira retorna ou modifica dados
 * de um usuário diferente do autenticado (anti-IDOR/BOLA).
 *
 * A proteção é exercida em TransactionRepository.findById(id, userKey)
 * que filtra WHERE transaction_id = ? AND user_key = ?.
 * Ao não encontrar o registro para o userKey errado, o service lança 404.
 */
@ExtendWith(MockitoExtension.class)
class TransactionAuthorizationTest {

    @Mock
    TransactionRepository transactionRepository;

    @InjectMocks
    TransactionService transactionService;

    UUID ownerKey;
    UUID attackerKey;
    UUID transactionId;
    Transaction ownerTransaction;

    @BeforeEach
    void setUp() {
        ownerKey = UUID.randomUUID();
        attackerKey = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        ownerTransaction = new Transaction(
                transactionId,
                ownerKey,
                TransactionType.DESPESA,
                PaymentMethod.PIX,
                InstitutionId.NUBANK,
                LocalDate.now(),
                CategoryId.ALIMENTACAO,
                "Almoço",
                null,
                new BigDecimal("35.90"),
                null,
                null,
                false,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("getById: atacante não pode ler transação de outro usuário")
    void getById_withWrongUser_throws404() {
        when(transactionRepository.findById(transactionId, attackerKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(attackerKey, transactionId))
                .isInstanceOf(FinlumiaException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("getById: dono acessa sua própria transação com sucesso")
    void getById_withOwner_succeeds() {
        when(transactionRepository.findById(transactionId, ownerKey))
                .thenReturn(Optional.of(ownerTransaction));

        var result = transactionService.getById(ownerKey, transactionId);
        assert result != null;
        assert result.id().equals(transactionId);
    }

    @Test
    @DisplayName("update: atacante não pode atualizar transação de outro usuário")
    void update_withWrongUser_throws404() {
        when(transactionRepository.findById(transactionId, attackerKey))
                .thenReturn(Optional.empty());

        TransactionCreateRequest req = new TransactionCreateRequest(
                TransactionType.DESPESA, PaymentMethod.PIX, InstitutionId.NUBANK,
                LocalDate.now(), CategoryId.ALIMENTACAO, "Modificado", null,
                new BigDecimal("99.00"), null, null, false, null);

        assertThatThrownBy(() -> transactionService.update(attackerKey, transactionId, req))
                .isInstanceOf(FinlumiaException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("patch: atacante não pode alterar campos de transação alheia")
    void patch_withWrongUser_throws404() {
        when(transactionRepository.findById(transactionId, attackerKey))
                .thenReturn(Optional.empty());

        TransactionPatchRequest req = new TransactionPatchRequest(CategoryId.SAUDE, null, null, null);

        assertThatThrownBy(() -> transactionService.patch(attackerKey, transactionId, req))
                .isInstanceOf(FinlumiaException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("delete SINGLE: atacante não pode excluir transação de outro usuário")
    void delete_withWrongUser_throws404() {
        when(transactionRepository.findById(transactionId, attackerKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(attackerKey, transactionId, DeleteMode.SINGLE))
                .isInstanceOf(FinlumiaException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("delete FROM_HERE: atacante não pode excluir recorrências de outro usuário")
    void deleteFromHere_withWrongUser_throws404() {
        when(transactionRepository.findById(transactionId, attackerKey))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(attackerKey, transactionId, DeleteMode.FROM_HERE))
                .isInstanceOf(FinlumiaException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("batchDelete: IDs de outro usuário retornam 0 deleted (user_key filtra no SQL)")
    void batchDelete_withWrongUser_deletesZeroRows() {
        List<UUID> ids = List.of(transactionId);
        when(transactionRepository.batchSoftDelete(ids, attackerKey)).thenReturn(0);

        var result = transactionService.batchDelete(attackerKey, new BatchDeleteRequest(ids));

        assert result.deleted() == 0 : "Nenhuma transação alheia deve ser excluída";
    }
}
