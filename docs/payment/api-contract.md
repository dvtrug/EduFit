# Payment API contract

Base: `/api/v1/payment`. Cookie `EDUFIT_SESSION`, CSRF như SDK `fetchApi`. Actor lấy từ phiên đăng nhập; VND nguyên; không lưu balance ở client.

| Method | Path | Nội dung |
|---|---|---|
| GET | /wallet?offset=0 | Wallet available/held; entries, topups, withdrawals (50/trang); capability cấu hình |
| POST | /topups | `{amount}` từ 10.000 tới 100.000.000; trả payOS checkout hoặc manual QR; chưa credit |
| GET | /sessions?from=ISO&to=ISO | Theo lớp được phép xem, khoảng tối đa 93 ngày; giá snapshot hoặc báo giá hiện hành |
| GET | /sessions/{sessionId} | Báo giá/trạng thái buổi, dùng trong chi tiết lịch học |
| POST | /sessions/{id}/fund | `{amount}` phải khớp giá được hiển thị; reserve ví actor; một payer/buổi |
| POST | /sessions/{id}/confirm | Gia sư hoặc learner hợp lệ xác nhận; chỉ COMPLETED và sau endAt |
| POST | /sessions/{id}/dispute | `{text}` tối đa 2000 ký tự; giữ tiền; deadline server mở +24h |
| POST | /sessions/{id}/report | `{amount,text}` trong 24h; số tiền 0..amount đã giữ; text tối đa 4000 |
| POST | /withdrawals | `{amount,bank,account,holder}`; TUTOR; reserve tiền và chờ Admin, một yêu cầu pending/tài khoản |
| POST | /webhooks/payos | Public, miễn CSRF chỉ URL này; bắt buộc HMAC, order/amount/account/link/currency đúng; probe ký hợp lệ không có order chỉ ACK, không credit |

Admin: `/api/v1/admin/payment`, role ADMIN bắt buộc cả HTTP security và application.

- GET `?offset=0`: nạp/rút, 50/trang.
- GET `/disputes?offset=0`: hai báo cáo thống nhất chờ duyệt.
- POST `/topups/{id}/confirm`: `{approve:true,note,reference}`. Chỉ manual, phải đối soát giao dịch thật; reference duy nhất, chuẩn hóa khoảng trắng.
- POST `/withdrawals/{id}/review`: `{approve,note,reference}`. Approve có nghĩa đã chuyển ngân hàng thật, không chỉ duyệt yêu cầu.
- POST `/disputes/{sessionId}/review`: `{approve,note}`. Duyệt đúng mức hai bên thống nhất; từ chối hoàn tất cả held; khoản tính phí >0 chỉ khi buổi COMPLETED.

## Trạng thái và thời hạn

UNFUNDED -> FUNDED -> AWAITING_CONFIRMATION -> SETTLED. Autoaccept phía learner tại endAt+1h chỉ khi COMPLETED; tutor phải xác nhận riêng. DISPUTED không autoaccept. Hai báo cáo cùng mức -> AWAITING_ADMIN (không hết hạn trong lúc Admin xử lý). DISPUTED hết 24h -> EXPIRED và trả held về payer. Admin từ chối hoặc mức thống nhất 0 -> REFUNDED.

Mọi settlement ghi payer release, tutor income, commission và audit cùng transaction. 10% tính trên settledAmount, làm tròn xuống VND. Parent khác payer chỉ được đọc nếu còn link, không xác nhận tài chính. Student của lớp có thể xác nhận/report dù Parent trả. Parent revoked mất quyền xem buổi; entry ví cá nhân vẫn giữ nhưng chỉ có reference ID, không kèm dữ liệu học sinh.

## Lịch sử dữ liệu

V1.16 thêm payment_wallet, payment_charge, payment_topup, payment_withdrawal, payment_entry, payment_audit và payment_mutex. File SQL mới là schema thực thi; không sửa V1.01–V1.15. `payment_entry` là sổ bất biến; trạng thái payment_charge là aggregate điều phối tất toán. Refund tiền giữ là trả số dư nội bộ, không tự hoàn chuyển khoản ra ngân hàng.


## payOS completion V1.17

- `GET /api/v1/admin/payment/configuration`: Admin-only readiness booleans + frontendUrl/webhookPath, no secrets. Admin overview also includes `configuration`.
- `POST /api/v1/payment/topups/{id}/refresh`: owner/Admin; query signed provider status outside DB transaction, rate-limited by persisted 30s lease, exact order/amount/account/link/reference before idempotent credit. Return current topup. No front-end success flag accepted.
- Withdrawal input adds optional `bankBin` matching six digits; current UI always sends selected bank BIN. Legacy clients/rows without BIN remain manual only.
- `POST /api/v1/admin/payment/withdrawals/{id}/payout` body `{approve:true,note}`: Admin-only, requires payout-enabled/credentials and BIN; persists PROCESSING before HTTP, same withdrawal UUID used for reference/idempotency; repeated approval never creates another external request.
- `POST /api/v1/admin/payment/withdrawals/{id}/refresh`: Admin-only reconciliation; 60s lease shared by API and job. Job queries only, never reposts transfer after timeout.
- Withdrawal views add `bank_bin`, `provider_id`, `provider_state`, `provider_message`. Status adds PROCESSING and FAILED. Manual review is blocked after PROCESSING; verified terminal failure releases funds, uncertain status retains held.
- Payment notifications use shared sink, type prefix PAYMENT_, target PAYMENT_WALLET; transaction failure rolls back both wallet and notification. No notification module internals imported.

See `payos-setup.md` for kênh thu/kênh chi configuration and handling ambiguous outcomes. Provider request/response signatures follow official payOS Java SDK (BODY raw query vs HEADER URI-encoded deep-sorted data).
