package br.com.finlumia.configurator.services;

import br.com.finlumia.configurator.models.CreateTableRequest;
import br.com.finlumia.configurator.models.SchemaEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.UpdateTableRequest;
import br.com.finlumia.configurator.views.PagedResponse;
import br.com.finlumia.configurator.views.PaginationMeta;
import br.com.finlumia.configurator.views.TableResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TableService {

    private final JdbcTemplate jdbc;

    public TableService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<TableResponse> ROW_MAPPER = (rs, rowNum) -> mapRow(rs);

    private static TableResponse mapRow(ResultSet rs) throws SQLException {
        String schemaStr = rs.getString("tab_schema");
        SchemaEnum schema = null;
        if (schemaStr != null) {
            try { schema = SchemaEnum.valueOf(schemaStr.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        String statusStr = rs.getString("tab_status");
        StatusEnum status = null;
        if (statusStr != null) {
            try { status = StatusEnum.valueOf(statusStr); } catch (IllegalArgumentException ignored) {}
        }

        return new TableResponse(
                UUID.fromString(rs.getString("tab_key")),
                rs.getString("tab_nome"),
                schema,
                rs.getLong("row_count"),
                rs.getLong("size_kb"),
                status,
                rs.getString("tab_descricao"),
                rs.getTimestamp("tab_criado_em").toInstant(),
                rs.getTimestamp("tab_atualizado_em").toInstant());
    }

    public PagedResponse<TableResponse> list(int page, int pageSize, String search, StatusEnum status,
                                             String sortBy, String sortOrder, SchemaEnum schema) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(200, pageSize));
        int offset = (page - 1) * pageSize;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE t.d_e_l_e_t_e = FALSE");

        if (search != null && !search.isBlank()) {
            where.append(" AND t.tab_nome ILIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (status != null) {
            where.append(" AND t.tab_status = ?");
            params.add(status.name());
        }
        if (schema != null) {
            where.append(" AND t.tab_schema = ?");
            params.add(schema.name().toLowerCase());
        }

        String orderCol = resolveTableSortColumn(sortBy);
        String orderDir = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        String countSql = "SELECT COUNT(*) FROM configurator.\"TAB\" t " + where;
        long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        String dataSql = """
                SELECT t.tab_key, t.tab_nome, t.tab_schema, t.tab_descricao,
                       t.tab_status, t.tab_criado_em, t.tab_atualizado_em,
                       COALESCE(s.n_live_tup, 0)                                AS row_count,
                       COALESCE(pg_total_relation_size(
                           quote_ident(t.tab_schema) || '.' || quote_ident(t.tab_nome)
                       ) / 1024, 0)                                             AS size_kb
                FROM configurator."TAB" t
                LEFT JOIN pg_stat_user_tables s
                       ON s.schemaname = t.tab_schema AND s.relname = t.tab_nome
                """ + where + " ORDER BY " + orderCol + " " + orderDir
                + " LIMIT ? OFFSET ?";

        params.add(pageSize);
        params.add(offset);

        List<TableResponse> data = jdbc.query(dataSql, ROW_MAPPER, params.toArray());
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PagedResponse<>(data, new PaginationMeta(page, pageSize, total, totalPages));
    }

    public TableResponse getById(UUID id) {
        String sql = """
                SELECT t.tab_key, t.tab_nome, t.tab_schema, t.tab_descricao,
                       t.tab_status, t.tab_criado_em, t.tab_atualizado_em,
                       COALESCE(s.n_live_tup, 0) AS row_count,
                       COALESCE(pg_total_relation_size(
                           quote_ident(t.tab_schema) || '.' || quote_ident(t.tab_nome)
                       ) / 1024, 0) AS size_kb
                FROM configurator."TAB" t
                LEFT JOIN pg_stat_user_tables s
                       ON s.schemaname = t.tab_schema AND s.relname = t.tab_nome
                WHERE t.tab_key = ? AND t.d_e_l_e_t_e = FALSE
                """;
        List<TableResponse> result = jdbc.query(sql, ROW_MAPPER, id);
        if (result.isEmpty()) throw new FinlumiaException(404, "Nao encontrado", "Tabela nao encontrada.");
        return result.get(0);
    }

    public TableResponse create(CreateTableRequest request) {
        String checkSql = "SELECT COUNT(*) FROM configurator.\"TAB\" WHERE tab_nome = ? AND tab_schema = ? AND d_e_l_e_t_e = FALSE";
        long count = jdbc.queryForObject(checkSql, Long.class, request.name(), request.schema().name().toLowerCase());
        if (count > 0) throw new FinlumiaException(409, "Conflito", "Tabela ja existe neste schema.");

        StatusEnum status = request.status() != null ? request.status() : StatusEnum.ativo;
        String sql = """
                INSERT INTO configurator."TAB" (tab_nome, tab_schema, tab_descricao, tab_status)
                VALUES (?, ?, ?, ?) RETURNING tab_key
                """;
        UUID key = jdbc.queryForObject(sql, UUID.class,
                request.name(), request.schema().name().toLowerCase(), request.description(), status.name());
        return getById(key);
    }

    public TableResponse update(UUID id, UpdateTableRequest request) {
        getById(id);
        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();

        if (request.description() != null) { sets.add("tab_descricao = ?"); params.add(request.description()); }
        if (request.status() != null) { sets.add("tab_status = ?"); params.add(request.status().name()); }

        if (!sets.isEmpty()) {
            sets.add("tab_atualizado_em = NOW()");
            params.add(id);
            jdbc.update("UPDATE configurator.\"TAB\" SET " + String.join(", ", sets) + " WHERE tab_key = ?", params.toArray());
        }
        return getById(id);
    }

    public void delete(UUID id) {
        getById(id);
        long fieldCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM configurator.\"FIE\" WHERE fie_tab_key = ? AND d_e_l_e_t_e = FALSE", Long.class, id);
        if (fieldCount > 0) throw new FinlumiaException(409, "Conflito", "Tabela possui campos ativos.");

        jdbc.update("UPDATE configurator.\"TAB\" SET d_e_l_e_t_e = TRUE WHERE tab_key = ?", id);
    }

    private String resolveTableSortColumn(String sortBy) {
        if (sortBy == null) return "t.tab_nome";
        return switch (sortBy) {
            case "name" -> "t.tab_nome";
            case "schema" -> "t.tab_schema";
            case "status" -> "t.tab_status";
            case "created_at" -> "t.tab_criado_em";
            case "updated_at" -> "t.tab_atualizado_em";
            case "row_count" -> "row_count";
            case "size_kb" -> "size_kb";
            default -> "t.tab_nome";
        };
    }
}
