package br.com.finlumia.identify.repositorys;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.finlumia.identify.models.AccessGrant;
import br.com.finlumia.identify.models.AccessOperation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccessControlRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccessControlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isActiveUser(UUID userKey) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM identify.users
                    WHERE users_key = ?
                      AND users_ativo = TRUE
                      AND d_e_l_e_t_e = FALSE
                )
                """;

        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, userKey);
        return Boolean.TRUE.equals(exists);
    }

    public boolean resourceExists(String resourceName) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM identify.resources
                    WHERE resource_nome = ?
                )
                """;

        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, normalizeResourceName(resourceName));
        return Boolean.TRUE.equals(exists);
    }

    public Optional<AccessGrant> findGrant(UUID userKey, String resourceName, AccessOperation operation) {
        String permissionColumn = permissionColumn(operation);
        String sql = """
                SELECT
                    r.resource_key,
                    r.resource_nome,
                    ARRAY_AGG(DISTINCT ur.users_roles_role ORDER BY ur.users_roles_role) AS roles
                FROM identify.users_roles ur
                JOIN identify.permissions p
                  ON p.permission_role_nome = ur.users_roles_role
                JOIN identify.resources r
                  ON r.resource_key = p.permission_resource_key
                WHERE ur.users_roles_user_key = ?
                  AND r.resource_nome = ?
                  AND p.%s = TRUE
                GROUP BY r.resource_key, r.resource_nome
                """.formatted(permissionColumn);

        List<AccessGrant> grants = jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> new AccessGrant(
                        resultSet.getObject("resource_key", UUID.class),
                        resultSet.getString("resource_nome"),
                        List.of((String[]) resultSet.getArray("roles").getArray())),
                userKey,
                normalizeResourceName(resourceName));

        return grants.stream().findFirst();
    }

    private String permissionColumn(AccessOperation operation) {
        return switch (operation) {
            case CREATE -> "permission_can_create";
            case READ -> "permission_can_read";
            case UPDATE -> "permission_can_update";
            case DELETE -> "permission_can_delete";
        };
    }

    private String normalizeResourceName(String resourceName) {
        return resourceName == null ? "" : resourceName.trim().toLowerCase();
    }
}
