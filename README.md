# EduFit

[![Backend CI](https://github.com/OWNER/REPOSITORY/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/OWNER/REPOSITORY/actions/workflows/backend-ci.yml)

EduFit hiện gồm backend Java 21 và Spring Boot 4.1 theo kiến trúc modular monolith, hạ tầng PostgreSQL 17 để chạy trên máy cá nhân, bộ quy ước mã nguồn có thể kiểm chứng và quy trình kiểm tra chất lượng bằng GitHub Actions.

## Yêu cầu môi trường

Cài đặt và khởi động Docker Desktop. Với quy trình chạy bằng Docker thông thường, bạn không cần cài Java hoặc Maven trên máy.

## Khởi động PostgreSQL và backend

Tại thư mục gốc của repository, chạy một lệnh:

```bash
docker compose up --build -d
```

Kiểm tra trạng thái của hai dịch vụ:

```bash
docker compose ps
docker compose logs backend
```

Backend đã sẵn sàng khi truy cập `http://localhost:8080/actuator/health` và nhận được phản hồi có `"status":"UP"`. Phản hồi health có kiểm tra trạng thái cơ sở dữ liệu ở bên trong nhưng chủ động ẩn thông tin chi tiết của từng thành phần.

Cấu hình mặc định cho môi trường local hoạt động mà không cần file `.env`. Nếu muốn đổi cổng hoặc thông tin đăng nhập, hãy sao chép `.env.example` thành `.env`; file `.env` đã được Git bỏ qua.

PostgreSQL mặc định được mở tại cổng `5433` trên máy host để tránh trùng với cổng phổ biến của PostgreSQL đã cài sẵn. Bên trong mạng Compose, các container vẫn kết nối qua cổng `5432`.

## Khởi tạo hoặc kiểm tra schema rỗng

PostgreSQL entrypoint tự chạy `database/init/001-create-schema.sql` trong lần khởi động đầu tiên. Script có tính lặp an toàn và có thể chạy lại thủ công.

Trên Windows PowerShell:

```powershell
.\scripts\init-db.ps1
```

Trên macOS hoặc Linux:

```bash
sh ./scripts/init-db.sh
```

Volume có tên `edufit_postgres_data` giữ nguyên schema và dữ liệu trong tương lai khi chạy `docker compose down` rồi khởi động lại. Không chạy `docker compose down -v` trừ khi bạn chủ động muốn xóa dữ liệu cơ sở dữ liệu trên máy.

## Kiểm tra chất lượng backend trên máy

Nếu máy đã cài Java 21, hãy dùng Maven Wrapper đi kèm dự án:

```bash
cd backend
sh ./mvnw -B -ntp verify
```

Trên Windows PowerShell, dùng:

```powershell
cd backend
.\mvnw.cmd -B -ntp verify
```

Xem `CONTRIBUTING.md` để biết quy tắc định dạng, đặt tên, branch, review và commit. Xem `docs/backend-architecture.md` để hiểu ranh giới giữa các module.

## Thiết lập GitHub sau lần push đầu tiên

1. Thay `OWNER/REPOSITORY` trong badge CI ở đầu file bằng owner và tên repository thật trên GitHub.
2. Áp dụng các quy tắc bảo vệ branch trong `docs/github-branch-protection.md`.
3. Mở một PR thử nghiệm có lỗi lint hoặc lỗi biên dịch cố ý, sau đó xác nhận `Backend CI` thất bại và PR bị chặn merge.
4. Sửa lỗi và xác nhận cùng một kiểm tra chuyển sang trạng thái thành công trong vòng năm phút.
