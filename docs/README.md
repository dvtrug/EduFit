# Backend Documentation Index

## Discovery

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
- [Connection module flow](connection/connection-module-flow.md): liên kết Parent/Student và connection workflow.
- [Connection refactor plan](../tasks/connection/plan.md) và [tasks](../tasks/connection/todo.md): thực thi trên feat/connection, commit/push từng task.
- [Connection HTTP contract](connection/connection-api-contract.md) và [Phase 0 validation](connection/phase0-validation.md) / [Phase 1 validation](connection/phase1-validation.md) / [Phase 2 validation](connection/phase2-validation.md): baseline tích hợp, quyết định cho refactor và kết quả kiểm chứng.
- [Connection Flyway fix](connection/flyway-fix-validation.md): manifest tương thích main, backup/clone/local validation và phạm vi DB hỗ trợ.
- [Project overview](PROJECT_OVERVIEW.md): bối cảnh dự án; phần kế hoạch/lịch sử không phải chứng nhận hoàn thành của mọi module.

Tài liệu Discovery hiện tại mô tả code, không coi prototype/roadmap hoặc ảnh cũ là
hợp đồng runtime. Khi sửa Discovery: đọc tài liệu liên quan trước, cập nhật contract,
flow và nguồn SVG/PNG cùng thay đổi; kiểm tra score/filter/query count bằng tests.

- [Connection merge main 14/10/2026](connection/main-merge-validation.md): conflict resolution, row-lock reload, manifest13 và integration validation.
