# Discovery API Contract

> Hợp đồng hiện thực sau T14, bàn giao T15 ngày 2026-10-10 trên `feat/discovery`.
> Dành cho frontend và module backend tích hợp. [Flow/code](discovery-module-flow.md),
> [performance evidence](discovery-performance-report.md), [mục lục](README.md).

## Hai loại API

HTTP có base path `/api/v1/discovery`, trả web DTO trong `ApiResponse`.
`discovery.api` là public Java contract trong cùng JVM, không phải một route HTTP
`/api/` mới. Không import controller, application service hoặc entity từ module khác.

| HTTP | Quyền | Payload thành công (`data`) |
| --- | --- | --- |
| `GET /api/v1/discovery/tutors` | Public | `Page<TutorDiscoveryCardResponse>` |
| `GET /api/v1/discovery/tutors/{tutorId}` | Public | `TutorDetailResponse` |
| `POST /api/v1/discovery/match` | Session, STUDENT/PARENT và quyền trên goal | `List<TutorMatchResponse>` |

Thành công HTTP 200 có `{ success: true, data, message, timestamp }`. Lỗi theo
`ApiError` có `{ code, message, details, timestamp }`, không nằm trong `data` và
không có `success` field. Timestamp là ISO-8601 UTC. Các field nullable từ hồ sơ
(headline/bio/area/rating...) có thể là null; client không nên giả định có giá trị.

Nguồn: [DiscoveryController](../backend/modules/discovery/src/main/java/vn/edufit/discovery/web/DiscoveryController.java),
[ApiResponse](../backend/platform/shared/src/main/java/vn/edufit/shared/response/ApiResponse.java),
[ApiError](../backend/platform/shared/src/main/java/vn/edufit/shared/response/ApiError.java).

## Search

`GET /api/v1/discovery/tutors`. Mọi filter đều tùy chọn và kết hợp bằng AND;
keyword tìm trên nhiều field bằng OR. Chỉ trả tutor VERIFIED.

| Query | Kiểu / mặc định | Hành vi |
| --- | --- | --- |
| `subjectId`, `levelId` | Integer >= 1 | Nếu gửi cả hai, phải cùng một dòng tutor-subject; level riêng tìm bất kỳ môn có cấp đó. |
| `area` | String | Trim, bỏ qua blank; contains không phân biệt hoa/thường. Không đồng nhất với matching yêu cầu cùng khu vực cho offline. |
| `mode` | ONLINE / OFFLINE / BOTH, không phân biệt hoa/thường | ONLINE nhận ONLINE/BOTH; OFFLINE nhận OFFLINE/BOTH; BOTH chỉ nhận tutor BOTH. Bỏ trống không lọc. |
| `minPrice`, `maxPrice` | Long >= 0 | Hai biên inclusive; nếu có cả hai thì min <= max. |
| `minRating` | Decimal 0..5 | Inclusive; tutor rating null không đạt filter này. |
| `keyword` | String | Trim, bỏ qua blank; contains case-insensitive trong displayName/headline/bio/subject name. Dùng SQL LIKE, không phải full-text/ranked relevance; `%` và `_` chưa escape như literal. |
| `dayOfWeek` | Integer 1..7 (1 = Monday) | Gửi cùng availableFrom/availableTo. |
| `availableFrom`, `availableTo` | LocalTime, ví dụ `18:00` | To > From trong cùng ngày. Có ít nhất một slot cùng ngày giao với khoảng yêu cầu; không đòi phủ kín, chạm biên không tính giao. |
| `page` | int, 0 | Zero-based; giá trị âm chuẩn hóa thành 0. |
| `size` | int, 10 | Clamp vào 1..100, không tự trả 400 khi ngoài khoảng. |
| `sortBy` | `rating` | `rating`/`relevance` -> ratingAvg; `price` -> pricePerSession; `experience` -> experienceYears. Không nhận property tùy ý. |
| `sortDirection` | `desc` | `asc` hoặc `desc`. Rating/relevance luôn có secondary reviewCount DESC. |

`relevance` hiện là alias rating, không phải matchScore. Không có unique tutorId
tie-break cho search: không bảo đảm thứ tự giữa các dòng cùng sort key, nhất là
phân trang khi dữ liệu đang thay đổi. Candidate matching có ordering riêng.

```http
GET /api/v1/discovery/tutors?subjectId=1&levelId=3&mode=ONLINE&minRating=4&dayOfWeek=1&availableFrom=18:00&availableTo=20:00&page=0&size=10&sortBy=price&sortDirection=asc
```

`data.content` là mảng cards; Page metadata gồm `number`, `size`, `totalElements`,
`totalPages`, `first`, `last`, `numberOfElements`, `empty` và metadata Spring Data.
Không có kết quả vẫn 200 với content rỗng. Không tự coi cấu trúc Page framework là
cursor contract ổn định giữa mọi phiên bản Spring.

