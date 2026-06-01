package br.com.finlumia.identify.repositorys;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import br.com.finlumia.identify.models.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final RowMapper<User> USER_ROW_MAPPER = (resultSet, rowNum) -> new User(
            resultSet.getObject("users_key", UUID.class),
            resultSet.getString("users_email"),
            resultSet.getString("users_senha_hash"),
            resultSet.getBoolean("users_ativo"),
            resultSet.getTimestamp("users_criado_em").toInstant());

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findActiveByEmail(String email) {
        String sql = """
                SELECT users_key, users_email, users_senha_hash, users_ativo, users_criado_em
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
                SELECT users_key, users_email, users_senha_hash, users_ativo, users_criado_em
                FROM identify.users
                WHERE users_key = ?
                  AND users_ativo = TRUE
                  AND d_e_l_e_t_e = FALSE
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, USER_ROW_MAPPER, userKey).stream().findFirst();
    }
}
