package br.com.finlumia.configurator.repository.sql;

/**
 * SQL estático do módulo de tabelas do configurador.
 * <p>
 * O <strong>CREATE TABLE físico</strong> (objeto no PostgreSQL) <strong>não</strong> é uma única string
 * fixa aqui: o comando é montado em
 * {@link br.com.finlumia.configurator.services.TableService} a partir de
 * {@link br.com.finlumia.configurator.repository.sql.FieldSql#SELECT_FIELDS_BY_TABLE_KEY},
 * porque colunas, tipos, {@code NOT NULL}, {@code DEFAULT}, PK, FK e índices vêm de
 * {@code configurator.fields}. Os prefixos DDL usados nessa montagem estão em
 * {@link #DDL_CREATE_SCHEMA_IF_NOT_EXISTS}, {@link #DDL_CREATE_TABLE_IF_NOT_EXISTS} e
 * {@link #DDL_CREATE_INDEX_IF_NOT_EXISTS}.
 * </p>
 */
public final class TableSql {

    private TableSql() {
    }

    /** Primeira etapa da criação física: garantir o schema de destino. */
    public static final String DDL_CREATE_SCHEMA_IF_NOT_EXISTS = "CREATE SCHEMA IF NOT EXISTS ";

    /**
     * Início do {@code CREATE TABLE} físico; em seguida vêm {@code "schema"."tabela" ( ... )}.
     * O conteúdo entre parênteses é gerado dinamicamente no serviço.
     */
    public static final String DDL_CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS ";

    /** Índices auxiliares após a tabela; o nome e as colunas são definidos no serviço. */
    public static final String DDL_CREATE_INDEX_IF_NOT_EXISTS = "CREATE INDEX IF NOT EXISTS ";

    public static final String INSERT_TABLE = """
            INSERT INTO configurator.tables (
                l_o_c_k,
                d_e_l_e_t_e,
                tab_schema_name,
                tab_table_name,
                tab_display_name,
                tab_description,
                tab_created_by,
                tab_updated_by,
                tab_create_tables
            ) VALUES (
                ?,  -- 1: l_o_c_k
                ?,  -- 2: d_e_l_e_t_e
                ?,  -- 3: tab_schema_name
                ?,  -- 4: tab_table_name
                ?,  -- 5: tab_display_name
                ?,  -- 6: tab_description
                ?,  -- 7: tab_created_by
                ?,   -- 8: tab_updated_by
                ?    -- 9: tab_create_tables
            )
            ON CONFLICT ON CONSTRAINT uq_configurator_tables_schema_table DO NOTHING
            """;

    public static final String UPDATE_TABLE = """
            UPDATE configurator.tables
            SET
                l_o_c_k          = ?,  -- 1: l_o_c_k
                tab_schema_name  = ?,  -- 2: tab_schema_name
                tab_table_name   = ?,  -- 3: tab_table_name
                tab_display_name = ?,  -- 4: tab_display_name
                tab_description  = ?,  -- 5: tab_description
                tab_updated_by   = ?   -- 6: tab_updated_by
            WHERE
                k_e_y       = ?        -- 7: k_e_y
                AND d_e_l_e_t_e = FALSE
                """;

    public static final String DELETE_TABLE = """
                UPDATE configurator.tables
            SET
                d_e_l_e_t_e    = TRUE,
                tab_updated_by = ?   -- 1: tab_updated_by
            WHERE
                k_e_y              = ?    -- 2: k_e_y
                AND d_e_l_e_t_e    = FALSE
                AND tab_create_tables = FALSE
                """;

    /**
     * Lê registros em {@code configurator.tables} que ainda não foram materializados no banco
     * ({@code tab_create_tables = FALSE}), para execução do CREATE TABLE físico.
     */
    public static final String CREATE_TABLE_SELECT_PENDING = """
            SELECT
                k_e_y,
                tab_schema_name,
                tab_table_name,
                tab_display_name
            FROM configurator.tables
            WHERE
                d_e_l_e_t_e = FALSE
                AND l_o_c_k = FALSE
                AND tab_create_tables = FALSE
            ORDER BY k_e_y
            """;

    public static final String UPDATE_TABLE_MARK_PHYSICAL_CREATED = """
            UPDATE configurator.tables
            SET
                tab_create_tables = TRUE,
                tab_updated_by    = ?   -- 1: tab_updated_by
            WHERE
                k_e_y              = ?    -- 2: k_e_y
                AND d_e_l_e_t_e    = FALSE
                AND tab_create_tables = FALSE
            """;
}