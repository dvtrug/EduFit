# AI + Discovery — Phase 2

Nhánh thực thi: `feat/discovery-ai`, base `origin/main@24dc2f1`. Phạm vi B05–B07 trong [plan](../tasks/ai-discovery/plan.md); frontend và main không thay đổi. Tiếp nối [Phase 1](ai-discovery-backend-contract.md).

## B05 — Audit failure isolation

`AiRequestLogger` dùng `TransactionTemplate` REQUIRES_NEW; try/catch bao quanh execute, gồm cả acquisition, flush và commit. Chỉ log “committed” sau execute thành công. Khi DB lỗi, ghi cảnh báo kỹ thuật không có prompt/output/raw exception và không thử ghi ERROR thứ hai cho cùng attempt. Provider success vẫn trả kết quả; provider error/timeout vẫn giữ loại lỗi gốc để Discovery fallback.

Transaction nghiệp vụ bị rollback không xóa log provider đã commit. Usage read chạy REQUIRES_NEW read-only; khi store lỗi, Discovery bỏ gọi provider, trả rule-based và transaction nghiệp vụ còn dùng được. Đây là cơ chế fail-closed trước B07; usage count không phải quota atomic.

Best-effort audit không bảo đảm lưu log nếu DB hỏng. Không nhận thành công về persistence khi commit lỗi. Database/pool overhead nằm ngoài provider deadline 15s.

Thiết kế dựa trên [Spring TransactionTemplate](https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html). Test `AiLogPostgresTest` dùng PostgreSQL thật với deferred constraint trigger để lỗi tại commit; trigger đếm insert dùng sequence không rollback để chứng minh không có lần ghi ERROR thứ hai. Docker không khả dụng làm test fail, không skip.

Evidence/lệnh test và trạng thái các task: [checklist](../tasks/ai-discovery/todo.md).

## B06 — Persistence contract

Migration mới `V1.12` tạo `ai_match_quota_actor` (khóa theo actor) và `ai_match_reservation`. Chỉ lưu actor, reservation ID, thời điểm/lease và trạng thái; không lưu prompt/output. Reservation MATCH_EXPLANATION có các trạng thái RESERVED/SUCCESS/ERROR/TIMEOUT/EXPIRED. TUTOR_BIO tiếp tục độc lập, không bị áp BR-25.

Upgrade backfill mọi log MATCH_EXPLANATION cũ thành reservation terminal, giữ log ID/time/status. Audit log giữ nguyên; quota sẽ chỉ count reservation, không cộng hai bảng. Student/Parent dùng actor user ID riêng. Lease expiry không hoàn lại quota; mọi reservation vẫn tính trong rolling 24h. Migration cũ không bị sửa.

Java port `AiMatchQuota.tryReserve(actorUserId)` trả ID nếu commit reservation thành công, empty nếu hết quota; store lỗi phải throw AiUnavailableException. Không dùng precheck count rồi call. Commit persistence contract mới chỉ chuẩn bị schema/API; B06 implementation và B07 wiring mới kích hoạt admission runtime.
