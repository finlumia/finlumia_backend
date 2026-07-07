-- =============================================================
-- MIGRATION 007 — Seed completo do schema configurator
-- Registra todas as tabelas, campos, indices e menu do sistema.
-- Idempotente: pode ser re-executado sem erros.
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
-- Corrigir duplicatas do MEN geradas por execucoes duplas
-- ---------------------------------------------------------------
DELETE FROM configurator."MEN" a
USING configurator."MEN" b
WHERE a.men_key > b.men_key
  AND a.men_label = b.men_label
  AND COALESCE(a.men_rota, '') = COALESCE(b.men_rota, '');

-- ---------------------------------------------------------------
-- TAB — Registro de todas as tabelas gerenciadas
-- ---------------------------------------------------------------
INSERT INTO configurator."TAB" (tab_nome, tab_schema, tab_descricao, tab_status)
VALUES
    -- schema: identify
    ('users',                   'identify',     'Usuarios do sistema com credenciais, perfil e papel',          'ativo'),
    ('token_blacklist',         'identify',     'JTIs de tokens JWT invalidados por logout ou revogacao',       'ativo'),
    ('refresh_tokens',          'identify',     'Tokens de renovacao de sessao JWT dos usuarios',               'ativo'),
    ('roles',                   'identify',     'Papeis disponiveis no sistema (ADMIN, USER etc)',              'ativo'),
    ('resources',               'identify',     'Recursos protegidos por controle de acesso RBAC',              'ativo'),
    ('permissions',             'identify',     'Matriz de permissoes: papel x recurso x operacoes CRUD',       'ativo'),
    ('users_roles',             'identify',     'Vinculo entre usuarios e seus papeis no sistema',              'ativo'),
    ('password_reset_sessions', 'identify',     'Sessoes de recuperacao de senha com OTP e token de reset',     'ativo'),
    -- schema: movement
    ('transactions',            'movement',     'Transacoes financeiras dos usuarios (receitas e despesas)',    'ativo'),
    ('import_jobs',             'movement',     'Jobs de importacao de extrato bancario e OCR',                 'ativo'),
    -- schema: configurator
    ('TAB',                     'configurator', 'Registro de tabelas gerenciadas pelo configurador',            'ativo'),
    ('FIE',                     'configurator', 'Registro de campos e colunas das tabelas gerenciadas',         'ativo'),
    ('IDX',                     'configurator', 'Registro de indices das tabelas gerenciadas',                  'ativo'),
    ('FUN',                     'configurator', 'Registro de funcoes definidas no banco de dados',              'ativo'),
    ('GEN',                     'configurator', 'Registro de triggers (gatilhos) das tabelas',                  'ativo'),
    ('PAR',                     'configurator', 'Matriz de permissoes do modulo configurador por papel',        'ativo'),
    ('MEN',                     'configurator', 'Itens de menu e navegacao lateral do configurador',            'ativo'),
    ('ORD',                     'configurator', 'Ordenacao de exibicao de campos por tabela na interface',      'ativo')
ON CONFLICT (tab_nome, tab_schema) DO NOTHING;

-- ---------------------------------------------------------------
-- FIE — Campos de cada tabela (usando subquery para tab_key)
-- Insercao segura com NOT EXISTS para garantir idempotencia.
-- ---------------------------------------------------------------

