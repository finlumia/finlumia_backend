-- =============================================================
-- MIGRATION 003 — Colunas de perfil do usuário + password_reset
-- Complementa a migration 001 que criou apenas os campos básicos.
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

-- ---------------------------------------------------------------
-- Colunas de perfil adicionais em identify.users
-- (o código Java lê estas colunas em UserRepository / UserProfileRecord)
-- ---------------------------------------------------------------
ALTER TABLE identify.users
    ADD COLUMN IF NOT EXISTS users_nome          VARCHAR(255),
    ADD COLUMN IF NOT EXISTS users_papel         VARCHAR(50)  DEFAULT 'viewer',
    ADD COLUMN IF NOT EXISTS users_status        VARCHAR(20)  DEFAULT 'ativo',
    ADD COLUMN IF NOT EXISTS users_mfa           BOOLEAN      DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS users_locale        VARCHAR(10)  DEFAULT 'pt-BR',
    ADD COLUMN IF NOT EXISTS users_tema          VARCHAR(20)  DEFAULT 'light',
    ADD COLUMN IF NOT EXISTS users_ultimo_login  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS users_atualizado_em TIMESTAMP    DEFAULT NOW();

-- users_senha_hash pode ser NULL em contas criadas via OAuth
ALTER TABLE identify.users
    ALTER COLUMN users_senha_hash DROP NOT NULL;

-- Preenche colunas de perfil no usuário semente criado pela migration 001
UPDATE identify.users
SET users_papel         = 'admin',
    users_status        = 'ativo',
    users_mfa           = FALSE,
    users_locale        = 'pt-BR',
    users_tema          = 'light',
    users_atualizado_em = NOW()
WHERE users_papel IS NULL;

-- ---------------------------------------------------------------
-- Tabela de sessões de recuperação de senha
-- (usada por PasswordResetRepository — ausente nas migrations 001/002)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identify.password_reset_sessions (
    prs_email          VARCHAR(255) NOT NULL,
    prs_otp_hash       VARCHAR(255),
    prs_expires_at     TIMESTAMP NOT NULL,
    prs_reset_session  VARCHAR(255),
    d_e_l_e_t_e        BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_identify_prs_email
    ON identify.password_reset_sessions (prs_email)
    WHERE d_e_l_e_t_e = FALSE;

CREATE INDEX IF NOT EXISTS ix_identify_prs_session
    ON identify.password_reset_sessions (prs_reset_session)
    WHERE d_e_l_e_t_e = FALSE AND prs_reset_session IS NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_event_trigger WHERE evtname = 'trg_add_default_columns'
    ) THEN
        EXECUTE 'ALTER EVENT TRIGGER trg_add_default_columns ENABLE';
    END IF;
END;
$$;
