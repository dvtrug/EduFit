-- Chuyển dữ liệu cũ sang mô hình đăng ký kích hoạt trực tiếp.
UPDATE accounts
SET status = 'ACTIVE'
WHERE status = 'PENDING_VERIFICATION';

-- Token xác minh email không còn thuộc phạm vi IAM Authentication.
DELETE FROM account_tokens
WHERE token_type = 'EMAIL_VERIFICATION';

ALTER TABLE accounts
    DROP CONSTRAINT IF EXISTS chk_accounts_status;

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_status
    CHECK (status IN ('ACTIVE', 'DEACTIVATED'));

ALTER TABLE account_tokens
    DROP CONSTRAINT IF EXISTS chk_account_tokens_type;

ALTER TABLE account_tokens
    ADD CONSTRAINT chk_account_tokens_type
    CHECK (token_type = 'PASSWORD_RESET');
