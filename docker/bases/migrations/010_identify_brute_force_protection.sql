-- =============================================================
-- MIGRATION 010 — identify: proteção contra brute force
--
-- Adiciona colunas de controle de tentativas de login para
-- implementar bloqueio de conta após falhas repetidas.
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

ALTER TABLE identify.users
    ADD COLUMN IF NOT EXISTS users_failed_attempts INTEGER   NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS users_locked_until    TIMESTAMP;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_event_trigger WHERE evtname = 'trg_add_default_columns'
    ) THEN
        EXECUTE 'ALTER EVENT TRIGGER trg_add_default_columns ENABLE';
    END IF;
END;
$$;
