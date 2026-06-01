ALTER EVENT TRIGGER trg_add_default_columns DISABLE;

CREATE SCHEMA IF NOT EXISTS identify;

CREATE TABLE IF NOT EXISTS identify.users (
    users_key         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    users_email       VARCHAR(255) UNIQUE NOT NULL,
    users_senha_hash  VARCHAR(255) NOT NULL,
    users_ativo       BOOLEAN DEFAULT TRUE,
    users_criado_em   TIMESTAMP DEFAULT NOW(),
    d_e_l_e_t_e       BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS identify.token_blacklist (
    blacklist_key         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blacklist_jti         TEXT UNIQUE NOT NULL,
    blacklist_revogado_em TIMESTAMP DEFAULT NOW(),
    blacklist_expira_em   TIMESTAMP NOT NULL,
    d_e_l_e_t_e           BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS identify.refresh_tokens (
    refresh_key       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    refresh_users_key UUID NOT NULL REFERENCES identify.users(users_key),
    refresh_token     TEXT UNIQUE NOT NULL,
    refresh_expira_em TIMESTAMP NOT NULL,
    refresh_revogado  BOOLEAN DEFAULT FALSE,
    refresh_criado_em TIMESTAMP DEFAULT NOW(),
    d_e_l_e_t_e       BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS ix_identify_refresh_tokens_user_active
    ON identify.refresh_tokens (refresh_users_key)
    WHERE d_e_l_e_t_e = FALSE AND refresh_revogado = FALSE;

CREATE INDEX IF NOT EXISTS ix_identify_token_blacklist_expira
    ON identify.token_blacklist (blacklist_expira_em)
    WHERE d_e_l_e_t_e = FALSE;

INSERT INTO identify.users (users_email, users_senha_hash, users_ativo)
VALUES (
    'admin@finlumia.local',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    TRUE
)
ON CONFLICT (users_email) DO NOTHING;

ALTER EVENT TRIGGER trg_add_default_columns ENABLE;
