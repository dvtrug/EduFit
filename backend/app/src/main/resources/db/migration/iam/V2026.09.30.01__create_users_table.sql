-- ==============================================================================
-- Migration: V2026.09.30.01__create_users_table.sql
-- Module: IAM (Identity & Access Management)
-- Mục đích: Khởi tạo bảng tài khoản người dùng chính của hệ thống EduFit
-- Căn cứ kiến trúc:
--   - ADR-001 (Decision 2, 5): PostgreSQL hệ thống ghi nhận duy nhất; Session server-side
--   - ADR-002 (Decision 3b): Quản lý migration tập trung tại module app
--   - SRS Yêu cầu phi chức năng:
--       + NFR-01: Mật khẩu an toàn băm bằng thuật toán Argon2id
--       + NFR-04 & FR-04: Hết hạn phiên 60 phút, hủy tức thì khi đổi mật khẩu
--       + NFR-07: Phân quyền 4 vai trò (STUDENT, PARENT, TUTOR, ADMIN)
--       + NFR-15: Khóa lạc quan (Optimistic Locking) chống xung đột ghi đè đồng thời
--       + NFR-18: Chuẩn hóa thời gian múi giờ UTC/Asia:Ho_Chi_Minh qua TIMESTAMPTZ
--   - Docs: docs/authentication-contract.md (Quy chuẩn định dạng API và Threat Model)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS users (
    -- 1. Khóa chính dạng UUID:
    --    - Tránh tấn công dò quét tuần tự (ID Enumeration Attack: /users/1, /users/2).
    --    - Cho phép tầng ứng dụng sinh ID trước khi ghi xuống DB nếu cần.
    --    - Tương thích 100% với UUID userId trong CurrentUser và EduFitUserDetails.
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 2. Email đăng nhập duy nhất (Unique Identity):
    --    - Bắt buộc không được để trống.
    --    - Ràng buộc UNIQUE ở tầng DB ngăn chặn triệt để xung đột tài khoản trùng lặp.
    email VARCHAR(255) NOT NULL,

    -- 3. Mật khẩu băm an toàn (NFR-01):
    --    - Bắt buộc sử dụng chuỗi băm Argon2id (độ dài thực tế ~90-120 ký tự).
    --    - Tuyệt đối không bao giờ lưu trữ mật khẩu ở dạng văn bản thô (plain-text).
    password_hash VARCHAR(255) NOT NULL,

    -- 4. Họ và tên người dùng (authentication-contract.md):
    --    - Thu thập ngay khi đăng ký tài khoản (UC1.1).
    --    - Trả về trong thông tin phiên đăng nhập để hiển thị trên giao diện người dùng.
    full_name VARCHAR(100) NOT NULL,

    -- 5. Vai trò tài khoản (NFR-07 Role-based Authorization):
    --    - Hỗ trợ 4 vai trò chính: STUDENT, PARENT, TUTOR, ADMIN.
    --    - Phòng thủ theo chiều sâu (Defense-in-depth) bằng CHECK constraint ở mức CSDL.
    role VARCHAR(20) NOT NULL,

    -- 6. Trạng thái hoạt động của tài khoản:
    --    - Mặc định khi vừa đăng ký: PENDING_VERIFICATION (chờ bấm link email kích hoạt).
    --    - Sau khi kích hoạt email thành công: ACTIVE.
    --    - Các trạng thái khác: INACTIVE (tạm ngưng), LOCKED (bị khóa), DEACTIVATED (hủy).
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_VERIFICATION',

    -- 7. Cơ chế phòng thủ tấn công Brute-force mật khẩu (Threat Model):
    --    - failed_login_attempts: Đếm số lần nhập sai liên tiếp (reset về 0 khi đăng nhập đúng).
    --    - locked_until: Thời điểm hết hạn khóa tài khoản (khóa tạm 15 phút sau 5 lần sai).
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,

    -- 8. Dấu thời gian đăng nhập thành công gần nhất (BR-06, NFR-18):
    --    - Ghi nhận thời điểm đăng nhập thành công cuối cùng của người dùng.
    --    - Phục vụ kiểm toán bảo mật và đồng bộ với thuộc tính lastLoginAt trong thực thể Account.
    last_login_at TIMESTAMPTZ,

    -- 9. Dấu thời gian đổi mật khẩu phục vụ hủy phiên (FR-04, NFR-04):
    --    - Ghi nhận thời điểm đổi mật khẩu lần cuối.
    --    - So sánh với thời điểm tạo phiên để vô hiệu hóa ngay lập tức toàn bộ phiên cũ.
    password_changed_at TIMESTAMPTZ,

    -- 9. Phiên bản phục vụ Khóa lạc quan (JPA Optimistic Locking - NFR-15):
    --    - Tự động tăng khi có cập nhật bản ghi thông qua annotation @Version của JPA.
    --    - Ngăn chặn hai giao dịch cùng sửa thông tin tài khoản đè lên nhau.
    version BIGINT NOT NULL DEFAULT 0,

    -- 10. Dấu thời gian kiểm toán (Audit Timestamps - NFR-18):
    --     - Sử dụng kiểu TIMESTAMPTZ (Timestamp with Time Zone) chuẩn của PostgreSQL.
    --     - Luôn lưu theo giờ chuẩn UTC, đảm bảo nhất quán và không bị lệch múi giờ máy chủ.
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Ràng buộc toàn vẹn dữ liệu ở mức Cơ sở dữ liệu (Database-level Constraints)
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'PARENT', 'TUTOR', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'INACTIVE', 'LOCKED', 'DEACTIVATED'))
);

