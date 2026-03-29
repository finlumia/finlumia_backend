from pydantic import BaseModel
from typing import Optional

class PaginatedResponse(BaseModel):
    code: int
    title: str
    message: str
    data: Optional = None
    total: int = 0
    page: int = 1
    page_size: int = 20
    total_pages: int = 0


def paginated_response(
    data: Optional,
    total: int,
    page: int,
    page_size: int,
    message: str = 'Records retrieved successfully'
) -> PaginatedResponse:
    import math
    return PaginatedResponse(
        code=200,
        title='Success',
        message=message,
        data=data,
        total=total,
        page=page,
        page_size=page_size,
        total_pages=math.ceil(total / page_size) if page_size > 0 else 0
    )