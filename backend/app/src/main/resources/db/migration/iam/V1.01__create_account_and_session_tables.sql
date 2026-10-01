-- =========================================================================
-- EduFit Database Migration V1.01: Module IAM & Spring Session
-- =========================================================================

-- 0. CÁC EXTENSION HỆ THỐNG
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- 1. BẢNG TÀI KHOẢN (ACCOUNT)
CREATE TABLE IF NOT EXISTS account (
    user_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_account_email UNIQUE (email),
    CONSTRAINT chk_account_role CHECK (role IN ('STUDENT', 'PARENT', 'TUTOR', 'ADMIN')),
    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'DEACTIVATED'))
);

CREATE INDEX IF NOT EXISTS idx_account_email ON account (email);
CREATE INDEX IF NOT EXISTS idx_account_status ON account (status);

-- 2. BẢNG TOKEN XÁC THỰC MỘT LẦN (EMAIL_TOKEN)
CREATE TABLE IF NOT EXISTS email_token (
    token_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES account(user_id) ON DELETE CASCADE,
    type        VARCHAR(30) NOT NULL,
    token_hash  VARCHAR(128) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_email_token_hash UNIQUE (token_hash),
    CONSTRAINT chk_email_token_type CHECK (type IN ('VERIFY_EMAIL', 'RESET_PASSWORD'))
);

CREATE INDEX IF NOT EXISTS idx_email_token_lookup ON email_token (token_hash, type);
CREATE INDEX IF NOT EXISTS idx_email_token_user_type ON email_token (user_id, type);

-- 3. CÁC BẢNG QUẢN LÝ PHIÊN LÀM VIỆC (SPRING_SESSION JDBC)
CREATE TABLE IF NOT EXISTS SPRING_SESSION (
    PRIMARY_ID            CHAR(36) NOT NULL,
    SESSION_ID            CHAR(36) NOT NULL,
    CREATION_TIME         BIGINT NOT NULL,
    LAST_ACCESS_TIME      BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME           BIGINT NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX IF NOT EXISTS SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID    CHAR(36) NOT NULL,
    ATTRIBUTE_NAME        VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES       BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
