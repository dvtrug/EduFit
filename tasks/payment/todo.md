# Checklist triển khai Payment

Mỗi task hoàn thành: cập nhật bằng chứng, commit riêng theo phần, push `feat/payment`.

- [x] P00: audit develop, chốt quy tắc và ghi plan. Kiểm chứng: working tree sạch trước triển khai; hoa hồng được user chọn 10%.
- [x] P01: module, state machine thu phí/tranh chấp và tests. AC: 1h chỉ auto phía learner, 24h trả held nếu chưa đồng thuận; hai báo cáo khớp chờ Admin.
- [x] P02: migration, ví/sổ, cấp tiền, xác nhận/tất toán và scheduler. AC: transaction/idempotence/authorization; hoàn đúng payer; migration mới không đổi history.
- [ ] P03: nạp QR/payOS webhook và manual đối soát; rút tiền/Admin. AC: không tự credit, chữ ký/amount/reference đúng, xử lý trùng; Admin review có mã giao dịch và audit.
- [ ] P04: SDK và FE Student/Parent/Tutor. AC: nạp, sổ, cấp tiền, confirm, dispute/report, rút; error/empty/pending và không dữ liệu giả.
- [ ] P05: FE Admin và tích hợp navigation/dashboard. AC: duyệt hai báo cáo, nạp/rút; bỏ số liệu thanh toán giả, badge sắp ra mắt.
- [ ] P06: kiểm thử tích hợp, security review, lint/type-check/build, tài liệu vận hành và báo cáo. AC: ghi rõ kết quả thực chạy và cấu hình cần có.

## Commit / push

Ghi SHA và kiểm chứng sau mỗi phần tại `docs/payment/verification-report.md`.
