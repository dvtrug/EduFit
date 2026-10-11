# Connection — HTTP contract và quyết định refactor

Ngày chốt: 11/10/2026. Scope UC3.1–UC3.5; [plan](../../tasks/connection/plan.md), [tasks](../../tasks/connection/todo.md), [baseline](phase0-validation.md). T01 chỉ chốt tài liệu; các mục **đích** chưa được triển khai trong Phase 0.

Nguồn đối chiếu: `docs/SRS Edufit.docx` (UC3.1–3.5, BR-26–38), ConnectionController/ConnectionService, ProfileFacade/ConnectionGoalDto, IAM UserSummaryView, ApiResponse/ApiError/FieldErrorItem, GlobalExceptionHandler và SecurityConfig/application.yml trên base tích hợp. Tài liệu nguồn là bằng chứng nghiệp vụ, không tự mở rộng quyền thực thi.

## 1. Quy ước chung và quyền

- Prefix: `/api/v1/connections`. JSON UTF-8, Content-Type `application/json` cho body. Tất cả success hiện tại là **200**, kể cả tạo request/link/class; giữ để không đổi client.
- Actor lấy từ CurrentUser/session **EDUFIT_SESSION**; không nhận actor/role/sender từ request. Session HttpOnly, SameSite=Lax, timeout 60 phút theo main hiện tại.
- `parentUserId`, inviter/invited/sender IDs là account IDs; `studentId`, `tutorId` là profile IDs. Không dùng thay thế cho nhau. `goalId` thuộc Student được xác thực qua ProfileFacade.
- Timestamp là ISO-8601 Instant UTC. `deadline` của goal là LocalDate `YYYY-MM-DD` hoặc null, không chuyển thành một Instant giả.
- Main mới đang miễn CSRF toàn `/api/v1/**`; **baseline Phase 0 chưa bảo vệ CSRF cho Connection**. Đích T21: chỉ thu hẹp exemption đối với prefix Connection để các write routes yêu cầu cookie `XSRF-TOKEN` và header `X-XSRF-TOKEN`. Giữ policy các API khác/CORS/session; test filter chain thật phải chứng minh write thiếu token ->403, token hợp lệ -> xử lý nghiệp vụ. Client phải gửi token trước khi bật thay đổi này; endpoint lấy token hiện có là `GET /api/v1/auth/csrf`.
- Parent chỉ được thao tác dữ liệu Student khi link hiện tại CONFIRMED. PENDING/DECLINED/REVOKED không cấp quyền. Student giữ quyền với request do Parent đã gửi sau revoke. Tutor chỉ inbox và respond request đúng tutor profile của mình.

Success: `{ "success": true, "data": <object|array|null>, "message": "Thành công", "timestamp": "...Z" }`.

Error: `{ "code": "...", "message": "...", "details": [], "timestamp": "...Z" }`. Error **không có** `success:false`. Field error gồm `field`, `message`, `rejectedValue`; không echo email/message/reason vào lỗi/log. Timestamp động, assertion kiểm tra parseable/type hoặc dùng clock cố định, không so literal runtime.

## 2. Mười routes hiện có phải giữ

Các response names ở bảng là schema mục 3, nằm trong `ApiResponse.data`.

