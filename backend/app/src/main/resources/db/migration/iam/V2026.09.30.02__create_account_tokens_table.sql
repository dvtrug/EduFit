-- ==============================================================================
-- Migration: V2026.09.30.02__create_account_tokens_table.sql
-- Module: IAM (Identity & Access Management)
-- Mục đích: Khởi tạo bảng lưu trữ mã băm Token phục vụ kích hoạt email và đặt lại mật khẩu
-- Căn cứ kiến trúc:
--   - SRS: UC1.1 (Đăng ký & Xác thực email), UC1.4 (Quên & Đặt lại mật khẩu)
--   - Docs: docs/authentication-contract.md (Mục 2.3, 2.9 và Threat Model)
--   - Tiêu chuẩn bảo mật OWASP:
--       + Tuyệt đối không lưu token dạng văn bản thô (raw token) vào cơ sở dữ liệu.
--       + Bắt buộc chỉ lưu chuỗi băm SHA-256 (64 ký tự hex) để phòng ngừa rò rỉ CSDL.
--       + Cơ chế tiêu thụ nguyên tử (Atomic consumption) thông qua cột used_at chống Replay.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS account_tokens (
    -- 1. Khóa chính bản ghi dạng UUID:
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 2. Mã định danh người dùng sở hữu token:
    --    - Khóa ngoại tham chiếu trực tiếp đến bảng users(id).
    --    - Khi tài khoản bị xóa (ON DELETE CASCADE), toàn bộ token liên quan tự động xóa theo.
    user_id UUID NOT NULL,

    -- 3. Chuỗi băm mật mã SHA-256 của Token (64 ký tự hex):
    --    - Token thô (256-bit SecureRandom) được gửi cho người dùng qua email URL.
    --    - Cơ sở dữ liệu chỉ lưu chuỗi băm một chiều SHA-256 của token này.
    --    - Kẻ xấu dù có đọc trộm được CSDL cũng không thể giải mã ngược lại token thô.
    --    - Ràng buộc UNIQUE bảo đảm không bao giờ có 2 token trùng nhau.
    token_hash VARCHAR(64) NOT NULL,

    -- 4. Loại mục đích sử dụng của Token (Token Type):
    --    - 'VERIFY_EMAIL': Phục vụ kích hoạt tài khoản đăng ký mới (Hạn 24 giờ).
    --    - 'RESET_PASSWORD': Phục vụ đặt lại mật khẩu khi quên (Hạn 30 phút).
    token_type VARCHAR(30) NOT NULL,

    -- 5. Thời điểm hết hạn hiệu lực của Token (Expiration Time):
    --    - Được tính toán tại thời điểm phát hành (NOW() + thời gian sống).
    --    - Quá mốc này token sẽ bị từ chối với mã lỗi TOKEN_EXPIRED.
    expires_at TIMESTAMPTZ NOT NULL,

    -- 6. Thời điểm token đã được sử dụng (Used Timestamp):
    --    - Mặc định là NULL khi mới tạo (chưa sử dụng).
    --    - Khi người dùng click link thành công, hệ thống cập nhật used_at = CURRENT_TIMESTAMP.
    --    - Nếu used_at != NULL, token bị từ chối ngay lập tức với mã lỗi TOKEN_ALREADY_USED.
    --    - Đảm bảo tính chất "Chỉ dùng một lần duy nhất" (Single-use guarantee).
    used_at TIMESTAMPTZ,

    -- 7. Thời điểm tạo token (UTC):
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Ràng buộc khóa ngoại và toàn vẹn dữ liệu ở mức Database
    CONSTRAINT fk_account_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_account_tokens_hash UNIQUE (token_hash),
    CONSTRAINT chk_account_tokens_type CHECK (token_type IN ('VERIFY_EMAIL', 'RESET_PASSWORD'))
);

-- ==============================================================================
-- Chỉ mục (Indexes) tối ưu hóa truy vấn xác thực token
-- ==============================================================================

-- Tăng tốc độ tìm kiếm token khi người dùng click vào liên kết từ email
CREATE INDEX IF NOT EXISTS idx_account_tokens_hash ON account_tokens (token_hash);

-- Hỗ trợ truy vấn và vô hiệu hóa các token cũ khi người dùng yêu cầu gửi lại email mới
CREATE INDEX IF NOT EXISTS idx_account_tokens_user_type ON account_tokens (user_id, token_type);

-- ==============================================================================
-- Chú thích tài liệu hóa trực tiếp trong PostgreSQL (PostgreSQL Comments)
-- ==============================================================================
COMMENT ON TABLE account_tokens IS 'Bảng lưu trữ mã băm Token phục vụ kích hoạt tài khoản và đặt lại mật khẩu (IAM Module)';
COMMENT ON COLUMN account_tokens.id IS 'Mã định danh duy nhất của bản ghi token';
COMMENT ON COLUMN account_tokens.user_id IS 'Mã người dùng sở hữu token (Tham chiếu bảng users)';
COMMENT ON COLUMN account_tokens.token_hash IS 'Mã băm SHA-256 của token ngẫu nhiên gửi qua email (OWASP Standard)';
COMMENT ON COLUMN account_tokens.token_type IS 'Loại token: VERIFY_EMAIL (hạn 24h) hoặc RESET_PASSWORD (hạn 30m)';
COMMENT ON COLUMN account_tokens.expires_at IS 'Thời điểm hết hạn hiệu lực của token (UTC)';
COMMENT ON COLUMN account_tokens.used_at IS 'Thời điểm token đã được sử dụng (NULL = chưa dùng, có giá trị = đã dùng)';
COMMENT ON COLUMN account_tokens.created_at IS 'Thời điểm tạo token (UTC)';
