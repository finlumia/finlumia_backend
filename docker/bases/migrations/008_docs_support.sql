-- =============================================================
-- MIGRATION 008 — Schema docs (helpdesk de suporte)
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

CREATE SCHEMA IF NOT EXISTS docs;

-- ---------------------------------------------------------------
-- Sequencia para geração de ticket_code
-- ---------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS docs.ticket_seq START 1 INCREMENT 1;

-- ---------------------------------------------------------------
-- docs.tickets
-- Tabela principal de tickets de suporte abertos pelos usuários.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS docs.tickets (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_code  VARCHAR(20)  NOT NULL UNIQUE,
    user_id      UUID         NOT NULL REFERENCES identify.users(users_key),
    title        VARCHAR(255) NOT NULL,
    category     VARCHAR(20)  NOT NULL CHECK (category IN ('duvida','bug','melhoria','acesso','outros')),
    priority     VARCHAR(10)  NOT NULL DEFAULT 'media' CHECK (priority IN ('baixa','media','alta','urgente')),
    status       VARCHAR(20)  NOT NULL DEFAULT 'aberto' CHECK (status IN ('aberto','em_analise','respondido','fechado')),
    description  TEXT         NOT NULL,
    assigned_to  VARCHAR(100),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_tickets_user_id    ON docs.tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status     ON docs.tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_category   ON docs.tickets(category);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON docs.tickets(created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_tickets_code ON docs.tickets(ticket_code) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------
-- docs.ticket_responses
-- Histórico de respostas e comentários em um ticket.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS docs.ticket_responses (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id   UUID        NOT NULL REFERENCES docs.tickets(id) ON DELETE CASCADE,
    author_id   UUID        NOT NULL REFERENCES identify.users(users_key),
    author_role VARCHAR(20) NOT NULL CHECK (author_role IN ('user','admin','gerente','suporte')),
    message     TEXT        NOT NULL,
    is_internal BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ticket_responses_ticket_id   ON docs.ticket_responses(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_responses_created_at  ON docs.ticket_responses(created_at DESC);

-- ---------------------------------------------------------------
-- docs.ticket_attachments
-- Arquivos anexados a tickets ou respostas.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS docs.ticket_attachments (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id        UUID         NOT NULL REFERENCES docs.tickets(id) ON DELETE CASCADE,
    response_id      UUID         REFERENCES docs.ticket_responses(id) ON DELETE SET NULL,
    uploaded_by      UUID         NOT NULL REFERENCES identify.users(users_key),
    file_name        VARCHAR(255) NOT NULL,
    file_size_bytes  INTEGER      NOT NULL,
    mime_type        VARCHAR(100) NOT NULL,
    storage_path     VARCHAR(500) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ticket_attachments_ticket_id ON docs.ticket_attachments(ticket_id);

-- ---------------------------------------------------------------
-- Função e trigger: updated_at automático
-- ---------------------------------------------------------------
CREATE OR REPLACE FUNCTION docs.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_tickets_updated_at ON docs.tickets;
CREATE TRIGGER trg_tickets_updated_at
    BEFORE UPDATE ON docs.tickets
    FOR EACH ROW EXECUTE FUNCTION docs.set_updated_at();

DROP TRIGGER IF EXISTS trg_ticket_responses_updated_at ON docs.ticket_responses;
CREATE TRIGGER trg_ticket_responses_updated_at
    BEFORE UPDATE ON docs.ticket_responses
    FOR EACH ROW EXECUTE FUNCTION docs.set_updated_at();

-- ---------------------------------------------------------------
-- Função e trigger: geração automática de ticket_code
-- Formato: TK-YYYY-NNN (ex: TK-2024-087)
-- ---------------------------------------------------------------
CREATE OR REPLACE FUNCTION docs.generate_ticket_code()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.ticket_code IS NULL OR NEW.ticket_code = '' THEN
        NEW.ticket_code := 'TK-' || EXTRACT(YEAR FROM NOW())::TEXT
                        || '-' || LPAD(nextval('docs.ticket_seq')::TEXT, 3, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_tickets_code_gen ON docs.tickets;
CREATE TRIGGER trg_tickets_code_gen
    BEFORE INSERT ON docs.tickets
    FOR EACH ROW EXECUTE FUNCTION docs.generate_ticket_code();
