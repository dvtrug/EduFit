# Kế hoạch hoàn thiện module Progress

## 1. Mục tiêu và trạng thái hiện tại

Xây dựng đầy đủ Progress MVP theo luồng:

`Tutor tạo Learning Plan → quản lý Milestone → ghi nhận kết quả buổi học → hệ thống tính tiến độ → Student/Parent xem dashboard → Tutor xem lịch sử giảng dạy`

Hiện tại:

- Module `progress` chưa có Java source.
- Database đã có `learning_plan`, `milestone`, `plan_history`, `session_note`.
- Frontend chưa có `features/progress`; dashboard đang dùng dữ liệu demo và `localStorage`.
- Scheduling đã được triển khai trên `feature/backend-schedule`, có `SessionFacade` và `SessionOutcomeRecordedEvent`.
- Nhánh Scheduling đang tách khá xa nhánh hiện tại, nên phải rebase/cherry-pick 7 commit Scheduling lên code mới trước khi bắt đầu Progress.
- Progress không chỉnh sửa nghiệp vụ Connection; chỉ yêu cầu Connection cung cấp facade đọc lớp và kiểm tra quyền.

## 2. Ranh giới module và hợp đồng tích hợp

### Connection cung cấp cho Progress

Mở rộng `ConnectionFacade` theo hướng chỉ đọc:

- `findClassById(classId)`: trả `classId`, `studentId`, `tutorId`, `goalId`, trạng thái và thời gian lớp.
- `findClassesByStudent(studentId)`.
- `findClassesByTutor(tutorId)`.
- `hasConfirmedParentLink(parentUserId, studentId)`.
- `findConfirmedParentUserIds(studentId)` để gửi notification.

Progress không được truy cập repository hoặc JPA entity của Connection.

### Scheduling cung cấp cho Progress

Giữ các contract hiện có và bổ sung dạng additive:

- `findSessionsByClassId(classId, from, to)` để dựng timeline và chi tiết lớp.
- `getSessionStatsByClassIds(classIds, from, to)` để lấy theo batch:
  - completed;
  - absent;
  - cancelled;
  - upcoming;
  - next session;
  - last session.
- Giữ `getCompletedStatsByClassId` để không phá consumer hiện tại.

Sửa các điểm lệch trong nhánh Scheduling trước khi nối Progress:

- `RecordOutcomeRequest.noteContent` tăng giới hạn từ 1.000 lên 5.000 ký tự.
- Chuẩn hóa việc phân biệt `account.userId` và `tutor_profile.tutorId` khi ghi outcome.
- Dùng `SessionOutcomeRecordedEvent` làm nguồn tích hợp duy nhất; loại bỏ đường `ProgressSyncPort` no-op để tránh hai cơ chế đồng bộ.
- Event được xử lý đồng bộ trong transaction để outcome và session note cùng thành công hoặc cùng rollback.

### Progress sở hữu

- Learning plan và milestone.
- Công thức tiến độ.
- Lịch sử thay đổi kế hoạch.
- Session note bất biến.
- Dashboard tiến độ và teaching history.
- Notification khi tạo plan hoặc milestone chuyển sang `ACHIEVED`.

Outcome `COMPLETED/ABSENT` vẫn thuộc Scheduling.

## 3. Quy tắc nghiệp vụ và API

### Công thức tiến độ

```text
progressPercent =
  tổng weight của milestone ACHIEVED
  / tổng weight của milestone không archived
  × 100
```

- Kết quả làm tròn số nguyên theo `HALF_UP`.
- Milestone archived không tham gia tử số hoặc mẫu số.
- Lớp chưa có learning plan trả `progressPercentage: null`, không hiển thị `0%`.
- Mọi màn Student, Parent và Tutor phải dùng cùng `ProgressCalculator`, không tự tính riêng ở frontend.

### Quản lý Learning Plan

Các endpoint:

- `POST /api/v1/progress/plans`
- `GET /api/v1/progress/plans/{classId}`
- `PUT /api/v1/progress/plans/{planId}`
- `PATCH /api/v1/progress/milestones/{milestoneId}/status`

Tạo plan nhận:

```json
{
  "classId": "uuid",
  "summary": "string | null",
  "milestones": [
    {
      "title": "string",
      "description": "string | null",
      "targetDate": "yyyy-MM-dd | null",
      "weight": 1
    }
  ]
}
```