| Method và suffix | Input | Actor/phạm vi | Success data |
| --- | --- | --- | --- |
| POST `/parent-student/invite` | `{targetEmail}`: bắt buộc, nonblank/email | PARENT hoặc STUDENT, mời account đối vai trò | null; message luôn `Nếu tài khoản hợp lệ, lời mời sẽ được gửi` |
| GET `/parent-student/invitations` | Không body | PARENT/STUDENT, chỉ lời mời mình gửi/nhận | `InvitationResponse[]`, gồm lịch sử |
| POST `/parent-student/respond` | `{invitationId,accept}` | Chỉ account nhận lời mời | `LinkResponse` nếu accept; null nếu decline |
| POST `/parent-student/invitations/{id}/withdraw` | UUID path, không body | Chỉ người gửi lời mời | null |
| GET `/parent-student/links` | Không body | PARENT: links của mình; STUDENT: links của profile mình | `LinkResponse[]`, chỉ CONFIRMED |
| DELETE `/parent-student/links/{id}` | UUID path; optional body `{reason}` | STUDENT sở hữu hoặc ADMIN; Parent bị 403 | null |
| POST `/requests` | `{goalId,tutorId,desiredSlots,message?}` | STUDENT hoặc Parent có link CONFIRMED với Student sở hữu goal | `RequestResponse` |
| GET `/requests` | Optional query `studentId` UUID | STUDENT: luôn profile mình; PARENT: bắt buộc Student có link; TUTOR: inbox mình, không mở scope bằng query | Learner: `RequestResponse[]`; Tutor: `TutorInboxResponse[]` |
| POST `/requests/{id}/cancel` | UUID path, không body | STUDENT sở hữu hoặc Parent còn link CONFIRMED; không bắt buộc là người gửi ban đầu | null |
| POST `/requests/{id}/respond` | UUID path, `{accept,reason?}` | TUTOR đích | `ClassResponse` nếu accept; null nếu reject |

Query `studentId` của Student/Tutor không cho phép đổi scope; giữ hành vi hiện có là service bỏ qua giá trị đó. Không quảng bá nó như bộ lọc inbox Tutor. UUID malformed sẽ có 400 ở đích T21.

## 3. Response schemas

| Schema | Fields hiện có | Bổ sung đích / task |
| --- | --- | --- |
| InvitationResponse | invitationId UUID, inviterUserId UUID, invitedUserId UUID, status string, createdAt Instant, expiresAt Instant | T18c: `reinviteAfter` Instant nullable, `inviterName` string nullable, `inviterRole` string nullable; chỉ hydrate lịch sử đã được scope |
| LinkResponse | linkId UUID, parentUserId UUID, studentId UUID, status string, confirmedAt Instant | Không thêm admin reason vào learner response |
| RequestResponse | requestId UUID, studentId UUID, tutorId UUID, goalId UUID, status string, createdAt Instant, expiresAt Instant | Giữ field names/types; không thay bằng inbox Tutor |
| TutorInboxResponse | requestId UUID, goalType string, subjectId integer, educationLevelId integer, desiredSlots string[], message string nullable, senderRole STUDENT/PARENT, status string, expiresAt Instant | T18c: `deadline` LocalDate nullable |
| ClassResponse | classId UUID, studentId UUID, tutorId UUID, goalId UUID | Giữ một goal/class; public facade nested ActiveClassView không đổi |

Tên/vai trò inviter đáp ứng UC3.2: màn hình consent còn phải giải thích Parent được xem báo cáo/lịch/progress và tìm/gửi request thay Student; Student có thể thu hồi quyền. Đây là dữ liệu cần cho màn hình tương lai, Phase 0 không triển khai frontend. Nếu inviter đã bị xóa, tên/role null và không cho accept; không dùng fallback giả Student.

Tutor inbox BR-37 không trả email/contact, parent account/profile/name, student account/profile hoặc goals khác. IAM batch summary có email nhưng mapper chỉ chọn senderRole. Nếu goal/sender metadata cần thiết bị thiếu: trả controlled 409 `CONFLICT_DETECTED` cho toàn response, thông điệp chung; không drop rows, không tạo role/goal giả. Goal có deadline null hợp lệ không phải missing metadata.

## 4. Input, thời gian và transitions đã chốt

