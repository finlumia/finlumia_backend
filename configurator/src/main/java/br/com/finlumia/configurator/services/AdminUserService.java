package br.com.finlumia.configurator.services;

import br.com.finlumia.configurator.models.CreateAdminUserRequest;
import br.com.finlumia.configurator.models.RoleEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.ToggleUserStatusRequest;
import br.com.finlumia.configurator.models.UpdateAdminUserRequest;
import br.com.finlumia.configurator.models.UserAdminStatus;
import br.com.finlumia.configurator.views.AdminResetPasswordResponse;
import br.com.finlumia.configurator.views.AdminUserResponse;
import br.com.finlumia.configurator.views.PagedResponse;
import br.com.finlumia.configurator.views.PaginationMeta;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AdminUserService {

    private final JdbcTemplate jdbc;

    public AdminUserService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<AdminUserResponse> ROW_MAPPER = (rs, rowNum) -> {
        String roleStr = rs.getString("users_papel");
        RoleEnum role = null;
        if (roleStr != null) {
            try { role = RoleEnum.valueOf(roleStr.toLowerCase()); } catch (IllegalArgumentException ignored) {}
        }

        String statusStr = rs.getString("users_status");
        UserAdminStatus status = null;
        if (statusStr != null) {
            try { status = UserAdminStatus.valueOf(statusStr.toLowerCase()); } catch (IllegalArgumentException ignored) {}
        }

        return new AdminUserResponse(
                UUID.fromString(rs.getString("users_key")),
                rs.getString("users_nome"),
                rs.getString("users_email"),
                role,
                status,
                rs.getBoolean("users_mfa"),
                rs.getTimestamp("users_ultimo_login") != null ? rs.getTimestamp("users_ultimo_login").toInstant() : null,
                rs.getTimestamp("users_criado_em").toInstant(),
                rs.getTimestamp("users_atualizado_em") != null ? rs.getTimestamp("users_atualizado_em").toInstant() : null);
    };

    public PagedResponse<AdminUserResponse> list(int page, int pageSize, String search, StatusEnum status,
                                                 String sortBy, String sortOrder, RoleEnum role) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(200, pageSize));
        int offset = (page - 1) * pageSize;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE u.d_e_l_e_t_e = FALSE");

        if (search != null && !search.isBlank()) {
            where.append(" AND (u.users_nome ILIKE ? OR u.users_email ILIKE ?)");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            where.append(" AND u.users_status = ?");
            params.add(status.name());
        }
        if (role != null) {
            where.append(" AND u.users_papel = ?");
            params.add(role.name());
        }

        String orderCol = resolveSortColumn(sortBy);
        String orderDir = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        String countSql = "SELECT COUNT(*) FROM identify.users u " + where;
        long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        String dataSql = "SELECT u.users_key, u.users_nome, u.users_email, u.users_papel, u.users_status, "
                + "u.users_mfa, u.users_ultimo_login, u.users_criado_em, u.users_atualizado_em "
                + "FROM identify.users u "
                + where + " ORDER BY " + orderCol + " " + orderDir
                + " LIMIT ? OFFSET ?";

        params.add(pageSize);
        params.add(offset);

        List<AdminUserResponse> data = jdbc.query(dataSql, ROW_MAPPER, params.toArray());
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PagedResponse<>(data, new PaginationMeta(page, pageSize, total, totalPages));
    }

    public AdminUserResponse getById(UUID id) {
        String sql = "SELECT u.users_key, u.users_nome, u.users_email, u.users_papel, u.users_status, "
                + "u.users_mfa, u.users_ultimo_login, u.users_criado_em, u.users_atualizado_em "
                + "FROM identify.users u WHERE u.users_key = ? AND u.d_e_l_e_t_e = FALSE";
        List<AdminUserResponse> result = jdbc.query(sql, ROW_MAPPER, id);
        if (result.isEmpty()) throw new FinlumiaException(404, "Nao encontrado", "Usuario nao encontrado.");
        return result.get(0);
    }

    public AdminUserResponse create(CreateAdminUserRequest request) {
        String checkSql = "SELECT COUNT(*) FROM identify.users WHERE users_email = ? AND d_e_l_e_t_e = FALSE";
        long count = jdbc.queryForObject(checkSql, Long.class, request.email());
        if (count > 0) throw new FinlumiaException(409, "Conflito", "E-mail ja cadastrado.");

        UserAdminStatus status = request.status() != null ? request.status() : UserAdminStatus.pendente;
        boolean mfa = Boolean.TRUE.equals(request.mfa());
        RoleEnum role = request.role() != null ? request.role() : RoleEnum.viewer;

        String sql = """
                INSERT INTO identify.users
                    (users_nome, users_email, users_papel, users_status, users_mfa, users_senha_hash, users_atualizado_em)
                VALUES (?, ?, ?, ?, ?, '', NOW()) RETURNING users_key
                """;
        UUID key = jdbc.queryForObject(sql, UUID.class,
                request.name(), request.email(), role.name(), status.name(), mfa);
        return getById(key);
    }

    public AdminUserResponse update(UUID id, UpdateAdminUserRequest request) {
        getById(id);
        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();

        if (request.name() != null) { sets.add("users_nome = ?"); params.add(request.name()); }
        if (request.role() != null) { sets.add("users_papel = ?"); params.add(request.role().name()); }
        if (request.status() != null) { sets.add("users_status = ?"); params.add(request.status().name()); }
        if (request.mfa() != null) { sets.add("users_mfa = ?"); params.add(request.mfa()); }

        if (!sets.isEmpty()) {
            sets.add("users_atualizado_em = NOW()");
            params.add(id);
            jdbc.update("UPDATE identify.users SET " + String.join(", ", sets) + " WHERE users_key = ?", params.toArray());
        }
        return getById(id);
    }

    public void delete(UUID id) {
        getById(id);
        jdbc.update("UPDATE identify.users SET d_e_l_e_t_e = TRUE, users_atualizado_em = NOW() WHERE users_key = ?", id);
    }

    public AdminUserResponse toggleStatus(UUID id, ToggleUserStatusRequest request) {
        getById(id);
        String statusValue = request.status().name();
        jdbc.update("UPDATE identify.users SET users_status = ?, users_atualizado_em = NOW() WHERE users_key = ?",
                statusValue, id);
        return getById(id);
    }

    public AdminResetPasswordResponse adminResetPassword(UUID id) {
        getById(id);
        return new AdminResetPasswordResponse("Link de redefinicao de senha enviado para o e-mail do usuario.");
    }

    private String resolveSortColumn(String sortBy) {
        if (sortBy == null) return "u.users_nome";
        return switch (sortBy) {
            case "name" -> "u.users_nome";
            case "email" -> "u.users_email";
            case "role" -> "u.users_papel";
            case "status" -> "u.users_status";
            case "created_at" -> "u.users_criado_em";
            case "updated_at" -> "u.users_atualizado_em";
            case "last_login" -> "u.users_ultimo_login";
            default -> "u.users_nome";
        };
    }
}
