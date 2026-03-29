from pydantic import BaseModel
from typing import Optional


class TablesResponse(BaseModel):
    k_e_y: int
    l_o_c_k: bool
    d_e_l_e_t_e: bool
    tab_schema_name: str
    tab_table_name: str
    tab_display_name: str
    tab_description: Optional[str]
    tab_created_by: Optional[int]
    tab_updated_by: Optional[int]