- Cả hai body respond: đích T04 dùng nullable Boolean + NotNull; thiếu/null ->400 `VALIDATION_FAILED`; `false` là decline/reject có chủ ý. Hiện primitive thiếu field bị hiểu false, đó là bug phải có regression.
- Request `goalId`,`tutorId`,`desiredSlots` bắt buộc; ít nhất một slot; mỗi string nonnull/nonblank và tối đa 100 ký tự. `message` optional, tối đa 500 ký tự. T13 giữ semantics Java String length hiện tại, không tự trim/cắt nội dung.
- Reject reason optional, tối đa 300 ký tự ở T14; hiện DB varchar(300) nhưng service chưa validate. Accept không sử dụng reason; không lưu nó làm lý do reject.
- Revoke reason: Admin bắt buộc nonblank; Student optional. Schema hiện là TEXT, SRS không quy định max, không tự áp giới hạn 300 của reject lên revoke. Admin reason lưu audit, không đưa vào notification hoặc learner response.
- Invitation TTL 14 ngày; cooldown cùng inviter/cặp sau decline 7 ngày; request TTL 7 ngày. `now >= expiresAt` là hết hạn; `now >= reinviteAfter` được mời lại. Quota 10 attempts/giờ kể cả email lạ, attempt thứ11 ->429; window cố định theo bản ghi quota, không gọi là sliding window.
- Invite tới email không tồn tại, sai role, inactive, self, duplicate PENDING, đã CONFIRMED hoặc đang cooldown: cùng 200/data null/message generic. Không trả flag tồn tại account/link. Cooldown chỉ hiện `reinviteAfter` trong GET lịch sử của actor đã biết invitation; POST giữ shape/message đồng nhất.
- Invitation PENDING -> ACCEPTED/DECLINED/WITHDRAWN/EXPIRED. Link CONFIRMED -> REVOKED; relink chỉ qua invitation mới. Request PENDING -> ACCEPTED/REJECTED/CANCELLED/EXPIRED; terminal ->409, không xử lý lại thành công giả.
- Accept invitation khi link đã CONFIRMED: đóng invitation thành **WITHDRAWN**, event reason nội bộ `SYSTEM_ALREADY_LINKED`, rồi HTTP409 sau khi transition commit. Không thêm CLOSED enum vào schema. T11a/T11b triển khai state/event; typed result T09 giữ expire/closure nhưng rollback các lỗi không được commit.
- Accept request revalidate goal ACTIVE, goal.studentId=request.studentId, Tutor VERIFIED/chuyên môn, chưa có ACTIVE class; một transaction tạo đúng một class và ACCEPTED. Không dùng request cancel để kết thúc class đã tạo.
- Sau revoke Parent không đọc/cancel/send thay Student hoặc đọc notification chứa dữ liệu Student; cho phép đúng thông báo thu hồi tối thiểu cho chính Parent, không kèm dữ liệu học tập/admin reason. T15 xác minh actual read API, không chỉ writer.

## 5. Grammar slots và dữ liệu legacy

Đích writer v1 giữ JSON **List<String>**, mỗi item canonical dạng `MONDAY 18:00-19:00`. Day là MONDAY..SUNDAY; HH:mm 24h, phút 00..59, start < end, cùng một ngày, không overnight/24:00. Reject duplicate và overlap trong cùng day; hai khoảng sát nhau được phép. Không tự sửa/sort strings do client gửi. Đây là lịch tuần theo **Asia/Ho_Chi_Minh**, không đổi giờ tuần sang UTC; timestamp của records vẫn UTC. Việc đặt session/date/availability thuộc Scheduling, không suy ra cam kết booking từ request slots.

Inventory source T01: ConnectionServiceTest có `Mon 18:00`, `Tue 19:00`; service hiện chỉ chặn null/blank/>100. Frontend services/features hiện chưa có Connection API writer thực. Chưa truy cập DB người dùng nên **chưa có inventory persisted data/clients ngoài repo**.

T13a cần parser strict-v1 và rollout setting `edufit.connection.slots.validation-mode`: mặc định `legacy-compatible` giữ validation hiện tại cho old writers; mode `strict-v1` áp grammar trên. Trong compatibility mode chuỗi cũ là legacy chưa được xác minh lịch, không gọi nó là interval hợp lệ. Writer mới gửi canonical v1; không thêm request field version tùy tiện. Trước bật strict phải inventory DB/clients, ghi số lượng/format và cập nhật writers. Tests phải chạy cả hai modes. Không bật strict âm thầm nếu chưa có evidence.

