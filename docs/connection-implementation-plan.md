# Plan hoàn thiện Connection backend

**Nhánh:** `feat/connection`, tạo từ `origin/main` tại `f077e55` (2026-10-08). **Phạm vi:** backend UC3.1–UC3.5 và các public contract cần cho Discovery/Scheduling; frontend làm theo roadmap riêng.

## Nguồn và hiện trạng

- [SRS](SRS%20Edufit.docx), Group 3 và BR-26–38: nguồn cho hành vi nghiệp vụ, quyền và trường hợp biên.
- [SADS](software-architecture-design-specification.md), mục 4.3–4.4: REST API và luồng Parent → Student → Tutor → Class.
- [Sprint plan](master_sprint_plan_and_ai_learning_framework.md), Sprint 3; [test specification](in-development-testing-specification.md), mục 7.2.
- [ADR-002](ADR-002-multi-module-architecture.md), [module guide](backend-module-structure-guide.md), [schema](edufit-schema.dbml), Flyway `V1.04__create_connection_and_class_tables.sql`.
- `modules/connection` hiện chỉ có `pom.xml`. Bốn bảng `link_invitation`, `parent_student_link`, `connection_request`, `class` đã có migration. IAM facade chỉ tìm tài khoản theo ID; Profile facade chỉ có vài truy vấn student/tutor, chưa có DTO goal phù hợp để Connection xác thực BR-33. `notification` và `platform/audit` cũng mới có `pom.xml`.

## Thứ tự thực hiện

| Bước | Phần việc | Điều kiện hoàn thành |
| --- | --- | --- |
| 1 | Chốt HTTP contract và ranh giới module. Thiết kế `ConnectionFacade` cho kiểm tra link CONFIRMED và class ACTIVE; thêm truy vấn email an toàn vào `IamFacade`, goal/tutor-subject vào `ProfileFacade` qua DTO. | Không import entity/repository của module khác; request/response và mã lỗi rõ ràng; route mới thống nhất với SADS/SRS. |
| 2 | Mapping JPA, repository và migration bổ sung. Dùng bốn bảng sẵn có, thêm `@Version`/locking nơi đổi trạng thái, giữ unique index cho PENDING request/invitation và ACTIVE class. | Có thể mời lại sau link REVOKED mà vẫn giữ lịch sử cũ; trùng thao tác đồng thời chỉ tạo một trạng thái/link/class hợp lệ. |
| 3 | UC3.1–3.2: Student hoặc Parent mời tài khoản đúng role qua email, xem sent/received, rút lời mời PENDING; người được mời chấp nhận/từ chối. | Lời mời không cấp quyền trước accept; chỉ invited user phản hồi; accept + link cùng transaction; hết hạn/cooldown, chống trùng và chống lộ sự tồn tại của email. |
| 4 | UC3.3: liệt kê link theo người dùng, Student/Admin thu hồi, Admin bắt buộc lý do; public facade kiểm tra quyền Parent theo link CONFIRMED. | Parent bị chặn ngay sau revoke; dữ liệu request/class cũ vẫn thuộc Student; Parent không tự revoke được. |
| 5 | UC3.4: Student hoặc Parent có link hợp lệ gửi, xem và hủy request PENDING cho tutor VERIFIED khớp subject+level của goal. | Goal thuộc Student; giới hạn 1 PENDING theo cặp, 5 PENDING mỗi Student, không có class ACTIVE; message ≤500, slot hợp lệ; Tutor chỉ thấy dữ liệu BR-37. |
| 6 | UC3.5: Tutor xem inbox, accept/reject request còn hiệu lực; accept tạo đúng một `class` ACTIVE trong cùng transaction. | Chỉ tutor đích phản hồi; request chuyển trạng thái đúng; không tạo hai class khi bấm đồng thời; class truy vấn được qua `ConnectionFacade` cho Scheduling. |
| 7 | Hết hạn, event, kiểm thử và tài liệu flow. Xử lý invitation/request quá hạn cả khi đọc/ghi; phát event sau các chuyển trạng thái; bổ sung docs code flow và API. | Unit test ma trận quyền/trạng thái, integration test PostgreSQL cho unique index/race/rollback, kiểm tra API với role thực và `mvn -pl modules/connection -am test`. |

