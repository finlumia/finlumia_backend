package br.com.finlumia.document.repositorys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consultas de agregação para os relatórios — lê diretamente da tabela
 * movement.transactions (mesmo banco Postgres, schema do módulo movement;
 * document não tem tabelas próprias). Todas as consultas filtram
 * d_e_l_e_t_e = FALSE (soft delete) e são escopadas por user_key.
 */
@Repository
public class ReportRepository {

    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Totals(BigDecimal totalIncome, BigDecimal totalExpenses) {}

    public record MonthlyRow(int year, int month, BigDecimal income, BigDecimal expenses) {}

    public record CategoryRow(String category, BigDecimal total, int transactions) {}

    public record InstitutionRow(String institution, BigDecimal total) {}

    /** Soma de receitas/despesas dentro de [start, end] (inclusive). */
    public Totals totals(UUID userKey, LocalDate start, LocalDate end) {
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN type = 'receita' THEN amount ELSE 0 END), 0) AS total_income,
                    COALESCE(SUM(CASE WHEN type = 'despesa' THEN amount ELSE 0 END), 0) AS total_expenses
                FROM movement.transactions
                WHERE user_key = ? AND d_e_l_e_t_e = FALSE AND date BETWEEN ? AND ?
                """;
        return jdbc.queryForObject(sql,
                (rs, rowNum) -> new Totals(rs.getBigDecimal("total_income"), rs.getBigDecimal("total_expenses")),
                userKey, start, end);
    }

    /** Saldo acumulado (receitas - despesas) de todo o histórico até uma data, inclusive. */
    public BigDecimal cumulativeBalanceUpTo(UUID userKey, LocalDate asOf) {
        String sql = """
                SELECT COALESCE(SUM(CASE WHEN type = 'receita' THEN amount ELSE -amount END), 0)
                FROM movement.transactions
                WHERE user_key = ? AND d_e_l_e_t_e = FALSE AND date <= ?
                """;
        BigDecimal result = jdbc.queryForObject(sql, BigDecimal.class, userKey, asOf);
        return result != null ? result : BigDecimal.ZERO;
    }

    /** Receitas/despesas agrupadas por mês dentro de [start, end]. Meses sem lançamento não aparecem. */
    public List<MonthlyRow> monthlyTotals(UUID userKey, LocalDate start, LocalDate end) {
        String sql = """
                SELECT
                    EXTRACT(YEAR FROM date)::int AS yr,
                    EXTRACT(MONTH FROM date)::int AS mo,
                    COALESCE(SUM(CASE WHEN type = 'receita' THEN amount ELSE 0 END), 0) AS income,
                    COALESCE(SUM(CASE WHEN type = 'despesa' THEN amount ELSE 0 END), 0) AS expenses
                FROM movement.transactions
                WHERE user_key = ? AND d_e_l_e_t_e = FALSE AND date BETWEEN ? AND ?
                GROUP BY 1, 2
                ORDER BY 1, 2
                """;
        return jdbc.query(sql,
                (rs, rowNum) -> new MonthlyRow(rs.getInt("yr"), rs.getInt("mo"), rs.getBigDecimal("income"), rs.getBigDecimal("expenses")),
                userKey, start, end);
    }

    /** Total por categoria dentro de [start, end], filtrado por tipo (receita|despesa). */
    public List<CategoryRow> categoryTotals(UUID userKey, LocalDate start, LocalDate end, String type) {
        String sql = """
                SELECT category, COALESCE(SUM(amount), 0) AS total, COUNT(*)::int AS cnt
                FROM movement.transactions
                WHERE user_key = ? AND d_e_l_e_t_e = FALSE AND type = ? AND date BETWEEN ? AND ?
                GROUP BY category
                ORDER BY total DESC
                """;
        return jdbc.query(sql,
                (rs, rowNum) -> new CategoryRow(rs.getString("category"), rs.getBigDecimal("total"), rs.getInt("cnt")),
                userKey, type, start, end);
    }

    /** Total de uma categoria específica dentro de [start, end], para qualquer tipo. Usado para calcular tendência. */
    public BigDecimal categoryTotal(UUID userKey, String category, LocalDate start, LocalDate end, String type) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM movement.transactions
                WHERE user_key = ? AND d_e_l_e_t_e = FALSE AND type = ? AND category = ? AND date BETWEEN ? AND ?
                """;
        BigDecimal result = jdbc.queryForObject(sql, BigDecimal.class, userKey, type, category, start, end);
        return result != null ? result : BigDecimal.ZERO;
    }

    /** Total por instituição dentro de [start, end], filtrado por tipo (receita|despesa). */
    public List<InstitutionRow> institutionTotals(UUID userKey, LocalDate start, LocalDate end, String type) {
        String sql = """
                SELECT institution, COALESCE(SUM(amount), 0) AS total
                FROM movement.transactions
                WHERE user_key = ? AND d_e_l_e_t_e = FALSE AND type = ? AND date BETWEEN ? AND ?
                GROUP BY institution
                ORDER BY total DESC
                """;
        return jdbc.query(sql,
                (rs, rowNum) -> new InstitutionRow(rs.getString("institution"), rs.getBigDecimal("total")),
                userKey, type, start, end);
    }
}
