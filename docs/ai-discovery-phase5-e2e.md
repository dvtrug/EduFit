# AI + Discovery backend — Phase 5 E2E contract

Tài liệu này chốt bằng chứng B13 cho đường đi từ Discovery sang Connection. Runbook
và smoke script dùng khi bàn giao frontend nằm ở B14.

## Luồng đã khóa bằng test

`AiDiscoveryConnectionE2ETest` khởi động application context đầy đủ với security
filter chain, JDBC session, Flyway và PostgreSQL thật. Chỉ Gemini là fake HTTP có
kiểm soát; IAM, Profile, Discovery, AI quota/log, Connection, Notification và Audit
dùng production bean.

Luồng Student chạy qua HTTP:

1. lấy CSRF token, đăng nhập và nhận cookie `EDUFIT_SESSION`;
2. cập nhật cấp học, tạo goal và thay toàn bộ availability;
3. search, đọc detail và match tutor;
4. tạo connection request;
5. Tutor đăng nhập, đọc inbox và accept;
6. hai accept đồng thời trả một `200` và một `409`; DB chỉ có một request
   `ACCEPTED` và một class `ACTIVE`.

Luồng Parent mời Student, Student accept, sau đó Parent đọc goal, match và gửi
request. Khi Student thu hồi link, ba thao tác Parent tương ứng đều bị từ chối
`403`; request bị từ chối trước khi Gemini được gọi.

## AI và fallback

Top match thành công trả explanation từ provider và `aiGenerated=true`; quota
reservation, `ai_request_log=SUCCESS` và `matching_run_log` được ghi với đúng actor,
student và goal. Fake provider còn chạy ba lỗi độc lập: HTTP 429, blocked prompt và
timeout. Cả ba vẫn trả ranking/explanation theo rule với `aiGenerated=false`, sau đó
Student vẫn tạo connection request được. Empty candidate trả danh sách rỗng, ghi
matching run có `result_count=0` và không gọi AI/không ghi AI log.

`MatchingWorkflowPostgresTest` tiếp tục kiểm chứng event chỉ được chuyển sau commit
và không được chuyển khi transaction rollback. `DiscoveryReadPostgresTest` tiếp tục
giữ query-count/candidate-cap regression; B13 không thay đổi query hoặc cap.

## Eligibility giữa Discovery và Connection

Discovery lấy candidate theo subject và cho scorer chấm độ gần của cấp học. Vì vậy
tutor dạy cùng subject ở cấp liền kề có thể xuất hiện trong ranking. Đây là gợi ý
khám phá, chưa phải xác nhận tutor được phép nhận goal.

Connection là cổng tạo quan hệ học thật và yêu cầu tutor `VERIFIED` đồng thời có
đúng bộ `(tutorId, subjectId, educationLevelId)`. Nếu chỉ lệch education level,
`POST /api/v1/connections/requests` trả HTTP `400`, code `INVALID_OPERATION`, message
`Gia sư không phù hợp mục tiêu`. Không tự làm mềm rule Connection và cũng không âm
thầm biến level thành hard filter của Discovery.

## Lỗi đồng thời đã sửa

Các thao tác Connection từng đọc entity, chờ PostgreSQL advisory lock rồi query
pessimistic lock trong cùng persistence context. Khi request khác cập nhật lúc đang
chờ, Hibernate phát hiện hai version của cùng entity và trả `500`. Service hiện
detach bản đọc dùng để xác định lock key trước khi query lại sau advisory lock. Cách
này áp dụng đồng nhất cho respond/withdraw invitation, revoke link, cancel và respond
request; trạng thái stale sau lock được map thành business conflict thay vì lỗi hệ
thống.