Readers giữ nguyên string[] đã lưu, gồm legacy điểm giờ không có giờ kết thúc; không đoán end time, không sửa/xóa/migrate dữ liệu đó chỉ để parser pass. Giá trị invalid ở strict-v1 ->400 `VALIDATION_FAILED`, vẫn đọc được lịch sử cũ. T25 bàn giao điều kiện bật strict; Phase 0 chỉ chốt chiến lược.

## 6. Routes bổ sung, scope và paging

T19a bổ sung GET `/parent-student/invitations/page`, `/parent-student/links/page`, `/requests/page`; actor/filter giống route list tương ứng. Giữ nguyên list cũ, không truncate và không đổi thành object.

Query: page zero-based mặc định0; size mặc định20, range1..100. Sai page/size ->400; không clamp âm hoặc overflow. Page vượt cuối ->items rỗng và totals đúng. Không nhận sort tự do. Invitation/request: createdAt DESC rồi ID DESC; link: confirmedAt DESC rồi linkId DESC. Offset paging có tie-breaker ổn định trên dataset không đổi; không hứa snapshot consistency khi có concurrent inserts.

`data` page là `{ "items": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }`. Không expose Spring Page implementation. History invitation/request có terminal statuses, luôn scoped; links page của learner vẫn chỉ CONFIRMED như list cũ. T18 additions áp cho cả list và page.

T19b bổ sung GET `/admin/parent-student/links?email=...&page=0&size=20` dưới prefix Connection; explicit service/HTTP ADMIN guard vì URL này không bắt đầu `/api/v1/admin`. Email bắt buộc, nonblank/email, tra exact email qua IAM theo semantics hiện tại, không fuzzy/public enumeration. ADMIN có thể tìm theo Parent hoặc Student; chỉ CONFIRMED links để phục vụ revoke UC3.3, page items dùng LinkResponse. Không match ->200 page rỗng; account Tutor/Admin ->rỗng. Không trả email/admin reason/history rộng hơn. Giữ DELETE hiện có để revoke; Parent/Student gọi lookup ->403.

## 7. Error/status contract và baseline gaps

| HTTP | Code | Ví dụ |
| --- | --- | --- |
| 400 | VALIDATION_FAILED | Missing/null accept, malformed JSON/UUID, invalid slots/size/length |
| 400 | INVALID_OPERATION | Goal không ACTIVE, Tutor không phù hợp, Admin thiếu reason |
| 401 | UNAUTHORIZED | Session không có/hết hạn ở authenticated routes |
| 403 | FORBIDDEN | Wrong role/owner/link; CSRF thiếu/sai ở đích T21 |
| 404 | RESOURCE_NOT_FOUND | UUID hợp lệ nhưng resource không có |
| 409 | CONFLICT_DETECTED | Terminal/expired/duplicate/pending limit/already ACTIVE class/controlled metadata inconsistency |
| 429 | RATE_LIMIT_EXCEEDED | Invite attempt thứ11 |
| 500 | INTERNAL_SERVER_ERROR | Lỗi ngoài dự kiến; không stack trace/SQL/PII |

Hiện GlobalExceptionHandler chưa có mapping riêng malformed JSON/type mismatch; có thể rơi catch-all500. T21 thêm mapping400 có regression và kiểm chứng các callers của shared handler. Business InvalidOperation hiện400 tiếp tục giữ, không tự đổi sang422. Field validation message có thể khác ngôn ngữ, không dựa vào text đó để branch client. Lỗi không chứng minh quyền biết resource tồn tại; tests phải kiểm tra không có dữ liệu/side effect bị lộ cho wrong owner.

## 8. Fixtures và acceptance cho các task sau

Fixture invite generic (giữ cho mọi outcome không quota):

```json
{"success":true,"data":null,"message":"Nếu tài khoản hợp lệ, lời mời sẽ được gửi","timestamp":"2026-10-11T03:00:00Z"}
```

Fixture POST request body canonical cho writer mới:

