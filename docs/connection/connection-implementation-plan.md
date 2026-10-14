# Connection Backend — Kế hoạch thực hiện và Hiện trạng hoàn tất

> **Trạng thái:** HOÀN TẤT 100% (Từ T00 đến T25) trên nhánh `feat/connection`.
> **Tài liệu đối chiếu:** [Plan refactor chi tiết](../../tasks/connection/plan.md), [Tasks checklist](../../tasks/connection/todo.md), [API Contract](connection-api-contract.md), [Module Flow](connection-module-flow.md), [Verification Report](connection-verification-report.md).

---

## 1. Nguồn nghiệp vụ và Phạm vi triển khai

- **Tài liệu nguồn:**
  - [SRS Edufit](../SRS%20Edufit.docx): Group 3 và các Business Rules BR-26 đến BR-38.
  - [SADS](../software-architecture-design-specification.md): Mục 4.3–4.4 (REST API và luồng Parent → Student → Tutor → Class).
  - [ADR-002](../ADR-002-multi-module-architecture.md): Kiến trúc đa module, module boundaries, tách biệt tầng `api`, `application`, `infra`, `web`.
  - [Master Sprint Plan](../master_sprint_plan_and_ai_learning_framework.md): Sprint 3 Connection.
- **Phạm vi triển khai:**
  - Backend Module Connection (`backend/modules/connection`): Hoàn chỉnh toàn bộ 5 Use Cases (UC3.1 đến UC3.5), toàn bộ 13 Business Rules (BR-26 đến BR-38), 4 Functional Requirements (FR-12 đến FR-15), và các Non-Functional Requirements (NFR-07, NFR-11, NFR-15, NFR-16).
  - Tích hợp 2 chiều mượt mà với Platform (`shared`, `ai`, `audit`, `storage`), và các Modules nghiệp vụ khác (`iam`, `profile`, `discovery`, `notification`, `scheduling`).

---

## 2. Kiến trúc Module Đã Hoàn Thiện

Module Connection được thiết kế theo kiến trúc phân tầng sạch (Clean / Layered Architecture theo ADR-002), loại bỏ hoàn toàn các phụ thuộc vòng (circular dependencies) và rò rỉ chi tiết cài đặt:

```
vn.edufit.connection
├── api/                                # Public API contracts duy nhất cho các module khác
│   ├── ConnectionFacade.java           # Public facade: hasConfirmedParentLink, findActiveClass
│   └── event/                          # Transactional Event Records (IDs, statuses, timestamps)
│       ├── LinkInvitationChangedEvent.java
│       ├── ParentStudentLinkChangedEvent.java
│       ├── ConnectionRequestChangedEvent.java
│       ├── TutoringClassCreatedEvent.java
│       └── EventContract.java
├── application/                        # Tầng nghiệp vụ (Business logic & Use Cases)
│   ├── dto/                            # Application Result DTOs & PageResponse
│   ├── policy/                         # Centralized transition guards, deadlines, input rules
│   │   ├── ConnectionTransitions.java  # Guard transitions, TTLs (14d, 7d), Cooldown (7d)
│   │   └── ConnectionRequestInput.java # Slot parsing & message validation
│   ├── port/                           # SPI Ports
│   │   └── ConnectionLock.java         # Advisory lock port
│   └── service/                        # Domain & Orchestration Services
│       ├── ConnectionFacadeImpl.java   # Implementation của ConnectionFacade
│       ├── LinkInvitationService.java  # UC3.1 & UC3.2 (Mời, phản hồi, rút lời mời)
│       ├── ParentStudentLinkService.java # UC3.3 (Thu hồi liên kết, audit & notification)
│       ├── ConnectionRequestService.java # UC3.4 & UC3.5 (Yêu cầu gia sư, phản hồi, tạo lớp)
│       ├── ConnectionQueryService.java # Truy vấn danh sách/phân trang, batch hydrate metadata
│       ├── ConnectionExpiryService.java # Xử lý quá hạn có giới hạn và an toàn
│       └── InvitationQuotaService.java # Giới hạn 10 lời mời/giờ (sliding quota row lock)
├── infra/                              # Tầng hạ tầng và kỹ thuật
│   ├── locking/                        # PostgreSQL Advisory Lock implementation
│   ├── persistence/
│   │   ├── entity/                     # JPA Entities (LinkInvitation, ParentStudentLink, ConnectionRequest, TutoringClass, ConnectionInviteQuota)
│   │   └── repository/                 # Spring Data JPA Repositories & FreshConnectionRowRepository
│   └── scheduling/                     # Background cron job
│       └── ConnectionExpiryJob.java    # Quét dọn tự động lời mời và yêu cầu quá hạn (FR-13)
└── web/                                # Tầng REST Controller & Exception Handlers
    ├── ConnectionController.java       # 14 endpoints (10 legacy compatible + 4 paged/admin)
    ├── ConnectionWriteExceptionHandler.java # Map OptimisticLock/Constraint thành HTTP 409
    ├── request/                        # HTTP Request bodies với Bean Validation
    └── response/                       # HTTP Response envelopes tuân thủ BR-37 Privacy
```

---

## 3. Bản đồ Nghiệp vụ: Use Cases & Business Rules

