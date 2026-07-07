-- ================================================================
-- FINLUMIA — SETUP COMPLETO DO BANCO DE DADOS
-- Banco: finlumia_transactions
-- Executar como superusuário (papadopoulos ou postgres)
--
-- Este script é idempotente: pode ser re-executado sem erros.
-- Ordem de execução: schemas → tabelas → índices → seeds → admin
-- ================================================================

-- Desabilitar o event trigger de colunas automáticas (se existir)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_event_trigger WHERE evtname = 'trg_add_default_columns'
    ) THEN
        EXECUTE 'ALTER EVENT TRIGGER trg_add_default_columns DISABLE';
    END IF;
END;
$$;

-- ================================================================
-- BLOCO 1 — SCHEMA: identify
-- Autenticação, JWT, RBAC e perfil de usuário
-- ================================================================

CREATE SCHEMA IF NOT EXISTS public;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

CREATE SCHEMA IF NOT EXISTS identify;

-- ---------------------------------------------------------------
-- identify.users
-- Tabela principal de usuários. Campos básicos (001) + perfil (003).
-- users_senha_hash pode ser NULL em contas OAuth (Google Login).
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identify.users (
    users_key          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    users_email        VARCHAR(255) UNIQUE NOT NULL,
    users_senha_hash   VARCHAR(255),                       -- NULL para contas OAuth
    users_nome         VARCHAR(255),
    users_papel        VARCHAR(50)  NOT NULL DEFAULT 'viewer',   -- admin | gerente | analista | viewer
    users_status       VARCHAR(20)  NOT NULL DEFAULT 'ativo',    -- ativo | inativo | pendente
    users_mfa          BOOLEAN      NOT NULL DEFAULT FALSE,
    users_locale       VARCHAR(10)  NOT NULL DEFAULT 'pt-BR',
    users_tema         VARCHAR(20)  NOT NULL DEFAULT 'light',    -- light | dark
    users_ultimo_login TIMESTAMP,
    users_ativo        BOOLEAN      NOT NULL DEFAULT TRUE,
    users_criado_em    TIMESTAMP    NOT NULL DEFAULT NOW(),
    users_atualizado_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e        BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Se a tabela já existia (criada pela migration 001 sem as colunas de perfil):
ALTER TABLE identify.users
    ADD COLUMN IF NOT EXISTS users_nome          VARCHAR(255),
    ADD COLUMN IF NOT EXISTS users_papel         VARCHAR(50)  NOT NULL DEFAULT 'viewer',
    ADD COLUMN IF NOT EXISTS users_status        VARCHAR(20)  NOT NULL DEFAULT 'ativo',
    ADD COLUMN IF NOT EXISTS users_mfa           BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS users_locale        VARCHAR(10)  NOT NULL DEFAULT 'pt-BR',
    ADD COLUMN IF NOT EXISTS users_tema          VARCHAR(20)  NOT NULL DEFAULT 'light',
    ADD COLUMN IF NOT EXISTS users_ultimo_login  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS users_atualizado_em TIMESTAMP    NOT NULL DEFAULT NOW();

-- Corrigir NOT NULL da senha (pode ser NULL em OAuth)
ALTER TABLE identify.users
    ALTER COLUMN users_senha_hash DROP NOT NULL;

-- ---------------------------------------------------------------
-- identify.token_blacklist
-- JTIs de tokens invalidados (logout / revogação).
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identify.token_blacklist (
    blacklist_key         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blacklist_jti         TEXT UNIQUE NOT NULL,
    blacklist_revogado_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    blacklist_expira_em   TIMESTAMP   NOT NULL,
    d_e_l_e_t_e           BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_identify_token_blacklist_expira
    ON identify.token_blacklist (blacklist_expira_em)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- identify.refresh_tokens
-- Tokens de renovação de sessão JWT.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identify.refresh_tokens (
    refresh_key       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    refresh_users_key UUID      NOT NULL REFERENCES identify.users(users_key) ON DELETE CASCADE,
    refresh_token     TEXT      UNIQUE NOT NULL,
    refresh_expira_em TIMESTAMP NOT NULL,
    refresh_revogado  BOOLEAN   NOT NULL DEFAULT FALSE,
    refresh_criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e       BOOLEAN   NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_identify_refresh_tokens_user_active
    ON identify.refresh_tokens (refresh_users_key)
    WHERE d_e_l_e_t_e = FALSE AND refresh_revogado = FALSE;

-- ---------------------------------------------------------------
-- identify.password_reset_sessions
-- OTP e sessão para fluxo de redefinição de senha.
-- (ausente nas migrations 001/002 — necessária para PasswordResetRepository)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identify.password_reset_sessions (
    prs_email         VARCHAR(255) NOT NULL,
    prs_otp_hash      VARCHAR(255),
    prs_expires_at    TIMESTAMP    NOT NULL,
    prs_reset_session VARCHAR(255),
    d_e_l_e_t_e       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_identify_prs_email
    ON identify.password_reset_sessions (prs_email)
    WHERE d_e_l_e_t_e = FALSE;

CREATE INDEX IF NOT EXISTS ix_identify_prs_session
    ON identify.password_reset_sessions (prs_reset_session)
    WHERE d_e_l_e_t_e = FALSE AND prs_reset_session IS NOT NULL;

-- ---------------------------------------------------------------
-- identify.roles
-- Papéis do sistema (RBAC).
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identify.roles (
    role_nome      VARCHAR(50) PRIMARY KEY,
    role_descricao TEXT
);

-- ---------------------------------------------------------------
-- identify.resources
-- Recursos/módulos controlados pelo RBAC.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identify.resources (
    resource_key       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_nome      VARCHAR(100) UNIQUE NOT NULL,
    resource_descricao TEXT
);

-- ---------------------------------------------------------------
-- identify.permissions
-- Matriz de permissões papel × recurso.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identify.permissions (
    permission_key          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_role_nome    VARCHAR(50) NOT NULL REFERENCES identify.roles(role_nome) ON DELETE CASCADE,
    permission_resource_key UUID        NOT NULL REFERENCES identify.resources(resource_key) ON DELETE CASCADE,
    permission_can_create   BOOLEAN     NOT NULL DEFAULT FALSE,
    permission_can_read     BOOLEAN     NOT NULL DEFAULT FALSE,
    permission_can_update   BOOLEAN     NOT NULL DEFAULT FALSE,
    permission_can_delete   BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (permission_role_nome, permission_resource_key)
);

CREATE INDEX IF NOT EXISTS ix_identify_permissions_role
    ON identify.permissions (permission_role_nome);

CREATE INDEX IF NOT EXISTS ix_identify_permissions_resource
    ON identify.permissions (permission_resource_key);

-- ---------------------------------------------------------------
-- identify.users_roles
-- Vínculo usuário ↔ papéis (N:N).
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identify.users_roles (
    users_roles_key      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    users_roles_user_key UUID        NOT NULL REFERENCES identify.users(users_key) ON DELETE CASCADE,
    users_roles_role     VARCHAR(50) NOT NULL REFERENCES identify.roles(role_nome) ON DELETE CASCADE,
    UNIQUE (users_roles_user_key, users_roles_role)
);

CREATE INDEX IF NOT EXISTS ix_identify_users_roles_user
    ON identify.users_roles (users_roles_user_key);

-- ================================================================
-- BLOCO 2 — SCHEMA: movement
-- Transações financeiras e importação de arquivos
-- ================================================================

CREATE SCHEMA IF NOT EXISTS movement;

-- ---------------------------------------------------------------
-- movement.transactions
-- Ledger de movimentações. user_key é Long (resolvido pelo
-- KeyUserInterceptor a partir do header "keyUser" do frontend).
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movement.transactions (
    transaction_id   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_key         BIGINT        NOT NULL,
    type             VARCHAR(10)   NOT NULL,   -- receita | despesa
    method           VARCHAR(20)   NOT NULL,   -- pix | credito | debito | dinheiro | ted | doc
    institution      VARCHAR(20)   NOT NULL,   -- nubank | itau | bb | bradesco | santander |
                                              --   picpay | inter | c6 | xp
    date             DATE          NOT NULL,
    category         VARCHAR(20)   NOT NULL,   -- alimentacao | saude | educacao | transporte |
                                              --   lazer | moradia | salario | vendas | tecnologia |
                                              --   marketing | servicos | investimento | outros
    description      VARCHAR(500)  NOT NULL,
    sub_description  VARCHAR(255),
    amount           DECIMAL(15,2) NOT NULL,
    notes            TEXT,
    tags             TEXT[],
    is_recurring     BOOLEAN       NOT NULL DEFAULT FALSE,
    recurring_id     UUID,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e      BOOLEAN       NOT NULL DEFAULT FALSE
);

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

CREATE INDEX IF NOT EXISTS ix_movement_transactions_search
    ON movement.transactions USING gin(to_tsvector('portuguese', description))
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- movement.import_jobs
-- Jobs de importação de arquivos (OFX, CSV, imagens/OCR).
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movement.import_jobs (
    job_id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_key        BIGINT        NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'pending',
                                          -- pending | processing | ready | completed | failed
    file_name       VARCHAR(255),
    file_type       VARCHAR(10),          -- ofx | csv | image
    total_rows      INTEGER,
    imported_rows   INTEGER,
    errors          TEXT[],
    ocr_description VARCHAR(500),
    ocr_amount      DECIMAL(15,2),
    ocr_date        DATE,
    ocr_category    VARCHAR(20),
    ocr_method      VARCHAR(20),
    ocr_confidence  DOUBLE PRECISION,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e     BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_movement_import_jobs_user
    ON movement.import_jobs (user_key)
    WHERE d_e_l_e_t_e = FALSE;

-- ================================================================
-- BLOCO 3 — SCHEMA: configurator
-- Metadados de tabelas, campos, índices, funções, gatilhos e
-- permissões de UI do configurador de banco de dados.
-- ================================================================

CREATE SCHEMA IF NOT EXISTS configurator;

-- ---------------------------------------------------------------
-- configurator.TAB — Registro de tabelas gerenciadas
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."TAB" (
    tab_key           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tab_nome          VARCHAR(63) NOT NULL,
    tab_schema        VARCHAR(20) NOT NULL DEFAULT 'public',
    tab_descricao     TEXT,
    tab_status        VARCHAR(10) NOT NULL DEFAULT 'ativo',
    tab_criado_em     TIMESTAMP   NOT NULL DEFAULT NOW(),
    tab_atualizado_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e       BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (tab_nome, tab_schema)
);

CREATE INDEX IF NOT EXISTS ix_configurator_tab_schema_status
    ON configurator."TAB" (tab_schema, tab_status)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- configurator.FIE — Campos/colunas das tabelas
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."FIE" (
    fie_key           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    fie_nome          VARCHAR(63) NOT NULL,
    fie_tab_key       UUID        NOT NULL REFERENCES configurator."TAB"(tab_key) ON DELETE RESTRICT,
    fie_tipo_dado     VARCHAR(20) NOT NULL,
    fie_tamanho       INTEGER,
    fie_nulo          BOOLEAN     NOT NULL DEFAULT TRUE,
    fie_default       VARCHAR(255),
    fie_e_primario    BOOLEAN     NOT NULL DEFAULT FALSE,
    fie_e_estrangeiro BOOLEAN     NOT NULL DEFAULT FALSE,
    fie_ref_tabela    VARCHAR(63),
    fie_ref_campo     VARCHAR(63),
    fie_status        VARCHAR(10) NOT NULL DEFAULT 'ativo',
    fie_criado_em     TIMESTAMP   NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e       BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_configurator_fie_tab
    ON configurator."FIE" (fie_tab_key)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- configurator.IDX — Índices registrados
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."IDX" (
    idx_key       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    idx_nome      VARCHAR(63) NOT NULL,
    idx_tab_key   UUID        NOT NULL REFERENCES configurator."TAB"(tab_key) ON DELETE RESTRICT,
    idx_schema    VARCHAR(20) NOT NULL DEFAULT 'public',
    idx_campos    TEXT        NOT NULL,
    idx_tipo      VARCHAR(10) NOT NULL DEFAULT 'btree',
    idx_unico     BOOLEAN     NOT NULL DEFAULT FALSE,
    idx_parcial   BOOLEAN     NOT NULL DEFAULT FALSE,
    idx_where     TEXT,
    idx_status    VARCHAR(10) NOT NULL DEFAULT 'ativo',
    idx_criado_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e   BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (idx_nome, idx_schema)
);

CREATE INDEX IF NOT EXISTS ix_configurator_idx_tab
    ON configurator."IDX" (idx_tab_key)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- configurator.FUN — Funções do banco de dados
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."FUN" (
    fun_key           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    fun_nome          VARCHAR(63) NOT NULL,
    fun_schema        VARCHAR(20) NOT NULL DEFAULT 'public',
    fun_linguagem     VARCHAR(20) NOT NULL DEFAULT 'plpgsql',
    fun_tipo_retorno  VARCHAR(63) NOT NULL DEFAULT 'void',
    fun_args          TEXT,
    fun_volatilidade  VARCHAR(10) NOT NULL DEFAULT 'VOLATILE',
    fun_corpo         TEXT        NOT NULL,
    fun_descricao     TEXT,
    fun_status        VARCHAR(10) NOT NULL DEFAULT 'ativo',
    fun_criado_em     TIMESTAMP   NOT NULL DEFAULT NOW(),
    fun_atualizado_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e       BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (fun_nome, fun_schema)
);

-- ---------------------------------------------------------------
-- configurator.GEN — Gatilhos (triggers)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."GEN" (
    gen_key        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    gen_nome       VARCHAR(63) NOT NULL,
    gen_tab_key    UUID        NOT NULL REFERENCES configurator."TAB"(tab_key) ON DELETE RESTRICT,
    gen_schema     VARCHAR(20) NOT NULL DEFAULT 'public',
    gen_evento     VARCHAR(10) NOT NULL,   -- INSERT | UPDATE | DELETE | TRUNCATE
    gen_timing     VARCHAR(12) NOT NULL DEFAULT 'AFTER',  -- BEFORE | AFTER | INSTEAD_OF
    gen_funcao     VARCHAR(63) NOT NULL,
    gen_habilitado BOOLEAN     NOT NULL DEFAULT TRUE,
    gen_descricao  TEXT,
    gen_status     VARCHAR(10) NOT NULL DEFAULT 'ativo',
    gen_criado_em  TIMESTAMP   NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e    BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (gen_nome, gen_tab_key)
);

CREATE INDEX IF NOT EXISTS ix_configurator_gen_tab
    ON configurator."GEN" (gen_tab_key)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- configurator.PAR — Permissões de UI por módulo/subsistema/papel
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."PAR" (
    par_key           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    par_modulo        VARCHAR(100) NOT NULL,
    par_subsistema    VARCHAR(100) NOT NULL,
    par_papel         VARCHAR(50)  NOT NULL,
    par_pode_ler      BOOLEAN      NOT NULL DEFAULT FALSE,
    par_pode_escrever BOOLEAN      NOT NULL DEFAULT FALSE,
    par_pode_deletar  BOOLEAN      NOT NULL DEFAULT FALSE,
    par_pode_admin    BOOLEAN      NOT NULL DEFAULT FALSE,
    d_e_l_e_t_e       BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (par_modulo, par_subsistema, par_papel)
);

CREATE INDEX IF NOT EXISTS ix_configurator_par_modulo_papel
    ON configurator."PAR" (par_modulo, par_papel)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- configurator.MEN — Itens de menu/navegação do frontend
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."MEN" (
    men_key          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    men_label        VARCHAR(100) NOT NULL,
    men_rota         VARCHAR(255),
    men_icone        VARCHAR(50),
    men_pai_key      UUID         REFERENCES configurator."MEN"(men_key) ON DELETE SET NULL,
    men_ordem        INTEGER      NOT NULL DEFAULT 0,
    men_papel_minimo VARCHAR(50)  NOT NULL DEFAULT 'viewer',
    men_ativo        BOOLEAN      NOT NULL DEFAULT TRUE,
    d_e_l_e_t_e      BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ---------------------------------------------------------------
-- configurator.ORD — Ordem de exibição dos campos por tabela
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."ORD" (
    ord_key     UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    ord_tab_key UUID    NOT NULL REFERENCES configurator."TAB"(tab_key) ON DELETE CASCADE,
    ord_fie_key UUID    NOT NULL REFERENCES configurator."FIE"(fie_key) ON DELETE CASCADE,
    ord_posicao INTEGER NOT NULL DEFAULT 0,
    d_e_l_e_t_e BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (ord_tab_key, ord_fie_key)
);

-- ================================================================
-- BLOCO 4 — SEEDS: papéis, recursos e permissões base
-- ================================================================

-- Papéis RBAC
INSERT INTO identify.roles (role_nome, role_descricao) VALUES
    ('ADMIN',    'Administrador com acesso total ao sistema'),
    ('GERENTE',  'Gerente com acesso amplo, exceto configurações críticas'),
    ('ANALISTA', 'Analista com acesso de leitura e operações básicas'),
    ('VIEWER',   'Visualizador somente-leitura')
ON CONFLICT (role_nome) DO NOTHING;

-- Recursos/módulos do sistema
INSERT INTO identify.resources (resource_nome, resource_descricao) VALUES
    ('financas',      'Módulo de finanças pessoais e transações'),
    ('investimentos', 'Módulo de investimentos'),
    ('relatorios',    'Módulo de relatórios e exportações'),
    ('configurador',  'Módulo de configuração do sistema')
ON CONFLICT (resource_nome) DO NOTHING;

-- Permissões do ADMIN (tudo liberado)
INSERT INTO identify.permissions (
    permission_role_nome, permission_resource_key,
    permission_can_create, permission_can_read,
    permission_can_update, permission_can_delete
)
SELECT 'ADMIN', r.resource_key, TRUE, TRUE, TRUE, TRUE
FROM identify.resources r
ON CONFLICT (permission_role_nome, permission_resource_key) DO NOTHING;

-- Permissões do GERENTE
INSERT INTO identify.permissions (
    permission_role_nome, permission_resource_key,
    permission_can_create, permission_can_read,
    permission_can_update, permission_can_delete
)
SELECT 'GERENTE', r.resource_key,
    CASE WHEN r.resource_nome IN ('financas','investimentos') THEN TRUE ELSE FALSE END,
    TRUE,
    CASE WHEN r.resource_nome IN ('financas','investimentos') THEN TRUE ELSE FALSE END,
    FALSE
FROM identify.resources r
WHERE r.resource_nome != 'configurador'
ON CONFLICT (permission_role_nome, permission_resource_key) DO NOTHING;

-- Permissões do ANALISTA (somente leitura + create em financas)
INSERT INTO identify.permissions (
    permission_role_nome, permission_resource_key,
    permission_can_create, permission_can_read,
    permission_can_update, permission_can_delete
)
SELECT 'ANALISTA', r.resource_key,
    CASE WHEN r.resource_nome = 'financas' THEN TRUE ELSE FALSE END,
    TRUE,
    FALSE,
    FALSE
FROM identify.resources r
WHERE r.resource_nome IN ('financas','relatorios')
ON CONFLICT (permission_role_nome, permission_resource_key) DO NOTHING;

-- Permissões do VIEWER (somente leitura em financas e relatórios)
INSERT INTO identify.permissions (
    permission_role_nome, permission_resource_key,
    permission_can_create, permission_can_read,
    permission_can_update, permission_can_delete
)
SELECT 'VIEWER', r.resource_key, FALSE, TRUE, FALSE, FALSE
FROM identify.resources r
WHERE r.resource_nome IN ('financas','relatorios')
ON CONFLICT (permission_role_nome, permission_resource_key) DO NOTHING;

-- Seeds do configurador.PAR
INSERT INTO configurator."PAR"
    (par_modulo, par_subsistema, par_papel,
     par_pode_ler, par_pode_escrever, par_pode_deletar, par_pode_admin)
VALUES
    ('configurator','tables',      'admin',   TRUE, TRUE, TRUE, TRUE),
    ('configurator','fields',      'admin',   TRUE, TRUE, TRUE, TRUE),
    ('configurator','indexes',     'admin',   TRUE, TRUE, TRUE, TRUE),
    ('configurator','functions',   'admin',   TRUE, TRUE, TRUE, TRUE),
    ('configurator','triggers',    'admin',   TRUE, TRUE, TRUE, TRUE),
    ('configurator','permissions', 'admin',   TRUE, TRUE, TRUE, TRUE),
    ('configurator','users',       'admin',   TRUE, TRUE, TRUE, TRUE),
    ('configurator','tables',      'gerente', TRUE, TRUE, FALSE, FALSE),
    ('configurator','fields',      'gerente', TRUE, TRUE, FALSE, FALSE),
    ('configurator','indexes',     'gerente', TRUE, TRUE, FALSE, FALSE),
    ('configurator','functions',   'gerente', TRUE, FALSE, FALSE, FALSE),
    ('configurator','triggers',    'gerente', TRUE, FALSE, FALSE, FALSE),
    ('configurator','permissions', 'gerente', TRUE, FALSE, FALSE, FALSE),
    ('configurator','users',       'gerente', TRUE, FALSE, FALSE, FALSE),
    ('configurator','tables',      'analista',TRUE, FALSE, FALSE, FALSE),
    ('configurator','fields',      'analista',TRUE, FALSE, FALSE, FALSE),
    ('configurator','indexes',     'analista',TRUE, FALSE, FALSE, FALSE),
    ('configurator','functions',   'analista',TRUE, FALSE, FALSE, FALSE),
    ('configurator','triggers',    'analista',TRUE, FALSE, FALSE, FALSE),
    ('configurator','permissions', 'analista',FALSE,FALSE, FALSE, FALSE),
    ('configurator','users',       'analista',FALSE,FALSE, FALSE, FALSE)
ON CONFLICT (par_modulo, par_subsistema, par_papel) DO NOTHING;

-- Seeds do configurator.MEN (menu de navegação)
INSERT INTO configurator."MEN"
    (men_label, men_rota, men_icone, men_ordem, men_papel_minimo)
VALUES
    ('Tabelas',    '/config/tables',      'table',     1, 'analista'),
    ('Campos',     '/config/fields',      'columns',   2, 'analista'),
    ('Índices',    '/config/indexes',     'bolt',      3, 'analista'),
    ('Funções',    '/config/functions',   'code',      4, 'gerente'),
    ('Triggers',   '/config/triggers',    'zap',       5, 'gerente'),
    ('Permissões', '/config/permissions', 'shield',    6, 'admin'),
    ('Usuários',   '/config/users',       'users',     7, 'admin')
ON CONFLICT DO NOTHING;

-- ================================================================
-- BLOCO 5 — USUÁRIO ADMINISTRADOR INICIAL
-- Nome:  Thiago Benevide de Moraes
-- Email: thiagobenevide@live.com
-- Senha: Br@sil2002  (BCrypt $2a$10, strength 10)
--
-- Para regenerar o hash:
--   new BCryptPasswordEncoder().encode("Br@sil2002")
-- ================================================================

INSERT INTO identify.users (
    users_email,
    users_senha_hash,
    users_nome,
    users_papel,
    users_status,
    users_mfa,
    users_locale,
    users_tema,
    users_ativo,
    users_atualizado_em
) VALUES (
    'thiagobenevide@live.com',
    '$2a$10$ckvpQYvvEbbyzQ1M9UfSMeEvL08rRIHO/7NsAAyFQnWmjeb4vXgPG',
    'Thiago Benevide de Moraes',
    'admin',
    'ativo',
    FALSE,
    'pt-BR',
    'light',
    TRUE,
    NOW()
) ON CONFLICT (users_email) DO UPDATE
    SET users_senha_hash      = EXCLUDED.users_senha_hash,
        users_nome            = EXCLUDED.users_nome,
        users_papel           = EXCLUDED.users_papel,
        users_status          = EXCLUDED.users_status,
        users_ativo           = EXCLUDED.users_ativo,
        users_atualizado_em   = NOW();

-- Vincular ao papel ADMIN
INSERT INTO identify.users_roles (users_roles_user_key, users_roles_role)
SELECT u.users_key, 'ADMIN'
FROM identify.users u
WHERE u.users_email = 'thiagobenevide@live.com'
ON CONFLICT (users_roles_user_key, users_roles_role) DO NOTHING;

-- ================================================================
-- BLOCO 6 — Reabilitar event trigger (se existia)
-- ================================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_event_trigger WHERE evtname = 'trg_add_default_columns'
    ) THEN
        EXECUTE 'ALTER EVENT TRIGGER trg_add_default_columns ENABLE';
    END IF;
END;
$$;

-- ================================================================
-- VERIFICAÇÃO FINAL
-- ================================================================
DO $$
DECLARE
    v_user_key UUID;
    v_roles    TEXT;
BEGIN
    SELECT u.users_key INTO v_user_key
    FROM identify.users u
    WHERE u.users_email = 'thiagobenevide@live.com'
      AND u.users_ativo = TRUE
      AND u.d_e_l_e_t_e = FALSE;

    SELECT STRING_AGG(ur.users_roles_role, ', ') INTO v_roles
    FROM identify.users_roles ur
    WHERE ur.users_roles_user_key = v_user_key;

    IF v_user_key IS NOT NULL THEN
        RAISE NOTICE '✓ Setup concluído!';
        RAISE NOTICE '  Usuário: thiagobenevide@live.com';
        RAISE NOTICE '  UUID: %', v_user_key;
        RAISE NOTICE '  Papéis: %', COALESCE(v_roles, 'nenhum');
    ELSE
        RAISE WARNING '✗ Usuário admin não encontrado após setup!';
    END IF;
END;
$$;
