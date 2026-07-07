package br.com.finlumia.configurator.services;

import br.com.finlumia.configurator.models.CreateTriggerRequest;
import br.com.finlumia.configurator.models.SchemaEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.ToggleTriggerRequest;
import br.com.finlumia.configurator.models.TriggerEventEnum;
import br.com.finlumia.configurator.models.TriggerTimingEnum;
import br.com.finlumia.configurator.models.UpdateTriggerRequest;
import br.com.finlumia.configurator.views.DbTriggerResponse;
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
public class TriggerService {

    private final JdbcTemplate jdbc;

    public TriggerService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<DbTriggerResponse> ROW_MAPPER = (rs, rowNum) -> {
        String schemaStr = rs.getString("gen_schema");
        SchemaEnum schema = null;
        if (schemaStr != null) {
            try { schema = SchemaEnum.valueOf(schemaStr.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        String eventStr = rs.getString("gen_evento");
        TriggerEventEnum event = null;
        if (eventStr != null) {
            try { event = TriggerEventEnum.valueOf(eventStr.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        String timingStr = rs.getString("gen_timing");
        TriggerTimingEnum timing = null;
        if (timingStr != null) {
            try { timing = TriggerTimingEnum.valueOf(timingStr.replace(" ", "_").toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        String statusStr = rs.getString("gen_status");
        StatusEnum status = null;
        if (statusStr != null) {
            try { status = StatusEnum.valueOf(statusStr); } catch (IllegalArgumentException ignored) {}
        }

        return new DbTriggerResponse(
                UUID.fromString(rs.getString("gen_key")),
                rs.getString("gen_nome"),
                UUID.fromString(rs.getString("gen_tab_key")),
                rs.getString("tab_nome"),
                schema,
                event,
                timing,
                rs.getString("gen_funcao"),
                rs.getBoolean("gen_habilitado"),
                rs.getString("gen_descricao"),
                status,
                rs.getTimestamp("gen_criado_em").toInstant());
    };

    public PagedResponse<DbTriggerResponse> list(int page, int pageSize, String search, StatusEnum status,
                                                 String sortBy, String sortOrder, UUID tableId,
                                                 TriggerEventEnum event, Boolean enabled) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(200, pageSize));
        int offset = (page - 1) * pageSize;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE g.d_e_l_e_t_e = FALSE");

        if (search != null && !search.isBlank()) {
            where.append(" AND g.gen_nome ILIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (status != null) {
            where.append(" AND g.gen_status = ?");
            params.add(status.name());
        }
        if (tableId != null) {
            where.append(" AND g.gen_tab_key = ?");
            params.add(tableId);
        }
        if (event != null) {
            where.append(" AND g.gen_evento = ?");
            params.add(event.name());
        }
        if (enabled != null) {
            where.append(" AND g.gen_habilitado = ?");
            params.add(enabled);
        }

        String orderCol = resolveSortColumn(sortBy);
        String orderDir = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        String countSql = "SELECT COUNT(*) FROM configurator.\"GEN\" g " + where;
        long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        String dataSql = """
                SELECT g.gen_key, g.gen_nome, g.gen_tab_key, t.tab_nome,
                       g.gen_schema, g.gen_evento, g.gen_timing, g.gen_funcao,
                       g.gen_habilitado, g.gen_descricao, g.gen_status, g.gen_criado_em
                FROM configurator."GEN" g
                JOIN configurator."TAB" t ON t.tab_key = g.gen_tab_key
                """ + where + " ORDER BY " + orderCol + " " + orderDir
                + " LIMIT ? OFFSET ?";

        params.add(pageSize);
        params.add(offset);

        List<DbTriggerResponse> data = jdbc.query(dataSql, ROW_MAPPER, params.toArray());
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PagedResponse<>(data, new PaginationMeta(page, pageSize, total, totalPages));
    }

    public DbTriggerResponse getById(UUID id) {
        String sql = """
                SELECT g.gen_key, g.gen_nome, g.gen_tab_key, t.tab_nome,
                       g.gen_schema, g.gen_evento, g.gen_timing, g.gen_funcao,
                       g.gen_habilitado, g.gen_descricao, g.gen_status, g.gen_criado_em
                FROM configurator."GEN" g
                JOIN configurator."TAB" t ON t.tab_key = g.gen_tab_key
                WHERE g.gen_key = ? AND g.d_e_l_e_t_e = FALSE
                """;
        List<DbTriggerResponse> result = jdbc.query(sql, ROW_MAPPER, id);
        if (result.isEmpty()) throw new FinlumiaException(404, "Nao encontrado", "Trigger nao encontrado.");
        return result.get(0);
    }

    public DbTriggerResponse create(CreateTriggerRequest request) {
        String checkSql = "SELECT COUNT(*) FROM configurator.\"GEN\" WHERE gen_nome = ? AND gen_tab_key = ? AND d_e_l_e_t_e = FALSE";
        long count = jdbc.queryForObject(checkSql, Long.class, request.name(), request.tableId());
        if (count > 0) throw new FinlumiaException(409, "Conflito", "Trigger com este nome ja existe na tabela.");

        StatusEnum status = request.status() != null ? request.status() : StatusEnum.ativo;
        boolean enabled = request.enabled() == null || request.enabled();

        String sql = """
                INSERT INTO configurator."GEN"
                    (gen_nome, gen_tab_key, gen_schema, gen_evento, gen_timing,
                     gen_funcao, gen_habilitado, gen_descricao, gen_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING gen_key
                """;
        UUID key = jdbc.queryForObject(sql, UUID.class,
                request.name(), request.tableId(), request.schema().name().toLowerCase(),
                request.event().name(), request.timing().name(),
                request.function(), enabled, request.description(), status.name());
        return getById(key);
    }

    public DbTriggerResponse update(UUID id, UpdateTriggerRequest request) {
        getById(id);
        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();

        if (request.function() != null) { sets.add("gen_funcao = ?"); params.add(request.function()); }
        if (request.enabled() != null) { sets.add("gen_habilitado = ?"); params.add(request.enabled()); }
        if (request.description() != null) { sets.add("gen_descricao = ?"); params.add(request.description()); }
        if (request.status() != null) { sets.add("gen_status = ?"); params.add(request.status().name()); }

        if (!sets.isEmpty()) {
            params.add(id);
            jdbc.update("UPDATE configurator.\"GEN\" SET " + String.join(", ", sets) + " WHERE gen_key = ?", params.toArray());
        }
        return getById(id);
    }

    public DbTriggerResponse toggle(UUID id, ToggleTriggerRequest request) {
        getById(id);
        jdbc.update("UPDATE configurator.\"GEN\" SET gen_habilitado = ? WHERE gen_key = ?",
                request.enabled(), id);
        return getById(id);
    }

    public void delete(UUID id) {
        getById(id);
        jdbc.update("UPDATE configurator.\"GEN\" SET d_e_l_e_t_e = TRUE WHERE gen_key = ?", id);
    }

    private String resolveSortColumn(String sortBy) {
        if (sortBy == null) return "g.gen_nome";
        return switch (sortBy) {
            case "name" -> "g.gen_nome";
            case "table" -> "t.tab_nome";
            case "event" -> "g.gen_evento";
            case "status" -> "g.gen_status";
            case "enabled" -> "g.gen_habilitado";
            case "created_at" -> "g.gen_criado_em";
            default -> "g.gen_nome";
        };
    }
}
