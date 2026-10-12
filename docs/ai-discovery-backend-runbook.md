# AI + Discovery backend runbook

Runbook này bàn giao backend ở Phase 5 trên `feat/discovery-ai`. Frontend có thể dùng
contract dưới đây để test mà không cần sửa backend. Script không tạo tài khoản Admin,
không chạy `docker compose down -v` và không xóa dữ liệu/volume hiện có.

## Runtime contract

- Base URL local: `http://localhost:8080`.
- Session cookie: `EDUFIT_SESSION`. Hãy dùng cùng một hostname trong cả phiên; không
  đổi qua lại giữa `localhost` và `127.0.0.1`.
- Trước mọi POST/PUT/DELETE: GET `/api/v1/auth/csrf`, giữ cookie trong cùng cookie jar,
  rồi gửi token trả về qua header `X-XSRF-TOKEN`.
- Response thành công dùng `{ "data": ... }`; lỗi dùng `ApiError`. Match yêu cầu
  Student hoặc Parent đã liên kết; search, detail và catalogs là public.
- OpenAPI JSON: `/v3/api-docs`; Swagger UI: `/swagger-ui.html`.

## Fixture local

Flyway tạo các fixture ổn định sau. Mật khẩu local dùng chung là `Password123@`.

| Vai trò | Email | ID cần dùng |
| --- | --- | --- |
| Student | `student.demo@edufit.vn` | student profile `11111111-1111-1111-1111-000000000001` |
| Tutor VERIFIED | `tutor.demo@edufit.vn` | tutor `22222222-2222-2222-2222-000000000001` |
| Parent | `parent.demo@edufit.vn` | đăng nhập rồi tạo liên kết qua invitation |

Tutor fixture dạy subject `1` ở education level `4`. Smoke script cập nhật Student về
level `4`, tạo goal subject `1`, availability thứ Hai 18:00–20:00, rồi dùng chính
`goalId` trả về. Catalog ID luôn lấy thêm được từ `/api/v1/catalogs`.

## Cold start và smoke offline

Từ root repository, dừng rồi dựng lại container mà vẫn giữ volume:

```powershell
docker compose down
$env:AI_PROVIDER = 'stub'
$env:GEMINI_API_KEY = ''
docker compose up --build -d
docker compose ps
./scripts/smoke-ai-discovery.ps1 -ExpectedAiMode offline -VerifyDatabase
```

Script kiểm tra health/Flyway gián tiếp qua ứng dụng đã UP, OpenAPI, public
search/detail/catalogs, session + CSRF, 401/403/400, Student và Parent matching,
revoke, no-result, Connection request và Tutor accept tạo class. Chế độ stub phải trả
`aiGenerated=false`; đây là offline pass, không phải bằng chứng Gemini thật.

Database có tính bền vững nên một fixture đã có class ACTIVE có thể làm lần chạy lại
Connection trả conflict. Dùng database local sạch cho lần nghiệm thu đầu tiên; không
thêm thao tác xóa volume vào collection tự động.

## Live Gemini opt-in

Không ghi key vào `.env`, script, collection, log hay command history. Chỉ nạp key vào
environment của process hiện tại và cấu hình backend bằng biến môi trường:

```powershell
$env:AI_PROVIDER = 'gemini'
$env:GEMINI_API_KEY = '<secret chỉ trong process hiện tại>'
$env:GEMINI_MODEL = 'gemini-3.5-flash-lite'
docker compose up --build -d backend
./scripts/smoke-ai-discovery.ps1 -ExpectedAiMode live -VerifyDatabase
```

Trước khi gọi backend, script gọi Models API bằng header `x-goog-api-key` và xác nhận
model hỗ trợ `generateContent`. Một live pass cần đồng thời: model check pass,
`aiGenerated=true`, explanation không rỗng dựa trên facts của match, và AI audit gần
nhất của Student có status `SUCCESS`. Output chỉ ghi provider mode/model, thời gian,
goal/tutor/class ID; không ghi key. Khi chưa có `GEMINI_API_KEY`, trạng thái bàn giao là
**offline pass, live pending**.

## Quota và fallback

Thêm `-ExerciseQuota` để gọi đủ hạn mức 10 AI attempts/24 giờ của actor; lượt sau đó
phải fallback và trả `aiGenerated=false`. HTTP 429, safety block, timeout và malformed
provider response đã được cover ở backend E2E bằng fake HTTP provider. Để quay về
chế độ không cần mạng:

```powershell
$env:AI_PROVIDER = 'stub'
$env:GEMINI_API_KEY = ''
docker compose up --build -d backend
./scripts/smoke-ai-discovery.ps1 -ExpectedAiMode offline
```

## Bàn giao cho frontend

| Việc | Route/payload chính | Kết quả cần xử lý |
| --- | --- | --- |
| Khởi tạo CSRF | `GET /api/v1/auth/csrf` | lưu cookie + `data.token` |
| Đăng nhập | `POST /api/v1/auth/login` với email/password | refresh CSRF sau login |
| Search/detail | `GET /api/v1/discovery/tutors`, `GET .../{tutorId}` | public, paged search |
| Goal/slots | `/api/v1/profiles/students/me/goals` và `.../{goalId}/availability` | dùng goal ID server trả về |
| Match | `POST /api/v1/discovery/match` với `{ goalId, topN }` | `aiGenerated` chỉ chứng minh output AI khi live |
| Parent | invite -> Student accept -> `GET /discovery/students/{studentId}/goals` | revoke xong trả 403 |
| Connection | `POST /api/v1/connections/requests`, Tutor respond | tạo class ACTIVE |

Các lỗi smoke cố định gồm: anonymous match 401, sai CSRF 403, `topN=11` trả 400,
Parent đã revoke trả 403, subject không có tutor trả danh sách rỗng. Chi tiết field,
pagination và ownership nằm trong [Discovery API contract](discovery-api-contract.md),
[Phase 4 HTTP contract](ai-discovery-phase4-contract.md) và
[Phase 5 E2E evidence](ai-discovery-phase5-e2e.md).
