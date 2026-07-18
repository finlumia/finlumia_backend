package br.com.finlumia.identify.repositorys;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TokenBlacklistRepository {

    private final JdbcTemplate jdbcTemplate;

    public TokenBlacklistRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsActiveByJti(String jti) {
        String sql = """
                SELECT COUNT(1)
                FROM identify.token_blacklist
                WHERE blacklist_jti = ?
                  AND blacklist_expira_em > NOW()
                  AND d_e_l_e_t_e = FALSE
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, jti);
        return count != null && count > 0;
    }

    public int deleteExpiredBlacklistEntries() {
        String sql = """
                UPDATE identify.token_blacklist
                SET d_e_l_e_t_e = TRUE
                WHERE blacklist_expira_em < NOW()
                  AND d_e_l_e_t_e = FALSE
                """;

        return jdbcTemplate.update(sql);
    }

    public void save(String jti, Instant expiresAt) {
        String sql = """
                INSERT INTO identify.token_blacklist (
                    blacklist_jti,
                    blacklist_expira_em
                ) VALUES (?, ?)
                ON CONFLICT (blacklist_jti) DO NOTHING
                """;

        jdbcTemplate.update(sql, jti, Timestamp.from(expiresAt));
    }
}
