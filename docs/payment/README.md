# Payment — tài liệu ví, học phí và chi trả

> Cập nhật 25/10/2026: [quy tắc 24h xác nhận / 48h giải quyết và tài khoản nhận tiền đã lưu](payment-policy-update.md) là nguồn hiện hành. Nội dung 1h/24h tranh chấp ở bằng chứng/phase cũ là lịch sử.

Cập nhật 24/10/2026 trên `feat/payment`, gồm bản sửa tài khoản ảo V1.18; phạm vi biên dịch/kiểm thử ghi tại báo cáo. Đọc theo thứ tự:

| Tài liệu | Dùng khi nào? |
|---|---|
| [Luồng, rule và cơ chế Payment — có ảnh diagram](payment-flow-and-rules.md) | Đọc từ góc nhìn người dùng: 7 ảnh, từng bước xử lý, 30 rule và ví dụ tiền trước/sau |
| [Giải trình kiến trúc và cơ chế](payment_implementation_plan.md) | Nghiệp vụ, tiền, module, file, schema và Q&A |
| [Task chi tiết từng phase](../../tasks/payment/plan.md) | Phạm vi, phụ thuộc, AC, commit/checkpoint |
| [Checklist](../../tasks/payment/todo.md) | Trạng thái code và việc vận hành còn mở |
| [API contract](api-contract.md) | FE/BE, phân quyền, validation |
| [Cấu hình payOS](payos-setup.md) | Env thu/chi, webhook, restart |
| [Runbook](runbook.md) | Thao tác và đối soát sự cố |
| [Báo cáo kiểm chứng](verification-report.md) | Bằng chứng thực chạy và giới hạn |

Nạp trước, giữ tiền theo buổi, hai phía xác nhận; tự tất toán sau 24h không khiếu nại khi COMPLETED; giải quyết khiếu nại 48h; báo cáo khớp chờ Admin; hoa hồng 10% phần thực tất toán. Thu nhập vào ví tutor trước, rút ngân hàng là workflow riêng.

Code tích hợp đã có; khóa, ngân hàng, public webhook và giao dịch live còn cần cấu hình/kiểm chứng. Không ghi secret vào docs/Git.
