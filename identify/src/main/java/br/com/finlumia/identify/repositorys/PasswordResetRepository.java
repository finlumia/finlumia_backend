package br.com.finlumia.identify.repositorys;

import br.com.finlumia.identify.models.PasswordResetRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class PasswordResetRepository {

    private static final RowMapper<PasswordResetRecord> ROW_MAPPER = (rs, rowNum) -> new PasswordResetRecord(
            rs.getString("prs_email"),
            rs.getString("prs_otp_hash"),
            rs.getTimestamp("prs_expires_at").toInstant(),
            rs.getString("prs_reset_session"));

    private final JdbcTemplate jdbcTemplate;

    public PasswordResetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String email, String otpHash, Instant expiresAt) {
        jdbcTemplate.update("""
                DELETE FROM identify.password_reset_sessions
                WHERE prs_email = ?
                """, email);
        jdbcTemplate.update("""
                INSERT INTO identify.password_reset_sessions (prs_email, prs_otp_hash, prs_expires_at)
                VALUES (?, ?, ?)
                """, email, otpHash, Timestamp.from(expiresAt));
    }

    public Optional<PasswordResetRecord> findActiveByEmail(String email) {
        return jdbcTemplate.query("""
                SELECT prs_email, prs_otp_hash, prs_expires_at, prs_reset_session
                FROM identify.password_reset_sessions
                WHERE prs_email = ?
                  AND prs_expires_at > NOW()
                  AND d_e_l_e_t_e = FALSE
                LIMIT 1
                """, ROW_MAPPER, email).stream().findFirst();
    }

    public Optional<PasswordResetRecord> findActiveByResetSession(String resetSession) {
        return jdbcTemplate.query("""
                SELECT prs_email, prs_otp_hash, prs_expires_at, prs_reset_session
                FROM identify.password_reset_sessions
                WHERE prs_reset_session = ?
                  AND prs_expires_at > NOW()
                  AND d_e_l_e_t_e = FALSE
                LIMIT 1
                """, ROW_MAPPER, resetSession).stream().findFirst();
    }

    public void promoteToResetSession(String email, String resetSession, Instant expiresAt) {
        jdbcTemplate.update("""
                UPDATE identify.password_reset_sessions
                SET prs_reset_session = ?, prs_expires_at = ?, prs_otp_hash = NULL
                WHERE prs_email = ?
                """, resetSession, Timestamp.from(expiresAt), email);
    }

    public void deleteByEmail(String email) {
        jdbcTemplate.update("""
                UPDATE identify.password_reset_sessions
                SET d_e_l_e_t_e = TRUE
                WHERE prs_email = ?
                """, email);
    }
}
