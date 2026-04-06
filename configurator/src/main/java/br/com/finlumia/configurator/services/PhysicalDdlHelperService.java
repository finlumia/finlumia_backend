package br.com.finlumia.configurator.services;

import java.util.Locale;

import br.com.finlumia.configurator.models.ConfiguratorFieldRow;
import br.com.finlumia.shared.exception.FinlumiaException;

public class PhysicalDdlHelperService {

    
    public static String nullSafe(String s) {
        return s != null ? s : "?";
    }

    public static void requireNonBlankIdentifier(String value, String columnLabel) {
        if (value == null || value.isBlank()) {
            throw new FinlumiaException(400, "Dados incompletos", "Valor nulo ou vazio em " + columnLabel + ".");
        }
        if (value.indexOf('\0') >= 0) {
            throw new FinlumiaException(400, "Identificador inválido", "Caracteres inválidos em " + columnLabel + ".");
        }
    }

    public static String quoteIdent(String ident) {
        requireNonBlankIdentifier(ident, "identificador");
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }

    public static String mapConfiguratorTypeToPostgres(ConfiguratorFieldRow f) {
        String raw = f.getDataType() == null ? "" : f.getDataType().trim();
        String u = raw.toUpperCase(Locale.ROOT);
        Integer len = f.getFieldLength();
        Integer prec = f.getFieldPrecision();
        Integer scale = f.getFieldScale();

        return switch (u) {
            case "VARCHAR", "VARCHAR2", "CHARACTER VARYING" ->
                    "VARCHAR(" + (len != null && len > 0 ? len : 255) + ")";
            case "CHAR", "CHARACTER", "NCHAR" ->
                    "CHAR(" + (len != null && len > 0 ? len : 1) + ")";
            case "TEXT", "STRING", "CLOB" -> "TEXT";
            case "INTEGER", "INT", "INT4" -> "INTEGER";
            case "BIGINT", "INT8" -> "BIGINT";
            case "SMALLINT", "INT2" -> "SMALLINT";
            case "SERIAL" -> "SERIAL";
            case "BIGSERIAL" -> "BIGSERIAL";
            case "NUMERIC", "DECIMAL" -> {
                if (prec != null && prec > 0 && scale != null && scale >= 0) {
                    yield "NUMERIC(" + prec + "," + scale + ")";
                }
                if (prec != null && prec > 0) {
                    yield "NUMERIC(" + prec + ")";
                }
                yield "NUMERIC";
            }
            case "REAL", "FLOAT4" -> "REAL";
            case "FLOAT", "FLOAT8", "DOUBLE", "DOUBLE PRECISION" -> "DOUBLE PRECISION";
            case "BOOLEAN", "BOOL" -> "BOOLEAN";
            case "DATE" -> "DATE";
            case "TIME" -> "TIME";
            case "TIMESTAMP", "DATETIME" -> "TIMESTAMP";
            case "TIMESTAMPTZ" -> "TIMESTAMP WITH TIME ZONE";
            case "UUID" -> "UUID";
            case "JSON" -> "JSON";
            case "JSONB" -> "JSONB";
            case "BYTEA", "BLOB" -> "BYTEA";
            default -> "TEXT";
        };
    }

    public static String formatDefaultClause(String pgType, String rawDefault) {
        if (rawDefault == null || rawDefault.isBlank()) {
            return null;
        }
        if (rawDefault.contains(";") || rawDefault.contains("--")) {
            return null;
        }
        String t = pgType.toUpperCase(Locale.ROOT);
        if (t.contains("SERIAL")) {
            return null;
        }
        String d = rawDefault.trim();
        if (t.contains("INT") || t.equals("SMALLINT") || t.equals("BIGINT")) {
            if (d.matches("^-?\\d+$")) {
                return " DEFAULT " + d;
            }
        }
        if (t.contains("NUMERIC") || t.contains("DECIMAL") || t.equals("REAL")
                || t.contains("DOUBLE")) {
            if (d.matches("^-?\\d+(\\.\\d+)?([Ee][+-]?\\d+)?$")) {
                return " DEFAULT " + d;
            }
        }
        if (t.equals("BOOLEAN")) {
            if (d.equalsIgnoreCase("true") || d.equalsIgnoreCase("false")) {
                return " DEFAULT " + d.toUpperCase(Locale.ROOT);
            }
        }
        return " DEFAULT '" + d.replace("'", "''") + "'";
    }

    /** Constraint nomeada para uso em {@code CREATE TABLE}. */
    public static String buildFkClause(String tableSchema, String tableName, ConfiguratorFieldRow f,
            String quotedCol) {
        String ref = f.getFkReferenceTable().trim();
        String refSchema = tableSchema;
        String refTable = ref;
        int dot = ref.indexOf('.');
        if (dot > 0) {
            refSchema = ref.substring(0, dot).trim();
            refTable = ref.substring(dot + 1).trim();
        }
        String refCol = f.getFkReferenceColumn().trim();
        String cname = "fk_" + tableName + "_" + f.getFieldName();
        if (cname.length() > 63) {
            cname = cname.substring(0, 63);
        }
        return "CONSTRAINT " + quoteIdent(cname) + " FOREIGN KEY (" + quotedCol + ") REFERENCES "
                + quoteIdent(refSchema) + "." + quoteIdent(refTable)
                + " (" + quoteIdent(refCol) + ")";
    }

    /** Cláusula {@code REFERENCES} inline para {@code ALTER TABLE ... ADD COLUMN}. */
    public static String buildInlineReferencesClause(ConfiguratorFieldRow f, String tableSchema) {
        if (!f.isForeignKey() || f.getFkReferenceTable() == null || f.getFkReferenceTable().isBlank()
                || f.getFkReferenceColumn() == null || f.getFkReferenceColumn().isBlank()) {
            return "";
        }
        String ref = f.getFkReferenceTable().trim();
        String refSchema = tableSchema;
        String refTable = ref;
        int dot = ref.indexOf('.');
        if (dot > 0) {
            refSchema = ref.substring(0, dot).trim();
            refTable = ref.substring(dot + 1).trim();
        }
        return " REFERENCES " + quoteIdent(refSchema) + "." + quoteIdent(refTable)
                + " (" + quoteIdent(f.getFkReferenceColumn().trim()) + ")";
    }
}