## API dự kiến

Giữ prefix `/api/v1/connections` của SADS:

| Nhóm | Endpoint |
| --- | --- |
| Lời mời | `POST /parent-student/invite`, `GET /parent-student/invitations`, `POST /parent-student/respond`, `POST /parent-student/invitations/{id}/withdraw` |
| Liên kết | `GET /parent-student/links`, `DELETE /parent-student/links/{id}` |
| Yêu cầu gia sư | `POST /requests`, `GET /requests` (scope theo role), `POST /requests/{id}/cancel`, `POST /requests/{id}/respond` |

Các DTO chỉ dùng ID và trường cần thiết. Các thao tác thay trạng thái kiểm tra `CurrentUser` trong service, không tin `studentId`, `parentId` hay `tutorId` do client tự khai. Lỗi trả qua `ApiResponse`/exception handler chung. Response inbox của Tutor giới hạn đúng dữ liệu BR-37.

## Quyết định cần review trước khi code

1. **Thời hạn lời mời:** SRS BR-29, SADS và schema ghi 14 ngày; test specification ghi 48 giờ. Đề xuất dùng **14 ngày**, cooldown sau decline **7 ngày**, request **7 ngày** theo SRS/schema; cập nhật test specification khi triển khai.
2. **Ai được mời:** SRS BR-27 cho cả Student và Parent chủ động mời, SADS chỉ mô tả Parent mời Student và field `studentEmail`. Đề xuất hỗ trợ **hai chiều** ở cùng route `/parent-student/invite` với field `targetEmail`, role người gửi quyết định role người nhận; cập nhật contract.
3. **Cấu trúc code:** `backend-module-structure-guide.md` liệt kê Connection trong mẫu có `domain/`, nhưng ADR-002 Decision 3a chốt Connection dùng service + JPA, không có `domain/` riêng. Đề xuất theo **ADR-002**; vẫn tách kiểm tra chuyển trạng thái đủ nhỏ để unit test.
4. **Relink sau revoke:** `parent_student_link` đang `UNIQUE(parent_user_id, student_id)` nên không thể lưu link mới cho cùng cặp theo BR-31. Đề xuất migration mới thay bằng unique **chỉ cho status CONFIRMED**, giữ hàng REVOKED làm lịch sử; không sửa migration V1.04 đã chạy.
5. **Notification và audit:** SRS yêu cầu thông báo (FR-12) và audit khi Admin revoke (FR-15), nhưng hai module này chưa có implementation. Connection sẽ phát public events; để coi toàn bộ FR-12/15 hoàn tất cần triển khai listener và audit port tương ứng trong các module sở hữu.
6. **Tích hợp Discovery:** nhánh này bắt đầu từ `main`, nơi chưa có thay đổi Discovery đang để dở ở checkout `feat/discovery`. Sau khi Discovery được tích hợp, dùng `ConnectionFacade` để cho Parent match goal của con theo link CONFIRMED; việc này cần test liên module và không mở quyền chỉ bằng role PARENT.

## Kiểm chứng cuối

- Test các bảng quyết định: role, owner, invitation PENDING/ACCEPTED/DECLINED/WITHDRAWN/EXPIRED, link CONFIRMED/REVOKED, request PENDING/ACCEPTED/REJECTED/CANCELLED/EXPIRED, class ACTIVE.
- Test đồng thời trên PostgreSQL thật: double accept, double invite/request, accept sau expiry/revoke, accept tạo class rollback khi unique constraint chặn.
- Kiểm tra bảo mật: email enumeration, dữ liệu Tutor nhìn thấy trước accept, Parent mất quyền ngay khi link bị revoke, Admin phải nhập reason.
- Chạy test module và reactor, kiểm tra migration từ DB sạch, rồi cập nhật tài liệu flow/API theo code thực tế.
