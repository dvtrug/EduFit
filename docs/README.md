# Thư Mục Tài Liệu Kỹ Thuật EduFit (EduFit Documentation Index)

Thư mục này lưu trữ toàn bộ tài liệu kiến trúc, đặc tả yêu cầu, lược đồ cơ sở dữ liệu, kế hoạch hiện thực hóa từng phân hệ và kế hoạch kiểm thử cốt lõi của nền tảng **EduFit**.

---

## 1. Yêu Cầu Nghiệp Vụ & Thiết Kế Kiến Trúc Tổng Thể

- **[SRS Edufit.docx](SRS%20Edufit.docx):** Bản đặc tả yêu cầu phần mềm chính thức (Software Requirements Specification) của toàn bộ hệ thống EduFit.
- **[SADS (Software Architecture Design Specification)](software-architecture-design-specification.md):** Tài liệu kiến trúc phần mềm chi tiết, bao quát kiến trúc Pragmatic Modular Monolith, Bounded Contexts, Non-Functional Requirements (NFRs), và thiết kế ranh giới liên module.
- **[PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md):** Bối cảnh tổng quan dự án, sứ mệnh sản phẩm và định hướng kiến trúc.
- **[Frontend Architecture Plan](frontend-master-roadmap-and-architecture-plan.md):** Kiến trúc tổng thể và lộ trình công nghệ cho ứng dụng Frontend (Next.js 16, React 19, TypeScript, Tailwind CSS).
- **[Master Sprint Plan & Framework](master_sprint_plan_and_ai_learning_framework.md):** Kế hoạch Master Sprint, phân chia giai đoạn và khung làm việc kiểm thử/học tập của dự án.

---

## 2. Quyết Định Kiến Trúc & Cẩm Nang Kỹ Thuật (ADRs & Guides)

- **[ADR-001 (Core Tech Stack)](ADR-001-tech-stack.md):** Quyết định lựa chọn công nghệ nền tảng (Java 21, Spring Boot 3, PostgreSQL, Maven, Next.js).
- **[ADR-002 (Multi-Module Architecture)](ADR-002-multi-module-architecture.md):** Quyết định ranh giới Modular Monolith, Clean Architecture, và nguyên tắc giao tiếp giữa các module qua Facade/SPI hoặc Domain Events.
- **[Backend Module Structure Guide](backend-module-structure-guide.md):** Hướng dẫn chuẩn hóa packaging và cấu trúc phân tầng (`domain`, `application`, `infrastructure`, `presentation`) cho từng module backend.

---

## 3. Kế Hoạch Hiện Thực Hóa & Giải Trình Kiến Trúc Phân Hệ (Module Implementation Plans)

- **[Connection Implementation Plan](connection/connection_implementation_plan.md):** Kế hoạch hiện thực hóa và giải trình kiến trúc phân hệ Connection (Kết nối Phụ huynh - Học sinh, Yêu cầu kết nối Gia sư, Gatekeeper chống trùng lịch).
- **[Scheduling Implementation Plan](scheduling/scheduling_implementation_plan.md):** Kế hoạch hiện thực hóa và giải trình kiến trúc phân hệ Scheduling (Quản lý buổi học, Chống Race Condition/Double-Booking, Batch Operations, Đổi lịch học UC4.8).

---

## 4. Lược Đồ Cơ Sở Dữ Liệu (Database Schema)

- **[EduFit Schema DBML](edufit-schema.dbml):** Lược đồ cơ sở dữ liệu quan hệ hoàn chỉnh của hệ thống PostgreSQL (29+ bảng nghiệp vụ).

---

## 5. Kế Hoạch & Tiêu Chuẩn Kiểm Thử (Testing & Quality Gates)

- **[EduFit Master Test Plan](edufit_master_test_plan.md):** Kế hoạch kiểm thử tổng thể của toàn dự án, bao quát kiểm thử đơn vị, kiểm thử tích hợp (Testcontainers), kiểm thử đồng thời (Concurrency / GiST exclusion), kiểm thử giao diện (Vitest) và bộ 19 quy tắc ArchUnit.
- **[In-Development Testing Specification](in-development-testing-specification.md):** Đặc tả chi tiết các kịch bản kiểm thử trong quá trình phát triển (In-Development Testing Specification) cho từng phân hệ.
