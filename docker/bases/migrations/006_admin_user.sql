-- =============================================================
-- MIGRATION 006 — Usuário administrador inicial
-- Administrador: Thiago Benevide de Moraes
-- E-mail:        thiagobenevide@live.com
-- Senha inicial: Br@sil2002  (BCrypt strength 10)
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
-- 1. Inserir usuário na tabela identify.users
-- ---------------------------------------------------------------
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
    SET users_nome            = EXCLUDED.users_nome,
        users_senha_hash      = EXCLUDED.users_senha_hash,
        users_papel           = EXCLUDED.users_papel,
        users_status          = EXCLUDED.users_status,
        users_ativo           = EXCLUDED.users_ativo,
        users_atualizado_em   = NOW();

-- ---------------------------------------------------------------
-- 2. Garantir que o papel ADMIN existe em identify.roles
-- ---------------------------------------------------------------
INSERT INTO identify.roles (role_nome, role_descricao)
VALUES ('ADMIN', 'Administrador com acesso total')
ON CONFLICT (role_nome) DO NOTHING;

-- ---------------------------------------------------------------
-- 3. Vincular usuário ao papel ADMIN em identify.users_roles
-- ---------------------------------------------------------------
INSERT INTO identify.users_roles (users_roles_user_key, users_roles_role)
SELECT u.users_key, 'ADMIN'
FROM identify.users u
WHERE u.users_email = 'thiagobenevide@live.com'
ON CONFLICT (users_roles_user_key, users_roles_role) DO NOTHING;

-- ---------------------------------------------------------------
-- 4. Recursos disponíveis (se ainda não existirem)
-- ---------------------------------------------------------------
INSERT INTO identify.resources (resource_nome, resource_descricao)
VALUES
    ('financas',      'Módulo de finanças pessoais'),
    ('investimentos', 'Módulo de investimentos'),
    ('relatorios',    'Módulo de relatórios e exportações'),
    ('configurador',  'Módulo de configuração do sistema')
ON CONFLICT (resource_nome) DO NOTHING;

-- ---------------------------------------------------------------
-- 5. Permissões totais para ADMIN em todos os recursos
-- ---------------------------------------------------------------
INSERT INTO identify.permissions (
    permission_role_nome,
    permission_resource_key,
    permission_can_create,
    permission_can_read,
    permission_can_update,
    permission_can_delete
)
SELECT
    'ADMIN',
    r.resource_key,
    TRUE, TRUE, TRUE, TRUE
FROM identify.resources r
ON CONFLICT (permission_role_nome, permission_resource_key) DO NOTHING;

-- ---------------------------------------------------------------
-- 6. Remover usuário de desenvolvimento padrão (se existir)
-- O usuário admin@finlumia.local da migration 001 pode ser mantido
-- para testes internos, mas aqui garantimos que o thiago é o admin real.
-- Descomente a linha abaixo apenas se quiser remover o usuário dev:
-- UPDATE identify.users SET d_e_l_e_t_e = TRUE
--     WHERE users_email = 'admin@finlumia.local';
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
