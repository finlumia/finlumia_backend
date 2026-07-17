package br.com.finlumia.movement.repositorys;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.finlumia.movement.models.Budget;
import br.com.finlumia.movement.models.BudgetFilters;
import br.com.finlumia.movement.models.BudgetScope;
import br.com.finlumia.movement.models.TransactionType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BudgetRepository {

    private final JdbcTemplate jdbc;

    public BudgetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Budget> MAPPER = (rs, rowNum) -> new Budget(
            rs.getObject("budget_id", UUID.class),
            rs.getObject("user_key", UUID.class),
            rs.getString("name"),
            TransactionType.fromValue(rs.getString("type")),
            BudgetScope.fromValue(rs.getString("scope")),
            rs.getString("scope_value"),
            rs.getBigDecimal("limit_amount"),
            rs.getObject("period_start", LocalDate.class),
            rs.getObject("period_end", LocalDate.class),
            rs.getTimestamp("notified_at") != null ? rs.getTimestamp("notified_at").toInstant() : null,
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    public Optional<Budget> findById(UUID id, UUID userKey) {
        String sql = """
                SELECT * FROM movement.budgets
                WHERE budget_id = ? AND user_key = ? AND d_e_l_e_t_e = FALSE
                """;
        return jdbc.query(sql, MAPPER, id, userKey).stream().findFirst();
    }

    public List<Budget> findAll(UUID userKey, BudgetFilters f) {
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM movement.budgets
                WHERE user_key = ? AND d_e_l_e_t_e = FALSE
                """);
        List<Object> params = new ArrayList<>();
        params.add(userKey);
        if (f.type() != null) { sb.append(" AND type = ?"); params.add(f.type().getValue()); }
        sb.append(" ORDER BY period_start DESC LIMIT ? OFFSET ?");
        params.add(f.pageSize());
        params.add((long) (f.page() - 1) * f.pageSize());
        return jdbc.query(sb.toString(), MAPPER, params.toArray());
    }

    public int count(UUID userKey, BudgetFilters f) {
        StringBuilder sb = new StringBuilder("""
                SELECT COUNT(*) FROM movement.budgets
                WHERE user_key = ? AND d_e_l_e_t_e = FALSE
                """);
        List<Object> params = new ArrayList<>();
        params.add(userKey);
        if (f.type() != null) { sb.append(" AND type = ?"); params.add(f.type().getValue()); }
        Integer result = jdbc.queryForObject(sb.toString(), Integer.class, params.toArray());
        return result != null ? result : 0;
    }

    public Budget save(Budget b) {
        String sql = """
                INSERT INTO movement.budgets
                    (budget_id, user_key, name, type, scope, scope_value, limit_amount,
                     period_start, period_end, created_at, updated_at, d_e_l_e_t_e)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), FALSE)
                """;
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setObject(1, b.id());
            ps.setObject(2, b.userKey());
            ps.setString(3, b.name());
            ps.setString(4, b.type().getValue());
            ps.setString(5, b.scope().getValue());
            ps.setString(6, b.scopeValue());
            ps.setBigDecimal(7, b.limitAmount());
            ps.setObject(8, b.periodStart());
            ps.setObject(9, b.periodEnd());
            return ps;
        });
        return findById(b.id(), b.userKey()).orElseThrow();
    }

    public Budget update(UUID id, UUID userKey, Budget b) {
        String sql = """
                UPDATE movement.budgets SET
                    name = ?, type = ?, scope = ?, scope_value = ?, limit_amount = ?,
                    period_start = ?, period_end = ?, notified_at = NULL, updated_at = NOW()
                WHERE budget_id = ? AND user_key = ? AND d_e_l_e_t_e = FALSE
                """;
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, b.name());
            ps.setString(2, b.type().getValue());
            ps.setString(3, b.scope().getValue());
            ps.setString(4, b.scopeValue());
            ps.setBigDecimal(5, b.limitAmount());
            ps.setObject(6, b.periodStart());
            ps.setObject(7, b.periodEnd());
            ps.setObject(8, id);
            ps.setObject(9, userKey);
            return ps;
        });
        return findById(id, userKey).orElseThrow();
    }

    public int softDelete(UUID id, UUID userKey) {
        String sql = """
                UPDATE movement.budgets SET d_e_l_e_t_e = TRUE, updated_at = NOW()
                WHERE budget_id = ? AND user_key = ? AND d_e_l_e_t_e = FALSE
                """;
        return jdbc.update(sql, id, userKey);
    }

    /**
     * Orçamentos candidatos a alerta: mesmo tipo, período cobrindo a data do
     * lançamento recém-criado, ainda não notificados nesta janela.
     */
    public List<Budget> findActiveMatchingBudgets(UUID userKey, TransactionType type, LocalDate date) {
        String sql = """
                SELECT * FROM movement.budgets
                WHERE user_key = ? AND type = ? AND period_start <= ? AND period_end >= ?
                  AND notified_at IS NULL AND d_e_l_e_t_e = FALSE
                """;
        return jdbc.query(sql, MAPPER, userKey, type.getValue(), date, date);
    }

    public BigDecimal sumForBudget(UUID userKey, Budget b) {
        StringBuilder sb = new StringBuilder("""
                SELECT COALESCE(SUM(amount), 0) FROM movement.transactions
                WHERE user_key = ? AND type = ? AND date BETWEEN ? AND ? AND d_e_l_e_t_e = FALSE
                """);
        List<Object> params = new ArrayList<>(List.of(userKey, b.type().getValue(), b.periodStart(), b.periodEnd()));
        switch (b.scope()) {
            case CATEGORIA -> { sb.append(" AND category = ?"); params.add(b.scopeValue()); }
            case FORMA_PAGAMENTO -> { sb.append(" AND method = ?"); params.add(b.scopeValue()); }
            case BANCO -> { sb.append(" AND institution = ?"); params.add(b.scopeValue()); }
            case GERAL -> { /* sem filtro adicional */ }
        }
        BigDecimal total = jdbc.queryForObject(sb.toString(), BigDecimal.class, params.toArray());
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * UPDATE condicional em notified_at — a própria linha do banco serve de
     * lock: só um chamador concorrente consegue afetar 1 linha.
     */
    public boolean markNotified(UUID id) {
        String sql = "UPDATE movement.budgets SET notified_at = NOW() WHERE budget_id = ? AND notified_at IS NULL";
        return jdbc.update(sql, id) == 1;
    }
}
