-- =============================================================
-- MIGRATION 005 — Schema configurator
-- Tabelas de metadados do configurador de banco de dados.
-- Nomes curtos de 3 letras seguem convenção interna do projeto.
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

CREATE SCHEMA IF NOT EXISTS configurator;

-- ---------------------------------------------------------------
-- configurator.TAB — Registro de tabelas gerenciadas
-- (TableController / TableService / TableResponse)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."TAB" (
    tab_key           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tab_nome          VARCHAR(63) NOT NULL,
    tab_schema        VARCHAR(20) NOT NULL DEFAULT 'public',
    tab_descricao     TEXT,
    tab_status        VARCHAR(10) NOT NULL DEFAULT 'ativo',   -- ativo | inativo
    tab_criado_em     TIMESTAMP   NOT NULL DEFAULT NOW(),
    tab_atualizado_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e       BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (tab_nome, tab_schema)
);

CREATE INDEX IF NOT EXISTS ix_configurator_tab_schema
    ON configurator."TAB" (tab_schema)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- configurator.FIE — Registro de campos/colunas
-- (FieldController / FieldService / FieldResponse)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."FIE" (
    fie_key              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    fie_nome             VARCHAR(63) NOT NULL,
    fie_tab_key          UUID        NOT NULL REFERENCES configurator."TAB"(tab_key) ON DELETE RESTRICT,
    fie_tipo_dado        VARCHAR(20) NOT NULL,  -- uuid | varchar | integer | bigint | bool |
                                               -- timestamp | decimal | text | jsonb | serial
    fie_tamanho          INTEGER,
    fie_nulo             BOOLEAN     NOT NULL DEFAULT TRUE,
    fie_default          VARCHAR(255),
    fie_e_primario       BOOLEAN     NOT NULL DEFAULT FALSE,
    fie_e_estrangeiro    BOOLEAN     NOT NULL DEFAULT FALSE,
    fie_ref_tabela       VARCHAR(63),
    fie_ref_campo        VARCHAR(63),
    fie_status           VARCHAR(10) NOT NULL DEFAULT 'ativo',
    fie_criado_em        TIMESTAMP   NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e          BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_configurator_fie_tab
    ON configurator."FIE" (fie_tab_key)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- configurator.IDX — Registro de índices
-- (IndexController / IndexService / DbIndexResponse)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."IDX" (
    idx_key         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    idx_nome        VARCHAR(63) NOT NULL,
    idx_tab_key     UUID        NOT NULL REFERENCES configurator."TAB"(tab_key) ON DELETE RESTRICT,
    idx_schema      VARCHAR(20) NOT NULL DEFAULT 'public',
    idx_campos      TEXT        NOT NULL,  -- lista de campos separados por vírgula
    idx_tipo        VARCHAR(10) NOT NULL DEFAULT 'btree',  -- btree | hash | gin | gist | brin
    idx_unico       BOOLEAN     NOT NULL DEFAULT FALSE,
    idx_parcial     BOOLEAN     NOT NULL DEFAULT FALSE,
    idx_where       TEXT,
    idx_status      VARCHAR(10) NOT NULL DEFAULT 'ativo',
    idx_criado_em   TIMESTAMP   NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e     BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (idx_nome, idx_schema)
);

CREATE INDEX IF NOT EXISTS ix_configurator_idx_tab
    ON configurator."IDX" (idx_tab_key)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- configurator.FUN — Registro de funções do banco
-- (FunctionController / FunctionService / DbFunctionResponse)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."FUN" (
    fun_key           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    fun_nome          VARCHAR(63) NOT NULL,
    fun_schema        VARCHAR(20) NOT NULL DEFAULT 'public',
    fun_linguagem     VARCHAR(20) NOT NULL DEFAULT 'plpgsql',  -- sql | plpgsql | javascript
    fun_tipo_retorno  VARCHAR(63) NOT NULL DEFAULT 'void',
    fun_args          TEXT,
    fun_volatilidade  VARCHAR(10) NOT NULL DEFAULT 'VOLATILE',  -- VOLATILE | STABLE | IMMUTABLE
    fun_corpo         TEXT        NOT NULL,
    fun_descricao     TEXT,
    fun_status        VARCHAR(10) NOT NULL DEFAULT 'ativo',
    fun_criado_em     TIMESTAMP   NOT NULL DEFAULT NOW(),
    fun_atualizado_em TIMESTAMP   NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e       BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (fun_nome, fun_schema)
);

-- ---------------------------------------------------------------
-- configurator.GEN — Registro de triggers (gatilhos)
-- (TriggerController / TriggerService / DbTriggerResponse)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."GEN" (
    gen_key         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    gen_nome        VARCHAR(63) NOT NULL,
    gen_tab_key     UUID        NOT NULL REFERENCES configurator."TAB"(tab_key) ON DELETE RESTRICT,
    gen_schema      VARCHAR(20) NOT NULL DEFAULT 'public',
    gen_evento      VARCHAR(10) NOT NULL,  -- INSERT | UPDATE | DELETE | TRUNCATE
    gen_timing      VARCHAR(12) NOT NULL DEFAULT 'AFTER',  -- BEFORE | AFTER | INSTEAD_OF
    gen_funcao      VARCHAR(63) NOT NULL,
    gen_habilitado  BOOLEAN     NOT NULL DEFAULT TRUE,
    gen_descricao   TEXT,
    gen_status      VARCHAR(10) NOT NULL DEFAULT 'ativo',
    gen_criado_em   TIMESTAMP   NOT NULL DEFAULT NOW(),
    d_e_l_e_t_e     BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (gen_nome, gen_tab_key)
);

CREATE INDEX IF NOT EXISTS ix_configurator_gen_tab
    ON configurator."GEN" (gen_tab_key)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- configurator.PAR — Matriz de permissões por módulo/subsistema/papel
-- (PermissionController / PermissionService / PermissionResponse)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."PAR" (
    par_key           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    par_modulo        VARCHAR(100) NOT NULL,
    par_subsistema    VARCHAR(100) NOT NULL,
    par_papel         VARCHAR(50)  NOT NULL,  -- admin | gerente | analista | viewer
    par_pode_ler      BOOLEAN      NOT NULL DEFAULT FALSE,
    par_pode_escrever BOOLEAN      NOT NULL DEFAULT FALSE,
    par_pode_deletar  BOOLEAN      NOT NULL DEFAULT FALSE,
    par_pode_admin    BOOLEAN      NOT NULL DEFAULT FALSE,
    d_e_l_e_t_e       BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (par_modulo, par_subsistema, par_papel)
);

CREATE INDEX IF NOT EXISTS ix_configurator_par_modulo
    ON configurator."PAR" (par_modulo)
    WHERE d_e_l_e_t_e = FALSE;

CREATE INDEX IF NOT EXISTS ix_configurator_par_papel
    ON configurator."PAR" (par_papel)
    WHERE d_e_l_e_t_e = FALSE;

-- ---------------------------------------------------------------
-- configurator.MEN — Itens de menu/navegação do frontend
-- (estrutura de navegação lateral do configurador)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."MEN" (
    men_key         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    men_label       VARCHAR(100) NOT NULL,
    men_rota        VARCHAR(255),
    men_icone       VARCHAR(50),
    men_pai_key     UUID         REFERENCES configurator."MEN"(men_key) ON DELETE SET NULL,
    men_ordem       INTEGER      NOT NULL DEFAULT 0,
    men_papel_minimo VARCHAR(50) DEFAULT 'viewer',
    men_ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    d_e_l_e_t_e     BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ---------------------------------------------------------------
-- configurator.ORD — Ordenação de exibição de campos por tabela
-- (display order de FIE dentro de TAB na UI)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS configurator."ORD" (
    ord_key     UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    ord_tab_key UUID    NOT NULL REFERENCES configurator."TAB"(tab_key) ON DELETE CASCADE,
    ord_fie_key UUID    NOT NULL REFERENCES configurator."FIE"(fie_key) ON DELETE CASCADE,
    ord_posicao INTEGER NOT NULL DEFAULT 0,
    d_e_l_e_t_e BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (ord_tab_key, ord_fie_key)
);

-- ---------------------------------------------------------------
-- Dados iniciais do configurator.PAR — permissões base
-- ---------------------------------------------------------------
INSERT INTO configurator."PAR"
    (par_modulo, par_subsistema, par_papel,
     par_pode_ler, par_pode_escrever, par_pode_deletar, par_pode_admin)
VALUES
    -- Admin tem acesso total a tudo
    ('configurator', 'tables',      'admin', TRUE, TRUE, TRUE, TRUE),
    ('configurator', 'fields',      'admin', TRUE, TRUE, TRUE, TRUE),
    ('configurator', 'indexes',     'admin', TRUE, TRUE, TRUE, TRUE),
    ('configurator', 'functions',   'admin', TRUE, TRUE, TRUE, TRUE),
    ('configurator', 'triggers',    'admin', TRUE, TRUE, TRUE, TRUE),
    ('configurator', 'permissions', 'admin', TRUE, TRUE, TRUE, TRUE),
    ('configurator', 'users',       'admin', TRUE, TRUE, TRUE, TRUE),
    -- Gerente pode ler e escrever, não deletar nem administrar
    ('configurator', 'tables',      'gerente', TRUE, TRUE, FALSE, FALSE),
    ('configurator', 'fields',      'gerente', TRUE, TRUE, FALSE, FALSE),
    ('configurator', 'indexes',     'gerente', TRUE, TRUE, FALSE, FALSE),
    ('configurator', 'functions',   'gerente', TRUE, FALSE, FALSE, FALSE),
    ('configurator', 'triggers',    'gerente', TRUE, FALSE, FALSE, FALSE),
    ('configurator', 'permissions', 'gerente', TRUE, FALSE, FALSE, FALSE),
    ('configurator', 'users',       'gerente', TRUE, FALSE, FALSE, FALSE),
    -- Analista só lê
    ('configurator', 'tables',      'analista', TRUE, FALSE, FALSE, FALSE),
    ('configurator', 'fields',      'analista', TRUE, FALSE, FALSE, FALSE),
    ('configurator', 'indexes',     'analista', TRUE, FALSE, FALSE, FALSE),
    ('configurator', 'functions',   'analista', TRUE, FALSE, FALSE, FALSE),
    ('configurator', 'triggers',    'analista', TRUE, FALSE, FALSE, FALSE),
    ('configurator', 'permissions', 'analista', FALSE, FALSE, FALSE, FALSE),
    ('configurator', 'users',       'analista', FALSE, FALSE, FALSE, FALSE),
    -- Viewer não acessa o configurator
    ('configurator', 'tables',      'viewer', FALSE, FALSE, FALSE, FALSE),
    ('configurator', 'fields',      'viewer', FALSE, FALSE, FALSE, FALSE),
    ('configurator', 'indexes',     'viewer', FALSE, FALSE, FALSE, FALSE),
    ('configurator', 'functions',   'viewer', FALSE, FALSE, FALSE, FALSE),
    ('configurator', 'triggers',    'viewer', FALSE, FALSE, FALSE, FALSE),
    ('configurator', 'permissions', 'viewer', FALSE, FALSE, FALSE, FALSE),
    ('configurator', 'users',       'viewer', FALSE, FALSE, FALSE, FALSE)
ON CONFLICT (par_modulo, par_subsistema, par_papel) DO NOTHING;

-- ---------------------------------------------------------------
-- Dados iniciais do configurator.MEN — menu de navegação padrão
-- ---------------------------------------------------------------
INSERT INTO configurator."MEN"
    (men_label, men_rota, men_icone, men_ordem, men_papel_minimo)
VALUES
    ('Tabelas',     '/config/tables',      'table',      1, 'analista'),
    ('Campos',      '/config/fields',      'columns',    2, 'analista'),
    ('Índices',     '/config/indexes',     'bolt',       3, 'analista'),
    ('Funções',     '/config/functions',   'code',       4, 'gerente'),
    ('Triggers',    '/config/triggers',    'zap',        5, 'gerente'),
    ('Permissões',  '/config/permissions', 'shield',     6, 'admin'),
    ('Usuários',    '/config/users',       'users',      7, 'admin')
ON CONFLICT DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_event_trigger WHERE evtname = 'trg_add_default_columns'
    ) THEN
        EXECUTE 'ALTER EVENT TRIGGER trg_add_default_columns ENABLE';
    END IF;
END;
$$;
