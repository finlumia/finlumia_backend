-- =============================================================
-- MIGRATION 011 — identify: refresh_tokens revoked_at timestamp
--
-- Adiciona coluna refresh_revogado_em para auditoria de quando
-- o token foi revogado e por qual motivo.
-- =============================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_event_trigger WHERE evtname = 'trg_add_default_columns'
    ) THEN
        EXECUTE 'ALTER EVENT TRIGGER trg_add_default_columns DISABLE';
    END IF;
END;
$$;

ALTER TABLE identify.refresh_tokens
    ADD COLUMN IF NOT EXISTS refresh_revogado_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS refresh_revogado_motivo VARCHAR(50);

CREATE INDEX IF NOT EXISTS ix_identify_refresh_tokens_revogado_em
    ON identify.refresh_tokens (refresh_revogado_em)
    WHERE refresh_revogado = TRUE AND d_e_l_e_t_e = FALSE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_event_trigger WHERE evtname = 'trg_add_default_columns'
    ) THEN
        EXECUTE 'ALTER EVENT TRIGGER trg_add_default_columns ENABLE';
    END IF;
END;
$$;
