ALTER EVENT TRIGGER trg_add_default_columns DISABLE;

CREATE TABLE IF NOT EXISTS identify.roles (
    role_nome        VARCHAR(50) PRIMARY KEY,
    role_descricao   TEXT
);

CREATE TABLE IF NOT EXISTS identify.resources (
    resource_key       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_nome      VARCHAR(100) UNIQUE NOT NULL,
    resource_descricao TEXT
);

CREATE TABLE IF NOT EXISTS identify.permissions (
    permission_key          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_role_nome    VARCHAR(50) NOT NULL REFERENCES identify.roles(role_nome),
    permission_resource_key UUID NOT NULL REFERENCES identify.resources(resource_key),
    permission_can_create   BOOLEAN NOT NULL DEFAULT FALSE,
    permission_can_read     BOOLEAN NOT NULL DEFAULT FALSE,
    permission_can_update   BOOLEAN NOT NULL DEFAULT FALSE,
    permission_can_delete   BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (permission_role_nome, permission_resource_key)
);

CREATE TABLE IF NOT EXISTS identify.users_roles (
    users_roles_key      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    users_roles_user_key UUID NOT NULL REFERENCES identify.users(users_key),
    users_roles_role     VARCHAR(50) NOT NULL REFERENCES identify.roles(role_nome),
    UNIQUE (users_roles_user_key, users_roles_role)
);

CREATE INDEX IF NOT EXISTS ix_identify_users_roles_user
    ON identify.users_roles (users_roles_user_key);

CREATE INDEX IF NOT EXISTS ix_identify_permissions_role
    ON identify.permissions (permission_role_nome);

CREATE INDEX IF NOT EXISTS ix_identify_permissions_resource
    ON identify.permissions (permission_resource_key);

INSERT INTO identify.roles (role_nome, role_descricao)
VALUES ('ADMIN', 'Administrador com acesso total')
ON CONFLICT (role_nome) DO NOTHING;

INSERT INTO identify.roles (role_nome, role_descricao)
VALUES ('USER', 'Usuario padrao')
ON CONFLICT (role_nome) DO NOTHING;

INSERT INTO identify.resources (resource_nome, resource_descricao)
VALUES ('financas', 'Modulo de financas')
ON CONFLICT (resource_nome) DO NOTHING;

INSERT INTO identify.resources (resource_nome, resource_descricao)
VALUES ('investimentos', 'Modulo de investimentos')
ON CONFLICT (resource_nome) DO NOTHING;

INSERT INTO identify.resources (resource_nome, resource_descricao)
VALUES ('relatorios', 'Modulo de relatorios')
ON CONFLICT (resource_nome) DO NOTHING;

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
    TRUE,
    TRUE,
    TRUE,
    TRUE
FROM identify.resources r
ON CONFLICT (permission_role_nome, permission_resource_key) DO NOTHING;

INSERT INTO identify.users_roles (users_roles_user_key, users_roles_role)
SELECT u.users_key, 'ADMIN'
FROM identify.users u
WHERE u.users_email = 'admin@finlumia.local'
ON CONFLICT (users_roles_user_key, users_roles_role) DO NOTHING;

ALTER EVENT TRIGGER trg_add_default_columns ENABLE;
