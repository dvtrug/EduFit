# EduFit — Danh sách chỉnh sửa Database Schema (SRS §3.1)

> **Nguồn đối chiếu (ưu tiên cao → thấp):** Use Case Model v2 + Use Case Descriptions Group 1–6 → ADR-001/002 → README / sprint plan (các tài liệu cũ hơn, chỉ dùng khi không mâu thuẫn).
> **Phạm vi bản này:** chỉ sửa những gì mâu thuẫn với Use Case đã chốt. Các điểm còn mở được ghi ở mục 7 và để mở rộng/chỉnh sửa sau.
> **File kèm theo:** `edufit-schema.dbml` (dán vào dbdiagram.io để xem sơ đồ).

---

## 1. Quyết định của team áp dụng cho bản này

| # | Quyết định | Ghi chú |
|---|---|---|
| D1 | Đồng ý toàn bộ các mục "cần sửa" và "bảng còn thiếu" ở bản rà soát | Xem mục 3, 4 |
| D2 | **Đổi tên `Engagement` → `class`** | Xem mục 2 (cảnh báo nhầm lẫn) |
| D3 | **Dùng tên bảng `account`** (thay cho `User`) | Xem mục 2.2 |
| D4 | Ưu tiên theo Use Case Model/Descriptions đã tạo | Những chỗ còn mơ hồ để mở rộng sau (mục 7) |
| D5 | Bỏ thanh toán hoàn toàn (khớp quyết định đã chốt trên Jira) | Xóa bảng `Payment` |
| D6 | Giữ tên `tutoring_session` thay cho `Session` | Tránh nhầm với HTTP session (`spring_session`) |

---

## 2. Ghi chú để tránh nhầm lẫn (QUAN TRỌNG)

### 2.1 `class` trong DB = `Engagement` trong Use Case / BR / ADR

Các tài liệu đã viết (Use Case Model, Group 3–6, BR-38, BR-39, BR-49…) gọi quan hệ dạy–học là **Engagement**. Nhóm quyết định dùng tên **`class`** ở tầng DB/code. Hai tên này là **một thực thể duy nhất**:

| Ở tài liệu (UC, BR) | Ở DB / code | Ý nghĩa |
|---|---|---|
| Engagement | `class` | Quan hệ dạy–học giữa **đúng 1 Tutor và 1 Student**, gắn với **1 learning goal**, sinh ra khi Tutor chấp nhận connection request (UC3.5, BR-38) |
| Engagement ACTIVE | `class.status = 'ACTIVE'` | |
| `engagement_id` | `class_id` | FK trong `tutoring_session`, `learning_plan` |

**`class` KHÔNG phải lớp học nhóm.** Mỗi class luôn chỉ có 1 tutor + 1 student (Assumption của UC4.1: "no group lessons"). Section 3 của Use Case Model từng ghi *"Class concept is not used"*, ý là "không dùng khái niệm lớp nhiều học sinh"; nay tên này chỉ được tái sử dụng cho Engagement.

**Việc cần làm để đồng bộ tài liệu (không gấp, nhưng nên làm trước khi nộp SRS):**
1. Thêm 1 dòng thuật ngữ vào SRS (Glossary / Appendix): *"Class (DB) ≡ Engagement (use case documents)"*.
2. Sửa câu trong Section 3 của Use Case Model: thay "Class concept is not used" bằng "Class means a 1–1 tutor–student teaching relationship (formerly named Engagement)".
3. Trong code Java: entity `ClassEntity` (tránh `Class` trùng `java.lang.Class`); tên bảng `class` không phải từ khóa dành riêng của PostgreSQL nhưng nên đặt `@Table(name = "class")` rõ ràng.
4. Màn hình 12 "Tutoring Class Screen" và 22 "Tutoring Class Management Screen" trong SRS giờ khớp tên với DB, không cần đổi.

### 2.2 Bảng `User` đổi thành `account`

Bảng lưu tài khoản đăng nhập của cả 4 vai trò được đặt tên **`account`** (khớp ADR-002: module `iam`, migration `create_account_table`). Lý do: `User` là từ khóa của PostgreSQL, giữ tên đó buộc phải quote ở mọi SQL/JPA/native query.

