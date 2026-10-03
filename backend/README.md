# Hướng Dẫn & Tài Liệu Kỹ Thuật: Database Migration (Flyway) - Backend EduFit

Tài liệu này giải thích chi tiết mục đích của nhánh `feature/database-migration-flyway` (Giai đoạn 3), lý do kiến trúc bắt buộc phải sử dụng **Flyway**, danh sách các file được chỉnh sửa/thêm mới và vai trò của từng thành phần trong hệ sinh thái Modular Monolith của EduFit.

---

## 1. Mục Đích Của Nhánh Này (`feature/database-migration-flyway`)

Nhánh này được tạo ra để giải quyết bài toán **Quản trị Cơ sở Dữ liệu Tập trung (Database as Code)** cho toàn bộ dự án EduFit trước khi các thành viên bước vào phát triển chi tiết các tính năng nghiệp vụ:
1. **Thiết lập cơ chế Migration chuẩn hóa:** Thay thế hoàn toàn cách tạo bảng thủ công hoặc phụ thuộc vào Hibernate tự sinh schema.
2. **Khởi tạo Schema CSDL đầu tiên cho Module IAM (Sprint 0):** Tạo bảng người dùng (`users`) và bảng mã xác thực (`account_tokens`) với đầy đủ các ràng buộc an ninh (Argon2, chống Brute-force NFR-03, bảo mật phiên NFR-04, khóa lạc quan NFR-15).
3. **Đồng bộ hóa cấu hình Runtime:** Đảm bảo toàn bộ nhóm dev, môi trường Docker Compose và máy chủ CI/CD chạy cùng một cấu hình phiên làm việc và CSDL thống nhất.
4. **Tự động hóa kiểm thử Schema:** Sử dụng Testcontainers PostgreSQL để CI tự động phát hiện lỗi cú pháp SQL hoặc sai lệch ràng buộc trước khi code được merge vào `main`.

---

## 2. Tại Sao Bắt Buộc Phải Dùng Flyway?

Trong các dự án phần mềm theo nhóm, việc quản lý cơ sở dữ liệu thường gặp các vấn đề nghiêm trọng nếu không có công cụ Database Migration:

| Vấn đề khi làm thủ công / DDL-Auto                                                                                                                                                                | Giải pháp triệt để từ Flyway trong EduFit                                                                                                                                                                                                   |
| :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Không dùng `ddl-auto: update`:** Hibernate tự sinh schema thường thiếu index tối ưu, thiếu ràng buộc toàn vẹn cứng (`CHECK constraint`), dễ làm mất dữ liệu hoặc sinh kiểu dữ liệu không chuẩn. | **Schema được viết tay bằng SQL thuần:** Lập trình viên kiểm soát 100% kiểu dữ liệu (UUID, TIMESTAMPTZ, Argon2 hash), chỉ mục (B-Tree index) và ràng buộc toàn vẹn ở tầng DB.                                                               |
| **Xung đột khi làm việc nhóm (Schema Drift):** 4-5 thành viên cùng sửa bảng trên máy cá nhân hoặc DBeaver, khi merge code backend sẽ crash vì DB của người này thiếu cột của người kia.           | **Quản lý phiên bản như mã nguồn (Database as Code):** Mọi thay đổi cấu trúc bảng đều là một file script có gắn mã phiên bản (versioned script) được commit vào Git. Khi app khởi động, Flyway tự động kiểm tra và chạy các script chưa có. |
| **Lịch sử thay đổi minh bạch:** Không biết ai đã chạy câu lệnh `ALTER TABLE` nào vào thời điểm nào.                                                                                               | **Bảng `flyway_schema_history`:** Flyway tự động ghi nhận tên script, người chạy, thời gian thực thi và mã băm SHA-256 (`checksum`) của file.                                                                                               |
| **Chống sửa đổi file cũ:** Thành viên sửa lại file migration đã chạy làm sai lệch môi trường.                                                                                                     | **Cưỡng chế Checksum (`validate-on-migrate: true`):** Nếu một file script cũ bị chỉnh sửa dù chỉ 1 ký tự, Flyway sẽ từ chối khởi động app ngay lập tức để bảo vệ dữ liệu.                                                                   |
| **Chống nhảy cóc phiên bản:** Các script không được chạy lộn xộn.                                                                                                                                 | **Cưỡng chế thứ tự thời gian (`out-of-order: false`):** Đảm bảo tính nhất quán tuyệt đối giữa môi trường Local Dev, Staging và Production.                                                                                                  |

