# Backend Documentation Index

## Discovery

- [AI provider contract — Phase 1](ai-discovery-backend-contract.md): cấu hình Gemini/stub, provenance, HTTP provider errors và cách nạp environment.
- [AI + Discovery — Phase 2](ai-discovery-phase2-contract.md): audit transaction isolation, quota và provider deadline.
- [AI + Discovery — Phase 3](ai-discovery-phase3-contract.md): Student goals/slots, readiness và Parent goal selection qua confirmed link.
- [AI + Discovery — Phase 4](ai-discovery-phase4-contract.md): JDBC session, CSRF/CORS và HTTP integration contract.
- [AI + Discovery — Phase 5 E2E](ai-discovery-phase5-e2e.md): luồng Discovery -> Connection, fallback và eligibility rule.
- [AI + Discovery backend runbook](ai-discovery-backend-runbook.md): cold start, fixture, smoke offline/live và contract bàn giao frontend.

Trạng thái bàn giao T15 ngày 2026-10-10 trên `feat/discovery`. Đọc theo thứ tự:

1. [Discovery API contract](discovery-api-contract.md): ba HTTP endpoints, query/body/response/errors, quyền, Java facade và event.
2. [Discovery module flow](discovery-module-flow.md): package/class, ownership, search/detail/matching, công thức năm yếu tố, AI, transaction và Q&A; diagram PNG nhúng trực tiếp.
3. [Discovery PostgreSQL performance](discovery-performance-report.md): query count, local p95, query plans, candidate-cap recall và giới hạn bằng chứng.
4. [Execution plan](../tasks/plan.md) và [checklist](../tasks/todo.md): task/phase/commit và kiểm chứng đã chạy; đây là lịch sử triển khai.

## Kiến trúc và module liên quan

- [SADS](software-architecture-design-specification.md): kiến trúc và NFR tổng thể; phần Discovery trỏ tới contract hiện thực.
- [ADR-002](ADR-002-multi-module-architecture.md): ranh giới Maven/module và public API.
- [Backend module structure guide](backend-module-structure-guide.md): vai trò api/web/application/domain/infra.
- [Authentication contract](authentication-contract.md): thiết kế xác thực; đối chiếu code SecurityConfig khi tích hợp runtime.
- [Connection module flow](connection-module-flow.md): liên kết Parent/Student và connection workflow.
- [Project overview](PROJECT_OVERVIEW.md): bối cảnh dự án; phần kế hoạch/lịch sử không phải chứng nhận hoàn thành của mọi module.

Tài liệu Discovery hiện tại mô tả code, không coi prototype/roadmap hoặc ảnh cũ là
hợp đồng runtime. Khi sửa Discovery: đọc tài liệu liên quan trước, cập nhật contract,
flow và nguồn SVG/PNG cùng thay đổi; kiểm tra score/filter/query count bằng tests.
