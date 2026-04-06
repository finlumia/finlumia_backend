package br.com.finlumia.configurator.repository.sql;

/**
 * SQL do configurador de campos e consultas auxiliares à sincronização física.
 */
public final class FieldSql {

    private FieldSql() {
    }

    /**
     * Tabelas já criadas no PostgreSQL ({@code tab_create_tables = TRUE}), para aplicar
     * {@code ALTER TABLE ... ADD COLUMN} nos campos definidos no configurador.
     */
    public static final String SELECT_TABLES_FOR_FIELD_PHYSICAL_SYNC = """
            SELECT
                k_e_y,
                tab_schema_name,
                tab_table_name,
                tab_display_name
            FROM configurator.tables
            WHERE
                d_e_l_e_t_e = FALSE
                AND l_o_c_k = FALSE
                AND tab_create_tables = TRUE
            ORDER BY k_e_y
            """;

    /**
     * Verifica se a coluna já existe na tabela física ({@code pg_catalog}).
     */
    public static final String PG_COLUMN_EXISTS = """
            SELECT EXISTS (
                SELECT 1
                FROM pg_catalog.pg_attribute a
                INNER JOIN pg_catalog.pg_class c ON c.oid = a.attrelid
                INNER JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = ?
                  AND c.relname = ?
                  AND a.attname = ?
                  AND a.attnum > 0
                  AND NOT a.attisdropped
            )
            """;

    /**
     * Indica se a tabela física já possui chave primária.
     */
    public static final String PG_TABLE_HAS_PRIMARY_KEY = """
            SELECT EXISTS (
                SELECT 1
                FROM pg_catalog.pg_constraint con
                INNER JOIN pg_catalog.pg_class rel ON rel.oid = con.conrelid
                INNER JOIN pg_catalog.pg_namespace n ON n.oid = rel.relnamespace
                WHERE con.contype = 'p'
                  AND n.nspname = ?
                  AND rel.relname = ?
            )
            """;

    /** Início do comando {@code ALTER TABLE "schema"."tabela" ADD COLUMN ...}. */
    public static final String DDL_ALTER_TABLE = "ALTER TABLE ";

    public static final String INSERT_FIELD = """
            INSERT INTO configurator.fields (
                l_o_c_k,
                d_e_l_e_t_e,
                fie_table_key,
                fie_field_name,
                fie_display_name,
                fie_description,
                fie_data_type,
                fie_field_length,
                fie_field_precision,
                fie_field_scale,
                fie_is_required,
                fie_is_primary_key,
                fie_is_foreign_key,
                fie_fk_reference_table,
                fie_fk_reference_column,
                fie_default_value,
                fie_is_unique,
                fie_is_indexed,
                fie_display_order,
                fie_is_visible,
                fie_is_editable,
                fie_sql_script_depara,
                fie_sql_mask,
                fie_validation_regex,
                fie_metadata,
                fie_created_by,
                fie_updated_by,
                fie_view_habilit
            ) VALUES (
                ?,  -- 1: l_o_c_k
                ?,  -- 2: d_e_l_e_t_e
                ?,  -- 3: fie_table_key
                ?,  -- 4: fie_field_name
                ?,  -- 5: fie_display_name
                ?,  -- 6: fie_description
                ?,  -- 7: fie_data_type
                ?,  -- 8: fie_field_length
                ?,  -- 9: fie_field_precision
                ?,  -- 10: fie_field_scale
                ?,  -- 11: fie_is_required
                ?,  -- 12: fie_is_primary_key
                ?,  -- 13: fie_is_foreign_key
                ?,  -- 14: fie_fk_reference_table
                ?,  -- 15: fie_fk_reference_column
                ?,  -- 16: fie_default_value
                ?,  -- 17: fie_is_unique
                ?,  -- 18: fie_is_indexed
                ?,  -- 19: fie_display_order
                ?,  -- 20: fie_is_visible
                ?,  -- 21: fie_is_editable
                ?,  -- 22: fie_sql_script_depara
                ?,  -- 23: fie_sql_mask
                ?,  -- 24: fie_validation_regex
                ?,  -- 25: fie_metadata (JSONB)
                ?,  -- 26: fie_created_by
                ?,  -- 27: fie_updated_by
                ?   -- 28: fie_view_habilit
            )
            ON CONFLICT ON CONSTRAINT uq_configurator_fields_table_field DO NOTHING
            """;

