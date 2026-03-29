from pydantic import BaseModel, field_validator, model_validator
from typing import Optional
import re

SAFE_NAME_REGEX = re.compile(r'^[a-z][a-z0-9_]{0,62}$')
MAX_DISPLAY_NAME_LENGTH = 255
MAX_DESCRIPTION_LENGTH = 2000


class TablesFilterRequest(BaseModel):
    search: Optional[str] = None
    d_e_l_e_t_e: Optional[bool] = False
    l_o_c_k: Optional[bool] = None
    page: int = 1
    page_size: int = 20

    @field_validator('page')
    @classmethod
    def validate_page(cls, value: int) -> int:
        if value < 1:
            raise ValueError('page must be >= 1')
        return value

    @field_validator('page_size')
    @classmethod
    def validate_page_size(cls, value: int) -> int:
        if value < 1 or value > 100:
            raise ValueError('page_size must be between 1 and 100')
        return value

    @field_validator('search')
    @classmethod
    def validate_search(cls, value: Optional[str]) -> Optional[str]:
        if value is None:
            return value

        value = value.strip()

        if len(value) > 100:
            raise ValueError('search term must be at most 100 characters')

        if re.search(r'[<>{};\'\"\\]', value):
            raise ValueError('search contains invalid characters')

        return value