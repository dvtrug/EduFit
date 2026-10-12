# Test frontend discovery với backend AI

Nhánh tích hợp: `feat/discovery-ai`, merge từ `feature/frontend-discovery`.
Yêu cầu: Java 21 nếu chạy backend từ IDE; Docker Desktop cho PostgreSQL/Mailpit;
Node.js >= 20.9 và npm cho frontend.

## Chạy local

Từ thư mục gốc repository:

```powershell
docker compose up -d --build backend
```

Đợi `http://localhost:8080/actuator/health` trả `UP` rồi chạy frontend:

```powershell
cd frontend
npm ci
npm run dev
```

Mở `http://localhost:3000/login`, bấm tài khoản demo Học sinh hoặc Phụ huynh.
Mật khẩu demo là `Password123@`; email `student.demo@edufit.vn`, `parent.demo@edufit.vn`,
`tutor.demo@edufit.vn`. Nếu chạy backend bằng IDE, dừng container backend trước bằng
`docker compose stop backend` để tránh trùng cổng 8080.

### Chạy backend từ IDE

Mở Docker Desktop, đợi engine sẵn sàng, rồi chạy tại thư mục gốc repository:

```powershell
docker compose up -d --wait db mail
docker compose stop backend
Test-NetConnection localhost -Port 5433
```

Khi database báo `Healthy` và `TcpTestSucceeded: True`, Run ứng dụng backend
trong IDE bằng JDK 21. Cấu hình mặc định kết nối PostgreSQL tại
`localhost:5433`; nếu đổi cổng publish `POSTGRES_PORT`, đặt `DB_PORT` tương ứng
trong cấu hình Run của IDE (hoặc đặt `DB_URL` đầy đủ).

Nếu Flyway báo `Connection to localhost:5433 refused`, PostgreSQL chưa nhận
kết nối: kiểm tra Docker Desktop đang chạy và `docker compose ps db` báo healthy,
rồi chạy lại lệnh bật `db mail` phía trên. Lỗi này xảy ra trước bước migration;
không cần sửa file SQL, chạy `flyway repair` hay xóa volume database.

Next.js proxy `/api/*` sang backend cổng 8080. Browser dùng cùng origin cổng 3000,
gửi cookie `EDUFIT_SESSION`; mỗi mutation lấy token CSRF mới trước khi gửi.
Không đặt Gemini API key trong frontend. Docker đọc `AI_PROVIDER`,
`GEMINI_API_KEY`, `GEMINI_MODEL` từ `.env` ở gốc repo.

## Luồng kiểm tra

1. Đăng nhập học sinh, mở `/student/tutors`. Danh sách, bộ lọc và chi tiết lấy
   từ API discovery; lỗi API hiển thị trên màn hình.
2. Chọn mục tiêu từ danh sách đã lưu. Seed `V1.13` thêm mục tiêu Toán lớp 9
   và các gia sư demo. Tab **Gợi ý phù hợp** gửi goalId thật sang API matching.
3. **Thay đổi nhu cầu** cập nhật hồ sơ học sinh, tạo mục tiêu qua POST rồi
   lưu lịch qua PUT `/{goalId}/availability`. Nếu lưu lịch thất bại, frontend
   cố archive mục tiêu vừa tạo và hiển thị lỗi, không ghép mục tiêu thiếu lịch.
4. **Xem lý do** hiển thị explanation và điểm do backend trả về.
   `aiGenerated=true` chỉ khi backend lấy được giải thích AI; khi fallback,
   frontend vẫn hiển thị kết quả theo tiêu chí và không gắn nhãn AI giả.
5. **Gửi yêu cầu** mở hộp xác nhận với mục tiêu, khung giờ và lời nhắn tùy chọn.
   Sau khi API connection thành công, tab **Yêu cầu đã gửi** đọc dữ liệu đã lưu.
   Bấm **Làm mới** để xem trạng thái phản hồi mới nhất.
6. Phụ huynh chỉ chọn được học sinh có liên kết `CONFIRMED`, đọc mục tiêu qua
   `/api/v1/discovery/students/{studentId}/goals`. Mục tiêu do học sinh quản lý;
   giao diện phụ huynh không gọi API chỉnh sửa hồ sơ/mục tiêu học sinh.

Không có liên kết xác nhận thì phụ huynh thấy hướng dẫn, không tự tạo liên kết.
Backend kiểm tra lại quyền sở hữu, subject/cấp lớp và yêu cầu trùng khi gửi kết nối.
Gia sư được gợi ý gần cấp lớp có thể không đủ điều kiện kết nối đúng cấp lớp;
lỗi backend được giữ lại trong hộp gửi yêu cầu.

Các dashboard khác vẫn có dữ liệu minh họa từ nhánh frontend; luồng discovery,
matching và danh sách yêu cầu ở trang tìm gia sư dùng API thật. Giao diện gia sư
phản hồi yêu cầu chưa nằm trong phạm vi tích hợp này; backend đã có
`POST /api/v1/connections/requests/{id}/respond` để test phản hồi.

## Kiểm chứng tự động

```powershell
# frontend
npm test
npm run type-check
npm run lint
npm run build

# backend, chạy tại backend/ với JAVA_HOME trỏ JDK 21 và Docker đang chạy
mvn -B -ntp -pl app -am '-Dtest=FlywayMigrationScriptTest,FlywayMigrationIT,AiDiscoveryConnectionE2ETest,DiscoveryHttpSecurityPostgresTest,LearningGoalPostgresTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Migration giữ nguyên V1.09–V1.12 đã phát hành; seed frontend chuyển sang V1.13
để không trùng migration quota AI. Không dùng `flyway repair` hoặc xóa volume DB.

## Kết quả kiểm tra ngày 12/10/2026

- Backend: 79 test PostgreSQL/E2E pass; kiểm tra migration script thêm 2 test pass.
- Frontend: 79 test pass, TypeScript và production build pass; lint không có error,
  còn các warning ở mã UI/hook hiện có.
- Browser: đăng nhập tài khoản học sinh demo; danh sách 6 gia sư và hồ sơ chi tiết
  tải từ API; mục tiêu seed 2030-06-01 cho 6 kết quả matching, có giải thích Gemini.
- Audit `MATCH_EXPLANATION=SUCCESS`, latency 8667 ms. Gửi yêu cầu từ hộp xác nhận
  qua frontend; danh sách yêu cầu và truy vấn database cùng xác nhận `PENDING`.
- Flyway trên DB đang dùng đã áp dụng thành công V1.13 sau V1.12, giữ nguyên dữ liệu.