### Card và nested DTO

| Card field | Kiểu / ý nghĩa |
| --- | --- |
| `tutorId` | UUID string |
| `displayName`, `headline` | String |
| `shortBio` | String; bio dài hơn 180 ký tự cắt 177 rồi thêm `...` |
| `teachingMode`, `area` | String |
| `pricePerSession` | Long, học phí mỗi buổi |
| `ratingAvg`, `reviewCount` | Decimal và Integer |
| `verified` | Boolean; true với tutor VERIFIED |
| `subjects` | Mảng `{ subjectId, subjectName, educationLevelId, educationLevelName }` |

Card không có email/điện thoại, availabilitySlots hay experienceYears. Search map
summary rồi hydrate subjects theo batch; nếu detail biến mất giữa hai lần đọc,
summary vẫn được trả với subjects rỗng. Không phải mọi card rỗng subjects đều có
nghĩa gia sư chưa khai báo môn.

## Tutor Detail

`GET /api/v1/discovery/tutors/{tutorId}` nhận UUID. Chỉ tutor VERIFIED được xem;
không tồn tại hoặc chưa VERIFIED đều 404 `RESOURCE_NOT_FOUND`.

`data` gồm `tutorId`, `displayName`, `headline`, **`bio` đầy đủ**, `teachingMethod`,
`teachingMode`, `area`, `pricePerSession`, `experienceYears`, `ratingAvg`,
`reviewCount`, `verified`, `subjects`, **`availabilitySlots`**. Không dùng tên
`slots` từ bản thiết kế cũ. Mỗi availability slot là
`{ dayOfWeek, startTime, endTime }`, giờ địa phương theo tuần, không phải Instant
UTC hoặc lịch booking của Scheduling. Nested subject giống card.

Nguồn mapping: [TutorDetailResponse](../backend/modules/discovery/src/main/java/vn/edufit/discovery/web/response/TutorDetailResponse.java).

## Matching HTTP

`POST /api/v1/discovery/match`, Content-Type `application/json`.

```json
{ "goalId": "00000000-0000-0000-0000-000000000001", "topN": 5 }
```

`goalId` bắt buộc; `topN` null/bỏ qua -> 5, giá trị gửi lên phải 1..10. Không
truyền actor/userId/role/criteria trong body; goal data được đọc từ Profile.

Theo [SecurityConfig](../backend/app/src/main/java/vn/edufit/config/SecurityConfig.java),
POST cần session cookie `EDUFIT_SESSION` và CSRF hợp lệ. Cookie repository dùng
`XSRF-TOKEN`/header `X-XSRF-TOKEN`; token acquisition phải theo cấu hình auth thực
tế, không giả định Discovery có endpoint cấp token. Chưa có bằng chứng browser/
security-filter end-to-end trong bộ Discovery standalone MockMvc tests.

Service kiểm tra actor trước, rồi input và goal. STUDENT phải có userId bằng
`goal.studentUserId`; PARENT phải được
`ConnectionFacade.hasConfirmedParentLink(actor.userId, goal.studentId)` xác nhận.
Invitation PENDING/DECLINED, link REVOKED hoặc không có link đều không cấp quyền.
Sau authorization, goal phải ACTIVE, có educationLevelId, budgetMax > 0 và ít
nhất một availability slot. Goal không tồn tại trả 404, không có quyền trả 403.

Response field của mỗi kết quả:

| Field | Nội dung |
| --- | --- |
| `tutorId` | UUID string |
| `matchScore` | Decimal 0..100, hai chữ số HALF_UP; không phải tỷ lệ 0..1 |
| `scoreBreakdown` | `{ scheduleFit, ratingFit, budgetFit, overlappingSlots, subjectFit, levelFit }` |
| `explanation` | String từ AI hoặc rule-based |
| `aiGenerated` | Boolean; chỉ Top 1 có thể true |
| `tutorInfo` | Card như search, không phải detail đầy đủ |

Năm điểm breakdown đều 0..100; overlappingSlots là số slot yêu cầu ban đầu có
ít nhất một giao, không phải điểm hoặc số phút. Các field cũ/routes giữ nguyên;
`subjectFit`, `levelFit` là bổ sung. Tổng có thể khác phép cộng các thành phần đã
round do implementation tổng hợp giá trị chưa round trước. Ví dụ payload sau
ứng với fixture **66.95** trong [flow/scoring](discovery-module-flow.md):

