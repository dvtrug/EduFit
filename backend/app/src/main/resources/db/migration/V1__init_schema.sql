-- =========================================================================
-- EduFit Database Migration V1: Khởi tạo bảng Accounts, Tokens và Spring Session
-- =========================================================================

-- 1. BẢNG TÀI KHOẢN (ACCOUNTS)
CREATE TABLE IF NOT EXISTS accounts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                   VARCHAR(255) NOT NULL,
    password_hash           VARCHAR(255) NOT NULL,
    full_name               VARCHAR(100) NOT NULL,
    role                    VARCHAR(20) NOT NULL,
    status                  VARCHAR(30) NOT NULL,
    failed_login_attempts   INT NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ,
    last_login_at           TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 BIGINT DEFAULT 0,

    CONSTRAINT uk_accounts_email UNIQUE (email),
    CONSTRAINT chk_accounts_role CHECK (role IN ('STUDENT', 'PARENT', 'TUTOR', 'ADMIN')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'DEACTIVATED'))
);

CREATE INDEX IF NOT EXISTS idx_accounts_email ON accounts (email);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts (status);


-- 2. BẢNG TOKEN XÁC THỰC MỘT LẦN (ACCOUNT_TOKENS)
CREATE TABLE IF NOT EXISTS account_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id  UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    token_type  VARCHAR(30) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_account_tokens_hash UNIQUE (token_hash),
    CONSTRAINT chk_account_tokens_type CHECK (token_type = 'PASSWORD_RESET')
);

CREATE INDEX IF NOT EXISTS idx_account_tokens_lookup ON account_tokens (token_hash, token_type);
CREATE INDEX IF NOT EXISTS idx_account_tokens_account_type ON account_tokens (account_id, token_type);


-- 3. CÁC BẢNG QUẢN LÝ PHIÊN LÀM VIỆC (SPRING_SESSION JDBC)
CREATE TABLE IF NOT EXISTS SPRING_SESSION (
    PRIMARY_ID              CHAR(36) NOT NULL,
    SESSION_ID              CHAR(36) NOT NULL,
    CREATION_TIME           BIGINT NOT NULL,
    LAST_ACCESS_TIME        BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL   INT NOT NULL,
    EXPIRY_TIME             BIGINT NOT NULL,
    PRINCIPAL_NAME          VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX IF NOT EXISTS SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID      CHAR(36) NOT NULL,
    ATTRIBUTE_NAME          VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES         BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