- Chỉ đổi **tên bảng**. Các cột vẫn giữ dạng `user_id` / `*_user_id` (ví dụ `parent_user_id`, `proposed_by`) vì nghĩa vẫn là "người dùng"; khóa chính là `account.user_id`.
- Enum vai trò vẫn là `user_role` (STUDENT, PARENT, TUTOR, ADMIN).
- Trong code Java: entity `Account`, `@Table(name = "account")`.
- Trong tài liệu SRS bảng 01 "User" (mục 3.1.b) nên đổi thành "Account"; "User" ở mức actor (Use Case Model) giữ nguyên, đó là khái niệm khác.

### 2.3 Đã đổi `Session` → `tutoring_session`

Chỉ là đặt tên để khỏi nhầm với HTTP session. Khái niệm trong Use Case vẫn là "Session".

---

## 3. Các bảng đã có — cần sửa

| # | Bảng (cũ → mới) | Vấn đề | Sửa | Căn cứ |
|---|---|---|---|---|
| 1 | `Payment` | Thanh toán nằm ngoài phạm vi | **Xóa bảng** | Use Case Model §3.1 (scope decisions), quyết định Jira |
| 2 | `Notification` | Mô tả có "payment receipts" | Bỏ phần payment; thêm `about_student_id` để lọc theo link của Parent | BR-61, bảng sự kiện Group 6 |
| 3 | `TutoringClass` → `class` | Tên cũ; chưa có quan hệ với request | Đổi tên (xem 2.1); thêm `connection_request_id` UNIQUE, `status`, `started_at`, `ended_at` | UC3.5, BR-38 |
| 4 | `SessionNote` | Có nhiều trường có cấu trúc | Chỉ giữ `note_text` (≤ 5000 ký tự, có thể rỗng), `tutor_id`, `created_at`; `UNIQUE(session_id)`; **không sửa sau khi lưu** | BR-47, BR-52, UC5.2 |
| 5 | `StudentProfile` | "Student có thể không có tài khoản" | `user_id NOT NULL UNIQUE`; thêm `education_level_id`, `area`, `profile_complete` | BR-04, BR-26, UC1.8, UC3.1 |
| 6 | `ParentStudentLink` | Thiếu trạng thái và thông tin thu hồi | Thêm `status` (CONFIRMED/REVOKED), `confirmed_at`, `revoked_at`, `revoked_by`; `UNIQUE(parent_user_id, student_id)` | BR-26, BR-30, BR-31 |
| 7 | `TutorSubject` | Thiếu cấp học | Khóa `(tutor_id, subject_id, education_level_id)` | BR-07, BR-20 |
| 8 | `TutorProfile` | Availability như một thuộc tính | Chuyển sang bảng `tutor_availability_slot` (weekly slot); thêm `status`, `rating_avg`, `review_count`, `version`, `created_at` | UC1.5 Notes, BR-20, BR-23, BR-41, FR-29, UC1.6.E2 |
| 9 | `LearningGoal` | Availability + thang "level" lẫn lộn | Slot sang `goal_availability_slot`; `current_level`/`desired_level` là thang năng lực có thứ tự (smallint); thêm `deadline`, `goal_type`, `status` (ACTIVE/ARCHIVED) | BR-11, BR-12 |
| 10 | `Session` → `tutoring_session` | Thiếu 6/8 trạng thái, hạn đề xuất, late, repeat note, UTC | Thêm cột (xem DBML); lưu thêm `tutor_id`, `student_id` (denormalize) để dùng exclusion constraint | BR-41→48, FR-22, NFR-17, NFR-18 |
| 11 | `CredentialDocument` | Gộp request, credential, file vào một bảng | Tách thành `verification_request` → `credential` → `credential_file` | UC1.10–1.12, BR-13/14/15 |
| 12 | `Review` | Thiếu rating/comment/ràng buộc | Thêm `rating`, `comment`, `reviewer_type`, `is_edited`, `completed_sessions_at_review`; `UNIQUE(tutor_id, student_id)` | BR-57/58/59, NFR-24 |
| 13 | `User` → `account` | Thiếu cột bảo mật | Thêm `status`, `failed_attempts`, `locked_until`, `last_login_at` | BR-06, NFR-05 |

> Không lưu **số buổi hoàn thành** và **progress %** thành cột: luôn tính từ dữ liệu gốc để mọi màn hình ra cùng một con số (NFR-23, BR-48, BR-51).

---

## 4. Các bảng cần thêm

