# Discovery backend: cấu trúc và luồng xử lý

> Mô tả code trên nhánh `feat/discovery`, ngày 2026-10-08. Đọc cùng [SADS](software-architecture-design-specification.md) để biết API dự kiến và [hướng dẫn cấu trúc module](backend-module-structure-guide.md) để biết quy tắc phân tầng.

## Phạm vi module

Discovery phục vụ tìm kiếm gia sư đã xác minh, xem hồ sơ công khai và xếp hạng gia sư theo một learning goal. Hồ sơ gia sư và goal thuộc Profile; Discovery chỉ đọc chúng qua `profile.api.ProfileFacade`. Discovery sở hữu `matching_run_log` để ghi mỗi lượt match, không lưu nội dung tiêu chí. AI chỉ viết lời giải thích cho kết quả đầu tiên, không quyết định điểm hoặc thứ hạng.

![Kiến trúc backend Discovery và các module liên quan](images/discovery-architecture.svg)

| Vị trí | Trách nhiệm |
| --- | --- |
| `web/DiscoveryController` | Nhận HTTP request, tạo `Pageable`, lấy `CurrentUser`, trả `ApiResponse`. |
| `web/request`, `web/response` | Kiểm tra đầu vào và định nghĩa DTO public; không trả JPA entity. |
| `application/service/DiscoveryQueryService` | Tìm kiếm và xem chi tiết qua Profile facade. |
| `application/service/TutorMatchingService` | Kiểm tra quyền/goal, chuyển DTO thành candidate, xếp hạng, ghi log. |
| `application/service/MatchExplanationService` | Kiểm tra quota AI, gọi gateway hoặc dùng câu giải thích theo quy tắc. |
| `domain/model`, `domain/service/MatchScorer` | Thuật toán Java thuần, không phụ thuộc Spring/JPA. |
| `infra/persistence/MatchingRunLog*` | Entity và repository của lượt match. |

Điểm vào code: [controller](../backend/modules/discovery/src/main/java/vn/edufit/discovery/web/DiscoveryController.java), [query service](../backend/modules/discovery/src/main/java/vn/edufit/discovery/application/service/DiscoveryQueryService.java), [matching service](../backend/modules/discovery/src/main/java/vn/edufit/discovery/application/service/TutorMatchingService.java), [scorer](../backend/modules/discovery/src/main/java/vn/edufit/discovery/domain/service/MatchScorer.java), [ProfileFacade](../backend/modules/profile/src/main/java/vn/edufit/profile/api/ProfileFacade.java).

## API và quyền truy cập

| Endpoint | Quyền | Input chính | Kết quả |
| --- | --- | --- | --- |
| `GET /api/v1/discovery/tutors` | Public | `subjectId`, `levelId`, `area`, `mode`, `minPrice`, `maxPrice`, `minRating`; thêm `keyword`, bộ lọc giờ, phân trang/sắp xếp | `ApiResponse<Page<TutorDiscoveryCardResponse>>` |
| `GET /api/v1/discovery/tutors/{tutorId}` | Public | UUID gia sư | `ApiResponse<TutorDetailResponse>`; 404 nếu không VERIFIED/không tồn tại |
| `POST /api/v1/discovery/match` | Có session, role STUDENT/PARENT | `{ "goalId": "UUID", "topN": 5 }`; `topN` mặc định 5, tối đa 10 | `ApiResponse<List<TutorMatchResponse>>` |

`SecurityConfig` mở public hai route GET; POST cần session và CSRF token. Service kiểm tra role rồi kiểm tra người sở hữu goal. **Hiện chỉ STUDENT sở hữu goal match được**: code so sánh `goal.studentUserId` với `CurrentUser.userId`. Role PARENT có trong contract nhưng chưa thể match thay con vì module Connection chưa có API xác minh liên kết phụ huynh–học sinh. Không được bỏ kiểm tra owner để mở role này.

Search giới hạn `page >= 0`, `size` trong 1..100 (mặc định 10). `sortBy` nhận `rating`, `relevance`, `price`, `experience`; `relevance` hiện được ánh xạ sang `ratingAvg`, không phải điểm matching. Mặc định rating giảm dần, sau đó `reviewCount` giảm dần. Lọc giờ yêu cầu gửi đủ `dayOfWeek` (1..7), `availableFrom`, `availableTo`, với giờ kết thúc sau giờ bắt đầu.

## Luồng tìm kiếm và chi tiết

![Luồng tìm kiếm gia sư đã xác minh và bổ sung chi tiết theo trang](images/discovery-search-flow.svg)

