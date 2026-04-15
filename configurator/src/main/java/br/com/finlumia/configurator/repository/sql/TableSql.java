package br.com.finlumia.configurator.repository.sql;

public class TableSql {

    private TableSql() {
    }

    public static final String INSERT_TABLE = """
            insert into configurator."tables" (
                l_o_c_k,
                d_e_l_e_t_e ,
                tab_schema_name,
                tab_table_name,
                tab_display_name,
                tab_description ,
                tab_created_by,
                tab_updated_by,
                tab_create_tables
            )values(
                ?,--l_o_c_k,
                ?,--d_e_l_e_t_e ,
                ?,--tab_schema_name,
                ?,--tab_table_name,
                ?,--tab_display_name,
                ?,--tab_description ,
                ?,--tab_created_by,
                ?,--tab_updated_by,
                ?--tab_create_tables
            ) ON CONFLICT (tab_schema_name, tab_table_name) WHERE (tab_create_tables = true or tab_create_tables = false)
            DO UPDATE SET
                tab_display_name = EXCLUDED.tab_display_name,
                tab_description = EXCLUDED.tab_description,
                tab_updated_by = EXCLUDED.tab_updated_by
                """;

    public static final String UPDATE_TABLE = """
                UPDATE configurator."tables"
                SET
                    tab_display_name = ?,
                    tab_description  = ?,
                    tab_updated_by   = ?
                WHERE k_e_y = ?
            """;

    public static final String DELETE_TABLE = """
               UPDATE configurator."tables"
                SET
                    d_e_l_e_t_e = true,
                WHERE k_e_y = ?
            """;

    public static final String SEARCH_TABLES_TO_CREATE = """
               select
                    tab_schema_name,
                    tab_table_name
                from configurator."tables" t
                    where t.tab_create_tables = false
                    and t.k_e_y = ?
            """;

}
