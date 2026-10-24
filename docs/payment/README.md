# Payment — tài liệu ví, học phí và chi trả

Cập nhật 24/10/2026 trên `feat/payment`, đối chiếu code `3966d54`. Đọc theo thứ tự:

| Tài liệu | Dùng khi nào? |
|---|---|
| [Giải trình kiến trúc và cơ chế](payment_implementation_plan.md) | Nghiệp vụ, tiền, module, file, schema và Q&A |
| [Task chi tiết từng phase](../../tasks/payment/plan.md) | Phạm vi, phụ thuộc, AC, commit/checkpoint |
| [Checklist](../../tasks/payment/todo.md) | Trạng thái code và việc vận hành còn mở |
| [API contract](api-contract.md) | FE/BE, phân quyền, validation |
| [Cấu hình payOS](payos-setup.md) | Env thu/chi, webhook, restart |
| [Runbook](runbook.md) | Thao tác và đối soát sự cố |
| [Báo cáo kiểm chứng](verification-report.md) | Bằng chứng thực chạy và giới hạn |

Nạp trước, giữ tiền theo buổi, hai phía xác nhận; auto learner sau 1h khi COMPLETED; tranh chấp 24h; báo cáo khớp chờ Admin; hoa hồng 10% phần thực tất toán. Thu nhập vào ví tutor trước, rút ngân hàng là workflow riêng.

Code tích hợp đã có; khóa, ngân hàng, public webhook và giao dịch live còn cần cấu hình/kiểm chứng. Không ghi secret vào docs/Git.