```json
{
  "success": true,
  "data": [{
    "tutorId": "00000000-0000-0000-0000-000000000002",
    "matchScore": 66.95,
    "scoreBreakdown": {
      "scheduleFit": 50.00, "ratingFit": 63.00, "budgetFit": 50.00,
      "overlappingSlots": 1, "subjectFit": 100.00, "levelFit": 50.00
    },
    "explanation": "Phù hợp môn 100.00%, cấp học 50.00%, lịch 50.00%, đánh giá 63.00% và ngân sách 50.00%; có 1 khung giờ giao nhau.",
    "aiGenerated": false,
    "tutorInfo": {
      "tutorId": "00000000-0000-0000-0000-000000000002",
      "displayName": "Gia sư mẫu", "headline": null, "shortBio": null,
      "teachingMode": "ONLINE", "area": "Hanoi", "pricePerSession": 390000,
      "ratingAvg": 4.5, "reviewCount": 2, "verified": true,
      "subjects": [{ "subjectId": 1, "subjectName": "Math", "educationLevelId": 80, "educationLevelName": "Primary" }]
    }
  }],
  "message": "Ghép đôi gia sư thành công",
  "timestamp": "2026-10-10T10:00:00Z"
}
```

AI không tính điểm/xếp hạng. Top 1 kiểm tra quota MATCH_EXPLANATION 10 lần/rolling
24h theo actor; hết quota, lỗi usage/gateway hoặc timeout dùng fallback, vẫn 200
**nếu phần DB/matching còn lại thành công**. Không có eligible candidate trả
`data: []`, không gọi AI nhưng vẫn ghi log/event. Ít hơn topN tutor thì trả ít hơn,
không padding. Top N chỉ đúng trong candidate set bounded mặc định 200, không
cam kết global best. Xem [báo cáo cap](discovery-performance-report.md).

## Lỗi đã định nghĩa trong code

| HTTP / code | Tình huống |
| --- | --- |
| 400 `VALIDATION_FAILED` | Bean Validation: ID filter < 1, giá/rating sai, time filter thiếu, topN ngoài 1..10, thiếu goalId |
| 400 `INVALID_OPERATION` | sortBy/sortDirection không hợp lệ; goal inactive/thiếu cấp/thiếu budget/thiếu lịch; facade topN hoặc goalId invalid |
| 401 `UNAUTHORIZED` | Security entry point cho request chưa xác thực khi filter đi đến bước auth |
| 403 `FORBIDDEN` | Actor sai role, Student không sở hữu hoặc Parent chưa confirmed; CSRF bị từ chối cũng có thể 403 trước controller |
| 404 `RESOURCE_NOT_FOUND` | Tutor thiếu/chưa VERIFIED hoặc goal không tồn tại |
| 500 | Lỗi hệ thống không được xử lý, ví dụ persistence failure; không cam kết 200 chỉ vì AI có fallback |

Không dùng mã dự kiến `INVALID_FILTER`, `TUTOR_NOT_FOUND`, `GOAL_NOT_FOUND` từ
bảng thiết kế cũ để tích hợp. Bảng này không cam kết malformed JSON/type-binding
đều được map 400: global handler hiện cần regression riêng cho các lỗi đó.
Nguồn: [GlobalExceptionHandler](../backend/app/src/main/java/vn/edufit/web/advice/GlobalExceptionHandler.java).

## Java Facade và Event

Inject [DiscoveryFacade](../backend/modules/discovery/src/main/java/vn/edufit/discovery/api/DiscoveryFacade.java), gọi
`findTopMatchesForGoal(CurrentUser actor, UUID goalId, int topN)`.
Actor phải từ trusted server context; facade không tự xác thực đối tượng giả mạo.
Không có topN mặc định trong chữ ký Java, caller truyền 5 khi cần default.

Trả `List<TutorMatchSummaryDto>` theo ranking gồm `tutorId`, `displayName`,
`matchScore`, `explanation`, `aiGenerated`. Không có HTTP envelope/card/breakdown.
Facade là **command có side effect**, không phải read-only query: dùng cùng
authorized matching service, AI quota, log và event như HTTP. Không dùng nó để
poll hoặc tạo preview không side effect. Exception được truyền cho caller Java,
không tự đổi sang HTTP status.

[MatchingExecutedEvent](../backend/modules/discovery/src/main/java/vn/edufit/discovery/api/event/MatchingExecutedEvent.java)
gồm `userId` (actor), `studentId`, `goalId`, `resultCount`, `executedAt`; không có
runId hoặc danh sách tutor. Mỗi successful run (kể cả rỗng) save log rồi publish
trong transaction. Consumer side effect phải xử lý AFTER_COMMIT; outer transaction
rollback thì không xử lý thành công. Event in-process, không durable/retry/outbox.
Chi tiết boundary và sequence nằm trong [flow](discovery-module-flow.md).

Profile caller hiện dùng `findVerifiedCandidatesBySubject(TutorCandidateCriteria)`;
API `findVerifiedTutorsForMatching()` đã bị xóa ở T14. Không copy ví dụ chữ ký
`(subjectId, levelId)` của proposal cũ: level là điểm mềm, không lọc retrieval.
