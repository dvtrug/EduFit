# Thư Mục Tài Liệu Kỹ Thuật EduFit (EduFit Documentation Index)

Thư mục này lưu trữ toàn bộ tài liệu kiến trúc, đặc tả yêu cầu, API contracts và kế hoạch kiểm thử cốt lõi của nền tảng **EduFit**.

---

## 1. Yêu Cầu Nghiệp Vụ & Thiết Kế Kiến Trúc Tổng Thể

- **[SRS Edufit.docx](SRS%20Edufit.docx):** Bản đặc tả yêu cầu phần mềm chính thức (Software Requirements Specification) của toàn bộ hệ thống EduFit.
- **[SADS (Software Architecture Design Specification)](software-architecture-design-specification.md):** Tài liệu kiến trúc phần mềm chi tiết, bao quát kiến trúc Pragmatic Modular Monolith, Bounded Contexts, Non-Functional Requirements (NFRs), và thiết kế liên module.
- **[PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md):** Bối cảnh tổng quan dự án, sứ mệnh sản phẩm và kiến trúc giải pháp.
- **[Frontend Architecture Plan](frontend-master-roadmap-and-architecture-plan.md):** Kiến trúc tổng thể và lộ trình công nghệ cho ứng dụng Frontend (Next.js 16, React 19, TypeScript, Tailwind CSS).

---

## 2. Quyết Định Kiến Trúc & Cẩm Nang Kỹ Thuật (ADRs & Guides)

- **[ADR-001 (Core Tech Stack)](ADR-001-tech-stack.md):** Quyết định lựa chọn công nghệ nền tảng (Java 21, Spring Boot 3, PostgreSQL, Maven, Next.js).
- **[ADR-002 (Multi-Module Architecture)](ADR-002-multi-module-architecture.md):** Quyết định ranh giới Modular Monolith, Clean Architecture, và nguyên tắc giao tiếp giữa các module qua Facade/SPI hoặc Domain Events.
- **[Backend Module Structure Guide](backend-module-structure-guide.md):** Hướng dẫn chuẩn hóa packaging và cấu trúc phân tầng (`domain`, `application`, `infrastructure`, `presentation`) cho từng module backend.

---

## 3. Lược Đồ Dữ Liệu & Hợp Đồng Giao Tiếp (Database & API Contracts)

- **[EduFit Schema DBML](edufit-schema.dbml):** Lược đồ cơ sở dữ liệu quan hệ hoàn chỉnh của hệ thống PostgreSQL (29+ bảng nghiệp vụ).
- **[Authentication API Contract](authentication-contract.md):** Hợp đồng API và luồng xác thực / phân quyền người dùng (Module IAM).
- **[Discovery API Contract](discovery-api-contract.md) & [Module Flow](discovery-module-flow.md):** Hợp đồng tìm kiếm gia sư, thuật toán matching điểm số và luồng xử lý module Discovery (kèm sơ đồ luồng tại [docs/images/](images/)).
- **[Connection API Contract](connection/connection-api-contract.md) & [Module Flow](connection/connection-module-flow.md):** Hợp đồng quản lý kết nối học tập, hồ sơ đề nghị sư phạm, và luồng kiểm soát Gatekeeper chống xung đột lịch.

---

## 4. Kế Hoạch & Tiêu Chuẩn Kiểm Thử (Testing & Quality Gates)

- **[EduFit Master Test Plan](edufit_master_test_plan.md):** Kế hoạch kiểm thử tổng thể của toàn dự án, bao quát kiểm thử đơn vị, kiểm thử tích hợp (Testcontainers), kiểm thử đồng thời (Concurrency / GiST exclusion), kiểm thử giao diện (Vitest) và bộ 19 quy tắc ArchUnit.
