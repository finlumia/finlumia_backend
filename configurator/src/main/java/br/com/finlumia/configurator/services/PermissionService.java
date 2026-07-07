package br.com.finlumia.configurator.services;

import br.com.finlumia.configurator.models.BatchUpdatePermissionsRequest;
import br.com.finlumia.configurator.models.CreatePermissionRequest;
import br.com.finlumia.configurator.models.PermissionUpdateItem;
import br.com.finlumia.configurator.models.RoleEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.UpdatePermissionRequest;
import br.com.finlumia.configurator.views.BatchUpdateResponse;
import br.com.finlumia.configurator.views.PagedResponse;
import br.com.finlumia.configurator.views.PaginationMeta;
import br.com.finlumia.configurator.views.PermissionMatrixResponse;
import br.com.finlumia.configurator.views.PermissionResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PermissionService {

    private final JdbcTemplate jdbc;

    public PermissionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PermissionResponse> ROW_MAPPER = (rs, rowNum) -> {
        String roleStr = rs.getString("par_papel");
        RoleEnum role = null;
        if (roleStr != null) {
            try { role = RoleEnum.valueOf(roleStr); } catch (IllegalArgumentException ignored) {}
        }

        return new PermissionResponse(
                UUID.fromString(rs.getString("par_key")),
                rs.getString("par_modulo"),
                rs.getString("par_subsistema"),
                role,
                rs.getBoolean("par_pode_ler"),
                rs.getBoolean("par_pode_escrever"),
                rs.getBoolean("par_pode_deletar"),
                rs.getBoolean("par_pode_admin"));
    };

    public PagedResponse<PermissionResponse> list(int page, int pageSize, String search, StatusEnum status,
                                                  String sortBy, String sortOrder, String module, RoleEnum role) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(200, pageSize));
        int offset = (page - 1) * pageSize;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE p.d_e_l_e_t_e = FALSE");

        if (search != null && !search.isBlank()) {
            where.append(" AND (p.par_modulo ILIKE ? OR p.par_subsistema ILIKE ?)");
            String like = "%" + search.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (module != null && !module.isBlank()) {
            where.append(" AND p.par_modulo = ?");
            params.add(module);
        }
        if (role != null) {
            where.append(" AND p.par_papel = ?");
            params.add(role.name());
        }

        String orderCol = resolveSortColumn(sortBy);
        String orderDir = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        String countSql = "SELECT COUNT(*) FROM configurator.\"PAR\" p " + where;
        long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        String dataSql = "SELECT * FROM configurator.\"PAR\" p "
                + where + " ORDER BY " + orderCol + " " + orderDir
                + " LIMIT ? OFFSET ?";

        params.add(pageSize);
        params.add(offset);

        List<PermissionResponse> data = jdbc.query(dataSql, ROW_MAPPER, params.toArray());
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PagedResponse<>(data, new PaginationMeta(page, pageSize, total, totalPages));
    }

    public PermissionMatrixResponse getMatrix() {
        String sql = "SELECT * FROM configurator.\"PAR\" WHERE d_e_l_e_t_e = FALSE ORDER BY par_modulo, par_subsistema, par_papel";
        List<PermissionResponse> all = jdbc.query(sql, ROW_MAPPER);

        Map<String, List<PermissionResponse>> matrix = new LinkedHashMap<>();
        for (PermissionResponse p : all) {
            String key = p.module() + "/" + p.subsystem();
            matrix.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }
        return new PermissionMatrixResponse(matrix);
    }

    public PermissionResponse create(CreatePermissionRequest request) {
        String checkSql = "SELECT COUNT(*) FROM configurator.\"PAR\" WHERE par_modulo = ? AND par_subsistema = ? AND par_papel = ? AND d_e_l_e_t_e = FALSE";
        long count = jdbc.queryForObject(checkSql, Long.class, request.module(), request.subsystem(), request.role().name());
        if (count > 0) throw new FinlumiaException(409, "Conflito", "Permissao ja existe para este modulo/role.");

        String sql = """
                INSERT INTO configurator."PAR"
                    (par_modulo, par_subsistema, par_papel, par_pode_ler, par_pode_escrever, par_pode_deletar, par_pode_admin)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING par_key
                """;
        UUID key = jdbc.queryForObject(sql, UUID.class,
                request.module(), request.subsystem(), request.role().name(),
                request.canRead(), request.canWrite(), request.canDelete(), request.canAdmin());
        return getById(key);
    }

    public PermissionResponse update(UUID id, UpdatePermissionRequest request) {
        getById(id);
        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();

        if (request.canRead() != null) { sets.add("par_pode_ler = ?"); params.add(request.canRead()); }
        if (request.canWrite() != null) { sets.add("par_pode_escrever = ?"); params.add(request.canWrite()); }
        if (request.canDelete() != null) { sets.add("par_pode_deletar = ?"); params.add(request.canDelete()); }
        if (request.canAdmin() != null) { sets.add("par_pode_admin = ?"); params.add(request.canAdmin()); }

        if (!sets.isEmpty()) {
            params.add(id);
            jdbc.update("UPDATE configurator.\"PAR\" SET " + String.join(", ", sets) + " WHERE par_key = ?", params.toArray());
        }
        return getById(id);
    }

    public BatchUpdateResponse batchUpdate(BatchUpdatePermissionsRequest request) {
        int updated = 0;
        for (PermissionUpdateItem item : request.permissions()) {
            int rows = jdbc.update("""
                    UPDATE configurator."PAR"
                    SET par_pode_ler = ?, par_pode_escrever = ?, par_pode_deletar = ?, par_pode_admin = ?
                    WHERE par_key = ? AND d_e_l_e_t_e = FALSE
                    """,
                    item.canRead(), item.canWrite(), item.canDelete(), item.canAdmin(), item.id());
            updated += rows;
        }
        return new BatchUpdateResponse(updated);
    }

    public void delete(UUID id) {
        getById(id);
        jdbc.update("UPDATE configurator.\"PAR\" SET d_e_l_e_t_e = TRUE WHERE par_key = ?", id);
    }

    private PermissionResponse getById(UUID id) {
        String sql = "SELECT * FROM configurator.\"PAR\" WHERE par_key = ? AND d_e_l_e_t_e = FALSE";
        List<PermissionResponse> result = jdbc.query(sql, ROW_MAPPER, id);
        if (result.isEmpty()) throw new FinlumiaException(404, "Nao encontrado", "Permissao nao encontrada.");
        return result.get(0);
    }

    private String resolveSortColumn(String sortBy) {
        if (sortBy == null) return "p.par_modulo";
        return switch (sortBy) {
            case "module" -> "p.par_modulo";
            case "subsystem" -> "p.par_subsistema";
            case "role" -> "p.par_papel";
            default -> "p.par_modulo";
        };
    }
}
