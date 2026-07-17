-- =============================================================
-- MIGRATION 014 — movement.budgets (orçamentos)
-- Limite (despesa) ou meta (receita) por período fixo, com alerta
-- por e-mail disparado uma única vez quando o total é atingido.
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
-- movement.budgets
-- scope=geral considera todos os lançamentos do tipo (type) no
-- período; categoria/forma_pagamento/banco restringem por scope_value
-- (mesmos valores de CategoryId/PaymentMethod/InstitutionId).
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movement.budgets (
    budget_id      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_key       UUID          NOT NULL,
    name           VARCHAR(255)  NOT NULL,
    type           VARCHAR(10)   NOT NULL,   -- receita | despesa
    scope          VARCHAR(20)   NOT NULL,   -- geral | categoria | forma_pagamento | banco
    scope_value    VARCHAR(20),              -- obrigatorio quando scope != geral
    limit_amount   DECIMAL(15,2) NOT NULL,
    period_start   DATE          NOT NULL,
    period_end     DATE          NOT NULL,
    notified_at    TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e    BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_movement_budgets_user
    ON movement.budgets (user_key)
    WHERE d_e_l_e_t_e = FALSE;

CREATE INDEX IF NOT EXISTS ix_movement_budgets_period
    ON movement.budgets (user_key, period_start, period_end)
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