| Bảng | Module (ADR-002) | Căn cứ |
|---|---|---|
| `email_token` | iam | NFR-03 (token verify/reset, lưu hash, dùng 1 lần) |
| `education_level` | profile | BR-20; Admin mở rộng được (UC1.5 Assumptions) |
| `tutor_availability_slot` | profile | BR-20, BR-41, UC4.2 |
| `goal_availability_slot` | profile | BR-11, BR-20 |
| `verification_request` | verification | UC1.10, BR-14, BR-15 |
| `credential` | verification | UC1.10 |
| `credential_file` | verification | UC1.11, BR-13 |
| `link_invitation` | connection | UC3.1/3.2, BR-27→29 |
| `connection_request` | connection | UC3.4/3.5, BR-32→37 (hiện **chưa có chỗ lưu**) |
| `session_reschedule_proposal` | scheduling | BR-45, Group 4 design #5 |
| `session_history` | scheduling | FR-21 |
| `learning_plan` | progress | UC5.1, BR-49 |
| `milestone` | progress | UC5.1, BR-50, BR-51 |
| `plan_history` | progress | FR-23, FR-24 |
| `audit_log` | platform/audit | FR-02, FR-15 |
| `ai_request_log` | platform/ai | FR-05, BR-10, BR-25 |
| `matching_run_log` | discovery | FR-10 |

Tổng sau chỉnh sửa: **29 bảng** (bỏ `Payment`, thêm 17 bảng, đổi tên 3 bảng).

---

## 5. Ràng buộc phải viết ở tầng DB (Flyway migration)

dbdiagram.io **không** vẽ được partial index / exclusion constraint, nên các ràng buộc này chỉ có trong migration, không có trong file DBML. Viết vào migration của module tương ứng:

```sql
-- scheduling (NFR-17, BR-42): chống double booking
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE tutoring_session ADD CONSTRAINT no_tutor_overlap
  EXCLUDE USING gist (tutor_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&)
  WHERE (status = 'SCHEDULED');

ALTER TABLE tutoring_session ADD CONSTRAINT no_student_overlap
  EXCLUDE USING gist (student_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&)
  WHERE (status = 'SCHEDULED');

ALTER TABLE tutoring_session ADD CONSTRAINT chk_session_time
  CHECK (end_at > start_at
     AND end_at - start_at BETWEEN interval '30 minutes' AND interval '240 minutes');  -- BR-41

-- BR-28: tối đa 1 invitation PENDING cho mỗi cặp tài khoản
CREATE UNIQUE INDEX uq_invitation_pending ON link_invitation
  (LEAST(inviter_user_id, invited_user_id), GREATEST(inviter_user_id, invited_user_id))
  WHERE status = 'PENDING';

-- BR-34: tối đa 1 request PENDING cho mỗi (student, tutor)
CREATE UNIQUE INDEX uq_request_pending ON connection_request (student_id, tutor_id)
  WHERE status = 'PENDING';

-- BR-38: tối đa 1 class ACTIVE cho mỗi (student, tutor)
CREATE UNIQUE INDEX uq_class_active ON class (student_id, tutor_id)
  WHERE status = 'ACTIVE';

-- BR-14: tối đa 1 verification request PENDING cho mỗi tutor
CREATE UNIQUE INDEX uq_verification_pending ON verification_request (tutor_id)
  WHERE status = 'PENDING';

-- BR-44: tối đa 1 reschedule proposal PENDING cho mỗi session
CREATE UNIQUE INDEX uq_reschedule_pending ON session_reschedule_proposal (session_id)
  WHERE status = 'PENDING';

-- CHECK còn lại
ALTER TABLE review     ADD CONSTRAINT chk_rating  CHECK (rating BETWEEN 1 AND 5);            -- BR-58
ALTER TABLE review     ADD CONSTRAINT chk_comment CHECK (char_length(comment) BETWEEN 10 AND 1000);
ALTER TABLE milestone  ADD CONSTRAINT chk_weight  CHECK (weight BETWEEN 1 AND 5);            -- BR-50
ALTER TABLE tutor_availability_slot ADD CONSTRAINT chk_slot_t CHECK (end_time > start_time);
ALTER TABLE goal_availability_slot  ADD CONSTRAINT chk_slot_g CHECK (end_time > start_time);
```

