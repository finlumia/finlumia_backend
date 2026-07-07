package br.com.finlumia.movement.repositorys;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import br.com.finlumia.movement.models.CategoryId;
import br.com.finlumia.movement.models.InstitutionId;
import br.com.finlumia.movement.models.PaymentMethod;
import br.com.finlumia.movement.models.Transaction;
import br.com.finlumia.movement.models.TransactionFilters;
import br.com.finlumia.movement.models.TransactionType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbc;

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Transaction> MAPPER = (rs, rowNum) -> {
        List<String> tags = null;
        Array tagsArr = rs.getArray("tags");
        if (tagsArr != null) {
            tags = Arrays.asList((String[]) tagsArr.getArray());
        }
        String recurringIdStr = rs.getString("recurring_id");
        return new Transaction(
                rs.getObject("transaction_id", UUID.class),
                rs.getObject("user_key", UUID.class),
                TransactionType.fromValue(rs.getString("type")),
                PaymentMethod.fromValue(rs.getString("method")),
                InstitutionId.fromValue(rs.getString("institution")),
                rs.getObject("date", LocalDate.class),
                CategoryId.fromValue(rs.getString("category")),
                rs.getString("description"),
                rs.getString("sub_description"),
                rs.getBigDecimal("amount"),
                rs.getString("notes"),
                tags,
                rs.getBoolean("is_recurring"),
                recurringIdStr != null ? UUID.fromString(recurringIdStr) : null,
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    };

    public Optional<Transaction> findById(UUID id, UUID userKey) {
        String sql = """
                SELECT * FROM movement.transactions
                WHERE transaction_id = ? AND user_key = ? AND d_e_l_e_t_e = FALSE
                """;
        return jdbc.query(sql, MAPPER, id, userKey).stream().findFirst();
    }

    public List<Transaction> findAll(UUID userKey, TransactionFilters f) {
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM movement.transactions
                WHERE user_key = ? AND d_e_l_e_t_e = FALSE
                """);
        List<Object> params = new ArrayList<>();
        params.add(userKey);
        applyFilters(sb, params, f);
        sb.append(" ORDER BY ").append(f.sortBy().toColumn()).append(" ").append(f.sortOrder().getValue());
        sb.append(" LIMIT ? OFFSET ?");
        params.add(f.pageSize());
        params.add((long) (f.page() - 1) * f.pageSize());
        return jdbc.query(sb.toString(), MAPPER, params.toArray());
    }

    public int count(UUID userKey, TransactionFilters f) {
        StringBuilder sb = new StringBuilder("""
                SELECT COUNT(*) FROM movement.transactions
                WHERE user_key = ? AND d_e_l_e_t_e = FALSE
                """);
        List<Object> params = new ArrayList<>();
        params.add(userKey);
        applyFilters(sb, params, f);
        Integer result = jdbc.queryForObject(sb.toString(), Integer.class, params.toArray());
        return result != null ? result : 0;
    }

    public TotalsData totals(UUID userKey, TransactionFilters f) {
        StringBuilder sb = new StringBuilder("""
                SELECT
                    COALESCE(SUM(CASE WHEN type = 'receita' THEN amount ELSE 0 END), 0) AS total_income,
                    COALESCE(SUM(CASE WHEN type = 'despesa' THEN amount ELSE 0 END), 0) AS total_expenses
                FROM movement.transactions
                WHERE user_key = ? AND d_e_l_e_t_e = FALSE
                """);
        List<Object> params = new ArrayList<>();
        params.add(userKey);
        applyFilters(sb, params, f);
        return jdbc.queryForObject(sb.toString(),
                (rs, rowNum) -> new TotalsData(rs.getBigDecimal("total_income"), rs.getBigDecimal("total_expenses")),
                params.toArray());
    }

    public record TotalsData(BigDecimal totalIncome, BigDecimal totalExpenses) {}

    public boolean existsDuplicate(UUID userKey, LocalDate date, BigDecimal amount, String description) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM movement.transactions
                    WHERE user_key = ? AND date = ? AND amount = ? AND description = ?
                      AND d_e_l_e_t_e = FALSE
                )
                """;
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, userKey, date, amount, description));
    }

    public Transaction save(Transaction t) {
        String sql = """
                INSERT INTO movement.transactions
                    (transaction_id, user_key, type, method, institution, date, category,
                     description, sub_description, amount, notes, tags, is_recurring, recurring_id,
                     created_at, updated_at, d_e_l_e_t_e)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), FALSE)
                """;
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setObject(1, t.id());
            ps.setObject(2, t.userKey());
            ps.setString(3, t.type().getValue());
            ps.setString(4, t.method().getValue());
            ps.setString(5, t.institution().getValue());
            ps.setObject(6, t.date());
            ps.setString(7, t.category().getValue());
            ps.setString(8, t.description());
            ps.setString(9, t.subDescription());
            ps.setBigDecimal(10, t.amount());
            ps.setString(11, t.notes());
            ps.setArray(12, t.tags() != null ? conn.createArrayOf("text", t.tags().toArray(new String[0])) : null);
            ps.setBoolean(13, t.isRecurring());
            ps.setObject(14, t.recurringId());
            return ps;
        });
        return findById(t.id(), t.userKey()).orElseThrow();
    }

    public Transaction update(UUID id, UUID userKey, Transaction t) {
        String sql = """
                UPDATE movement.transactions SET
                    type = ?, method = ?, institution = ?, date = ?, category = ?,
                    description = ?, sub_description = ?, amount = ?, notes = ?, tags = ?,
                    is_recurring = ?, recurring_id = ?, updated_at = NOW()
                WHERE transaction_id = ? AND user_key = ? AND d_e_l_e_t_e = FALSE
                """;
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, t.type().getValue());
            ps.setString(2, t.method().getValue());
            ps.setString(3, t.institution().getValue());
            ps.setObject(4, t.date());
            ps.setString(5, t.category().getValue());
            ps.setString(6, t.description());
            ps.setString(7, t.subDescription());
            ps.setBigDecimal(8, t.amount());
            ps.setString(9, t.notes());
            ps.setArray(10, t.tags() != null ? conn.createArrayOf("text", t.tags().toArray(new String[0])) : null);
            ps.setBoolean(11, t.isRecurring());
            ps.setObject(12, t.recurringId());
            ps.setObject(13, id);
            ps.setObject(14, userKey);
            return ps;
        });
        return findById(id, userKey).orElseThrow();
    }

    public Transaction patch(UUID id, UUID userKey, CategoryId category, String description, String notes, List<String> tags) {
        StringBuilder sb = new StringBuilder("UPDATE movement.transactions SET updated_at = NOW()");
        List<Object> params = new ArrayList<>();
        if (category != null) { sb.append(", category = ?"); params.add(category.getValue()); }
        if (description != null) { sb.append(", description = ?"); params.add(description); }
        if (notes != null) { sb.append(", notes = ?"); params.add(notes); }
        sb.append(" WHERE transaction_id = ? AND user_key = ? AND d_e_l_e_t_e = FALSE");
        params.add(id);
        params.add(userKey);

        if (tags != null) {
            String tagsSql = sb.toString().replace("SET updated_at = NOW()", "SET updated_at = NOW(), tags = ?");
            jdbc.update(conn -> {
                PreparedStatement ps = conn.prepareStatement(tagsSql);
                int idx = 1;
                ps.setArray(idx++, conn.createArrayOf("text", tags.toArray(new String[0])));
                if (category != null) ps.setString(idx++, category.getValue());
                if (description != null) ps.setString(idx++, description);
                if (notes != null) ps.setString(idx++, notes);
                ps.setObject(idx++, id);
                ps.setObject(idx, userKey);
                return ps;
            });
        } else {
            jdbc.update(sb.toString(), params.toArray());
        }
        return findById(id, userKey).orElseThrow();
    }

    public int softDelete(UUID id, UUID userKey) {
        String sql = """
                UPDATE movement.transactions SET d_e_l_e_t_e = TRUE, updated_at = NOW()
                WHERE transaction_id = ? AND user_key = ? AND d_e_l_e_t_e = FALSE
                """;
        return jdbc.update(sql, id, userKey);
    }

    public int softDeleteFromHere(UUID id, UUID userKey) {
        String sql = """
                UPDATE movement.transactions SET d_e_l_e_t_e = TRUE, updated_at = NOW()
                WHERE user_key = ?
                  AND d_e_l_e_t_e = FALSE
                  AND recurring_id = (
                      SELECT recurring_id FROM movement.transactions
                      WHERE transaction_id = ? AND user_key = ? AND recurring_id IS NOT NULL
                  )
                  AND date >= (
                      SELECT date FROM movement.transactions WHERE transaction_id = ? AND user_key = ?
                  )
                """;
        return jdbc.update(sql, userKey, id, userKey, id, userKey);
    }

    public int softDeleteAll(UUID id, UUID userKey) {
        String sql = """
                UPDATE movement.transactions SET d_e_l_e_t_e = TRUE, updated_at = NOW()
                WHERE user_key = ?
                  AND d_e_l_e_t_e = FALSE
                  AND recurring_id = (
                      SELECT recurring_id FROM movement.transactions
                      WHERE transaction_id = ? AND user_key = ? AND recurring_id IS NOT NULL
                  )
                """;
        return jdbc.update(sql, userKey, id, userKey);
    }

    public int batchSoftDelete(List<UUID> ids, UUID userKey) {
        String placeholders = ids.stream().map(u -> "?").collect(Collectors.joining(","));
        String sql = "UPDATE movement.transactions SET d_e_l_e_t_e = TRUE, updated_at = NOW() " +
                     "WHERE transaction_id IN (" + placeholders + ") AND user_key = ? AND d_e_l_e_t_e = FALSE";
        List<Object> params = new ArrayList<>(ids);
        params.add(userKey);
        return jdbc.update(sql, params.toArray());
    }

    private void applyFilters(StringBuilder sb, List<Object> params, TransactionFilters f) {
        if (f.type() != null) { sb.append(" AND type = ?"); params.add(f.type().getValue()); }
        if (f.method() != null) { sb.append(" AND method = ?"); params.add(f.method().getValue()); }
        if (f.institution() != null) { sb.append(" AND institution = ?"); params.add(f.institution().getValue()); }
        if (f.category() != null) { sb.append(" AND category = ?"); params.add(f.category().getValue()); }
        if (f.dateStart() != null) { sb.append(" AND date >= ?"); params.add(f.dateStart()); }
        if (f.dateEnd() != null) { sb.append(" AND date <= ?"); params.add(f.dateEnd()); }
        if (f.amountMin() != null) { sb.append(" AND amount >= ?"); params.add(f.amountMin()); }
        if (f.amountMax() != null) { sb.append(" AND amount <= ?"); params.add(f.amountMax()); }
        if (f.search() != null && !f.search().isBlank()) {
            sb.append(" AND (description ILIKE ? OR sub_description ILIKE ?)");
            String like = "%" + f.search().trim() + "%";
            params.add(like);
            params.add(like);
        }
    }
}
