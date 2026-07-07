package br.com.finlumia.configurator.services;

import br.com.finlumia.configurator.models.CreateFieldRequest;
import br.com.finlumia.configurator.models.DataTypeEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.UpdateFieldRequest;
import br.com.finlumia.configurator.views.FieldResponse;
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
public class FieldService {

    private final JdbcTemplate jdbc;

    public FieldService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<FieldResponse> ROW_MAPPER = (rs, rowNum) -> {
        String dataTypeStr = rs.getString("fie_tipo_dado");
        DataTypeEnum dataType = null;
        if (dataTypeStr != null) {
            try { dataType = DataTypeEnum.valueOf(dataTypeStr); } catch (IllegalArgumentException ignored) {}
        }

        String statusStr = rs.getString("fie_status");
        StatusEnum status = null;
        if (statusStr != null) {
            try { status = StatusEnum.valueOf(statusStr); } catch (IllegalArgumentException ignored) {}
        }

        return new FieldResponse(
                UUID.fromString(rs.getString("fie_key")),
                rs.getString("fie_nome"),
                UUID.fromString(rs.getString("fie_tab_key")),
                rs.getString("tab_nome"),
                dataType,
                rs.getObject("fie_tamanho") != null ? rs.getInt("fie_tamanho") : null,
                rs.getBoolean("fie_nulo"),
                rs.getString("fie_default"),
                rs.getBoolean("fie_e_primario"),
                rs.getBoolean("fie_e_estrangeiro"),
                rs.getString("fie_ref_tabela"),
                rs.getString("fie_ref_campo"),
                status,
                rs.getTimestamp("fie_criado_em").toInstant());
    };

    public PagedResponse<FieldResponse> list(int page, int pageSize, String search, StatusEnum status,
                                             String sortBy, String sortOrder, UUID tableId) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(200, pageSize));
        int offset = (page - 1) * pageSize;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE f.d_e_l_e_t_e = FALSE");

        if (search != null && !search.isBlank()) {
            where.append(" AND f.fie_nome ILIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (status != null) {
            where.append(" AND f.fie_status = ?");
            params.add(status.name());
        }
        if (tableId != null) {
            where.append(" AND f.fie_tab_key = ?");
            params.add(tableId);
        }

        String orderCol = resolveSortColumn(sortBy);
        String orderDir = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        String countSql = "SELECT COUNT(*) FROM configurator.\"FIE\" f " + where;
        long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        String dataSql = """
                SELECT f.fie_key, f.fie_nome, f.fie_tab_key, t.tab_nome,
                       f.fie_tipo_dado, f.fie_tamanho, f.fie_nulo, f.fie_default,
                       f.fie_e_primario, f.fie_e_estrangeiro, f.fie_ref_tabela,
                       f.fie_ref_campo, f.fie_status, f.fie_criado_em
                FROM configurator."FIE" f
                JOIN configurator."TAB" t ON t.tab_key = f.fie_tab_key
                """ + where + " ORDER BY " + orderCol + " " + orderDir
                + " LIMIT ? OFFSET ?";

        params.add(pageSize);
        params.add(offset);

        List<FieldResponse> data = jdbc.query(dataSql, ROW_MAPPER, params.toArray());
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PagedResponse<>(data, new PaginationMeta(page, pageSize, total, totalPages));
    }

    public FieldResponse getById(UUID id) {
        String sql = """
                SELECT f.fie_key, f.fie_nome, f.fie_tab_key, t.tab_nome,
                       f.fie_tipo_dado, f.fie_tamanho, f.fie_nulo, f.fie_default,
                       f.fie_e_primario, f.fie_e_estrangeiro, f.fie_ref_tabela,
                       f.fie_ref_campo, f.fie_status, f.fie_criado_em
                FROM configurator."FIE" f
                JOIN configurator."TAB" t ON t.tab_key = f.fie_tab_key
                WHERE f.fie_key = ? AND f.d_e_l_e_t_e = FALSE
                """;
        List<FieldResponse> result = jdbc.query(sql, ROW_MAPPER, id);
        if (result.isEmpty()) throw new FinlumiaException(404, "Nao encontrado", "Campo nao encontrado.");
        return result.get(0);
    }

    public FieldResponse create(CreateFieldRequest request) {
        String checkSql = "SELECT COUNT(*) FROM configurator.\"FIE\" WHERE fie_nome = ? AND fie_tab_key = ? AND d_e_l_e_t_e = FALSE";
        long count = jdbc.queryForObject(checkSql, Long.class, request.name(), request.tableId());
        if (count > 0) throw new FinlumiaException(409, "Conflito", "Campo ja existe nesta tabela.");

        StatusEnum status = request.status() != null ? request.status() : StatusEnum.ativo;
        boolean nullable = request.nullable() == null || request.nullable();
        boolean isPrimary = Boolean.TRUE.equals(request.isPrimary());
        boolean isForeign = Boolean.TRUE.equals(request.isForeign());

        String sql = """
                INSERT INTO configurator."FIE"
                    (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_default,
                     fie_e_primario, fie_e_estrangeiro, fie_ref_tabela, fie_ref_campo, fie_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING fie_key
                """;
        UUID key = jdbc.queryForObject(sql, UUID.class,
                request.name(), request.tableId(), request.dataType().name(),
                request.length(), nullable, request.defaultValue(),
                isPrimary, isForeign, request.referencesTable(), request.referencesField(),
                status.name());
        return getById(key);
    }

    public FieldResponse update(UUID id, UpdateFieldRequest request) {
        getById(id);
        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();

        if (request.name() != null) { sets.add("fie_nome = ?"); params.add(request.name()); }
        if (request.tableId() != null) { sets.add("fie_tab_key = ?"); params.add(request.tableId()); }
        if (request.dataType() != null) { sets.add("fie_tipo_dado = ?"); params.add(request.dataType().name()); }
        if (request.length() != null) { sets.add("fie_tamanho = ?"); params.add(request.length()); }
        if (request.nullable() != null) { sets.add("fie_nulo = ?"); params.add(request.nullable()); }
        if (request.defaultValue() != null) { sets.add("fie_default = ?"); params.add(request.defaultValue()); }
        if (request.isPrimary() != null) { sets.add("fie_e_primario = ?"); params.add(request.isPrimary()); }
        if (request.isForeign() != null) { sets.add("fie_e_estrangeiro = ?"); params.add(request.isForeign()); }
        if (request.referencesTable() != null) { sets.add("fie_ref_tabela = ?"); params.add(request.referencesTable()); }
        if (request.referencesField() != null) { sets.add("fie_ref_campo = ?"); params.add(request.referencesField()); }
        if (request.status() != null) { sets.add("fie_status = ?"); params.add(request.status().name()); }

        if (!sets.isEmpty()) {
            params.add(id);
            jdbc.update("UPDATE configurator.\"FIE\" SET " + String.join(", ", sets) + " WHERE fie_key = ?", params.toArray());
        }
        return getById(id);
    }

    public void delete(UUID id) {
        getById(id);
        jdbc.update("UPDATE configurator.\"FIE\" SET d_e_l_e_t_e = TRUE WHERE fie_key = ?", id);
    }

    private String resolveSortColumn(String sortBy) {
        if (sortBy == null) return "f.fie_nome";
        return switch (sortBy) {
            case "name" -> "f.fie_nome";
            case "table" -> "t.tab_nome";
            case "data_type" -> "f.fie_tipo_dado";
            case "status" -> "f.fie_status";
            case "created_at" -> "f.fie_criado_em";
            default -> "f.fie_nome";
        };
    }
}
