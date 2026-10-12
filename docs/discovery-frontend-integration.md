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
`npm run dev` và `npm run start` cố định cổng 3000. Nếu cổng đang được sử dụng,
dừng frontend cũ trước khi chạy lại; không chuyển sang 3001 vì backend chỉ cho
phép origin `http://localhost:3000` và `http://127.0.0.1:3000`. Request đăng nhập
từ origin 3001 bị trả `403 Invalid CORS request` dù CSRF hợp lệ.
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

### Matching theo bốn tiêu chí

- Nhu cầu học 40%: trung bình điểm đúng môn và cấp học. Môn là điều kiện đầu vào;
  cấp học gần nhau vẫn là gợi ý, nhưng giải thích phải nêu độ lệch.
- Năng lực/độ tin cậy 25%: ứng viên phải có hồ sơ VERIFIED; điểm dùng đánh giá
  có điều chỉnh theo số lượt phản hồi. Kinh nghiệm tự khai là dữ kiện tư vấn,
  chưa phải chứng nhận chuyên môn hoặc một điểm cộng riêng trong công thức.
- Lịch/hình thức 25%: điểm lịch theo thời lượng trùng; hình thức và khu vực
  học trực tiếp là điều kiện lọc trước khi xếp hạng.
- Ngân sách 10%: học phí so với mức tối đa người học đặt.

Backend tính điểm và xếp hạng ổn định; AI giải thích lựa chọn, không tự đổi điểm
hoặc thứ tự. Gia sư đầu danh sách được làm nổi bật, các lựa chọn còn lại nằm dưới.
API giữ nguyên scoreBreakdown hiện có; UI gộp subjectFit và levelFit để hiển thị
điểm nhu cầu học. Điểm phù hợp là điểm xếp hạng, không phải xác suất học thành công.

Một matching run gọi AI một lần cho toàn bộ kết quả (tối đa 10 người), dùng quota
của người gửi yêu cầu, kể cả khi phụ huynh tìm cho con. Ngữ cảnh gồm loại mục tiêu,
mức hiện tại/mong muốn, hạn học, ngân sách, lịch, điểm số và dữ kiện riêng từng gia sư.
Không gửi tên, email, số điện thoại, bio hay phương pháp tự do của gia sư sang AI.
Chưa chấm semantic độ phù hợp với luyện thi/nâng điểm vì thiếu dữ liệu thành tích
giảng dạy tương ứng; AI không được suy diễn thành tích, bằng cấp hoặc kết quả học.

Mỗi lý do AI dài 1–2 câu, tối đa 180 ký tự, chọn một góc đánh giá riêng từ nhu cầu
người học và dữ kiện từng gia sư; không bắt mọi nhận xét liệt kê đủ bốn tiêu chí.
Nhu cầu hiện lấy từ form mục tiêu học đã lưu, chưa có ô prompt tự do riêng.
Mỗi lý do được gắn theo vị trí ứng viên trong danh sách đã xếp hạng. Thiếu mục,
trùng chỉ số, nội dung quá dài hoặc cùng mẫu chỉ thay số sẽ dùng lời giải thích
dựa trên dữ kiện riêng của ứng viên đó và trả aiGenerated=false. Provider lỗi,
quota hết hoặc chế độ stub cũng dùng fallback; giao diện phân biệt hai nguồn rõ ràng.
Fallback cũng tối đa 180 ký tự, tập trung vào một điểm đáng chú ý hoặc hạn chế;
các hồ sơ tương đương có thể chung nhận xét khi chưa đủ dữ kiện để phân biệt.

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

Bổ sung cho thay đổi matching bốn tiêu chí:

- Sau khi rút ngắn nhận xét: 61 test backend pass, gồm kiểm tra loại nhận xét
  quá 180 ký tự, cùng mẫu chỉ thay số, matching, controller và PostgreSQL/E2E.
- Kiểm tra riêng Gemini bằng câu tổng hợp không chứa dữ liệu cá nhân nhận HTTP 200:
  key dùng được tại thời điểm kiểm tra. Database xác nhận tài khoản demo đủ 10 lượt
  trong 24 giờ; đây là nguyên nhân matching bị chặn trước khi gọi provider.
  Kiểm tra kết nối này chưa xác minh chất lượng nhận xét Gemini mới trên dữ liệu thật.
- 234 test backend liên quan pass (profile, scoring, discovery, quyền truy cập,
  mục tiêu học, E2E connection, quota đồng thời và giải thích theo nhóm).
- 81 test frontend pass; TypeScript, production build và lint hai file UI/test đã sửa pass.
- Smoke trên backend tạm trả đủ 6 ứng viên, đứng đầu Cô Trần Thu Hà 99.50 điểm;
  mỗi fallback có dữ kiện riêng, gồm trường hợp vượt ngân sách và thiếu lịch.
- Chưa nhận được lý do Gemini thật trong smoke này: tài khoản student.demo đã
  dùng đủ 10 lượt AI trong cửa sổ 24 giờ. Không reset hoặc bỏ qua quota để test.
  Backend tạm đã dừng sau kiểm tra; cần restart backend IDE để nạp code mới.

- Backend: 79 test PostgreSQL/E2E pass; kiểm tra migration script thêm 2 test pass.
- Frontend: 79 test pass, TypeScript và production build pass; lint không có error,
  còn các warning ở mã UI/hook hiện có.
- Browser: đăng nhập tài khoản học sinh demo; danh sách 6 gia sư và hồ sơ chi tiết
  tải từ API; mục tiêu seed 2030-06-01 cho 6 kết quả matching, có giải thích Gemini.
- Audit `MATCH_EXPLANATION=SUCCESS`, latency 8667 ms. Gửi yêu cầu từ hộp xác nhận
  qua frontend; danh sách yêu cầu và truy vấn database cùng xác nhận `PENDING`.
- Flyway trên DB đang dùng đã áp dụng thành công V1.13 sau V1.12, giữ nguyên dữ liệu.
