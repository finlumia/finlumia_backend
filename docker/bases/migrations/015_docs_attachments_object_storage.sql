-- =============================================================
-- MIGRATION 015 — docs.ticket_attachments passa a usar object
-- storage (MinIO) em vez de disco local do container.
-- storage_path e reaproveitada: passa a guardar a object key do
-- arquivo bruto no bucket em vez de um caminho de disco.
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

COMMENT ON COLUMN docs.ticket_attachments.storage_path IS
    'Object key do arquivo bruto no bucket MinIO (antes era caminho em disco local)';

ALTER TABLE docs.ticket_attachments
    ADD COLUMN IF NOT EXISTS converted_object_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS thumbnail_object_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS conversion_status     VARCHAR(20) NOT NULL DEFAULT 'not_applicable',
    ADD COLUMN IF NOT EXISTS conversion_error       TEXT,
    ADD COLUMN IF NOT EXISTS converted_at           TIMESTAMPTZ;

ALTER TABLE docs.ticket_attachments DROP CONSTRAINT IF EXISTS chk_ticket_attachments_conversion_status;
ALTER TABLE docs.ticket_attachments ADD CONSTRAINT chk_ticket_attachments_conversion_status
    CHECK (conversion_status IN ('not_applicable','pending','processing','completed','failed'));

CREATE INDEX IF NOT EXISTS idx_ticket_attachments_conversion_status
    ON docs.ticket_attachments(conversion_status)
    WHERE conversion_status IN ('pending','processing');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_event_trigger WHERE evtname = 'trg_add_default_columns'
    ) THEN
        EXECUTE 'ALTER EVENT TRIGGER trg_add_default_columns ENABLE';
    END IF;
END;
$$;