Bộ lọc chạy trong `profile.infra.persistence.repository.TutorProfileRepository.searchVerifiedTutors`. Sau khi Profile trả một trang, Discovery lấy chi tiết theo lô ID để bổ sung `subjects`, tránh truy vấn riêng cho từng card. Nếu profile biến mất giữa hai lần đọc, card vẫn được tạo từ summary và `subjects` rỗng. Chi tiết một gia sư đi qua `ProfileFacade.findVerifiedTutorDetail`, trả bio, phương pháp dạy, môn/cấp học và lịch rảnh. Hồ sơ chưa VERIFIED không xuất hiện trong search/detail.

## Luồng matching

![Luồng matching từ kiểm tra quyền đến xếp hạng, giải thích và ghi log](images/discovery-matching-flow.svg)

Goal phải `ACTIVE`, có cấp học, `budgetMax > 0` và ít nhất một slot. Profile trả toàn bộ tutor `VERIFIED`; Discovery lọc và xếp hạng trong bộ nhớ, sau đó mới áp dụng `topN`. Chưa có prefilter hay giới hạn ứng viên ở DB, nên cần đo với dữ liệu lớn trước khi khẳng định đạt NFR-14 (<500ms p95).

`MatchScorer` loại tutor nếu không khớp cặp `(subjectId, educationLevelId)`, không hợp mode/khu vực hoặc không có slot giao nhau. ONLINE nhận tutor ONLINE/BOTH. OFFLINE nhận OFFLINE/BOTH với khu vực trùng. BOTH nhận tutor ONLINE hoặc tutor có khu vực trùng. Hai slot giao nhau khi cùng ngày và `startA < endB && endA > startB`; chỉ chạm ranh giới giờ không tính là giao.

Với tutor vượt qua điều kiện loại trừ, điểm được tính:

```text
scheduleFit = số slot yêu cầu giao với ít nhất 1 slot tutor / tổng slot yêu cầu * 100
ratingFit   = clamp(ratingAvg, 0, 5) * 20; nếu chưa có review thì 50
budgetFit   = 100 nếu giá <= budgetMax; giảm tuyến tính về 0 tại 120% budgetMax
matchScore  = 0.4 * scheduleFit + 0.3 * ratingFit + 0.3 * budgetFit
```

Điểm tổng làm tròn 2 chữ số. Khi bằng điểm: ưu tiên nhiều slot giao hơn, nhiều review hơn, ngày đăng ký sớm hơn, rồi `tutorId`. `budgetMin` có trong goal nhưng chưa dùng trong công thức. Môn/cấp học và mode/khu vực là điều kiện loại trừ, không cộng điểm. Đây là chính sách **đang chạy**; công thức trong sprint plan và test specification chưa được đồng bộ với code.

Chỉ kết quả đầu tiên gọi AI (`MATCH_EXPLANATION`), tối đa 10 yêu cầu trong 24 giờ gần nhất cho mỗi user theo `AiUsageQuery`. Các kết quả còn lại dùng giải thích theo điểm. AI lỗi, timeout hoặc hết quota sẽ trả fallback với `aiGenerated=false`. `matching_run_log` ghi user, student, goal, số kết quả và thời điểm; không ghi prompt hay criteria.

## Việc còn phụ thuộc module khác

- Connection cần public facade xác minh parent-student link đã xác nhận. Sau đó mới mở `TutorMatchingService.validateAccess` cho PARENT truy cập goal của con. Hiện Connection chỉ có `pom.xml`.
- SADS dự kiến `DiscoveryFacade`, `MatchingExecutedEvent`, dữ liệu review/scheduling cho UC2.5; các contract/consumer này chưa có trong code. Dependency trong `pom.xml` chưa đồng nghĩa với triển khai.
- Chưa có integration/performance test với PostgreSQL cho truy vấn nhiều bộ lọc và việc tải toàn bộ tutor khi match.

## Đọc code và chạy test

Bắt đầu từ `DiscoveryController`; theo `DiscoveryQueryService` cho GET hoặc `TutorMatchingService` cho POST; sau đó đọc `ProfileFacade` và `MatchScorer`. Unit tests ở `backend/modules/discovery/src/test/java/vn/edufit/discovery/`: `DiscoveryQueryServiceTest`, `TutorMatchingServiceTest`, `MatchScorerTest`, `MatchExplanationServiceTest`. Từ thư mục `backend`, dùng JDK 21 chạy `mvn -pl modules/discovery -am test`. Khi Connection/Profile thêm API cho PARENT, cập nhật tài liệu và test quyền cùng lúc.
