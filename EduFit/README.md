# EduFit

EduFit là nền tảng kết nối gia sư với học sinh và phụ huynh, đồng thời theo dõi
toàn bộ quá trình học tập sau khi hai bên kết nối. Hệ thống xác minh hồ sơ gia
sư, ghép cặp bằng các quy tắc có thể giải thích, quản lý lịch học theo cơ chế
đồng thuận và đo tiến độ từ dữ liệu buổi học cùng các cột mốc có trọng số.

Repository hiện đang ở giai đoạn xây dựng nền tảng. Kiến trúc backend, hợp đồng
dùng chung, cấu hình bảo mật cơ bản và ranh giới module đã được khởi tạo. Các
module nghiệp vụ và giao diện sản phẩm chưa được triển khai.

## Chức năng chính

- Đăng ký, xác minh email, đăng nhập, đăng xuất và khôi phục mật khẩu
- Hồ sơ gia sư, hồ sơ học sinh và mục tiêu học tập có cấu trúc
- Gửi bằng cấp, chứng chỉ và quy trình Admin xác minh gia sư
- Tìm kiếm, lọc và ghép cặp gia sư bằng bộ quy tắc
- Liên kết phụ huynh-học sinh và yêu cầu kết nối gia sư-học sinh
- Đề xuất, xác nhận, đổi lịch, hủy và ghi nhận kết quả buổi học
- Kế hoạch học tập, cột mốc, ghi chú buổi học và dashboard tiến độ
- Đánh giá gia sư và thông báo trong ứng dụng
- AI hỗ trợ viết tiểu sử gia sư và diễn giải kết quả ghép cặp

Các tính năng AI chỉ đóng vai trò hỗ trợ. Quản lý hồ sơ, tìm kiếm, ghép cặp và
theo dõi tiến độ vẫn phải hoạt động khi nhà cung cấp AI bên ngoài gặp sự cố.

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1, Maven |
| Dữ liệu | PostgreSQL 17, Spring Data JPA, Hibernate, Flyway |
| Bảo mật | Spring Security, server-side session, Argon2, CSRF |
| Tài liệu API | OpenAPI qua springdoc |
| Frontend | Next.js 16 App Router, React 19, TypeScript, Tailwind CSS 4 |
| Kiểm thử | JUnit 5, ArchUnit, Spring MVC Test; dự kiến dùng Testcontainers |
| Hạ tầng local | Docker Compose |

## Cấu trúc repository

```text
EduFit/
├── backend/
│   ├── app/                    # Ứng dụng Spring Boot chạy thật và cấu hình toàn cục
│   ├── modules/                # Các module nghiệp vụ
│   │   ├── iam/                # Xác thực và quản lý tài khoản
│   │   ├── profile/            # Hồ sơ gia sư và học sinh
│   │   ├── verification/       # Xác minh bằng cấp gia sư
│   │   ├── discovery/          # Tìm kiếm và ghép cặp theo quy tắc
│   │   ├── connection/         # Liên kết phụ huynh và kết nối gia sư
│   │   ├── scheduling/         # Vòng đời buổi học và chống trùng lịch
│   │   ├── progress/           # Kế hoạch, cột mốc và dashboard tiến độ
│   │   ├── review/             # Đánh giá và điểm trung bình của gia sư
│   │   └── notification/       # Thông báo trong ứng dụng
│   └── platform/               # Các năng lực kỹ thuật dùng chung
│       ├── shared/             # Contract, response, exception và value object
│       ├── ai/                 # Cổng tích hợp LLM bên ngoài
│       ├── storage/            # Cổng lưu trữ tài liệu xác minh riêng tư
│       └── audit/              # Nhật ký quyết định và sự kiện bảo mật
├── frontend/                   # Ứng dụng Next.js
├── docs/                       # SRS, ADR và hướng dẫn repository
├── compose.yaml                # PostgreSQL local và backend container dự kiến
└── .env.example               # Giá trị cấu hình Compose mẫu
```

## Kiến trúc

### Backend

Backend là modular monolith được tổ chức bằng Maven multi-module.
`backend/app` là module duy nhất có thể chạy; các module nghiệp vụ và platform
là JAR được nạp vào cùng một Spring ApplicationContext.

