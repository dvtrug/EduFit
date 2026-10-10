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
- [Connection module flow](connection-module-flow.md): liên kết Parent/Student và connection workflow.
- [Project overview](PROJECT_OVERVIEW.md): bối cảnh dự án; phần kế hoạch/lịch sử không phải chứng nhận hoàn thành của mọi module.

Tài liệu Discovery hiện tại mô tả code, không coi prototype/roadmap hoặc ảnh cũ là
hợp đồng runtime. Khi sửa Discovery: đọc tài liệu liên quan trước, cập nhật contract,
flow và nguồn SVG/PNG cùng thay đổi; kiểm tra score/filter/query count bằng tests.