-- Helper: insere campo apenas se ainda nao existe
-- identify.users
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'users' AND tab_schema = 'identify' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.def, 'ativo'
    FROM (VALUES
        ('users_key',          'uuid',      NULL, FALSE, TRUE,  FALSE, 'gen_random_uuid()'),
        ('users_email',        'varchar',    255, FALSE, FALSE, FALSE, NULL),
        ('users_senha_hash',   'varchar',    255, TRUE,  FALSE, FALSE, NULL),
        ('users_nome',         'varchar',    255, TRUE,  FALSE, FALSE, NULL),
        ('users_papel',        'varchar',     50, FALSE, FALSE, FALSE, '''viewer'''),
        ('users_status',       'varchar',     20, FALSE, FALSE, FALSE, '''ativo'''),
        ('users_mfa',          'bool',       NULL, FALSE, FALSE, FALSE, 'false'),
        ('users_locale',       'varchar',     10, FALSE, FALSE, FALSE, '''pt-BR'''),
        ('users_tema',         'varchar',     20, FALSE, FALSE, FALSE, '''light'''),
        ('users_ultimo_login', 'timestamp',  NULL, TRUE,  FALSE, FALSE, NULL),
        ('users_ativo',        'bool',       NULL, FALSE, FALSE, FALSE, 'true'),
        ('users_criado_em',    'timestamp',  NULL, FALSE, FALSE, FALSE, 'NOW()'),
        ('users_atualizado_em','timestamp',  NULL, FALSE, FALSE, FALSE, 'NOW()'),
        ('d_e_l_e_t_e',        'bool',       NULL, FALSE, FALSE, FALSE, 'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- identify.token_blacklist
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'token_blacklist' AND tab_schema = 'identify' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.def, 'ativo'
    FROM (VALUES
        ('blacklist_key',        'uuid',      NULL, FALSE, TRUE,  FALSE, 'gen_random_uuid()'),
        ('blacklist_jti',        'text',      NULL, FALSE, FALSE, FALSE, NULL),
        ('blacklist_revogado_em','timestamp', NULL, FALSE, FALSE, FALSE, 'NOW()'),
        ('blacklist_expira_em',  'timestamp', NULL, FALSE, FALSE, FALSE, NULL),
        ('d_e_l_e_t_e',          'bool',      NULL, FALSE, FALSE, FALSE, 'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- identify.refresh_tokens
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'refresh_tokens' AND tab_schema = 'identify' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_ref_tabela, fie_ref_campo, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.ref_tab, col.ref_col, col.def, 'ativo'
    FROM (VALUES
        ('refresh_key',       'uuid',      NULL, FALSE, TRUE,  FALSE, NULL,    NULL,         'gen_random_uuid()'),
        ('refresh_users_key', 'uuid',      NULL, FALSE, FALSE, TRUE,  'users', 'users_key',  NULL),
        ('refresh_token',     'text',      NULL, FALSE, FALSE, FALSE, NULL,    NULL,         NULL),
        ('refresh_expira_em', 'timestamp', NULL, FALSE, FALSE, FALSE, NULL,    NULL,         NULL),
        ('refresh_revogado',  'bool',      NULL, TRUE,  FALSE, FALSE, NULL,    NULL,         'false'),
        ('refresh_criado_em', 'timestamp', NULL, TRUE,  FALSE, FALSE, NULL,    NULL,         'NOW()'),
        ('d_e_l_e_t_e',       'bool',      NULL, TRUE,  FALSE, FALSE, NULL,    NULL,         'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, ref_tab, ref_col, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- identify.roles
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'roles' AND tab_schema = 'identify' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.def, 'ativo'
    FROM (VALUES
        ('role_nome',      'varchar', 50,   FALSE, TRUE,  FALSE, NULL),
        ('role_descricao', 'text',    NULL, TRUE,  FALSE, FALSE, NULL)
    ) AS col(nome, tipo, tam, nulo, pk, fk, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- identify.resources
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'resources' AND tab_schema = 'identify' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.def, 'ativo'
    FROM (VALUES
        ('resource_key',       'uuid',    NULL, FALSE, TRUE,  FALSE, 'gen_random_uuid()'),
        ('resource_nome',      'varchar',  100, FALSE, FALSE, FALSE, NULL),
        ('resource_descricao', 'text',    NULL, TRUE,  FALSE, FALSE, NULL)
    ) AS col(nome, tipo, tam, nulo, pk, fk, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- identify.permissions
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'permissions' AND tab_schema = 'identify' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_ref_tabela, fie_ref_campo, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.ref_tab, col.ref_col, col.def, 'ativo'
    FROM (VALUES
        ('permission_key',          'uuid',    NULL, FALSE, TRUE,  FALSE, NULL,        NULL,           'gen_random_uuid()'),
        ('permission_role_nome',    'varchar',   50, FALSE, FALSE, TRUE,  'roles',     'role_nome',    NULL),
        ('permission_resource_key', 'uuid',    NULL, FALSE, FALSE, TRUE,  'resources', 'resource_key', NULL),
        ('permission_can_create',   'bool',    NULL, FALSE, FALSE, FALSE, NULL,        NULL,           'false'),
        ('permission_can_read',     'bool',    NULL, FALSE, FALSE, FALSE, NULL,        NULL,           'false'),
        ('permission_can_update',   'bool',    NULL, FALSE, FALSE, FALSE, NULL,        NULL,           'false'),
        ('permission_can_delete',   'bool',    NULL, FALSE, FALSE, FALSE, NULL,        NULL,           'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, ref_tab, ref_col, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- identify.users_roles
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'users_roles' AND tab_schema = 'identify' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_ref_tabela, fie_ref_campo, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.ref_tab, col.ref_col, col.def, 'ativo'
    FROM (VALUES
        ('users_roles_key',      'uuid',    NULL, FALSE, TRUE,  FALSE, NULL,    NULL,         'gen_random_uuid()'),
        ('users_roles_user_key', 'uuid',    NULL, FALSE, FALSE, TRUE,  'users', 'users_key',  NULL),
        ('users_roles_role',     'varchar',   50, FALSE, FALSE, TRUE,  'roles', 'role_nome',  NULL)
    ) AS col(nome, tipo, tam, nulo, pk, fk, ref_tab, ref_col, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- identify.password_reset_sessions
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'password_reset_sessions' AND tab_schema = 'identify' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.def, 'ativo'
    FROM (VALUES
        ('prs_email',         'varchar', 255, FALSE, FALSE, FALSE, NULL),
        ('prs_otp_hash',      'varchar', 255, TRUE,  FALSE, FALSE, NULL),
        ('prs_expires_at',    'timestamp',NULL,FALSE, FALSE, FALSE, NULL),
        ('prs_reset_session', 'varchar', 255, TRUE,  FALSE, FALSE, NULL),
        ('d_e_l_e_t_e',       'bool',    NULL, FALSE, FALSE, FALSE, 'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- movement.transactions
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'transactions' AND tab_schema = 'movement' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.def, 'ativo'
    FROM (VALUES
        ('transaction_id',   'uuid',      NULL, FALSE, TRUE,  FALSE, 'gen_random_uuid()'),
        ('user_key',         'bigint',    NULL, FALSE, FALSE, FALSE, NULL),
        ('type',             'varchar',    10,  FALSE, FALSE, FALSE, NULL),
        ('method',           'varchar',    20,  FALSE, FALSE, FALSE, NULL),
        ('institution',      'varchar',    20,  FALSE, FALSE, FALSE, NULL),
        ('date',             'timestamp', NULL, FALSE, FALSE, FALSE, NULL),
        ('category',         'varchar',    20,  FALSE, FALSE, FALSE, NULL),
        ('description',      'varchar',   500,  FALSE, FALSE, FALSE, NULL),
        ('sub_description',  'varchar',   255,  TRUE,  FALSE, FALSE, NULL),
        ('amount',           'decimal',   NULL, FALSE, FALSE, FALSE, NULL),
        ('notes',            'text',      NULL, TRUE,  FALSE, FALSE, NULL),
        ('tags',             'text',      NULL, TRUE,  FALSE, FALSE, NULL),
        ('is_recurring',     'bool',      NULL, FALSE, FALSE, FALSE, 'false'),
        ('recurring_id',     'uuid',      NULL, TRUE,  FALSE, FALSE, NULL),
        ('created_at',       'timestamp', NULL, FALSE, FALSE, FALSE, 'NOW()'),
        ('updated_at',       'timestamp', NULL, FALSE, FALSE, FALSE, 'NOW()'),
        ('d_e_l_e_t_e',      'bool',      NULL, FALSE, FALSE, FALSE, 'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- movement.import_jobs
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'import_jobs' AND tab_schema = 'movement' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.def, 'ativo'
    FROM (VALUES
        ('job_id',          'uuid',      NULL, FALSE, TRUE,  FALSE, 'gen_random_uuid()'),
        ('user_key',        'bigint',    NULL, FALSE, FALSE, FALSE, NULL),
        ('status',          'varchar',    20,  FALSE, FALSE, FALSE, '''pending'''),
        ('file_name',       'varchar',   255,  TRUE,  FALSE, FALSE, NULL),
        ('file_type',       'varchar',    10,  TRUE,  FALSE, FALSE, NULL),
        ('total_rows',      'integer',   NULL, TRUE,  FALSE, FALSE, NULL),
        ('imported_rows',   'integer',   NULL, TRUE,  FALSE, FALSE, NULL),
        ('errors',          'text',      NULL, TRUE,  FALSE, FALSE, NULL),
        ('ocr_description', 'varchar',   500,  TRUE,  FALSE, FALSE, NULL),
        ('ocr_amount',      'decimal',   NULL, TRUE,  FALSE, FALSE, NULL),
        ('ocr_date',        'timestamp', NULL, TRUE,  FALSE, FALSE, NULL),
        ('ocr_category',    'varchar',    20,  TRUE,  FALSE, FALSE, NULL),
        ('ocr_method',      'varchar',    20,  TRUE,  FALSE, FALSE, NULL),
        ('ocr_confidence',  'decimal',   NULL, TRUE,  FALSE, FALSE, NULL),
        ('created_at',      'timestamp', NULL, FALSE, FALSE, FALSE, 'NOW()'),
        ('d_e_l_e_t_e',     'bool',      NULL, FALSE, FALSE, FALSE, 'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- configurator.TAB
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'TAB' AND tab_schema = 'configurator' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.def, 'ativo'
    FROM (VALUES
        ('tab_key',           'uuid',    NULL, FALSE, TRUE,  FALSE, 'gen_random_uuid()'),
        ('tab_nome',          'varchar',   63, FALSE, FALSE, FALSE, NULL),
        ('tab_schema',        'varchar',   20, FALSE, FALSE, FALSE, '''public'''),
        ('tab_descricao',     'text',    NULL, TRUE,  FALSE, FALSE, NULL),
        ('tab_status',        'varchar',   10, FALSE, FALSE, FALSE, '''ativo'''),
        ('tab_criado_em',     'timestamp',NULL, FALSE, FALSE, FALSE, 'NOW()'),
        ('tab_atualizado_em', 'timestamp',NULL, FALSE, FALSE, FALSE, 'NOW()'),
        ('d_e_l_e_t_e',       'bool',    NULL, FALSE, FALSE, FALSE, 'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- configurator.FIE
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'FIE' AND tab_schema = 'configurator' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_ref_tabela, fie_ref_campo, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.ref_tab, col.ref_col, col.def, 'ativo'
    FROM (VALUES
        ('fie_key',           'uuid',    NULL, FALSE, TRUE,  FALSE, NULL,  NULL,      'gen_random_uuid()'),
        ('fie_nome',          'varchar',   63, FALSE, FALSE, FALSE, NULL,  NULL,      NULL),
        ('fie_tab_key',       'uuid',    NULL, FALSE, FALSE, TRUE,  'TAB', 'tab_key', NULL),
        ('fie_tipo_dado',     'varchar',   20, FALSE, FALSE, FALSE, NULL,  NULL,      NULL),
        ('fie_tamanho',       'integer', NULL, TRUE,  FALSE, FALSE, NULL,  NULL,      NULL),
        ('fie_nulo',          'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'true'),
        ('fie_default',       'varchar', 255, TRUE,  FALSE, FALSE,  NULL,  NULL,      NULL),
        ('fie_e_primario',    'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'false'),
        ('fie_e_estrangeiro', 'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'false'),
        ('fie_ref_tabela',    'varchar',   63, TRUE,  FALSE, FALSE, NULL,  NULL,      NULL),
        ('fie_ref_campo',     'varchar',   63, TRUE,  FALSE, FALSE, NULL,  NULL,      NULL),
        ('fie_status',        'varchar',   10, FALSE, FALSE, FALSE, NULL,  NULL,      '''ativo'''),
        ('fie_criado_em',     'timestamp',NULL, FALSE, FALSE, FALSE, NULL, NULL,      'NOW()'),
        ('d_e_l_e_t_e',       'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, ref_tab, ref_col, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- configurator.IDX
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'IDX' AND tab_schema = 'configurator' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_ref_tabela, fie_ref_campo, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.ref_tab, col.ref_col, col.def, 'ativo'
    FROM (VALUES
        ('idx_key',     'uuid',    NULL, FALSE, TRUE,  FALSE, NULL,  NULL,      'gen_random_uuid()'),
        ('idx_nome',    'varchar',   63, FALSE, FALSE, FALSE, NULL,  NULL,      NULL),
        ('idx_tab_key', 'uuid',    NULL, FALSE, FALSE, TRUE,  'TAB', 'tab_key', NULL),
        ('idx_schema',  'varchar',   20, FALSE, FALSE, FALSE, NULL,  NULL,      '''public'''),
        ('idx_campos',  'text',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      NULL),
        ('idx_tipo',    'varchar',   10, FALSE, FALSE, FALSE, NULL,  NULL,      '''btree'''),
        ('idx_unico',   'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'false'),
        ('idx_parcial', 'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'false'),
        ('idx_where',   'text',    NULL, TRUE,  FALSE, FALSE, NULL,  NULL,      NULL),
        ('idx_status',  'varchar',   10, FALSE, FALSE, FALSE, NULL,  NULL,      '''ativo'''),
        ('idx_criado_em','timestamp',NULL,FALSE, FALSE, FALSE, NULL, NULL,      'NOW()'),
        ('d_e_l_e_t_e', 'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, ref_tab, ref_col, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- configurator.FUN
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'FUN' AND tab_schema = 'configurator' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.def, 'ativo'
    FROM (VALUES
        ('fun_key',           'uuid',    NULL, FALSE, TRUE,  FALSE, 'gen_random_uuid()'),
        ('fun_nome',          'varchar',   63, FALSE, FALSE, FALSE, NULL),
        ('fun_schema',        'varchar',   20, FALSE, FALSE, FALSE, '''public'''),
        ('fun_linguagem',     'varchar',   20, FALSE, FALSE, FALSE, '''plpgsql'''),
        ('fun_tipo_retorno',  'varchar',   63, FALSE, FALSE, FALSE, '''void'''),
        ('fun_args',          'text',    NULL, TRUE,  FALSE, FALSE, NULL),
        ('fun_volatilidade',  'varchar',   10, FALSE, FALSE, FALSE, '''VOLATILE'''),
        ('fun_corpo',         'text',    NULL, FALSE, FALSE, FALSE, NULL),
        ('fun_descricao',     'text',    NULL, TRUE,  FALSE, FALSE, NULL),
        ('fun_status',        'varchar',   10, FALSE, FALSE, FALSE, '''ativo'''),
        ('fun_criado_em',     'timestamp',NULL, FALSE, FALSE, FALSE, 'NOW()'),
        ('fun_atualizado_em', 'timestamp',NULL, FALSE, FALSE, FALSE, 'NOW()'),
        ('d_e_l_e_t_e',       'bool',    NULL, FALSE, FALSE, FALSE, 'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- configurator.GEN
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'GEN' AND tab_schema = 'configurator' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_ref_tabela, fie_ref_campo, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.ref_tab, col.ref_col, col.def, 'ativo'
    FROM (VALUES
        ('gen_key',        'uuid',    NULL, FALSE, TRUE,  FALSE, NULL,  NULL,      'gen_random_uuid()'),
        ('gen_nome',       'varchar',   63, FALSE, FALSE, FALSE, NULL,  NULL,      NULL),
        ('gen_tab_key',    'uuid',    NULL, FALSE, FALSE, TRUE,  'TAB', 'tab_key', NULL),
        ('gen_schema',     'varchar',   20, FALSE, FALSE, FALSE, NULL,  NULL,      '''public'''),
        ('gen_evento',     'varchar',   10, FALSE, FALSE, FALSE, NULL,  NULL,      NULL),
        ('gen_timing',     'varchar',   12, FALSE, FALSE, FALSE, NULL,  NULL,      '''AFTER'''),
        ('gen_funcao',     'varchar',   63, FALSE, FALSE, FALSE, NULL,  NULL,      NULL),
        ('gen_habilitado', 'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'true'),
        ('gen_descricao',  'text',    NULL, TRUE,  FALSE, FALSE, NULL,  NULL,      NULL),
        ('gen_status',     'varchar',   10, FALSE, FALSE, FALSE, NULL,  NULL,      '''ativo'''),
        ('gen_criado_em',  'timestamp',NULL, FALSE, FALSE, FALSE, NULL, NULL,      'NOW()'),
        ('d_e_l_e_t_e',    'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, ref_tab, ref_col, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- configurator.PAR
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'PAR' AND tab_schema = 'configurator' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.def, 'ativo'
    FROM (VALUES
        ('par_key',           'uuid',    NULL, FALSE, TRUE,  FALSE, 'gen_random_uuid()'),
        ('par_modulo',        'varchar',  100, FALSE, FALSE, FALSE, NULL),
        ('par_subsistema',    'varchar',  100, FALSE, FALSE, FALSE, NULL),
        ('par_papel',         'varchar',   50, FALSE, FALSE, FALSE, NULL),
        ('par_pode_ler',      'bool',    NULL, FALSE, FALSE, FALSE, 'false'),
        ('par_pode_escrever', 'bool',    NULL, FALSE, FALSE, FALSE, 'false'),
        ('par_pode_deletar',  'bool',    NULL, FALSE, FALSE, FALSE, 'false'),
        ('par_pode_admin',    'bool',    NULL, FALSE, FALSE, FALSE, 'false'),
        ('d_e_l_e_t_e',       'bool',    NULL, FALSE, FALSE, FALSE, 'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- configurator.MEN
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'MEN' AND tab_schema = 'configurator' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_ref_tabela, fie_ref_campo, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.ref_tab, col.ref_col, col.def, 'ativo'
    FROM (VALUES
        ('men_key',         'uuid',    NULL, FALSE, TRUE,  FALSE, NULL,  NULL,      'gen_random_uuid()'),
        ('men_label',       'varchar',  100, FALSE, FALSE, FALSE, NULL,  NULL,      NULL),
        ('men_rota',        'varchar',  255, TRUE,  FALSE, FALSE, NULL,  NULL,      NULL),
        ('men_icone',       'varchar',   50, TRUE,  FALSE, FALSE, NULL,  NULL,      NULL),
        ('men_pai_key',     'uuid',    NULL, TRUE,  FALSE, TRUE,  'MEN', 'men_key', NULL),
        ('men_ordem',       'integer', NULL, FALSE, FALSE, FALSE, NULL,  NULL,      '0'),
        ('men_papel_minimo','varchar',   50, TRUE,  FALSE, FALSE, NULL,  NULL,      '''viewer'''),
        ('men_ativo',       'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'true'),
        ('d_e_l_e_t_e',     'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, ref_tab, ref_col, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- configurator.ORD
DO $$
DECLARE v_tab UUID;
BEGIN
    SELECT tab_key INTO v_tab FROM configurator."TAB"
    WHERE tab_nome = 'ORD' AND tab_schema = 'configurator' AND d_e_l_e_t_e = FALSE;
    IF v_tab IS NULL THEN RETURN; END IF;

    INSERT INTO configurator."FIE"
        (fie_nome, fie_tab_key, fie_tipo_dado, fie_tamanho, fie_nulo, fie_e_primario, fie_e_estrangeiro, fie_ref_tabela, fie_ref_campo, fie_default, fie_status)
    SELECT col.nome, v_tab, col.tipo, col.tam::integer, col.nulo, col.pk, col.fk, col.ref_tab, col.ref_col, col.def, 'ativo'
    FROM (VALUES
        ('ord_key',     'uuid',    NULL, FALSE, TRUE,  FALSE, NULL,  NULL,      'gen_random_uuid()'),
        ('ord_tab_key', 'uuid',    NULL, FALSE, FALSE, TRUE,  'TAB', 'tab_key', NULL),
        ('ord_fie_key', 'uuid',    NULL, FALSE, FALSE, TRUE,  'FIE', 'fie_key', NULL),
        ('ord_posicao', 'integer', NULL, FALSE, FALSE, FALSE, NULL,  NULL,      '0'),
        ('d_e_l_e_t_e', 'bool',    NULL, FALSE, FALSE, FALSE, NULL,  NULL,      'false')
    ) AS col(nome, tipo, tam, nulo, pk, fk, ref_tab, ref_col, def)
    WHERE NOT EXISTS (
        SELECT 1 FROM configurator."FIE"
        WHERE fie_nome = col.nome AND fie_tab_key = v_tab AND d_e_l_e_t_e = FALSE
    );
END;
$$;

-- ---------------------------------------------------------------
-- IDX — Indices relevantes registrados no configurador
-- ---------------------------------------------------------------
DO $$
DECLARE
    v_tab_users              UUID;
    v_tab_token              UUID;
    v_tab_refresh            UUID;
    v_tab_permissions        UUID;
    v_tab_users_roles        UUID;
    v_tab_prs                UUID;
    v_tab_transactions       UUID;
    v_tab_import_jobs        UUID;
    v_tab_tab                UUID;
    v_tab_fie                UUID;
    v_tab_idx                UUID;
    v_tab_gen                UUID;
    v_tab_par                UUID;
BEGIN
    SELECT tab_key INTO v_tab_users        FROM configurator."TAB" WHERE tab_nome='users'                   AND tab_schema='identify'     AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_token        FROM configurator."TAB" WHERE tab_nome='token_blacklist'         AND tab_schema='identify'     AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_refresh      FROM configurator."TAB" WHERE tab_nome='refresh_tokens'         AND tab_schema='identify'     AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_permissions  FROM configurator."TAB" WHERE tab_nome='permissions'            AND tab_schema='identify'     AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_users_roles  FROM configurator."TAB" WHERE tab_nome='users_roles'            AND tab_schema='identify'     AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_prs          FROM configurator."TAB" WHERE tab_nome='password_reset_sessions' AND tab_schema='identify'    AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_transactions FROM configurator."TAB" WHERE tab_nome='transactions'           AND tab_schema='movement'     AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_import_jobs  FROM configurator."TAB" WHERE tab_nome='import_jobs'            AND tab_schema='movement'     AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_tab          FROM configurator."TAB" WHERE tab_nome='TAB'                    AND tab_schema='configurator' AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_fie          FROM configurator."TAB" WHERE tab_nome='FIE'                    AND tab_schema='configurator' AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_idx          FROM configurator."TAB" WHERE tab_nome='IDX'                    AND tab_schema='configurator' AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_gen          FROM configurator."TAB" WHERE tab_nome='GEN'                    AND tab_schema='configurator' AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_par          FROM configurator."TAB" WHERE tab_nome='PAR'                    AND tab_schema='configurator' AND d_e_l_e_t_e=FALSE;

    INSERT INTO configurator."IDX"
        (idx_nome, idx_tab_key, idx_schema, idx_campos, idx_tipo, idx_unico, idx_parcial, idx_where, idx_status)
    VALUES
        -- identify.users
        ('users_pkey',                  v_tab_users,       'identify',     'users_key',             'btree', TRUE,  FALSE, NULL,                                  'ativo'),
        ('users_users_email_key',       v_tab_users,       'identify',     'users_email',           'btree', TRUE,  FALSE, NULL,                                  'ativo'),
        -- identify.token_blacklist
        ('token_blacklist_pkey',                  v_tab_token,  'identify', 'blacklist_key',         'btree', TRUE,  FALSE, NULL,                                  'ativo'),
        ('token_blacklist_blacklist_jti_key',     v_tab_token,  'identify', 'blacklist_jti',         'btree', TRUE,  FALSE, NULL,                                  'ativo'),
        ('ix_identify_token_blacklist_expira',    v_tab_token,  'identify', 'blacklist_expira_em',   'btree', FALSE, TRUE,  'd_e_l_e_t_e = FALSE',                 'ativo'),
        -- identify.refresh_tokens
        ('refresh_tokens_pkey',                        v_tab_refresh, 'identify', 'refresh_key',        'btree', TRUE,  FALSE, NULL,                             'ativo'),
        ('refresh_tokens_refresh_token_key',           v_tab_refresh, 'identify', 'refresh_token',      'btree', TRUE,  FALSE, NULL,                             'ativo'),
        ('ix_identify_refresh_tokens_user_active',     v_tab_refresh, 'identify', 'refresh_users_key',  'btree', FALSE, TRUE,  'd_e_l_e_t_e = FALSE AND refresh_revogado = FALSE', 'ativo'),
        -- identify.permissions
        ('permissions_pkey',                              v_tab_permissions, 'identify', 'permission_key',                               'btree', TRUE,  FALSE, NULL,  'ativo'),
        ('ix_identify_permissions_role',                  v_tab_permissions, 'identify', 'permission_role_nome',                         'btree', FALSE, FALSE, NULL,  'ativo'),
        ('ix_identify_permissions_resource',              v_tab_permissions, 'identify', 'permission_resource_key',                      'btree', FALSE, FALSE, NULL,  'ativo'),
        ('permissions_role_resource_key',                 v_tab_permissions, 'identify', 'permission_role_nome, permission_resource_key', 'btree', TRUE,  FALSE, NULL,  'ativo'),
        -- identify.users_roles
        ('users_roles_pkey',                          v_tab_users_roles, 'identify', 'users_roles_key',                               'btree', TRUE,  FALSE, NULL,  'ativo'),
        ('ix_identify_users_roles_user',              v_tab_users_roles, 'identify', 'users_roles_user_key',                          'btree', FALSE, FALSE, NULL,  'ativo'),
        ('users_roles_user_role_key',                 v_tab_users_roles, 'identify', 'users_roles_user_key, users_roles_role',        'btree', TRUE,  FALSE, NULL,  'ativo'),
        -- identify.password_reset_sessions
        ('ix_identify_prs_email',   v_tab_prs, 'identify', 'prs_email',          'btree', FALSE, TRUE, 'd_e_l_e_t_e = FALSE',                                   'ativo'),
        ('ix_identify_prs_session', v_tab_prs, 'identify', 'prs_reset_session',  'btree', FALSE, TRUE, 'd_e_l_e_t_e = FALSE AND prs_reset_session IS NOT NULL',  'ativo'),
        -- movement.transactions
        ('transactions_pkey',              v_tab_transactions, 'movement', 'transaction_id',            'btree', TRUE,  FALSE, NULL,                             'ativo'),
        ('ix_movement_transactions_user',  v_tab_transactions, 'movement', 'user_key',                  'btree', FALSE, TRUE,  'd_e_l_e_t_e = FALSE',            'ativo'),
        ('ix_movement_transactions_date',  v_tab_transactions, 'movement', 'user_key, date',            'btree', FALSE, TRUE,  'd_e_l_e_t_e = FALSE',            'ativo'),
        -- movement.import_jobs
        ('import_jobs_pkey',              v_tab_import_jobs, 'movement', 'job_id',   'btree', TRUE,  FALSE, NULL,                  'ativo'),
        ('ix_movement_import_jobs_user',  v_tab_import_jobs, 'movement', 'user_key', 'btree', FALSE, TRUE,  'd_e_l_e_t_e = FALSE', 'ativo'),
        -- configurator.TAB
        ('TAB_pkey',                  v_tab_tab, 'configurator', 'tab_key',            'btree', TRUE,  FALSE, NULL,                  'ativo'),
        ('TAB_nome_schema_key',       v_tab_tab, 'configurator', 'tab_nome, tab_schema','btree', TRUE,  FALSE, NULL,                  'ativo'),
        ('ix_configurator_tab_schema',v_tab_tab, 'configurator', 'tab_schema',          'btree', FALSE, TRUE,  'd_e_l_e_t_e = FALSE', 'ativo'),
        -- configurator.FIE
        ('FIE_pkey',                 v_tab_fie, 'configurator', 'fie_key',    'btree', TRUE,  FALSE, NULL,                  'ativo'),
        ('ix_configurator_fie_tab',  v_tab_fie, 'configurator', 'fie_tab_key','btree', FALSE, TRUE,  'd_e_l_e_t_e = FALSE', 'ativo'),
        -- configurator.IDX
        ('IDX_pkey',                   v_tab_idx, 'configurator', 'idx_key',             'btree', TRUE,  FALSE, NULL,                  'ativo'),
        ('IDX_nome_schema_key',        v_tab_idx, 'configurator', 'idx_nome, idx_schema', 'btree', TRUE,  FALSE, NULL,                  'ativo'),
        ('ix_configurator_idx_tab',    v_tab_idx, 'configurator', 'idx_tab_key',          'btree', FALSE, TRUE,  'd_e_l_e_t_e = FALSE', 'ativo'),
        -- configurator.GEN
        ('GEN_pkey',                   v_tab_gen, 'configurator', 'gen_key',              'btree', TRUE,  FALSE, NULL,                  'ativo'),
        ('GEN_nome_tab_key',           v_tab_gen, 'configurator', 'gen_nome, gen_tab_key','btree', TRUE,  FALSE, NULL,                  'ativo'),
        ('ix_configurator_gen_tab',    v_tab_gen, 'configurator', 'gen_tab_key',          'btree', FALSE, TRUE,  'd_e_l_e_t_e = FALSE', 'ativo'),
        -- configurator.PAR
        ('PAR_pkey',                         v_tab_par, 'configurator', 'par_key',                              'btree', TRUE,  FALSE, NULL,                  'ativo'),
        ('PAR_modulo_subsistema_papel_key',  v_tab_par, 'configurator', 'par_modulo, par_subsistema, par_papel','btree', TRUE,  FALSE, NULL,                  'ativo'),
        ('ix_configurator_par_modulo',       v_tab_par, 'configurator', 'par_modulo',                          'btree', FALSE, TRUE,  'd_e_l_e_t_e = FALSE', 'ativo'),
        ('ix_configurator_par_papel',        v_tab_par, 'configurator', 'par_papel',                           'btree', FALSE, TRUE,  'd_e_l_e_t_e = FALSE', 'ativo')
    ON CONFLICT (idx_nome, idx_schema) DO NOTHING;
END;
$$;

-- ---------------------------------------------------------------
-- FUN — Funcoes uteis do banco de dados
-- ---------------------------------------------------------------
INSERT INTO configurator."FUN"
    (fun_nome, fun_schema, fun_linguagem, fun_tipo_retorno, fun_args, fun_volatilidade, fun_corpo, fun_descricao, fun_status)
VALUES
    (
        'fn_soft_delete_users',
        'identify',
        'plpgsql',
        'trigger',
        NULL,
        'VOLATILE',
        $fn$
BEGIN
    NEW.d_e_l_e_t_e := TRUE;
    NEW.users_atualizado_em := NOW();
    RETURN NEW;
END;
        $fn$,
        'Funcao de trigger para marcar usuario como deletado (soft delete)',
        'ativo'
    ),
    (
        'fn_update_timestamp',
        'public',
        'plpgsql',
        'trigger',
        NULL,
        'VOLATILE',
        $fn$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
        $fn$,
        'Atualiza automaticamente o campo updated_at antes de cada UPDATE',
        'ativo'
    ),
    (
        'fn_revoke_user_tokens',
        'identify',
        'plpgsql',
        'void',
        'p_user_key UUID',
        'VOLATILE',
        $fn$
BEGIN
    UPDATE identify.refresh_tokens
    SET refresh_revogado = TRUE
    WHERE refresh_users_key = p_user_key
      AND refresh_revogado = FALSE
      AND d_e_l_e_t_e = FALSE;
END;
        $fn$,
        'Revoga todos os refresh tokens ativos de um usuario',
        'ativo'
    ),
    (
        'fn_cleanup_expired_blacklist',
        'identify',
        'plpgsql',
        'integer',
        NULL,
        'VOLATILE',
        $fn$
DECLARE
    v_deleted INTEGER;
BEGIN
    UPDATE identify.token_blacklist
    SET d_e_l_e_t_e = TRUE
    WHERE blacklist_expira_em < NOW()
      AND d_e_l_e_t_e = FALSE;
    GET DIAGNOSTICS v_deleted = ROW_COUNT;
    RETURN v_deleted;
END;
        $fn$,
        'Remove tokens expirados da blacklist para manutencao do banco',
        'ativo'
    ),
    (
        'fn_user_has_permission',
        'identify',
        'plpgsql',
        'boolean',
        'p_user_key UUID, p_resource_nome VARCHAR, p_operation VARCHAR',
        'STABLE',
        $fn$
DECLARE
    v_result BOOLEAN := FALSE;
BEGIN
    SELECT TRUE INTO v_result
    FROM identify.users_roles ur
    JOIN identify.permissions p ON p.permission_role_nome = ur.users_roles_role
    JOIN identify.resources r ON r.resource_key = p.permission_resource_key
    WHERE ur.users_roles_user_key = p_user_key
      AND r.resource_nome = p_resource_nome
      AND CASE p_operation
            WHEN 'create' THEN p.permission_can_create
            WHEN 'read'   THEN p.permission_can_read
            WHEN 'update' THEN p.permission_can_update
            WHEN 'delete' THEN p.permission_can_delete
            ELSE FALSE
          END = TRUE
    LIMIT 1;
    RETURN COALESCE(v_result, FALSE);
END;
        $fn$,
        'Verifica se um usuario tem permissao para uma operacao em um recurso',
        'ativo'
    )
ON CONFLICT (fun_nome, fun_schema) DO NOTHING;

-- ---------------------------------------------------------------
-- GEN — Triggers registrados
-- ---------------------------------------------------------------
DO $$
DECLARE
    v_tab_users        UUID;
    v_tab_transactions UUID;
BEGIN
    SELECT tab_key INTO v_tab_users        FROM configurator."TAB" WHERE tab_nome='users'        AND tab_schema='identify' AND d_e_l_e_t_e=FALSE;
    SELECT tab_key INTO v_tab_transactions FROM configurator."TAB" WHERE tab_nome='transactions' AND tab_schema='movement' AND d_e_l_e_t_e=FALSE;

    IF v_tab_users IS NOT NULL THEN
        INSERT INTO configurator."GEN"
            (gen_nome, gen_tab_key, gen_schema, gen_evento, gen_timing, gen_funcao, gen_habilitado, gen_descricao, gen_status)
        VALUES
            (
                'trg_users_soft_delete',
                v_tab_users,
                'identify',
                'DELETE',
                'BEFORE',
                'fn_soft_delete_users',
                TRUE,
                'Intercepta DELETE e converte em soft delete atualizando d_e_l_e_t_e e users_atualizado_em',
                'ativo'
            )
        ON CONFLICT (gen_nome, gen_tab_key) DO NOTHING;
    END IF;

    IF v_tab_transactions IS NOT NULL THEN
        INSERT INTO configurator."GEN"
            (gen_nome, gen_tab_key, gen_schema, gen_evento, gen_timing, gen_funcao, gen_habilitado, gen_descricao, gen_status)
        VALUES
            (
                'trg_transactions_update_ts',
                v_tab_transactions,
                'movement',
                'UPDATE',
                'BEFORE',
                'fn_update_timestamp',
                TRUE,
                'Atualiza automaticamente updated_at em cada UPDATE na tabela de transacoes',
                'ativo'
            )
        ON CONFLICT (gen_nome, gen_tab_key) DO NOTHING;
    END IF;
END;
$$;

-- ---------------------------------------------------------------
-- ORD — Ordenacao de exibicao dos campos por tabela
-- Registra a ordem padrao para as tabelas principais.
-- ---------------------------------------------------------------
DO $$
DECLARE
    v_tab UUID;
    v_fie UUID;
    v_pos INTEGER;
    rec   RECORD;
BEGIN
    FOR rec IN
        SELECT t.tab_key, f.fie_key,
               ROW_NUMBER() OVER (PARTITION BY f.fie_tab_key ORDER BY
                   CASE f.fie_e_primario WHEN TRUE THEN 0 ELSE 1 END,
                   f.fie_criado_em
               ) AS pos
        FROM configurator."FIE" f
        JOIN configurator."TAB" t ON t.tab_key = f.fie_tab_key
        WHERE f.d_e_l_e_t_e = FALSE AND t.d_e_l_e_t_e = FALSE
    LOOP
        INSERT INTO configurator."ORD" (ord_tab_key, ord_fie_key, ord_posicao)
        VALUES (rec.tab_key, rec.fie_key, rec.pos::INTEGER)
        ON CONFLICT (ord_tab_key, ord_fie_key) DO UPDATE SET ord_posicao = EXCLUDED.ord_posicao;
    END LOOP;
END;
$$;

-- ---------------------------------------------------------------
-- PAR — Completar matriz de permissoes (modulos identify e movement)
-- ---------------------------------------------------------------
INSERT INTO configurator."PAR"
    (par_modulo, par_subsistema, par_papel, par_pode_ler, par_pode_escrever, par_pode_deletar, par_pode_admin)
VALUES
    -- identify — admin
    ('identify', 'users',                   'admin',    TRUE,  TRUE,  TRUE,  TRUE),
    ('identify', 'roles',                   'admin',    TRUE,  TRUE,  TRUE,  TRUE),
    ('identify', 'resources',               'admin',    TRUE,  TRUE,  TRUE,  TRUE),
    ('identify', 'permissions',             'admin',    TRUE,  TRUE,  TRUE,  TRUE),
    ('identify', 'tokens',                  'admin',    TRUE,  FALSE, TRUE,  TRUE),
    -- identify — gerente
    ('identify', 'users',                   'gerente',  TRUE,  TRUE,  FALSE, FALSE),
    ('identify', 'roles',                   'gerente',  TRUE,  FALSE, FALSE, FALSE),
    ('identify', 'resources',               'gerente',  TRUE,  FALSE, FALSE, FALSE),
    ('identify', 'permissions',             'gerente',  TRUE,  FALSE, FALSE, FALSE),
    ('identify', 'tokens',                  'gerente',  FALSE, FALSE, FALSE, FALSE),
    -- identify — analista
    ('identify', 'users',                   'analista', TRUE,  FALSE, FALSE, FALSE),
    ('identify', 'roles',                   'analista', TRUE,  FALSE, FALSE, FALSE),
    ('identify', 'resources',               'analista', TRUE,  FALSE, FALSE, FALSE),
    ('identify', 'permissions',             'analista', FALSE, FALSE, FALSE, FALSE),
    ('identify', 'tokens',                  'analista', FALSE, FALSE, FALSE, FALSE),
    -- identify — viewer
    ('identify', 'users',                   'viewer',   FALSE, FALSE, FALSE, FALSE),
    ('identify', 'roles',                   'viewer',   FALSE, FALSE, FALSE, FALSE),
    ('identify', 'resources',               'viewer',   FALSE, FALSE, FALSE, FALSE),
    ('identify', 'permissions',             'viewer',   FALSE, FALSE, FALSE, FALSE),
    ('identify', 'tokens',                  'viewer',   FALSE, FALSE, FALSE, FALSE),
    -- movement — admin
    ('movement', 'transactions',            'admin',    TRUE,  TRUE,  TRUE,  TRUE),
    ('movement', 'import_jobs',             'admin',    TRUE,  TRUE,  TRUE,  TRUE),
    -- movement — gerente
    ('movement', 'transactions',            'gerente',  TRUE,  TRUE,  FALSE, FALSE),
    ('movement', 'import_jobs',             'gerente',  TRUE,  TRUE,  FALSE, FALSE),
    -- movement — analista
    ('movement', 'transactions',            'analista', TRUE,  FALSE, FALSE, FALSE),
    ('movement', 'import_jobs',             'analista', TRUE,  FALSE, FALSE, FALSE),
    -- movement — viewer
    ('movement', 'transactions',            'viewer',   TRUE,  FALSE, FALSE, FALSE),
    ('movement', 'import_jobs',             'viewer',   FALSE, FALSE, FALSE, FALSE)
ON CONFLICT (par_modulo, par_subsistema, par_papel) DO NOTHING;

-- ---------------------------------------------------------------
-- MEN — Garantir menu completo e sem duplicatas
-- MEN nao tem unique constraint alem da PK, entao usa WHERE NOT EXISTS.
-- ---------------------------------------------------------------
INSERT INTO configurator."MEN"
    (men_label, men_rota, men_icone, men_ordem, men_papel_minimo, men_ativo)
SELECT v.label, v.rota, v.icone, v.ordem::INTEGER, v.papel, v.ativo::BOOLEAN
FROM (VALUES
    ('Tabelas',    '/config/tables',      'table',   1, 'analista', TRUE),
    ('Campos',     '/config/fields',      'columns', 2, 'analista', TRUE),
    ('Indices',    '/config/indexes',     'bolt',    3, 'analista', TRUE),
    ('Funcoes',    '/config/functions',   'code',    4, 'gerente',  TRUE),
    ('Triggers',   '/config/triggers',    'zap',     5, 'gerente',  TRUE),
    ('Permissoes', '/config/permissions', 'shield',  6, 'admin',    TRUE),
    ('Usuarios',   '/config/users',       'users',   7, 'admin',    TRUE)
) AS v(label, rota, icone, ordem, papel, ativo)
WHERE NOT EXISTS (
    SELECT 1 FROM configurator."MEN"
    WHERE men_label = v.label
      AND COALESCE(men_rota, '') = COALESCE(v.rota, '')
      AND d_e_l_e_t_e = FALSE
);

-- ---------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_event_trigger WHERE evtname = 'trg_add_default_columns'
    ) THEN
        EXECUTE 'ALTER EVENT TRIGGER trg_add_default_columns ENABLE';
    END IF;
END;
$$;
