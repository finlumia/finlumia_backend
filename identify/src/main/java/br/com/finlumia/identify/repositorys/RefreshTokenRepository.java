package br.com.finlumia.identify.repositorys;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import br.com.finlumia.identify.models.RefreshTokenRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepository {

    private static final RowMapper<RefreshTokenRecord> REFRESH_TOKEN_ROW_MAPPER = (resultSet, rowNum) ->
            new RefreshTokenRecord(
                    resultSet.getObject("refresh_key", UUID.class),
                    resultSet.getObject("refresh_users_key", UUID.class),
                    resultSet.getString("refresh_token"),
                    resultSet.getTimestamp("refresh_expira_em").toInstant(),
                    resultSet.getBoolean("refresh_revogado"),
                    resultSet.getTimestamp("refresh_criado_em").toInstant());

    private final JdbcTemplate jdbcTemplate;

    public RefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(UUID userKey, String tokenHash, Instant expiresAt) {
        String sql = """
                INSERT INTO identify.refresh_tokens (
                    refresh_users_key,
                    refresh_token,
                    refresh_expira_em
                ) VALUES (?, ?, ?)
                """;

        jdbcTemplate.update(sql, userKey, tokenHash, Timestamp.from(expiresAt));
    }

    public Optional<RefreshTokenRecord> findActiveByTokenHash(String tokenHash) {
        String sql = """
                SELECT refresh_key, refresh_users_key, refresh_token, refresh_expira_em,
                       refresh_revogado, refresh_criado_em
                FROM identify.refresh_tokens
                WHERE refresh_token = ?
                  AND refresh_revogado = FALSE
                  AND refresh_expira_em > NOW()
                  AND d_e_l_e_t_e = FALSE
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, REFRESH_TOKEN_ROW_MAPPER, tokenHash).stream().findFirst();
    }

    public void revokeByTokenHash(String tokenHash) {
        String sql = """
                UPDATE identify.refresh_tokens
                SET refresh_revogado = TRUE,
                    refresh_revogado_em = NOW()
                WHERE refresh_token = ?
                  AND d_e_l_e_t_e = FALSE
                """;

        jdbcTemplate.update(sql, tokenHash);
    }

    public Optional<RefreshTokenRecord> findByTokenHashIncludeRevoked(String tokenHash) {
        String sql = """
                SELECT refresh_key, refresh_users_key, refresh_token, refresh_expira_em,
                       refresh_revogado, refresh_criado_em
                FROM identify.refresh_tokens
                WHERE refresh_token = ?
                  AND d_e_l_e_t_e = FALSE
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, REFRESH_TOKEN_ROW_MAPPER, tokenHash).stream().findFirst();
    }

    public int deleteExpiredTokens() {
        String sql = """
                UPDATE identify.refresh_tokens
                SET d_e_l_e_t_e = TRUE
                WHERE refresh_expira_em < NOW()
                  AND d_e_l_e_t_e = FALSE
                """;

        return jdbcTemplate.update(sql);
    }

    public void revokeAllByUserKey(UUID userKey) {
        String sql = """
                UPDATE identify.refresh_tokens
                SET refresh_revogado = TRUE,
                    refresh_revogado_em = NOW()
                WHERE refresh_users_key = ?
                  AND refresh_revogado = FALSE
                  AND d_e_l_e_t_e = FALSE
                """;

        jdbcTemplate.update(sql, userKey);
    }
}