Một module chỉ được đọc module khác qua package `api` công khai. Các package
nội bộ như `application`, `domain` và `infra` không phải giao diện liên
module. Khi use case gốc không cần dữ liệu trả về, notification và audit phản
ứng với synchronous domain event chạy trong cùng tiến trình.

Ba module có logic rủi ro cao nhất là `scheduling`, `progress` và
`discovery`. State machine và công thức của chúng cần được kiểm thử độc lập
với Spring Context.

Tất cả Flyway migration nằm trên một dòng thời gian chung tại:

```text
backend/app/src/main/resources/db/migration/
```

Không chỉnh sửa migration đã chạy trên môi trường dùng chung. Mọi thay đổi
schema tiếp theo phải được thêm bằng một migration mới.

### Frontend

Kiến trúc mục tiêu của frontend là package-by-feature. `src/app` chỉ chứa
route và layout mỏng; logic nghiệp vụ nằm trong `src/features`, với tên feature
tương ứng module backend. Server Component là mặc định cho thao tác đọc dữ
liệu; Client Component chỉ dùng khi cần tương tác trình duyệt hoặc local state.

Frontend hiện vẫn là scaffold mặc định của Next.js và chưa có cấu trúc feature
mục tiêu.

## Yêu cầu môi trường

- Java 21
- Maven 3.9 trở lên
- Node.js 20 trở lên và npm
- Docker Desktop hoặc runtime tương thích Docker Compose

Repository chưa có Maven Wrapper, vì vậy các lệnh backend bên dưới sử dụng
`mvn` được cài trên máy.

## Khởi động nhanh

### 1. Khởi động PostgreSQL

Từ thư mục gốc của repository:

```bash
docker compose up -d db
docker compose ps
```

PostgreSQL mặc định được mở tại `localhost:5433`:

| Cấu hình | Giá trị mặc định |
| --- | --- |
| Database | `edufit` |
| User | `edufit` |
| Password | `edufit-local-only` |
| Host port | `5433` |

Chỉ sao chép `.env.example` thành `.env` khi cần thay đổi cấu hình Compose.
Không commit file `.env`.

### 2. Build và kiểm thử backend

```bash
mvn -f backend/pom.xml -B -ntp verify
```

Để chạy backend với PostgreSQL từ Compose, cấu hình datasource trong terminal
hiện tại.

PowerShell:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5433"
$env:DB_NAME = "edufit"
$env:DB_USER = "edufit"
$env:DB_PASSWORD = "edufit-local-only"

mvn -f backend/pom.xml -pl app -am -DskipTests install
mvn -f backend/app/pom.xml spring-boot:run
```

Bash:

```bash
export DB_HOST=localhost
export DB_PORT=5433
export DB_NAME=edufit
export DB_USER=edufit
export DB_PASSWORD=edufit-local-only