```json
{"goalId":"10000000-0000-0000-0000-000000000001","tutorId":"20000000-0000-0000-0000-000000000001","desiredSlots":["MONDAY 18:00-19:00"],"message":"Em muốn ôn tập Toán."}
```

Fixture learner request data (không phải inbox Tutor):

```json
{"requestId":"30000000-0000-0000-0000-000000000001","studentId":"40000000-0000-0000-0000-000000000001","tutorId":"20000000-0000-0000-0000-000000000001","goalId":"10000000-0000-0000-0000-000000000001","status":"PENDING","createdAt":"2026-10-11T03:00:00Z","expiresAt":"2026-10-18T03:00:00Z"}
```

Fixture inbox data sau T18c:

```json
{"requestId":"30000000-0000-0000-0000-000000000001","goalType":"EXAM","subjectId":1,"educationLevelId":4,"desiredSlots":["MONDAY 18:00-19:00"],"message":null,"senderRole":"PARENT","status":"PENDING","expiresAt":"2026-10-18T03:00:00Z","deadline":"2027-01-01"}
```

Fixture missing accept sau T04 (field message minh họa):

```json
{"code":"VALIDATION_FAILED","message":"Dữ liệu đầu vào không hợp lệ.","details":[{"field":"accept","message":"must not be null","rejectedValue":null}],"timestamp":"2026-10-11T03:00:00Z"}
```

Fixture invitation data sau T18c:

```json
{"invitationId":"50000000-0000-0000-0000-000000000001","inviterUserId":"60000000-0000-0000-0000-000000000001","invitedUserId":"70000000-0000-0000-0000-000000000001","status":"DECLINED","createdAt":"2026-10-01T03:00:00Z","expiresAt":"2026-10-15T03:00:00Z","reinviteAfter":"2026-10-11T03:00:00Z","inviterName":"Phụ huynh mẫu","inviterRole":"PARENT"}
```

Fixture link data dùng cho respond accept và list links:

```json
{"linkId":"80000000-0000-0000-0000-000000000001","parentUserId":"60000000-0000-0000-0000-000000000001","studentId":"40000000-0000-0000-0000-000000000001","status":"CONFIRMED","confirmedAt":"2026-10-11T03:00:00Z"}
```

Fixture class data khi Tutor accept:

```json
{"classId":"90000000-0000-0000-0000-000000000001","studentId":"40000000-0000-0000-0000-000000000001","tutorId":"20000000-0000-0000-0000-000000000001","goalId":"10000000-0000-0000-0000-000000000001"}
```

List routes bọc các data item tương ứng trong array; decline/reject/withdraw/revoke/cancel dùng success envelope data null/message Thành công. T04 chụp shape baseline cho cả 10 routes trước thêm fields, T18 kiểm tra additions/privacy, T19 page totals/authorization, T21 filter/error mappings. UUID fixtures là dữ liệu test tổng hợp, không dùng account người thật.

Boundary matrix bắt buộc: accept omitted/null/false/true; message500/501; reason300/301; Admin reason null/blank; TTL ngay trước/đúng/sau14ngày và7ngày; quota10/11/reset1h; pending5/6; slot invalid weekday/minute/reversed/overnight/duplicate/overlap và adjacent hợp lệ; read legacy vẫn nguyên. CSV minh họa cũ trong testing spec vẫn có mốc47/49giờ và trạng thái REVOKED cho invitation; **không dùng làm oracle**. T20 sửa ma trận nguồn theo TTL14ngày và statuses đã chốt, không tạo error enums giả từ CSV cũ.

## 9. Giới hạn bàn giao Phase 0

Phase 0 không thay controller/service/entity theo contract mới, không triển khai event/job/strict writer/page routes/CSRF fix. Migration integration chỉ giải duplicate version theo baseline report. Mỗi task sau phải có focused tests + affected verify, commit và push riêng. Coverage strict baseline còn fail được theo dõi tách khỏi test success; không dùng upstream default false để gọi coverage đạt.