-- ==============================================================================
-- Chỉ mục (Indexes) tối ưu hóa hiệu năng truy vấn
-- ==============================================================================

-- Tăng tốc độ tìm kiếm tài khoản theo email khi thực hiện Đăng nhập và Đăng ký
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- Hỗ trợ lọc danh sách người dùng theo vai trò (ví dụ: tìm kiếm gia sư, danh sách học viên)
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);

-- Hỗ trợ lọc tài khoản theo trạng thái hoạt động
CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);

-- ==============================================================================
-- Chú thích tài liệu hóa trực tiếp trong PostgreSQL (PostgreSQL Comments)
-- ==============================================================================
COMMENT ON TABLE users IS 'Bảng lưu trữ thông tin tài khoản người dùng và xác thực của hệ thống EduFit (IAM Module)';
COMMENT ON COLUMN users.id IS 'Mã định danh duy nhất (UUID) của người dùng';
COMMENT ON COLUMN users.email IS 'Địa chỉ email đăng nhập duy nhất (Unique Identity)';
COMMENT ON COLUMN users.password_hash IS 'Chuỗi băm mật khẩu sử dụng thuật toán Argon2id (NFR-01)';
COMMENT ON COLUMN users.full_name IS 'Họ và tên đầy đủ của người dùng';
COMMENT ON COLUMN users.role IS 'Vai trò tài khoản: STUDENT, PARENT, TUTOR, ADMIN (NFR-07)';
COMMENT ON COLUMN users.status IS 'Trạng thái tài khoản: PENDING_VERIFICATION, ACTIVE, INACTIVE, LOCKED, DEACTIVATED';
COMMENT ON COLUMN users.failed_login_attempts IS 'Số lần đăng nhập sai mật khẩu liên tiếp để khóa sau 5 lần';
COMMENT ON COLUMN users.locked_until IS 'Thời điểm hết hạn khóa tài khoản tạm thời do nhập sai quá nhiều';
COMMENT ON COLUMN users.last_login_at IS 'Thời điểm đăng nhập thành công lần cuối (BR-06)';
COMMENT ON COLUMN users.password_changed_at IS 'Thời điểm đổi mật khẩu lần cuối để hủy các phiên đăng nhập cũ (FR-04)';
COMMENT ON COLUMN users.version IS 'Số phiên bản phục vụ Khóa lạc quan JPA chống ghi đè đồng thời (NFR-15)';
COMMENT ON COLUMN users.created_at IS 'Thời điểm tạo bản ghi (UTC)';
COMMENT ON COLUMN users.updated_at IS 'Thời điểm cập nhật bản ghi lần cuối (UTC)';
