-- =============================================================
-- MIGRATION 013 — identify: confirmacao de e-mail por codigo
--
-- Motivo: cadastro de conta (novo) e recuperacao de senha (ja existia,
-- mas nunca enviava e-mail de verdade) agora exigem confirmar a posse
-- do e-mail via codigo de 6 digitos. Espelha o padrao ja usado em
-- identify.password_reset_sessions (migration 003).
-- =============================================================

ALTER TABLE identify.users
    ADD COLUMN IF NOT EXISTS users_email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: usuarios criados antes desta feature nao ficam bloqueados.
UPDATE identify.users SET users_email_verified = TRUE WHERE users_email_verified = FALSE;

CREATE TABLE IF NOT EXISTS identify.email_verification_codes (
    evc_email       VARCHAR(255) NOT NULL,
    evc_code_hash   VARCHAR(255) NOT NULL,
    evc_expires_at  TIMESTAMP    NOT NULL,
    evc_created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e     BOOLEAN      DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_identify_evc_email
    ON identify.email_verification_codes (evc_email)
    WHERE d_e_l_e_t_e = FALSE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_event_trigger WHERE evtname = 'trg_add_default_columns'
    ) THEN
        EXECUTE 'ALTER EVENT TRIGGER trg_add_default_columns ENABLE';
    END IF;
END;
$$;
