package br.com.finlumia.identify.repositorys;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import br.com.finlumia.identify.models.User;
import br.com.finlumia.identify.models.UserProfileRecord;
import br.com.finlumia.identify.models.UserRole;
import br.com.finlumia.identify.models.UserStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final RowMapper<User> USER_ROW_MAPPER = (resultSet, rowNum) -> {
        Timestamp lockedTs = resultSet.getTimestamp("users_locked_until");
        return new User(
                resultSet.getObject("users_key", UUID.class),
                resultSet.getString("users_email"),
                resultSet.getString("users_senha_hash"),
                resultSet.getBoolean("users_ativo"),
                resultSet.getTimestamp("users_criado_em").toInstant(),
                resultSet.getInt("users_failed_attempts"),
                lockedTs != null ? lockedTs.toInstant() : null,
                resultSet.getBoolean("users_email_verified"));
    };

    private static final RowMapper<UserProfileRecord> PROFILE_ROW_MAPPER = (rs, rowNum) -> new UserProfileRecord(
            rs.getObject("users_key", UUID.class),
            rs.getString("users_nome"),
            rs.getString("users_email"),
            rs.getString("users_senha_hash"),
            UserRole.valueOf(rs.getString("users_papel")),
            UserStatus.valueOf(rs.getString("users_status")),
            rs.getBoolean("users_mfa"),
            rs.getString("users_locale"),
            rs.getString("users_tema"),
            rs.getTimestamp("users_ultimo_login") != null ? rs.getTimestamp("users_ultimo_login").toInstant() : null,
            rs.getBoolean("users_ativo"),
            rs.getTimestamp("users_criado_em").toInstant(),
            rs.getTimestamp("users_atualizado_em").toInstant());

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByEmail(String email) {
        String sql = """
                SELECT users_key, users_email, users_senha_hash, users_ativo, users_criado_em,
                       users_failed_attempts, users_locked_until, users_email_verified
                FROM identify.users
                WHERE users_email = ?
                  AND d_e_l_e_t_e = FALSE
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, USER_ROW_MAPPER, email).stream().findFirst();
    }

    public Optional<User> findActiveByEmail(String email) {
        String sql = """
                SELECT users_key, users_email, users_senha_hash, users_ativo, users_criado_em,
                       users_failed_attempts, users_locked_until, users_email_verified
                FROM identify.users
                WHERE users_email = ?
                  AND users_ativo = TRUE
                  AND d_e_l_e_t_e = FALSE
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, USER_ROW_MAPPER, email).stream().findFirst();
    }

    public Optional<User> findActiveByKey(UUID userKey) {
        String sql = """
                SELECT users_key, users_email, users_senha_hash, users_ativo, users_criado_em,
                       users_failed_attempts, users_locked_until, users_email_verified
                FROM identify.users
                WHERE users_key = ?
                  AND users_ativo = TRUE
                  AND d_e_l_e_t_e = FALSE
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, USER_ROW_MAPPER, userKey).stream().findFirst();
    }

    public void incrementFailedAttempts(String email) {
        jdbcTemplate.update("""
                UPDATE identify.users
                SET users_failed_attempts = users_failed_attempts + 1,
                    users_locked_until = CASE
                        WHEN users_failed_attempts + 1 >= 5
                        THEN NOW() + INTERVAL '15 minutes'
                        ELSE users_locked_until
                    END,
                    users_atualizado_em = NOW()
                WHERE users_email = ?
                """, email);
    }

    public void resetFailedAttempts(UUID userKey) {
        jdbcTemplate.update("""
                UPDATE identify.users
                SET users_failed_attempts = 0,
                    users_locked_until = NULL,
                    users_ultimo_login = NOW(),
                    users_atualizado_em = NOW()
                WHERE users_key = ?
                """, userKey);
    }

    public Optional<UserProfileRecord> findFullProfileByKey(UUID userKey) {
        return jdbcTemplate.query("""
                SELECT users_key, users_nome, users_email, users_senha_hash,
                       users_papel, users_status, users_mfa, users_locale, users_tema,
                       users_ultimo_login, users_ativo, users_criado_em, users_atualizado_em
                FROM identify.users
                WHERE users_key = ?
                  AND d_e_l_e_t_e = FALSE
                LIMIT 1
                """, PROFILE_ROW_MAPPER, userKey).stream().findFirst();
    }

    public Optional<UserProfileRecord> findFullProfileByEmail(String email) {
        return jdbcTemplate.query("""
                SELECT users_key, users_nome, users_email, users_senha_hash,
                       users_papel, users_status, users_mfa, users_locale, users_tema,
                       users_ultimo_login, users_ativo, users_criado_em, users_atualizado_em
                FROM identify.users
                WHERE users_email = ?
                  AND d_e_l_e_t_e = FALSE
                LIMIT 1
                """, PROFILE_ROW_MAPPER, email).stream().findFirst();
    }

    public void updateProfile(UUID userKey, String name, String locale, String theme) {
        jdbcTemplate.update("""
                UPDATE identify.users
                SET users_nome = ?, users_locale = ?, users_tema = ?, users_atualizado_em = NOW()
                WHERE users_key = ?
                """, name, locale, theme, userKey);
    }

    public void updatePassword(UUID userKey, String passwordHash) {
        jdbcTemplate.update("""
                UPDATE identify.users
                SET users_senha_hash = ?, users_atualizado_em = NOW()
                WHERE users_key = ?
                """, passwordHash, userKey);
    }

    public void updatePasswordByEmail(String email, String passwordHash) {
        jdbcTemplate.update("""
                UPDATE identify.users
                SET users_senha_hash = ?, users_atualizado_em = NOW()
                WHERE users_email = ?
                """, passwordHash, email);
    }

    public void updateMfa(UUID userKey, boolean mfa) {
        jdbcTemplate.update("""
                UPDATE identify.users
                SET users_mfa = ?, users_atualizado_em = NOW()
                WHERE users_key = ?
                """, mfa, userKey);
    }

    public UserProfileRecord createOAuthUser(String email, String name) {
        UUID newKey = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO identify.users
                    (users_key, users_nome, users_email, users_papel, users_status,
                     users_mfa, users_locale, users_tema, users_ativo, users_email_verified)
                VALUES (?, ?, ?, 'viewer', 'ativo', FALSE, 'pt-BR', 'light', TRUE, TRUE)
                """, newKey, name, email);
        return findFullProfileByKey(newKey).orElseThrow();
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM identify.users
                WHERE users_email = ? AND d_e_l_e_t_e = FALSE
                """, Integer.class, email);
        return count != null && count > 0;
    }

    public UserProfileRecord createLocalUser(String email, String name, String passwordHash) {
        UUID newKey = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO identify.users
                    (users_key, users_nome, users_email, users_senha_hash, users_papel, users_status,
                     users_mfa, users_locale, users_tema, users_ativo, users_email_verified)
                VALUES (?, ?, ?, ?, 'viewer', 'ativo', FALSE, 'pt-BR', 'light', TRUE, FALSE)
                """, newKey, name, email, passwordHash);
        return findFullProfileByKey(newKey).orElseThrow();
    }

    public void markEmailVerified(String email) {
        jdbcTemplate.update("""
                UPDATE identify.users
                SET users_email_verified = TRUE, users_atualizado_em = NOW()
                WHERE users_email = ?
                """, email);
    }
}
