# modules/configurator/tables/repository.py

from typing import Optional, List, Tuple
import asyncpg


class TablesRepository:

    def __init__(self, connection: asyncpg.Connection):
        self.conn = connection

    # ----------------------------------------------------------
    # CREATE
    # ----------------------------------------------------------
    async def create(
        self,
        tab_schema_name: str,
        tab_table_name: str,
        tab_display_name: str,
        tab_description: Optional[str],
        tab_created_by: Optional[int]
    ) -> dict:
        sql = """
            INSERT INTO configurator.tables (
                l_o_c_k,
                d_e_l_e_t_e,
                tab_schema_name,
                tab_table_name,
                tab_display_name,
                tab_description,
                tab_created_by,
                tab_updated_by
            )
            VALUES (
                FALSE,
                FALSE,
                $1,
                $2,
                $3,
                $4,
                $5,
                NULL
            )
            RETURNING
                k_e_y,
                l_o_c_k,
                d_e_l_e_t_e,
                tab_schema_name,
                tab_table_name,
                tab_display_name,
                tab_description,
                tab_created_by,
                tab_updated_by
        """
        row = await self.conn.fetchrow(
            sql,
            tab_schema_name,
            tab_table_name,
            tab_display_name,
            tab_description,
            tab_created_by
        )
        return dict(row)

    # ----------------------------------------------------------
    # READ — busca por k_e_y
    # ----------------------------------------------------------
    async def get_by_key(self, k_e_y: int) -> Optional[dict]:
        sql = """
            SELECT
                k_e_y,
                l_o_c_k,
                d_e_l_e_t_e,
                tab_schema_name,
                tab_table_name,
                tab_display_name,
                tab_description,
                tab_created_by,
                tab_updated_by
            FROM configurator.tables
            WHERE k_e_y = $1
        """
        row = await self.conn.fetchrow(sql, k_e_y)
        return dict(row) if row else None

    # ----------------------------------------------------------
    # READ — verifica duplicidade de schema + table
    # ----------------------------------------------------------
    async def exists_by_schema_table(
        self,
        tab_schema_name: str,
        tab_table_name: str,
        exclude_key: Optional[int] = None
    ) -> bool:
        sql = """
            SELECT 1
            FROM configurator.tables
            WHERE tab_schema_name = $1
              AND tab_table_name  = $2
              AND d_e_l_e_t_e     = FALSE
              AND ($3::INTEGER IS NULL OR k_e_y != $3)
            LIMIT 1
        """
        row = await self.conn.fetchrow(sql, tab_schema_name, tab_table_name, exclude_key)
        return row is not None

    # ----------------------------------------------------------
    # READ — listagem paginada
    # ----------------------------------------------------------
    async def find_all(
        self,
        search: Optional[str],
        d_e_l_e_t_e: Optional[bool],
        l_o_c_k: Optional[bool],
        offset: int,
        limit: int
    ) -> Tuple[List[dict], int]:

        filters = []
        params = []
        idx = 1

        if d_e_l_e_t_e is not None:
            filters.append(f'd_e_l_e_t_e = ${idx}')
            params.append(d_e_l_e_t_e)
            idx += 1

        if l_o_c_k is not None:
            filters.append(f'l_o_c_k = ${idx}')
            params.append(l_o_c_k)
            idx += 1

        if search:
            filters.append(
                f'(tab_display_name ILIKE ${idx} OR tab_table_name ILIKE ${idx})'
            )
            params.append(f'%{search}%')
            idx += 1

        where_clause = ('WHERE ' + ' AND '.join(filters)) if filters else ''

        count_sql = f"""
            SELECT COUNT(*) FROM configurator.tables
            {where_clause}
        """

        data_sql = f"""
            SELECT
                k_e_y,
                l_o_c_k,
                d_e_l_e_t_e,
                tab_schema_name,
                tab_table_name,
                tab_display_name,
                tab_description,
                tab_created_by,
                tab_updated_by
            FROM configurator.tables
            {where_clause}
            ORDER BY k_e_y ASC
            LIMIT ${idx} OFFSET ${idx + 1}
        """
        params_data = params + [limit, offset]

        total = await self.conn.fetchval(count_sql, *params)
        rows  = await self.conn.fetch(data_sql, *params_data)

        return [dict(r) for r in rows], total

    # ----------------------------------------------------------
    # UPDATE
    # ----------------------------------------------------------
    async def update(
        self,
        k_e_y: int,
        tab_display_name: Optional[str],
        tab_description: Optional[str],
        tab_updated_by: Optional[int]
    ) -> Optional[dict]:

        fields = []
        params = []
        idx = 1

        if tab_display_name is not None:
            fields.append(f'tab_display_name = ${idx}')
            params.append(tab_display_name)
            idx += 1

        if tab_description is not None:
            fields.append(f'tab_description = ${idx}')
            params.append(tab_description)
            idx += 1

        fields.append(f'tab_updated_by = ${idx}')
        params.append(tab_updated_by)
        idx += 1

        params.append(k_e_y)

        sql = f"""
            UPDATE configurator.tables
            SET {', '.join(fields)}
            WHERE k_e_y = ${idx}
              AND l_o_c_k   = FALSE
              AND d_e_l_e_t_e = FALSE
            RETURNING
                k_e_y,
                l_o_c_k,
                d_e_l_e_t_e,
                tab_schema_name,
                tab_table_name,
                tab_display_name,
                tab_description,
                tab_created_by,
                tab_updated_by
        """
        row = await self.conn.fetchrow(sql, *params)
        return dict(row) if row else None

    # ----------------------------------------------------------
    # DELETE (soft)
    # ----------------------------------------------------------
    async def soft_delete(
        self,
        k_e_y: int,
        tab_updated_by: Optional[int]
    ) -> Optional[dict]:
        sql = """
            UPDATE configurator.tables
            SET
                d_e_l_e_t_e   = TRUE,
                tab_updated_by = $2
            WHERE k_e_y     = $1
              AND l_o_c_k   = FALSE
              AND d_e_l_e_t_e = FALSE
            RETURNING
                k_e_y,
                l_o_c_k,
                d_e_l_e_t_e,
                tab_schema_name,
                tab_table_name,
                tab_display_name,
                tab_description,
                tab_created_by,
                tab_updated_by
        """
        row = await self.conn.fetchrow(sql, k_e_y, tab_updated_by)
        return dict(row) if row else None

    # ----------------------------------------------------------
    # LOCK toggle
    # ----------------------------------------------------------
    async def set_lock(
        self,
        k_e_y: int,
        lock_value: bool,
        tab_updated_by: Optional[int]
    ) -> Optional[dict]:
        sql = """
            UPDATE configurator.tables
            SET
                l_o_c_k       = $2,
                tab_updated_by = $3
            WHERE k_e_y       = $1
              AND d_e_l_e_t_e = FALSE
            RETURNING
                k_e_y,
                l_o_c_k,
                d_e_l_e_t_e,
                tab_schema_name,
                tab_table_name,
                tab_display_name,
                tab_description,
                tab_created_by,
                tab_updated_by
        """
        row = await self.conn.fetchrow(sql, k_e_y, lock_value, tab_updated_by)
        return dict(row) if row else None