Lưu ý: `UNIQUE(tutor_id, student_id)` của `review`, `UNIQUE(session_id)` của `session_note`, `UNIQUE(engagement→class_id)` của `learning_plan` đã có sẵn trong DBML.

---

## 6. Trình tự migration gợi ý (theo luật version của ADR-002)

FK xuyên module phải có version **muộn hơn** bảng được tham chiếu:

1. `account`, `email_token`
2. `education_level`, `subject`, `student_profile`, `tutor_profile`, `tutor_subject`, `*_availability_slot`, `learning_goal`
3. `verification_request`, `credential`, `credential_file`
4. `link_invitation`, `parent_student_link`, `connection_request`, `class`
5. `tutoring_session`, `session_reschedule_proposal`, `session_history` ← **ưu tiên làm trước theo sprint** (module rủi ro cao nhất)
6. `session_note`, `learning_plan`, `milestone`, `plan_history`
7. `review`, `notification`
8. `audit_log`, `ai_request_log`, `matching_run_log`

---

## 7. Điểm còn mở (để mở rộng / chỉnh sửa sau — KHÔNG sửa trong bản này)

| # | Vấn đề | Hiện trạng trong schema mới | Khi nào cần quay lại |
|---|---|---|---|
| O1 | Học sinh THCS **không có email/tài khoản** | Schema theo UC hiện tại: `student_profile.user_id NOT NULL` | Nếu team muốn Parent tạo hồ sơ cho con → thêm use case + sửa BR-26 + cho `user_id` nullable |
| O2 | Khối lớp "6–9" (README, màn hình 8, 29) vs "mọi cấp học" (Section 3) | Dùng bảng `education_level` do Admin quản lý, **chứa được cả hai** | Chỉ cần chốt dữ liệu seed, không cần đổi schema |
| O3 | Một class chứa 1 hay nhiều goal | Đang chọn **1 class = 1 goal = 1 learning plan** (Assumption UC5.1) | Nếu cho học nhiều môn với cùng tutor → bảng `class_goal` |
| O4 | Kết thúc class (ending) | `class.status` có `ENDED`, `end_reason`; **chưa có use case** | Khi viết flow trong SRS |
| O5 | Sửa/hoàn tác outcome, sửa note | Chưa hỗ trợ (UC4.6, UC5.2) | Nếu cần → thêm bảng revision |
| O6 | Báo cáo / gỡ review vi phạm | Chưa có bảng (Group 6 design #5) | Scope change |
| O7 | Recurrence có cấu trúc | `repeat_note` là text (FR-22) | Future work |
| O8 | `desired_slots` của connection request | Lưu `jsonb` cho gọn | Tách bảng nếu cần truy vấn theo slot |
| O9 | AI quiz, AI tiến độ, quiz/attempt | Không có bảng | Tầng mở rộng, chỉ nêu trong báo cáo |

---

## 8. Mâu thuẫn khác trong SRS (ngoài DB) — nên sửa khi rảnh

| # | Chỗ | Vấn đề | Hướng sửa (theo Use Case Descriptions) |
|---|---|---|---|
| 1 | Bảng UC tổng quan, UC1.2 | Ghi "Zalo OAuth" | Đặc tả: chỉ email/password |
| 2 | Screen 15 "Link Student Dialog" | Dùng mã liên kết | UC3.1: mời qua email |
| 3 | UC5.2, UC4.6 (bảng tổng quan) | "structured notes", "mandatory note" | Free text, không bắt buộc (BR-47/52) |
| 4 | Screen 24 "Session Report Editor" | Có attendance, assessment, milestone updates | Chỉ outcome + free-text note |
| 5 | Screen Authorization | Màn hình Tutor đánh dấu ở cột **Admin**; "Schedule Calendar" có Admin | Chuyển sang cột Tutor, bỏ Admin ở Calendar |
| 6 | Mục 3.1 SRS (ERD trong sprint plan) | `StudentRequest`, `Match`, `Progress`, `Payment` đã lỗi thời | Thay bằng danh sách 29 bảng |
| 7 | UC2.1 PRE-1 vs Screen Authorization | Đăng nhập vs Guest | Cần chốt (cũng nêu ở ADR-002 mục #7) |
| 8 | Section 3 Use Case Model | "Class concept is not used" | Sửa theo mục 2.1 |
| 9 | ADR-002 | Dùng `engagement` | Đổi thành `class` (tên `account` đã khớp) |
