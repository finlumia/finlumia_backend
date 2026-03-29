from pydantic import BaseModel, field_validator
from typing import Optional
import re

SAFE_NAME_REGEX = re.compile(r'^[a-z][a-z0-9_]{0,62}$')
MAX_DISPLAY_NAME_LENGTH = 255
MAX_DESCRIPTION_LENGTH = 2000


class TablesCreateRequest(BaseModel):
    tab_schema_name: str
    tab_table_name: str
    tab_display_name: str
    tab_description: Optional[str] = None
    tab_created_by: Optional[int] = None

    @field_validator('tab_schema_name', 'tab_table_name')
    @classmethod
    def validate_safe_name(cls, value: str, info) -> str:
        value = value.strip().lower()

        if not value:
            raise ValueError(f'{info.field_name} cannot be empty')

        if not SAFE_NAME_REGEX.match(value):
            raise ValueError(
                f'{info.field_name} must start with a letter, '
                f'contain only lowercase letters, numbers or underscores, '
                f'and be at most 63 characters'
            )

        # Bloqueia palavras reservadas do PostgreSQL
        reserved = {
            'select', 'insert', 'update', 'delete', 'drop', 'create',
            'alter', 'table', 'index', 'where', 'from', 'join', 'union',
            'exec', 'execute', 'truncate', 'grant', 'revoke', 'pg_',
        }
        if value in reserved or any(value.startswith(r) for r in reserved):
            raise ValueError(f'{info.field_name} contains a reserved keyword')

        return value

    @field_validator('tab_display_name')
    @classmethod
    def validate_display_name(cls, value: str) -> str:
        value = value.strip()

        if not value:
            raise ValueError('tab_display_name cannot be empty')

        if len(value) > MAX_DISPLAY_NAME_LENGTH:
            raise ValueError(
                f'tab_display_name must be at most {MAX_DISPLAY_NAME_LENGTH} characters'
            )

        # Bloqueia caracteres perigosos em display name
        if re.search(r'[<>{};\'\"\\]', value):
            raise ValueError('tab_display_name contains invalid characters')

        return value

    @field_validator('tab_description')
    @classmethod
    def validate_description(cls, value: Optional[str]) -> Optional[str]:
        if value is None:
            return value

        value = value.strip()

        if len(value) > MAX_DESCRIPTION_LENGTH:
            raise ValueError(
                f'tab_description must be at most {MAX_DESCRIPTION_LENGTH} characters'
            )

        if re.search(r'[<>{};\'\"\\]', value):
            raise ValueError('tab_description contains invalid characters')

        return value

    @field_validator('tab_created_by')
    @classmethod
    def validate_created_by(cls, value: Optional[int]) -> Optional[int]:
        if value is not None and value <= 0:
            raise ValueError('tab_created_by must be a positive integer')
        return value