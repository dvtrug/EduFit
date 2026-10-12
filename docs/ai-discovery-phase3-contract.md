# AI + Discovery — Phase 3 goal contract

Ngày 2026-10-12, nhánh `feat/discovery-ai`. B08–B10 trong [plan](../tasks/ai-discovery/plan.md). Tài liệu này chốt phần goal/readiness của B01; session/CSRF/CORS đầy đủ vẫn thuộc Phase 4. Không thay đổi frontend, scorer hoặc quyền ghi của Parent.

B08–B10 hoàn tất: [checklist/evidence](../tasks/ai-discovery/todo.md). 62 goal HTTP/service/PostgreSQL cases pass; Parent confirmed đọc/match được, Student thu hồi link bằng ConnectionService thật rồi Parent bị chặn ở request tiếp theo. Linked Parent vẫn bị chặn toàn bộ API ghi Student. Parent list có 4 business queries (link, student, goals, batch slots) với 3 goals; phép đo không bao gồm security/session filter hoặc provider latency.

## Routes và payload

- Student: POST/GET `/api/v1/profiles/students/me/goals` (201 create, 200 list).
- Student: PUT `/api/v1/profiles/students/me/goals/{goalId}` (full replacement các field bên dưới, giữ slots và owner).
- Student: POST `.../{goalId}/archive` (200, idempotent, giữ bản ghi và lịch sử; không có hard delete/unarchive).
- Student: PUT `.../{goalId}/availability` với `{"slots":[{"dayOfWeek":1,"startTime":"18:00","endTime":"20:00"}]}`.
- Parent: GET `/api/v1/discovery/students/{studentId}/goals` sau khi kiểm tra confirmed link bằng ConnectionFacade. Parent không được gọi API ghi Student. Không cache quyền; revoke có hiệu lực ở request đọc/match tiếp theo.

Create/update nhận:

```json
{"subjectId":1,"currentLevel":1,"desiredLevel":3,"goalType":"IMPROVE_GRADES","deadline":"2027-01-01","mode":"ONLINE","area":null,"budgetMin":0,"budgetMax":200000}
```

Actor lấy từ CurrentUser/session, studentId tra từ profile của actor. Không có actor, owner, status hoặc readiness trong writable DTO. Subject phải tồn tại và active. Cấp học lấy từ Student profile, không lấy từ level năng lực của goal. GoalType: CONSOLIDATE, IMPROVE_GRADES, ADVANCED_STUDY, EXAM_PREPARATION, OTHER. Mode: ONLINE, OFFLINE, BOTH; OFFLINE/BOTH cần area không trắng, tối đa 100 ký tự, trim trước lưu. BudgetMin nullable hoặc >=0; budgetMax >0; min <= max. Deadline phải sau ngày hiện tại của backend. Current/desired là smallint có thứ tự, desired > current; SRS/schema không chốt thêm thang 1–5/1–10 nên không tự thêm giới hạn đó.

Response dùng ApiResponse hiện hữu. `data` là goal hoặc danh sách goal sắp createdAt rồi goalId; gồm goalId, studentId, subjectId, educationLevelId, currentLevel, desiredLevel, goalType, deadline, mode, area, budgetMin, budgetMax, status, createdAt, updatedAt, availabilitySlots, readyForMatching, readinessReasons. Không trả email/contact/account ID của học sinh qua goal DTO. List bao gồm ACTIVE và ARCHIVED để caller giữ lịch sử, không báo archived là sẵn sàng.

Readiness là các điều kiện đầu vào matching hiện hữu: GOAL_ARCHIVED, EDUCATION_LEVEL_REQUIRED, BUDGET_MAX_REQUIRED, AREA_REQUIRED, AVAILABILITY_REQUIRED. Không phải profileComplete, không đảm bảo có tutor/kết quả hoặc Connection eligibility. Goal chưa lịch vẫn ACTIVE và hợp lệ, có AVAILABILITY_REQUIRED. Không tạo trạng thái DRAFT mới; deadline được validate khi ghi, không tự đổi eligibility deadline của matching lịch sử trong phase này.

## Slots và transaction

Replace toàn bộ slots trong một transaction, khóa goal cùng cơ chế với update/archive để các mutation đồng thời không trộn dữ liệu. Day 1–7; start < end và cùng ngày; dùng độ chính xác phút phù hợp scorer (seconds/nanos phải 0). Reject null item, duplicate và overlap cùng ngày; hai slot chạm biên được phép. Danh sách rỗng hợp lệ để clear, sau đó readiness false. Tối đa 100 slots/request để giới hạn payload. Validate trước delete; lỗi insert/flush/commit rollback cả delete lẫn insert. Không sửa cách tính overlap của scorer.

## Quyền và errors

Student chỉ list/ghi mục tiêu của mình. Thiếu danh tính hoặc role khác: 403 theo business authorization hiện hữu. Goal không tồn tại hoặc không thuộc Student: 404, không lộ owner. Student chưa có profile: 404, cần tạo/cập nhật profile trước. Archived goal không update/replace slots (400), không match hoặc gửi Connection request mới (400 theo rule hiện hữu); archive lại trả 200. Invalid catalogs/business fields: 400, structural Bean Validation: 400; ApiError schema hiện hữu. Parent route: chỉ PARENT có confirmed link, mọi state pending/declined/revoked/unlinked và Student/Tutor đều 403 trước khi đọc goals.

Bằng chứng Phase 3 dùng MockMvc HTTP mapping/validation + Spring transaction + PostgreSQL thật; CurrentUser được kiểm soát trong fixture, không dùng đó để nhận full security filter/session/CSRF đã pass. Full-filter và malformed JSON/enum/type matrix tổng thể thuộc B11/B12.

B09 đã kiểm chứng null JSON body trả nhầm 500 ở handler cũ; handler HttpMessageNotReadableException nay trả 400 VALIDATION_FAILED với thông báo cố định, không log/trả raw parser message. Đây là phần tối thiểu cần cho payload goal/slots mới; query/path type errors và toàn bộ matrix B12 vẫn pending.

Nguồn: [BR-12/schema review](edufit-schema-review-changes.md), [SRS](SRS%20Edufit.docx), [Spring Data JPA locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html), [Spring MVC validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html).
