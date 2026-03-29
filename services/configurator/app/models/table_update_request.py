from pydantic import BaseModel, field_validator, model_validator
from typing import Optional
import re

SAFE_NAME_REGEX = re.compile(r'^[a-z][a-z0-9_]{0,62}$')
MAX_DISPLAY_NAME_LENGTH = 255
MAX_DESCRIPTION_LENGTH = 2000


class TablesUpdateRequest(BaseModel):
    tab_display_name: Optional[str] = None
    tab_description: Optional[str] = None
    tab_updated_by: Optional[int] = None

    @field_validator('tab_display_name')
    @classmethod
    def validate_display_name(cls, value: Optional[str]) -> Optional[str]:
        if value is None:
            return value

        value = value.strip()

        if not value:
            raise ValueError('tab_display_name cannot be empty')

        if len(value) > MAX_DISPLAY_NAME_LENGTH:
            raise ValueError(
                f'tab_display_name must be at most {MAX_DISPLAY_NAME_LENGTH} characters'
            )

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

    @model_validator(mode='after')
    def validate_at_least_one_field(self) -> 'TablesUpdateRequest':
        if self.tab_display_name is None and self.tab_description is None:
            raise ValueError('At least one field must be provided for update')
        return self
