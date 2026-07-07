package br.com.finlumia.movement.repositorys;

import java.sql.Array;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.finlumia.movement.models.CategoryId;
import br.com.finlumia.movement.models.FileType;
import br.com.finlumia.movement.models.ImportJob;
import br.com.finlumia.movement.models.ImportStatus;
import br.com.finlumia.movement.models.PaymentMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ImportJobRepository {

    private final JdbcTemplate jdbc;

    public ImportJobRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ImportJob> MAPPER = (rs, rowNum) -> {
        List<String> errors = null;
        Array errorsArr = rs.getArray("errors");
        if (errorsArr != null) {
            errors = Arrays.asList((String[]) errorsArr.getArray());
        }

        String ocrCategoryStr = rs.getString("ocr_category");
        String ocrMethodStr = rs.getString("ocr_method");
        double ocrConf = rs.getDouble("ocr_confidence");
        boolean ocrConfNull = rs.wasNull();

        return new ImportJob(
                rs.getObject("job_id", UUID.class),
                rs.getObject("user_key", UUID.class),
                ImportStatus.fromValue(rs.getString("status")),
                rs.getString("file_name"),
                FileType.fromValue(rs.getString("file_type")),
                rs.getObject("total_rows", Integer.class),
                rs.getObject("imported_rows", Integer.class),
                errors,
                rs.getString("ocr_description"),
                rs.getBigDecimal("ocr_amount"),
                rs.getObject("ocr_date", LocalDate.class),
                ocrCategoryStr != null ? CategoryId.fromValue(ocrCategoryStr) : null,
                ocrMethodStr != null ? PaymentMethod.fromValue(ocrMethodStr) : null,
                ocrConfNull ? null : ocrConf,
                rs.getTimestamp("created_at").toInstant(),
                rs.getBytes("file_content")
        );
    };

    public ImportJob save(UUID id, UUID userKey, String fileName, String fileType) {
        String sql = """
                INSERT INTO movement.import_jobs
                    (job_id, user_key, status, file_name, file_type, created_at, d_e_l_e_t_e)
                VALUES (?, ?, 'pending', ?, ?, NOW(), FALSE)
                """;
        jdbc.update(sql, id, userKey, fileName, fileType);
        return findById(id, userKey).orElseThrow();
    }

    public ImportJob save(UUID id, UUID userKey, String fileName, String fileType, byte[] fileContent) {
        String sql = """
                INSERT INTO movement.import_jobs
                    (job_id, user_key, status, file_name, file_type, file_content, created_at, d_e_l_e_t_e)
                VALUES (?, ?, 'pending', ?, ?, ?, NOW(), FALSE)
                """;
        jdbc.update(sql, id, userKey, fileName, fileType, fileContent);
        return findById(id, userKey).orElseThrow();
    }

    public void clearFileContent(UUID id) {
        jdbc.update("UPDATE movement.import_jobs SET file_content = NULL WHERE job_id = ?", id);
    }

    public Optional<ImportJob> findById(UUID id, UUID userKey) {
        String sql = """
                SELECT * FROM movement.import_jobs
                WHERE job_id = ? AND user_key = ? AND d_e_l_e_t_e = FALSE
                """;
        return jdbc.query(sql, MAPPER, id, userKey).stream().findFirst();
    }

    public void updateStatus(UUID id, ImportStatus status, Integer totalRows, Integer importedRows, List<String> errors) {
        String sql = """
                UPDATE movement.import_jobs
                SET status = ?, total_rows = ?, imported_rows = ?, errors = ?
                WHERE job_id = ? AND d_e_l_e_t_e = FALSE
                """;
        jdbc.update(conn -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1, status.getValue());
            ps.setObject(2, totalRows);
            ps.setObject(3, importedRows);
            ps.setArray(4, errors != null ? conn.createArrayOf("text", errors.toArray(new String[0])) : null);
            ps.setObject(5, id);
            return ps;
        });
    }

    public void updateOcrResult(UUID id, String description, java.math.BigDecimal amount,
                                LocalDate date, CategoryId category, PaymentMethod method, double confidence) {
        String sql = """
                UPDATE movement.import_jobs
                SET status = 'ready', ocr_description = ?, ocr_amount = ?, ocr_date = ?,
                    ocr_category = ?, ocr_method = ?, ocr_confidence = ?
                WHERE job_id = ? AND d_e_l_e_t_e = FALSE
                """;
        jdbc.update(sql, description, amount, date,
                category != null ? category.getValue() : null,
                method != null ? method.getValue() : null,
                confidence, id);
    }
}