    public static final String UPDATE_FIELD = """
            UPDATE configurator.fields
            SET
                l_o_c_k                 = ?,  -- 1: l_o_c_k
                fie_table_key           = ?,  -- 2: fie_table_key
                fie_field_name          = ?,  -- 3: fie_field_name
                fie_display_name        = ?,  -- 4: fie_display_name
                fie_description         = ?,  -- 5: fie_description
                fie_data_type           = ?,  -- 6: fie_data_type
                fie_field_length        = ?,  -- 7: fie_field_length
                fie_field_precision     = ?,  -- 8: fie_field_precision
                fie_field_scale         = ?,  -- 9: fie_field_scale
                fie_is_required         = ?,  -- 10: fie_is_required
                fie_is_primary_key      = ?,  -- 11: fie_is_primary_key
                fie_is_foreign_key      = ?,  -- 12: fie_is_foreign_key
                fie_fk_reference_table  = ?,  -- 13: fie_fk_reference_table
                fie_fk_reference_column = ?,  -- 14: fie_fk_reference_column
                fie_default_value       = ?,  -- 15: fie_default_value
                fie_is_unique           = ?,  -- 16: fie_is_unique
                fie_is_indexed          = ?,  -- 17: fie_is_indexed
                fie_display_order       = ?,  -- 18: fie_display_order
                fie_is_visible          = ?,  -- 19: fie_is_visible
                fie_is_editable         = ?,  -- 20: fie_is_editable
                fie_sql_script_depara   = ?,  -- 21: fie_sql_script_depara
                fie_sql_mask            = ?,  -- 22: fie_sql_mask
                fie_validation_regex    = ?,  -- 23: fie_validation_regex
                fie_metadata            = ?,  -- 24: fie_metadata (JSONB)
                fie_updated_by          = ?,  -- 25: fie_updated_by
                fie_view_habilit        = ?   -- 26: fie_view_habilit
            WHERE
                k_e_y = ?                    -- 27: k_e_y
                AND d_e_l_e_t_e = FALSE
            """;

    public static final String DELETE_FIELD = """
            UPDATE configurator.fields
            SET
                d_e_l_e_t_e    = TRUE,
                fie_updated_by = ?   -- 1: fie_updated_by
            WHERE
                k_e_y           = ?   -- 2: k_e_y
                AND d_e_l_e_t_e = FALSE
            """;

    public static final String SELECT_FIELD_BY_KEY = """
            SELECT
                k_e_y,
                l_o_c_k,
                d_e_l_e_t_e,
                fie_table_key,
                fie_field_name,
                fie_display_name,
                fie_description,
                fie_data_type,
                fie_field_length,
                fie_field_precision,
                fie_field_scale,
                fie_is_required,
                fie_is_primary_key,
                fie_is_foreign_key,
                fie_fk_reference_table,
                fie_fk_reference_column,
                fie_default_value,
                fie_is_unique,
                fie_is_indexed,
                fie_display_order,
                fie_is_visible,
                fie_is_editable,
                fie_sql_script_depara,
                fie_sql_mask,
                fie_validation_regex,
                fie_metadata,
                fie_created_by,
                fie_updated_by,
                fie_view_habilit
            FROM configurator.fields
            WHERE
                k_e_y = ?
                AND d_e_l_e_t_e = FALSE
            """;

    public static final String SELECT_FIELDS_BY_TABLE_KEY = """
            SELECT
                k_e_y,
                l_o_c_k,
                d_e_l_e_t_e,
                fie_table_key,
                fie_field_name,
                fie_display_name,
                fie_description,
                fie_data_type,
                fie_field_length,
                fie_field_precision,
                fie_field_scale,
                fie_is_required,
                fie_is_primary_key,
                fie_is_foreign_key,
                fie_fk_reference_table,
                fie_fk_reference_column,
                fie_default_value,
                fie_is_unique,
                fie_is_indexed,
                fie_display_order,
                fie_is_visible,
                fie_is_editable,
                fie_sql_script_depara,
                fie_sql_mask,
                fie_validation_regex,
                fie_metadata,
                fie_created_by,
                fie_updated_by,
                fie_view_habilit
            FROM configurator.fields
            WHERE
                fie_table_key = ?
                AND d_e_l_e_t_e = FALSE
            ORDER BY fie_display_order, k_e_y
            """;
}
