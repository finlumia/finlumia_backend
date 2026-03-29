# modules/configurator/tables/controller.py

from fastapi import APIRouter, Depends, Request
from slowapi import Limiter
from slowapi.util import get_remote_address
from asyncpg import Connection

from modules.configurator.tables.schemas import (
    TablesCreateRequest,
    TablesUpdateRequest,
    TablesFilterRequest,
)
from modules.configurator.tables.service import TablesService
from modules.configurator.tables.repository import TablesRepository
from modules.configurator.tables.responses import OperationResponse, PaginatedResponse
from core.database import get_connection

router = APIRouter(prefix='/configurator/tables', tags=['Configurator — Tables'])
limiter = Limiter(key_func=get_remote_address)


def get_service(conn: Connection = Depends(get_connection)) -> TablesService:
    repository = TablesRepository(conn)
    return TablesService(repository)


# ----------------------------------------------------------
# POST — CREATE
# ----------------------------------------------------------
@router.post('/', response_model=OperationResponse, status_code=201)
@limiter.limit('30/minute')
async def create_table(
    request: Request,
    body: TablesCreateRequest,
    service: TablesService = Depends(get_service)
) -> OperationResponse:
    async with request.app.state.db_pool.acquire() as conn:
        async with conn.transaction():
            service.repo.conn = conn
            return await service.create(
                tab_schema_name=body.tab_schema_name,
                tab_table_name=body.tab_table_name,
                tab_display_name=body.tab_display_name,
                tab_description=body.tab_description,
                tab_created_by=body.tab_created_by
            )


# ----------------------------------------------------------
# GET — LIST
# ----------------------------------------------------------
@router.get('/', response_model=PaginatedResponse, status_code=200)
@limiter.limit('60/minute')
async def list_tables(
    request: Request,
    filters: TablesFilterRequest = Depends(),
    service: TablesService = Depends(get_service)
) -> PaginatedResponse:
    return await service.list_all(
        search=filters.search,
        d_e_l_e_t_e=filters.d_e_l_e_t_e,
        l_o_c_k=filters.l_o_c_k,
        page=filters.page,
        page_size=filters.page_size
    )


# ----------------------------------------------------------
# GET — DETAIL
# ----------------------------------------------------------
@router.get('/{k_e_y}', response_model=OperationResponse, status_code=200)
@limiter.limit('60/minute')
async def get_table(
    request: Request,
    k_e_y: int,
    service: TablesService = Depends(get_service)
) -> OperationResponse:
    if k_e_y <= 0:
        from modules.configurator.tables.responses import error_response
        return error_response(code=400, title='Bad Request', message='k_e_y must be a positive integer')

    return await service.get_by_key(k_e_y)


# ----------------------------------------------------------
# PATCH — UPDATE
# ----------------------------------------------------------
@router.patch('/{k_e_y}', response_model=OperationResponse, status_code=200)
@limiter.limit('30/minute')
async def update_table(
    request: Request,
    k_e_y: int,
    body: TablesUpdateRequest,
    service: TablesService = Depends(get_service)
) -> OperationResponse:
    async with request.app.state.db_pool.acquire() as conn:
        async with conn.transaction():
            service.repo.conn = conn
            return await service.update(
                k_e_y=k_e_y,
                tab_display_name=body.tab_display_name,
                tab_description=body.tab_description,
                tab_updated_by=body.tab_updated_by
            )


# ----------------------------------------------------------
# DELETE — SOFT DELETE
# ----------------------------------------------------------
@router.delete('/{k_e_y}', response_model=OperationResponse, status_code=200)
@limiter.limit('20/minute')
async def delete_table(
    request: Request,
    k_e_y: int,
    tab_updated_by: Optional[int] = None,
    service: TablesService = Depends(get_service)
) -> OperationResponse:
    async with request.app.state.db_pool.acquire() as conn:
        async with conn.transaction():
            service.repo.conn = conn
            return await service.delete(
                k_e_y=k_e_y,
                tab_updated_by=tab_updated_by
            )


# ----------------------------------------------------------
# PATCH — LOCK / UNLOCK
# ----------------------------------------------------------
@router.patch('/{k_e_y}/lock', response_model=OperationResponse, status_code=200)
@limiter.limit('20/minute')
async def lock_table(
    request: Request,
    k_e_y: int,
    lock_value: bool,
    tab_updated_by: Optional[int] = None,
    service: TablesService = Depends(get_service)
) -> OperationResponse:
    async with request.app.state.db_pool.acquire() as conn:
        async with conn.transaction():
            service.repo.conn = conn
            return await service.set_lock(
                k_e_y=k_e_y,
                lock_value=lock_value,
                tab_updated_by=tab_updated_by
            )