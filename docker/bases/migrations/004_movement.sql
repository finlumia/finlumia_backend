-- =============================================================
-- MIGRATION 004 — Schema movement (transações financeiras)
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

CREATE SCHEMA IF NOT EXISTS movement;

-- ---------------------------------------------------------------
-- movement.transactions
-- Ledger principal de movimentações financeiras do usuário.
-- user_key é um Long numérico resolvido pelo KeyUserInterceptor
-- a partir do header "keyUser" enviado pelo frontend.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movement.transactions (
    transaction_id   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_key         BIGINT       NOT NULL,
    type             VARCHAR(10)  NOT NULL,   -- receita | despesa
    method           VARCHAR(20)  NOT NULL,   -- pix | credito | debito | dinheiro | ted | doc
    institution      VARCHAR(20)  NOT NULL,   -- nubank | itau | bb | bradesco | santander | picpay | inter | c6 | xp
    date             DATE         NOT NULL,
    category         VARCHAR(20)  NOT NULL,   -- alimentacao | saude | educacao | transporte | lazer |
                                              -- moradia | salario | vendas | tecnologia | marketing |
                                              -- servicos | investimento | outros
    description      VARCHAR(500) NOT NULL,
    sub_description  VARCHAR(255),
    amount           DECIMAL(15,2) NOT NULL,
    notes            TEXT,
    tags             TEXT[],
    is_recurring     BOOLEAN      NOT NULL DEFAULT FALSE,
    recurring_id     UUID,                   -- self-ref para grupo de recorrentes
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e      BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Índices de performance para as queries do TransactionRepository
CREATE INDEX IF NOT EXISTS ix_movement_transactions_user
    ON movement.transactions (user_key)
    WHERE d_e_l_e_t_e = FALSE;

CREATE INDEX IF NOT EXISTS ix_movement_transactions_user_date
    ON movement.transactions (user_key, date DESC)
    WHERE d_e_l_e_t_e = FALSE;

CREATE INDEX IF NOT EXISTS ix_movement_transactions_recurring
    ON movement.transactions (recurring_id)
    WHERE d_e_l_e_t_e = FALSE AND recurring_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_movement_transactions_category
    ON movement.transactions (user_key, category)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- movement.import_jobs
-- Rastreamento de importações de arquivo (OFX, CSV, imagem/OCR).
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movement.import_jobs (
    job_id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_key        BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',  -- pending | processing | ready | completed | failed
    file_name       VARCHAR(255),
    file_type       VARCHAR(10),                              -- ofx | csv | image
    total_rows      INTEGER,
    imported_rows   INTEGER,
    errors          TEXT[],
    -- Campos preenchidos pelo processamento OCR (imagens de comprovante)
    ocr_description VARCHAR(500),
    ocr_amount      DECIMAL(15,2),
    ocr_date        DATE,
    ocr_category    VARCHAR(20),
    ocr_method      VARCHAR(20),
    ocr_confidence  DOUBLE PRECISION,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_movement_import_jobs_user
    ON movement.import_jobs (user_key)
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