### Quy chuẩn đặt tên Version Timestamp (Tránh xung đột Git)
EduFit quy định không sử dụng số thứ tự tăng dần kiểu cũ (`V1__`, `V2__`) vì rất dễ bị trùng khi 2 thành viên cùng tạo script ở 2 nhánh khác nhau. Thay vào đó, toàn bộ script tuân thủ quy tắc:
$$\mathbf{V\{yyyy\}.\{MM\}.\{dd\}.\{seq\}\_\_\{mo\_ta\_ngan\_gon\}.sql}$$
*Ví dụ:* `V2026.09.30.01__create_users_table.sql`.

---

## 3. Danh Sách Các File Đã Chỉnh Sửa (Modified Files)

### 1. `backend/app/pom.xml`
- **Thay đổi:** Bổ sung các dependency phục vụ kiểm thử tích hợp (scope `test`):
  - `org.testcontainers:postgresql`: Cung cấp khả năng tự động bật container PostgreSQL thật qua Docker trong lúc chạy test.
  - `org.testcontainers:junit-jupiter`: Tích hợp vòng đời của Testcontainers vào JUnit 5 (`@Container`, `@Testcontainers`).
- **Mục đích:** Giúp bài test migration có thể chạy độc lập, tự động trên máy dev hoặc trên GitHub Actions mà không đòi hỏi phải cài đặt sẵn database cục bộ.

### 2. `backend/app/src/main/resources/application.yml`
- **Thay đổi 1 (Bảo mật phiên - NFR-04):**
  - Cấu hình `server.servlet.session.timeout: 60m` (Hết hạn phiên sau 60 phút không tương tác - sliding inactivity).
  - Cấu hình cookie phiên: `name: EDUFIT_SESSION`, `http-only: true`, `same-site: lax` theo đúng đặc tả `docs/authentication-contract.md`.
- **Thay đổi 2 (Quản trị Flyway tập trung - ADR-002 Decision 3b):**
  - `spring.flyway.locations: classpath:db/migration`: Chỉ định Flyway quét đệ quy thư mục `db/migration` và tất cả thư mục module con bên trong.
  - `spring.flyway.out-of-order: false`: Bắt buộc thực thi tuần tự theo dòng thời gian.
  - `spring.flyway.validate-on-migrate: true`: Xác thực mã băm checksum trước khi chạy.
  - `spring.flyway.baseline-on-migrate: false`: CSDL khởi tạo chuẩn từ đầu, không dùng baseline giả lập.

### 3. `backend/app/src/main/resources/db/migration/V1__init_schema.sql` (Đã xóa)
- **Thay đổi:** Xóa bỏ file `V1__init_schema.sql` rỗng (0 bytes) ban đầu để thay thế bằng cấu trúc thư mục phân tách theo module chuẩn hóa.

---

## 4. Danh Sách Các File Được Thêm Mới (Added Files)

### 1. `backend/app/src/main/resources/db/migration/iam/V2026.09.30.01__create_users_table.sql`
- **Chức năng:** Khởi tạo bảng `users` phục vụ quản lý tài khoản người dùng và xác thực hệ thống.
- **Các đặc tính kỹ thuật:**
  - `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`: Sử dụng khóa chính UUID tránh đoán trước ID (Security ID Enumeration).
  - `email VARCHAR(255) NOT NULL UNIQUE`: Email định danh tài khoản, kiểm tra định dạng và bắt buộc chữ thường.
  - `password_hash VARCHAR(255) NOT NULL`: Lưu chuỗi băm mật khẩu thuật toán Argon2id (NFR-01).
  - `role VARCHAR(20) NOT NULL`: Check constraint phân quyền 4 vai trò (`CUSTOMER`, `PT`, `STAFF`, `MANAGER`).
  - `status VARCHAR(20) NOT NULL`: Check constraint quản lý trạng thái (`PENDING_VERIFICATION`, `ACTIVE`, `LOCKED`, `DEACTIVATED`).
  - `failed_login_attempts INT` & `locked_until TIMESTAMPTZ`: Cơ chế phòng thủ Brute-force (NFR-03) — tự động khóa tài khoản 15 phút nếu nhập sai quá 5 lần liên tiếp.
  - `version BIGINT NOT NULL DEFAULT 0`: Khóa lạc quan (JPA Optimistic Locking NFR-15) chống xung đột cập nhật đồng thời.
  - `created_at`, `updated_at TIMESTAMPTZ`: Quản lý thời gian có múi giờ chuẩn UTC (NFR-18).
  - Đánh sẵn B-Tree Index cho `email`, `role`, `status` để tối ưu hóa tốc độ truy vấn đăng nhập/lọc.