mvn -f backend/pom.xml -pl app -am -DskipTests install
mvn -f backend/app/pom.xml spring-boot:run
```

Khi backend đã chạy:

- Health check: <http://localhost:8080/actuator/health>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI document: <http://localhost:8080/v3/api-docs>

### 3. Chạy frontend

Mở terminal thứ hai:

```bash
cd frontend
npm ci
npm run dev
```

Truy cập <http://localhost:3000>.

## Các lệnh thường dùng

| Lệnh | Mục đích |
| --- | --- |
| `mvn -f backend/pom.xml -B -ntp verify` | Compile và chạy các test backend đã cấu hình |
| `mvn -f backend/pom.xml -pl app -am -DskipTests package` | Đóng gói backend cùng các module phụ thuộc |
| `npm --prefix frontend run dev` | Khởi động Next.js development server |
| `npm --prefix frontend run lint` | Chạy ESLint cho frontend |
| `npm --prefix frontend run build` | Tạo production build cho frontend |
| `docker compose up -d db` | Khởi động PostgreSQL local |
| `docker compose down` | Dừng Compose mà không xóa dữ liệu |

Không chạy `docker compose down -v` trừ khi chủ động muốn xóa volume PostgreSQL
trên máy.

## Cấu hình backend

Ứng dụng backend đọc các biến môi trường sau:

| Biến | Mặc định của ứng dụng | Giá trị khi dùng Compose local |
| --- | --- | --- |
| `DB_HOST` | `localhost` | `localhost` nếu backend chạy trên host |
| `DB_PORT` | `5432` | `5433` nếu backend chạy trên host |
| `DB_NAME` | `edufit_db` | `edufit` |
| `DB_USER` | `postgres` | `edufit` |
| `DB_PASSWORD` | `postgres` | `edufit-local-only` |

Secret của môi trường production phải được cấp bởi nền tảng triển khai và không
được lưu trong repository.

## Trạng thái triển khai

Đã có:

- Maven reactor và đồ thị phụ thuộc giữa các module
- Entry point của ứng dụng Spring Boot
- Cấu hình server-side session, CSRF, CORS và JSON security error
- Global exception handler và contract response dùng chung
- Cấu hình OpenAPI và scheduled job
- Business exception, `Money` và `TimeInterval` dạng nửa khoảng mở
- ArchUnit test và unit test cho shared kernel
- Scaffold Next.js, React, TypeScript và Tailwind

Chưa có:

- Database schema và domain migration
- Entity, repository, service và REST endpoint nghiệp vụ
- Luồng đăng ký và đăng nhập
- Route và component frontend theo feature
- TypeScript client sinh từ OpenAPI
- Adapter lưu trữ credential và tích hợp AI
- Docker image cho backend
- CI workflow được mô tả trong tài liệu dự án

Do chưa có `backend/Dockerfile`, không dùng `docker compose up --build` để
chạy toàn bộ stack. Hiện tại chỉ khởi động service `db`, sau đó chạy backend
và frontend bằng toolchain local như hướng dẫn phía trên.

## Các bất biến nghiệp vụ quan trọng

- Chỉ gia sư đã xác minh mới xuất hiện trong tìm kiếm và ghép cặp.
- Server luôn kiểm tra role, quyền sở hữu dữ liệu và link scope đang hoạt động.
- Khoảng thời gian của buổi học có dạng nửa mở: `[start, end)`.
- Khi chấp nhận lịch, hệ thống phải kiểm tra trùng lịch lần nữa trong cùng
  transaction và có bảo vệ đồng thời ở tầng database.
- Tiến độ được tính từ trọng số milestone đã hoàn thành, không do AI ước lượng.
- Mỗi cặp học sinh-gia sư chỉ có tối đa một review, được database đảm bảo.
- Credential là dữ liệu riêng tư và phải được kiểm tra theo nội dung file.
- API công khai và API cho gia sư không trả về danh tính người viết review.
- Thời gian được lưu dưới dạng UTC instant và hiển thị theo múi giờ người dùng;
  mặc định là `Asia/Ho_Chi_Minh`.

## Tài liệu

- [Software Requirement Specification](docs/SRS%20Edufit.docx)
- [ADR-001: Technology Stack](docs/ADR-001-tech-stack.md)
- [ADR-002: Multi-Module and Feature Architecture](docs/ADR-002-multi-module-architecture.md)
- [Hướng dẫn đóng góp](CONTRIBUTING.md)
- [GitHub Branch Protection](docs/github-branch-protection.md)

ADR-001 đã được chấp nhận. ADR-002 đang ở trạng thái đề xuất và được xem là kiến
trúc mục tiêu cho đến khi nhóm chấp nhận hoặc thay thế bằng ADR khác.

## Đóng góp

1. Tạo branch ngắn hạn với tiền tố `feature/`, `fix/`, `chore/` hoặc
   `refactor/`.
2. Giữ thay đổi trong module sở hữu và chỉ dùng public API của module khác.
3. Thêm Flyway migration mới cho mọi thay đổi schema.
4. Chạy backend verify và các kiểm tra frontend liên quan trước khi mở pull request.
5. Dùng Conventional Commits, ví dụ:

```text
feat(scheduling): reject overlapping confirmed sessions
fix(iam): invalidate other sessions after password change
docs: update local development instructions
```

Xem [CONTRIBUTING.md](CONTRIBUTING.md) để biết quy trình đầy đủ. Nếu tài liệu cũ
mâu thuẫn với source hiện tại, namespace `vn.edufit` và ranh giới module trong
ADR-002 được ưu tiên.

## Giấy phép

Dự án chưa chọn giấy phép. Cần thêm file `LICENSE` trước khi phân phối ra ngoài
phạm vi nhóm.