Cập nhật plan dùng full aggregate và bắt buộc gửi `version`:

```json
{
  "version": 2,
  "summary": "string | null",
  "milestones": [
    {
      "milestoneId": "uuid | null",
      "title": "string",
      "description": "string | null",
      "targetDate": "yyyy-MM-dd | null",
      "weight": 1
    }
  ]
}
```

Quy tắc:

- Chỉ Tutor của `class ACTIVE` được tạo và sửa.
- Mỗi class có đúng tối đa một learning plan.
- Plan có từ 1 đến 20 milestone đang hoạt động.
- Summary tối đa 1.000 ký tự; title bắt buộc và tối đa 200 ký tự.
- Weight là số nguyên 1–5.
- Target date mới hoặc vừa thay đổi không được trong quá khứ.
- Milestone chưa từng `IN_PROGRESS/ACHIEVED` được xóa thật.
- Milestone đã từng tiến triển chỉ được chuyển `isArchived=true`.
- Plan không được xóa.
- `learning_plan.version` dùng optimistic locking; version cũ trả `409 PLAN_VERSION_CONFLICT`.
- Mỗi thay đổi ghi `plan_history` với actor, action, giá trị cũ/mới và thời gian.

Cập nhật trạng thái milestone nhận `version`, `status`, `achievedDate`:

- Tutor có thể đặt `NOT_STARTED`, `IN_PROGRESS`, `ACHIEVED`.
- `ACHIEVED` mặc định ngày hiện tại; cho phép ngày quá khứ nhưng không cho phép tương lai.
- Khi chuyển khỏi `ACHIEVED`, xóa `achievedDate`.
- Chỉ phát notification khi chuyển từ trạng thái khác sang `ACHIEVED`.

### Session note

Progress lắng nghe `SessionOutcomeRecordedEvent`:

- Luôn tạo đúng một `session_note` cho outcome `COMPLETED` hoặc `ABSENT`, kể cả nội dung trống.
- Note tối đa 5.000 ký tự.
- Note không được sửa hoặc xóa.
- Unique `session_id` bảo vệ xử lý event lặp.
- Nội dung được lưu và hiển thị như plain text; không render HTML.

Thêm endpoint timeline có phân trang:

- `GET /api/v1/progress/classes/{classId}/notes?page=0&size=20&from=&to=`

Tutor của lớp, Student của lớp và Parent có link `CONFIRMED` được đọc.

### Dashboard

Các endpoint:

- `GET /api/v1/progress/dashboard/student/{studentId}?classId=&range=30D|90D|ALL`
- `GET /api/v1/progress/dashboard/parent`
- `GET /api/v1/progress/teaching-history?page=&size=&studentName=&status=&from=&to=`

Dashboard Student/Parent trả:

- Danh sách lớp để chuyển đổi; mặc định chọn lớp `ACTIVE` gần nhất.
- Goal, tutor và deadline.
- Plan summary, milestone hiện tại và `progressPercentage`.
- Số buổi completed, absent, cancelled, upcoming và buổi tiếp theo.
- Chuỗi `progressSeries` từ achievement date của milestone đang hoạt động.
- Mười session note gần nhất và `hasMoreNotes`.

Quy tắc biểu đồ:

- Bộ lọc 30 ngày, 90 ngày hoặc toàn bộ.
- Nếu milestone đã đạt trước khoảng lọc, tạo điểm mở đầu tại ngày đầu khoảng với phần trăm tích lũy.
- Nếu có dưới hai điểm, frontend hiển thị thông báo “Chưa đủ dữ liệu” thay vì vẽ biểu đồ giả.
- Parent dashboard nhóm theo từng học sinh và từng lớp; không tính trung bình giữa các con.

Teaching History trả danh sách lớp của Tutor cùng Student, goal, trạng thái lớp, progress, session metrics và lần học gần nhất; danh sách phải phân trang và tránh truy vấn N+1.

## 4. Frontend và trải nghiệm theo vai trò

### Tutor

- Tạo `/tutor/classes` làm màn Teaching History và danh sách lớp.
- Tạo `/tutor/classes/[classId]/plan` cho plan editor.
- Editor hỗ trợ thêm, sửa, sắp xếp, xóa/archive milestone và cập nhật trạng thái.
- Khi gặp `PLAN_VERSION_CONFLICT`, giữ dữ liệu form, tải phiên bản mới và yêu cầu Tutor đối chiếu trước khi lưu lại.
- Màn lớp hiển thị session history, note đã ghi và liên kết sang Learning Plan.

