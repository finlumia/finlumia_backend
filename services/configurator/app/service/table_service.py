# modules/configurator/tables/service.py

from typing import Optional
from repository import table_repository
from view import (
    operational_response,
    table_response,
    paginated_response,
)


class TablesService:

    def __init__(self, repository: table_repository):
        self.repo = repository

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
    ) -> operational_response:
        try:
            # Regra: não permite duplicidade de schema + table
            already_exists = await self.repo.exists_by_schema_table(
                tab_schema_name, tab_table_name
            )
            if already_exists:
                return error_response(
                    code=409,
                    title='Conflict',
                    message=f'Table "{tab_schema_name}.{tab_table_name}" is already registered'
                )

            record = await self.repo.create(
                tab_schema_name=tab_schema_name,
                tab_table_name=tab_table_name,
                tab_display_name=tab_display_name,
                tab_description=tab_description,
                tab_created_by=tab_created_by
            )

            return success_response(
                code=201,
                title='Created',
                message='Table registered successfully',
                data=TablesResponse(**record)
            )

        except Exception as e:
            return error_response(
                code=500,
                title='Internal Server Error',
                message=f'Unexpected error while creating table: {str(e)}'
            )

    # ----------------------------------------------------------
    # GET BY KEY
    # ----------------------------------------------------------
    async def get_by_key(self, k_e_y: int) -> operational_response:
        try:
            if k_e_y <= 0:
                return error_response(
                    code=400,
                    title='Bad Request',
                    message='k_e_y must be a positive integer'
                )

            record = await self.repo.get_by_key(k_e_y)

            if not record:
                return error_response(
                    code=404,
                    title='Not Found',
                    message=f'Table with k_e_y {k_e_y} not found'
                )

            return success_response(
                code=200,
                title='Success',
                message='Table retrieved successfully',
                data=TablesResponse(**record)
            )

        except Exception as e:
            return error_response(
                code=500,
                title='Internal Server Error',
                message=f'Unexpected error while retrieving table: {str(e)}'
            )

    # ----------------------------------------------------------
    # LIST
    # ----------------------------------------------------------
    async def list_all(
        self,
        search: Optional[str],
        d_e_l_e_t_e: Optional[bool],
        l_o_c_k: Optional[bool],
        page: int,
        page_size: int
    ) -> PaginatedResponse:
        try:
            offset = (page - 1) * page_size

            records, total = await self.repo.find_all(
                search=search,
                d_e_l_e_t_e=d_e_l_e_t_e,
                l_o_c_k=l_o_c_k,
                offset=offset,
                limit=page_size
            )

            data = [TablesResponse(**r) for r in records]

            return paginated_response(
                data=data,
                total=total,
                page=page,
                page_size=page_size,
                message='Tables retrieved successfully'
            )

        except Exception as e:
            return PaginatedResponse(
                code=500,
                title='Internal Server Error',
                message=f'Unexpected error while listing tables: {str(e)}',
                data=None,
                total=0,
                page=page,
                page_size=page_size,
                total_pages=0
            )

    # ----------------------------------------------------------
    # UPDATE
    # ----------------------------------------------------------
    async def update(
        self,
        k_e_y: int,
        tab_display_name: Optional[str],
        tab_description: Optional[str],
        tab_updated_by: Optional[int]
    ) -> OperationResponse:
        try:
            if k_e_y <= 0:
                return error_response(
                    code=400,
                    title='Bad Request',
                    message='k_e_y must be a positive integer'
                )

            # Verifica existência
            existing = await self.repo.get_by_key(k_e_y)
            if not existing:
                return error_response(
                    code=404,
                    title='Not Found',
                    message=f'Table with k_e_y {k_e_y} not found'
                )

            # Regra: bloqueado não pode ser editado
            if existing['l_o_c_k']:
                return error_response(
                    code=423,
                    title='Locked',
                    message='This table is locked and cannot be edited'
                )

            # Regra: deletado não pode ser editado
            if existing['d_e_l_e_t_e']:
                return error_response(
                    code=410,
                    title='Gone',
                    message='This table has been deleted and cannot be edited'
                )

            record = await self.repo.update(
                k_e_y=k_e_y,
                tab_display_name=tab_display_name,
                tab_description=tab_description,
                tab_updated_by=tab_updated_by
            )

            if not record:
                return error_response(
                    code=422,
                    title='Unprocessable',
                    message='Update could not be applied — record may be locked or deleted'
                )

            return success_response(
                code=200,
                title='Updated',
                message='Table updated successfully',
                data=TablesResponse(**record)
            )

        except Exception as e:
            return error_response(
                code=500,
                title='Internal Server Error',
                message=f'Unexpected error while updating table: {str(e)}'
            )

    # ----------------------------------------------------------
    # SOFT DELETE
    # ----------------------------------------------------------
    async def delete(
        self,
        k_e_y: int,
        tab_updated_by: Optional[int]
    ) -> OperationResponse:
        try:
            if k_e_y <= 0:
                return error_response(
                    code=400,
                    title='Bad Request',
                    message='k_e_y must be a positive integer'
                )

            existing = await self.repo.get_by_key(k_e_y)
            if not existing:
                return error_response(
                    code=404,
                    title='Not Found',
                    message=f'Table with k_e_y {k_e_y} not found'
                )

            # Regra: bloqueado não pode ser deletado
            if existing['l_o_c_k']:
                return error_response(
                    code=423,
                    title='Locked',
                    message='This table is locked and cannot be deleted'
                )

            # Regra: já deletado
            if existing['d_e_l_e_t_e']:
                return error_response(
                    code=410,
                    title='Gone',
                    message='This table has already been deleted'
                )

            record = await self.repo.soft_delete(
                k_e_y=k_e_y,
                tab_updated_by=tab_updated_by
            )

            return success_response(
                code=200,
                title='Deleted',
                message='Table deleted successfully',
                data=TablesResponse(**record)
            )

        except Exception as e:
            return error_response(
                code=500,
                title='Internal Server Error',
                message=f'Unexpected error while deleting table: {str(e)}'
            )

    # ----------------------------------------------------------
    # LOCK / UNLOCK
    # ----------------------------------------------------------
    async def set_lock(
        self,
        k_e_y: int,
        lock_value: bool,
        tab_updated_by: Optional[int]
    ) -> OperationResponse:
        try:
            if k_e_y <= 0:
                return error_response(
                    code=400,
                    title='Bad Request',
                    message='k_e_y must be a positive integer'
                )

            existing = await self.repo.get_by_key(k_e_y)
            if not existing:
                return error_response(
                    code=404,
                    title='Not Found',
                    message=f'Table with k_e_y {k_e_y} not found'
                )

            if existing['d_e_l_e_t_e']:
                return error_response(
                    code=410,
                    title='Gone',
                    message='Cannot lock/unlock a deleted table'
                )

            record = await self.repo.set_lock(
                k_e_y=k_e_y,
                lock_value=lock_value,
                tab_updated_by=tab_updated_by
            )

            action = 'locked' if lock_value else 'unlocked'

            return success_response(
                code=200,
                title='Success',
                message=f'Table {action} successfully',
                data=TablesResponse(**record)
            )

        except Exception as e:
            return error_response(
                code=500,
                title='Internal Server Error',
                message=f'Unexpected error while updating lock: {str(e)}'
            )