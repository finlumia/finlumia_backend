package br.com.finlumia.identify.repositorys;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import br.com.finlumia.identify.models.EmailVerificationRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class EmailVerificationRepository {

    private static final RowMapper<EmailVerificationRecord> ROW_MAPPER = (rs, rowNum) -> new EmailVerificationRecord(
            rs.getString("evc_email"),
            rs.getString("evc_code_hash"),
            rs.getTimestamp("evc_expires_at").toInstant());

    private final JdbcTemplate jdbcTemplate;

    public EmailVerificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String email, String codeHash, Instant expiresAt) {
        jdbcTemplate.update("""
                DELETE FROM identify.email_verification_codes
                WHERE evc_email = ?
                """, email);
        jdbcTemplate.update("""
                INSERT INTO identify.email_verification_codes (evc_email, evc_code_hash, evc_expires_at)
                VALUES (?, ?, ?)
                """, email, codeHash, Timestamp.from(expiresAt));
    }

    public Optional<EmailVerificationRecord> findActiveByEmail(String email) {
        return jdbcTemplate.query("""
                SELECT evc_email, evc_code_hash, evc_expires_at
                FROM identify.email_verification_codes
                WHERE evc_email = ?
                  AND evc_expires_at > NOW()
                  AND d_e_l_e_t_e = FALSE
                LIMIT 1
                """, ROW_MAPPER, email).stream().findFirst();
    }

    public void deleteByEmail(String email) {
        jdbcTemplate.update("""
                UPDATE identify.email_verification_codes
                SET d_e_l_e_t_e = TRUE
                WHERE evc_email = ?
                """, email);
    }
}
