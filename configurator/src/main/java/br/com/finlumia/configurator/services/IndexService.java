package br.com.finlumia.configurator.services;

import br.com.finlumia.configurator.models.CreateIndexRequest;
import br.com.finlumia.configurator.models.IndexTypeEnum;
import br.com.finlumia.configurator.models.SchemaEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.UpdateIndexRequest;
import br.com.finlumia.configurator.views.DbIndexResponse;
import br.com.finlumia.configurator.views.IndexStatsResponse;
import br.com.finlumia.configurator.views.PagedResponse;
import br.com.finlumia.configurator.views.PaginationMeta;
import br.com.finlumia.configurator.views.RebuildIndexResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class IndexService {

    private final JdbcTemplate jdbc;

    public IndexService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<DbIndexResponse> ROW_MAPPER = (rs, rowNum) -> {
        String schemaStr = rs.getString("idx_schema");
        SchemaEnum schema = null;
        if (schemaStr != null) {
            try { schema = SchemaEnum.valueOf(schemaStr.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        String typeStr = rs.getString("idx_tipo");
        IndexTypeEnum type = null;
        if (typeStr != null) {
            try { type = IndexTypeEnum.valueOf(typeStr); } catch (IllegalArgumentException ignored) {}
        }

        String statusStr = rs.getString("idx_status");
        StatusEnum status = null;
        if (statusStr != null) {
            try { status = StatusEnum.valueOf(statusStr); } catch (IllegalArgumentException ignored) {}
        }

        return new DbIndexResponse(
                UUID.fromString(rs.getString("idx_key")),
                rs.getString("idx_nome"),
                UUID.fromString(rs.getString("idx_tab_key")),
                rs.getString("tab_nome"),
                schema,
                rs.getString("idx_campos"),
                type,
                rs.getBoolean("idx_unico"),
                rs.getBoolean("idx_parcial"),
                rs.getString("idx_where"),
                rs.getLong("size_kb"),
                status,
                rs.getTimestamp("idx_criado_em").toInstant());
    };

    public PagedResponse<DbIndexResponse> list(int page, int pageSize, String search, StatusEnum status,
                                               String sortBy, String sortOrder, UUID tableId, IndexTypeEnum type) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(200, pageSize));
        int offset = (page - 1) * pageSize;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE i.d_e_l_e_t_e = FALSE");

        if (search != null && !search.isBlank()) {
            where.append(" AND i.idx_nome ILIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (status != null) {
            where.append(" AND i.idx_status = ?");
            params.add(status.name());
        }
        if (tableId != null) {
            where.append(" AND i.idx_tab_key = ?");
            params.add(tableId);
        }
        if (type != null) {
            where.append(" AND i.idx_tipo = ?");
            params.add(type.name());
        }

        String orderCol = resolveSortColumn(sortBy);
        String orderDir = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        String countSql = "SELECT COUNT(*) FROM configurator.\"IDX\" i " + where;
        long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        String dataSql = """
                SELECT i.idx_key, i.idx_nome, i.idx_tab_key, t.tab_nome,
                       i.idx_schema, i.idx_campos, i.idx_tipo, i.idx_unico,
                       i.idx_parcial, i.idx_where, i.idx_status, i.idx_criado_em,
                       COALESCE(
                           (SELECT pg_relation_size(c.oid) / 1024
                            FROM pg_class c
                            JOIN pg_namespace n ON n.oid = c.relnamespace
                            WHERE c.relname = i.idx_nome AND n.nspname = i.idx_schema
                              AND c.relkind = 'i'), 0) AS size_kb
                FROM configurator."IDX" i
                JOIN configurator."TAB" t ON t.tab_key = i.idx_tab_key
                """ + where + " ORDER BY " + orderCol + " " + orderDir
                + " LIMIT ? OFFSET ?";

        params.add(pageSize);
        params.add(offset);

        List<DbIndexResponse> data = jdbc.query(dataSql, ROW_MAPPER, params.toArray());
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PagedResponse<>(data, new PaginationMeta(page, pageSize, total, totalPages));
    }

    public DbIndexResponse getById(UUID id) {
        String sql = """
                SELECT i.idx_key, i.idx_nome, i.idx_tab_key, t.tab_nome,
                       i.idx_schema, i.idx_campos, i.idx_tipo, i.idx_unico,
                       i.idx_parcial, i.idx_where, i.idx_status, i.idx_criado_em,
                       COALESCE(
                           (SELECT pg_relation_size(c.oid) / 1024
                            FROM pg_class c
                            JOIN pg_namespace n ON n.oid = c.relnamespace
                            WHERE c.relname = i.idx_nome AND n.nspname = i.idx_schema
                              AND c.relkind = 'i'), 0) AS size_kb
                FROM configurator."IDX" i
                JOIN configurator."TAB" t ON t.tab_key = i.idx_tab_key
                WHERE i.idx_key = ? AND i.d_e_l_e_t_e = FALSE
                """;
        List<DbIndexResponse> result = jdbc.query(sql, ROW_MAPPER, id);
        if (result.isEmpty()) throw new FinlumiaException(404, "Nao encontrado", "Indice nao encontrado.");
        return result.get(0);
    }

    public DbIndexResponse create(CreateIndexRequest request) {
        String checkSql = "SELECT COUNT(*) FROM configurator.\"IDX\" WHERE idx_nome = ? AND idx_schema = ? AND d_e_l_e_t_e = FALSE";
        long count = jdbc.queryForObject(checkSql, Long.class, request.name(), request.schema().name().toLowerCase());
        if (count > 0) throw new FinlumiaException(409, "Conflito", "Indice com este nome ja existe neste schema.");

        StatusEnum status = request.status() != null ? request.status() : StatusEnum.ativo;
        boolean unique = Boolean.TRUE.equals(request.unique());
        boolean partial = Boolean.TRUE.equals(request.partial());

        String sql = """
                INSERT INTO configurator."IDX"
                    (idx_nome, idx_tab_key, idx_schema, idx_campos, idx_tipo,
                     idx_unico, idx_parcial, idx_where, idx_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING idx_key
                """;
        UUID key = jdbc.queryForObject(sql, UUID.class,
                request.name(), request.tableId(), request.schema().name().toLowerCase(),
                request.fields(), request.type().name(), unique, partial,
                request.whereClause(), status.name());
        return getById(key);
    }

    public DbIndexResponse update(UUID id, UpdateIndexRequest request) {
        getById(id);
        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();

        if (request.whereClause() != null) { sets.add("idx_where = ?"); params.add(request.whereClause()); }
        if (request.status() != null) { sets.add("idx_status = ?"); params.add(request.status().name()); }

        if (!sets.isEmpty()) {
            params.add(id);
            jdbc.update("UPDATE configurator.\"IDX\" SET " + String.join(", ", sets) + " WHERE idx_key = ?", params.toArray());
        }
        return getById(id);
    }

    public void delete(UUID id) {
        getById(id);
        jdbc.update("UPDATE configurator.\"IDX\" SET d_e_l_e_t_e = TRUE WHERE idx_key = ?", id);
    }

    public RebuildIndexResponse rebuild(UUID id) {
        DbIndexResponse index = getById(id);
        return new RebuildIndexResponse(UUID.randomUUID(), estimateRebuildSeconds(index.sizeKb()));
    }

    public IndexStatsResponse getStats(UUID id) {
        DbIndexResponse index = getById(id);
        String sql = """
                SELECT COALESCE(idx_scan, 0)       AS scans,
                       COALESCE(idx_tup_read, 0)   AS tuple_reads,
                       COALESCE(idx_blks_read, 0)  AS blks_read,
                       NULL::timestamptz            AS last_used
                FROM pg_stat_user_indexes s
                JOIN pg_namespace n ON n.nspname = ?
                WHERE s.indexrelname = ? AND n.nspname = s.schemaname
                LIMIT 1
                """;
        List<IndexStatsResponse> result = jdbc.query(sql,
                (rs, rowNum) -> new IndexStatsResponse(
                        index.sizeKb(),
                        rs.getLong("scans"),
                        rs.getLong("tuple_reads"),
                        rs.getLong("blks_read"),
                        rs.getTimestamp("last_used") != null ? rs.getTimestamp("last_used").toInstant() : null),
                index.schema() != null ? index.schema().name().toLowerCase() : "public",
                index.name());

        if (result.isEmpty()) {
            return new IndexStatsResponse(index.sizeKb(), 0, 0, 0, null);
        }
        return result.get(0);
    }

    private int estimateRebuildSeconds(long sizeKb) {
        return (int) Math.max(1, sizeKb / 10240);
    }

    private String resolveSortColumn(String sortBy) {
        if (sortBy == null) return "i.idx_nome";
        return switch (sortBy) {
            case "name" -> "i.idx_nome";
            case "table" -> "t.tab_nome";
            case "type" -> "i.idx_tipo";
            case "status" -> "i.idx_status";
            case "created_at" -> "i.idx_criado_em";
            default -> "i.idx_nome";
        };
    }
}