### Student

- Tạo `/student/progress`.
- Hiển thị selector lớp, progress tổng, milestone, session metrics, biểu đồ và note timeline.
- Chỉ đọc; không hiển thị action thay đổi milestone.
- Thay dữ liệu demo/localStorage trong Student Dashboard bằng dữ liệu Progress thật.

### Parent

- Tạo `/parent/progress`.
- Dữ liệu học sinh lấy từ link `CONFIRMED`, không dùng localStorage.
- Trang đầu hiển thị các học sinh và từng lớp; chọn card mở dashboard chi tiết dùng chung với Student.
- Parent chỉ đọc và mất quyền ngay ở request tiếp theo nếu liên kết bị revoke.
- Không hiển thị một “điểm trung bình tiến độ” chung giữa nhiều học sinh.

### UI chung

- Tạo `features/progress` và `services/progress`.
- Dùng component chung cho milestone list, progress summary, session metrics và note timeline.
- Biểu đồ dùng SVG/CSS nhẹ và accessible; chưa thêm thư viện chart mới.
- Tách rõ loading, error, retry và các empty state:
  - chưa có lớp;
  - có lớp nhưng chưa có plan;
  - có plan nhưng chưa có milestone đạt;
  - chưa có session/note.
- Mọi mutation đi qua `apiClient` để dùng CSRF hiện tại.

## 5. Thứ tự triển khai và kiểm thử

### Giai đoạn 0 — Đồng bộ phụ thuộc

- Rebase/cherry-pick Scheduling lên nhánh mới nhất.
- Chốt facade đọc lớp với người đang làm Connection.
- Bổ sung các query batch vào `SessionFacade`.
- Sửa giới hạn note và actor ID trong Scheduling.
- Chạy baseline backend trước khi viết Progress.

### Giai đoạn 1 — Learning Plan theo lát dọc

- Xây domain model, `ProgressCalculator`, persistence và history.
- Làm API create/get plan.
- Làm Tutor plan editor và Student read-only view.
- Checkpoint: Tutor tạo được plan và Student thấy cùng dữ liệu.

### Giai đoạn 2 — Milestone lifecycle

- Làm update full plan, optimistic locking, status và archive.
- Ghi history và notification.
- Nối progress percentage vào các view.
- Checkpoint: mọi vai trò nhìn cùng một phần trăm.

### Giai đoạn 3 — Session integration

- Lắng nghe outcome event và lưu immutable note.
- Bổ sung session metrics và notes API.
- Kiểm tra transaction rollback và event idempotency.
- Checkpoint: Tutor record outcome bên Scheduling, Student/Parent thấy note và số buổi.

### Giai đoạn 4 — Dashboard đầy đủ

- Hoàn thiện Student Progress, Parent Multi-Student và Tutor Teaching History.
- Nối các dashboard hiện tại với API thật.
- Bổ sung filter thời gian, progress series và phân trang.

### Kiểm thử bắt buộc

- `ProgressCalculator`: milestone 1–5 weight, archived, rounding, không có plan.
- Plan validation: 0/1/20/21 milestones, title trống, weight sai, target date quá khứ.
- Authorization/IDOR cho Student, Parent, Tutor khác lớp và link bị revoke.
- Optimistic locking khi hai tab cùng sửa plan.
- Xóa thật và archive đúng theo lịch sử milestone.
- Note boundary 0/1/5.000/5.001 ký tự và note không thể chỉnh sửa.
- Event outcome lặp không tạo hai note.
- Rollback outcome nếu lưu note thất bại.
- Contract test bảo đảm Student, Parent và Tutor nhận cùng progress.
- Frontend component test cho form, empty state, conflict và read-only permissions.
- E2E: Tutor tạo plan → cập nhật milestone → record session outcome → Student và Parent thấy tiến độ/note → Tutor thấy teaching history.

## 6. Giới hạn MVP

Không bao gồm AI progress insight, quiz/attempt, điểm bài tập, Student tự xác nhận milestone, chỉnh sửa session note, sửa outcome đã ghi, xóa learning plan hoặc tính trung bình tiến độ giữa nhiều học sinh.
