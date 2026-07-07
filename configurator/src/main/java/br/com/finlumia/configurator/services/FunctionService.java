package br.com.finlumia.configurator.services;

import br.com.finlumia.configurator.models.CreateFunctionRequest;
import br.com.finlumia.configurator.models.LanguageEnum;
import br.com.finlumia.configurator.models.SchemaEnum;
import br.com.finlumia.configurator.models.StatusEnum;
import br.com.finlumia.configurator.models.TestFunctionRequest;
import br.com.finlumia.configurator.models.UpdateFunctionRequest;
import br.com.finlumia.configurator.models.VolatilityEnum;
import br.com.finlumia.configurator.views.DbFunctionResponse;
import br.com.finlumia.configurator.views.PagedResponse;
import br.com.finlumia.configurator.views.PaginationMeta;
import br.com.finlumia.configurator.views.TestFunctionResponse;
import br.com.finlumia.shared.exception.FinlumiaException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FunctionService {

    private final JdbcTemplate jdbc;

    public FunctionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<DbFunctionResponse> ROW_MAPPER = (rs, rowNum) -> {
        String schemaStr = rs.getString("fun_schema");
        SchemaEnum schema = null;
        if (schemaStr != null) {
            try { schema = SchemaEnum.valueOf(schemaStr.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        String langStr = rs.getString("fun_linguagem");
        LanguageEnum language = null;
        if (langStr != null) {
            try { language = LanguageEnum.valueOf(langStr); } catch (IllegalArgumentException ignored) {}
        }

        String volStr = rs.getString("fun_volatilidade");
        VolatilityEnum volatility = null;
        if (volStr != null) {
            try { volatility = VolatilityEnum.valueOf(volStr.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }

        String statusStr = rs.getString("fun_status");
        StatusEnum status = null;
        if (statusStr != null) {
            try { status = StatusEnum.valueOf(statusStr); } catch (IllegalArgumentException ignored) {}
        }

        return new DbFunctionResponse(
                UUID.fromString(rs.getString("fun_key")),
                rs.getString("fun_nome"),
                schema,
                language,
                rs.getString("fun_tipo_retorno"),
                rs.getString("fun_args"),
                volatility,
                rs.getString("fun_corpo"),
                rs.getString("fun_descricao"),
                status,
                rs.getTimestamp("fun_criado_em").toInstant(),
                rs.getTimestamp("fun_atualizado_em").toInstant());
    };

    public PagedResponse<DbFunctionResponse> list(int page, int pageSize, String search, StatusEnum status,
                                                  String sortBy, String sortOrder, SchemaEnum schema, LanguageEnum language) {
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(200, pageSize));
        int offset = (page - 1) * pageSize;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE f.d_e_l_e_t_e = FALSE");

        if (search != null && !search.isBlank()) {
            where.append(" AND f.fun_nome ILIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (status != null) {
            where.append(" AND f.fun_status = ?");
            params.add(status.name());
        }
        if (schema != null) {
            where.append(" AND f.fun_schema = ?");
            params.add(schema.name().toLowerCase());
        }
        if (language != null) {
            where.append(" AND f.fun_linguagem = ?");
            params.add(language.name());
        }

        String orderCol = resolveSortColumn(sortBy);
        String orderDir = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        String countSql = "SELECT COUNT(*) FROM configurator.\"FUN\" f " + where;
        long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        String dataSql = "SELECT * FROM configurator.\"FUN\" f "
                + where + " ORDER BY " + orderCol + " " + orderDir
                + " LIMIT ? OFFSET ?";

        params.add(pageSize);
        params.add(offset);

        List<DbFunctionResponse> data = jdbc.query(dataSql, ROW_MAPPER, params.toArray());
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PagedResponse<>(data, new PaginationMeta(page, pageSize, total, totalPages));
    }

    public DbFunctionResponse getById(UUID id) {
        String sql = "SELECT * FROM configurator.\"FUN\" WHERE fun_key = ? AND d_e_l_e_t_e = FALSE";
        List<DbFunctionResponse> result = jdbc.query(sql, ROW_MAPPER, id);
        if (result.isEmpty()) throw new FinlumiaException(404, "Nao encontrado", "Funcao nao encontrada.");
        return result.get(0);
    }

    public DbFunctionResponse create(CreateFunctionRequest request) {
        String checkSql = "SELECT COUNT(*) FROM configurator.\"FUN\" WHERE fun_nome = ? AND fun_schema = ? AND d_e_l_e_t_e = FALSE";
        long count = jdbc.queryForObject(checkSql, Long.class, request.name(), request.schema().name().toLowerCase());
        if (count > 0) throw new FinlumiaException(409, "Conflito", "Funcao ja existe neste schema.");

        StatusEnum status = request.status() != null ? request.status() : StatusEnum.ativo;
        String sql = """
                INSERT INTO configurator."FUN"
                    (fun_nome, fun_schema, fun_linguagem, fun_tipo_retorno, fun_args,
                     fun_volatilidade, fun_corpo, fun_descricao, fun_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING fun_key
                """;
        UUID key = jdbc.queryForObject(sql, UUID.class,
                request.name(), request.schema().name().toLowerCase(), request.language().name(),
                request.returnType(), request.args(), request.volatility().name(),
                request.body(), request.description(), status.name());
        return getById(key);
    }

    public DbFunctionResponse update(UUID id, UpdateFunctionRequest request) {
        getById(id);
        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();

        if (request.name() != null) { sets.add("fun_nome = ?"); params.add(request.name()); }
        if (request.schema() != null) { sets.add("fun_schema = ?"); params.add(request.schema().name().toLowerCase()); }
        if (request.language() != null) { sets.add("fun_linguagem = ?"); params.add(request.language().name()); }
        if (request.returnType() != null) { sets.add("fun_tipo_retorno = ?"); params.add(request.returnType()); }
        if (request.args() != null) { sets.add("fun_args = ?"); params.add(request.args()); }
        if (request.volatility() != null) { sets.add("fun_volatilidade = ?"); params.add(request.volatility().name()); }
        if (request.body() != null) { sets.add("fun_corpo = ?"); params.add(request.body()); }
        if (request.description() != null) { sets.add("fun_descricao = ?"); params.add(request.description()); }
        if (request.status() != null) { sets.add("fun_status = ?"); params.add(request.status().name()); }

        if (!sets.isEmpty()) {
            sets.add("fun_atualizado_em = NOW()");
            params.add(id);
            jdbc.update("UPDATE configurator.\"FUN\" SET " + String.join(", ", sets) + " WHERE fun_key = ?", params.toArray());
        }
        return getById(id);
    }

    public void delete(UUID id) {
        getById(id);
        long trigCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM configurator.\"GEN\" WHERE gen_funcao = (SELECT fun_nome FROM configurator.\"FUN\" WHERE fun_key = ?) AND d_e_l_e_t_e = FALSE",
                Long.class, id);
        if (trigCount > 0) throw new FinlumiaException(409, "Conflito", "Funcao referenciada por trigger ativo.");
        jdbc.update("UPDATE configurator.\"FUN\" SET d_e_l_e_t_e = TRUE WHERE fun_key = ?", id);
    }

    public TestFunctionResponse test(UUID id, TestFunctionRequest request) {
        DbFunctionResponse fn = getById(id);
        long start = System.currentTimeMillis();
        try {
            String callSql = "SELECT " + fn.schema().name().toLowerCase() + ".\"" + fn.name() + "\"()";
            Object result = jdbc.queryForObject(callSql, Object.class);
            return new TestFunctionResponse(result, System.currentTimeMillis() - start);
        } catch (Exception e) {
            throw new FinlumiaException(400, "Erro na execucao", e.getMessage());
        }
    }

    private String resolveSortColumn(String sortBy) {
        if (sortBy == null) return "f.fun_nome";
        return switch (sortBy) {
            case "name" -> "f.fun_nome";
            case "schema" -> "f.fun_schema";
            case "language" -> "f.fun_linguagem";
            case "status" -> "f.fun_status";
            case "created_at" -> "f.fun_criado_em";
            case "updated_at" -> "f.fun_atualizado_em";
            default -> "f.fun_nome";
        };
    }
}
