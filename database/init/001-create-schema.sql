-- Khởi tạo ban đầu cho cơ sở dữ liệu EduFit trên Docker PostgreSQL
-- File này tự động được chạy trong lần khởi động đầu tiên của PostgreSQL container.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
