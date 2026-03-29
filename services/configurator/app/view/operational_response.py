from pydantic import BaseModel
from typing import Optional, Any

class OperationResponse(BaseModel):
    code: int
    title: str
    message: str
    data: Optional[Any] = None


def success_response(
    data: Any = None,
    message: str = 'Operation completed successfully',
    code: int = 200,
    title: str = 'Success'
) -> OperationResponse:
    return OperationResponse(
        code=code,
        title=title,
        message=message,
        data=data
    )


def error_response(
    message: str,
    code: int = 400,
    title: str = 'Error'
) -> OperationResponse:
    return OperationResponse(
        code=code,
        title=title,
        message=message,
        data=None
    )