| Use Case | Quy tắc áp dụng | Thành phần đảm nhiệm | Trạng thái |
|---|---|---|---|
| **UC3.1: Gửi lời mời liên kết** | BR-26 (Hai chiều Parent/Student), BR-27 (Tài khoản đối vai trò), BR-28 (Chống dò email, rate limit 10/h), BR-29 (TTL 14 ngày, Cooldown 7 ngày) | `LinkInvitationService`, `InvitationQuotaService`, `PostgresConnectionLock` | **ĐÃ HOÀN TẤT** |
| **UC3.2: Phản hồi lời mời** | BR-26 (Chỉ người nhận), BR-29 (Accept tạo link CONFIRMED, Decline đặt cooldown 7 ngày, Expired chặn hành động) | `LinkInvitationService`, `ParentStudentLinkRepository`, `LinkInvitationChangedEvent` | **ĐÃ HOÀN TẤT** |
| **UC3.3: Thu hồi liên kết & Kiểm tra quyền** | BR-30 (Chỉ Student sở hữu hoặc Admin có lý do bắt buộc), BR-31 (Quyền Parent mất ngay lập tức, dữ liệu Student giữ nguyên, cho phép relink qua lời mời mới) | `ParentStudentLinkService`, `AuditSink`, `ParentStudentLinkChangedEvent`, V1.11 migration | **ĐÃ HOÀN TẤT** |
| **UC3.4: Gửi & Quản lý yêu cầu gia sư** | BR-32 (Quyền gửi theo link hiện tại), BR-33 (Goal ACTIVE, Tutor VERIFIED + khớp môn/cấp), BR-34 (1 PENDING/cặp, tối đa 5 PENDING/student, không có class ACTIVE), BR-35 (TTL 7 ngày, slot hợp lệ, message ≤ 500 ký tự), BR-36 (Hủy yêu cầu PENDING) | `ConnectionRequestService`, `ConnectionTransitions`, `ConnectionRequestChangedEvent` | **ĐÃ HOÀN TẤT** |
| **UC3.5: Gia sư phản hồi & Tạo lớp học** | BR-38 (Accept tạo đúng 1 lớp ACTIVE cùng transaction, Reject kèm lý do tùy chọn ≤ 300 ký tự không tạo lớp) | `ConnectionRequestService`, `TutoringClassRepository`, `TutoringClassCreatedEvent` | **ĐÃ HOÀN TẤT** |
| **Tự động hết hạn (FR-13)** | BR-29, BR-35 (Now ≥ ExpiresAt chuyển sang EXPIRED khi truy cập hoặc qua scheduler nền) | `ConnectionExpiryService`, `ConnectionExpiryJob` | **ĐÃ HOÀN TẤT** |
| **Bảo mật & Quyền riêng tư (BR-37, NFR-16)** | Whitelist metadata inbox gia sư (không lộ email, số điện thoại, thông tin liên lạc của học sinh/phụ huynh), bảo vệ SPA CSRF | `ConnectionQueryService`, `TutorInboxResponse`, `SecurityConfig` | **ĐÃ HOÀN TẤT** |

---

## 4. Lộ trình thực hiện và Bằng chứng hoàn thành

Toàn bộ 24 nhiệm vụ từ Phase 0 đến Phase 6 đã hoàn thành xuất sắc và được push lên Git remote với commit riêng biệt:

1. **Phase 0 & Phase 1 (Baseline & Foundation):**
   - T00, T00a, T00b: Baseline snapshot, import integrity, tích hợp Discovery & main.
   - T01–T07: Chốt API contract, tách dịch vụ thành các service độc lập, mapping DTOs typed result.
2. **Phase 2 (Guards, Clock, Rollback & Events):**
   - T08a–T08c: Centralized transition guards, Clock IAM UTC thống nhất.
   - T09: Typed conflict result, default rollback cho write failures.
   - T10a–T10b: 4 public event records, transactional `BEFORE_COMMIT` event listener.
3. **Phase 3 (Use Case Vertical Slices):**
   - T11a–T11b: Two-way invitation, consent, cooldown, transactional events.
   - T12a–T12c: Student/Admin revoke, structured audit snapshot, notification.
   - T13a–T13c: Create/cancel request, slot grammar, pending count 5 quota, notification.
   - T14: Tutor accept/reject, exactly-one active class creation, recipient matrix.
   - T15: Current parent consent enforcement across all entrypoints.
4. **Phase 4 (Automated Bounded Expiry):**
   - T16: Centralized `ConnectionExpiryService` cho on-access và background job.
   - T17: `ConnectionExpiryJob` định kỳ multi-batch, an toàn deadlock và retry.
5. **Phase 5 (Performance, Privacy & History):**
   - T18a–T18c: Batch hydrate goal/sender metadata, BR-37 whitelist, deadline.
   - T19a–T19b: Scoped paginated endpoints (`/invitations/page`, `/links/page`, `/requests/page`) và Admin link lookup.
6. **Phase 6 (Comprehensive Quality Gate & Verification):**
   - T20: Parameterized DDT matrix đầy đủ (271 tests GREEN).
   - T21: Full Spring Security filter chain với real Argon2, session, và SPA CSRF.
   - T22a: Flyway clean/upgrade migration test trên PostgreSQL 17 thật, bảo toàn dữ liệu lịch sử và JSONB.
   - T22b: Atomic rollback matrix (outer rollback, listener failure rollback, audit/class failure rollback).
   - T22c: Concurrency race matrix với barriers và independent transactions (8 scenarios).
   - T23: ArchUnit boundary rules, non-empty assertions, negative probe verification.
   - T24: Final consumer regression (Discovery/Notification/Audit), full reactor test.
7. **Phase 7 (Documentation & Handover):**
   - T25: Đồng bộ toàn bộ tài liệu kiến trúc, API contract, sequence diagrams, verification report, và task checklists.
