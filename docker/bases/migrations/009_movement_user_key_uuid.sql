-- =============================================================
-- MIGRATION 009 — movement: user_key BIGINT → UUID
--
-- Motivo: a coluna user_key era BIGINT numérico preenchido pelo
-- cliente via header HTTP "keyUser", permitindo IDOR trivial.
-- Agora user_key é UUID e referencia diretamente identify.users.users_key,
-- derivado do JWT — o cliente nunca mais controla este valor.
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

-- Trunca os dados de desenvolvimento antes de alterar o tipo da coluna.
-- Em produção com dados reais, use uma estratégia de migração de dados.
TRUNCATE TABLE movement.import_jobs;
TRUNCATE TABLE movement.transactions;

ALTER TABLE movement.transactions
    ALTER COLUMN user_key TYPE UUID USING NULL;

ALTER TABLE movement.import_jobs
    ALTER COLUMN user_key TYPE UUID USING NULL;

-- Recria os índices para refletir o novo tipo
DROP INDEX IF EXISTS movement.ix_movement_transactions_user;
DROP INDEX IF EXISTS movement.ix_movement_transactions_user_date;
DROP INDEX IF EXISTS movement.ix_movement_transactions_category;
DROP INDEX IF EXISTS movement.ix_movement_import_jobs_user;

CREATE INDEX ix_movement_transactions_user
    ON movement.transactions (user_key)
    WHERE d_e_l_e_t_e = FALSE;

CREATE INDEX ix_movement_transactions_user_date
    ON movement.transactions (user_key, date DESC)
    WHERE d_e_l_e_t_e = FALSE;

CREATE INDEX ix_movement_transactions_category
    ON movement.transactions (user_key, category)
    WHERE d_e_l_e_t_e = FALSE;

CREATE INDEX ix_movement_import_jobs_user
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