### 2. `backend/app/src/main/resources/db/migration/iam/V2026.09.30.02__create_account_tokens_table.sql`
- **Chức năng:** Khởi tạo bảng `account_tokens` lưu trữ One-Time Token (mã dùng 1 lần) phục vụ:
  - Xác thực kích hoạt email đăng ký (UC1.1).
  - Đặt lại mật khẩu khi quên (UC1.4).
- **Các đặc tính kỹ thuật:**
  - `token_hash VARCHAR(64) NOT NULL UNIQUE`: **Tuyệt đối không lưu token thô** vào CSDL; chỉ lưu chuỗi băm SHA-256 (64 ký tự hex). Khi người dùng nhấn link, hệ thống băm chuỗi token nhận được rồi so khớp với cột này.
  - `user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE`: Ràng buộc khóa ngoại, tự động dọn dẹp token nếu tài khoản bị xóa.
  - `token_type VARCHAR(30) NOT NULL`: Check constraint (`VERIFY_EMAIL`, `RESET_PASSWORD`).
  - `expires_at TIMESTAMPTZ NOT NULL`: Thời điểm hết hạn của token.
  - `used_at TIMESTAMPTZ`: Đánh dấu mốc thời gian token đã được sử dụng nhằm triệt tiêu lỗ hổng Tấn công phát lại (Replay Attack).
  - Đánh Index trên `token_hash` để tốc độ xác thực đạt mức tối đa.

### 3. `backend/app/src/test/java/vn/edufit/db/FlywayMigrationScriptTest.java`
- **Chức năng (Unit Test quy chuẩn):**
  - Quét toàn bộ thư mục `db/migration` để kiểm tra tên file có khớp 100% biểu thức chính quy `V\d{4}\.\d{2}\.\d{2}\.\d{2}__[a-z0-9_]+\.sql` hay không.
  - Đảm bảo không có file rác hoặc file rỗng 0 bytes lọt vào repository.
  - Kiểm tra sự hiện diện của các ràng buộc bắt buộc trong nội dung SQL (`CREATE TABLE`, `CHECK constraint`, `UNIQUE`).
  - **Tốc độ:** Chạy cực nhanh (< 100ms), không cần Docker.

### 4. `backend/app/src/test/java/vn/edufit/db/FlywayMigrationIT.java`
- **Chức năng (Integration Test CSDL thật):**
  - Khởi động một container PostgreSQL 16 thật (`postgres:16-alpine`) thông qua Testcontainers.
  - Kích hoạt Flyway kết nối và chạy thực tế toàn bộ các script migration từ đầu đến cuối.
  - Kiểm tra kết quả: Xác nhận số lượng script thực thi thành công, bảng `users` và `account_tokens` được tạo đúng cấu trúc, và bảng `flyway_schema_history` lưu trạng thái `SUCCESS`.

### 5. `backend/giai_doan_3_database_migration_plan.md`
- **Chức năng:** Bản kế hoạch kiến trúc chi tiết, căn cứ theo các quyết định ADR-001, ADR-002 và yêu cầu SRS NFR.

---

## 5. Hướng Dẫn Kiểm Tra & Chạy Test

Để kiểm tra toàn bộ hệ thống migration và chạy các bài kiểm thử tự động, bạn mở terminal tại thư mục gốc của dự án hoặc thư mục `backend/` và thực hiện:

### Chạy kiểm tra quy chuẩn và migration:
```bash
# Tại thư mục backend:
./mvnw test -pl app
```

### Chạy kiểm thử toàn bộ dự án:
```bash
./mvnw test
```

> [!TIP]
> **Quy tắc cho các thành viên khi thêm bảng cho các module khác (`scheduling`, `subscription`, ...):**
> 1. Tạo thư mục migration tương ứng tại: `backend/app/src/main/resources/db/migration/<ten_module>/`
> 2. Đặt tên file theo đúng ngày tạo: `VYYYY.MM.DD.01__create_<ten_bang>_table.sql`
> 3. Không bao giờ chỉnh sửa nội dung của các file migration đã được merge vào nhánh `main`. Nếu cần thay đổi cấu trúc bảng, hãy tạo một script migration mới (ví dụ: `VYYYY.MM.DD.02__add_column_to_users.sql`).
