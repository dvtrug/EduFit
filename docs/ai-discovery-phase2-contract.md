# AI + Discovery — Phase 2

Hoàn tất 2026-10-12. Nhánh thực thi: `feat/discovery-ai`, base `origin/main@24dc2f1`. Phạm vi B05–B07 trong [plan](../tasks/ai-discovery/plan.md); frontend và main không thay đổi. Tiếp nối [Phase 1](ai-discovery-backend-contract.md).

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

## B06 — Atomic reservation implementation

`AiMatchQuotaService` chạy REQUIRES_NEW / READ_COMMITTED. Repository upsert actor gate, khóa dòng actor FOR UPDATE, expire lease cũ, count mọi trạng thái với `reserved_at > now - 24h`, rồi insert nếu chưa đủ 10. Quyết định chỉ được trả về sau commit. Không có HTTP trong transaction này. Actor gate được PostgreSQL khóa xuyên process; xem [PostgreSQL row locks](https://www.postgresql.org/docs/current/explicit-locking.html).

UTC Clock được inject (`aiQuotaClock`); timestamp chuẩn microsecond của PostgreSQL. Mốc đúng 24h bị loại khỏi cửa sổ; lỗi/timeout/SUCCESS/EXPIRED và reservation đang chạy đều tính theo thời điểm reserve, không theo lúc kết thúc. Timestamp tương lai do clock dịch ngược được tính bảo thủ để fail-closed. Lease là provider timeout cấu hình + 30s (mặc định 45s). Lazy expiry khi actor reserve lần tiếp theo hoặc khi finish: không cần tiến trình cũ sống, không xóa dữ liệu hoặc hoàn lượt. Trạng thái terminal ghi một lần; late completion không thay EXPIRED.

Reservation lỗi tại insert/commit -> rollback, safe AiUnavailableException, không cho phép gọi provider. Finish bị lỗi DB -> cảnh báo kỹ thuật và false; reservation vẫn tồn tại/đếm quota, lease sẽ hết hạn. PostgreSQL lock timeout 2s, statement timeout 3s và transaction timeout 5s giới hạn contention; thời gian lấy connection từ pool do cấu hình pool quản lý. Chưa coi đây là deadline tuyệt đối của HTTP request.

`AiQuotaPostgresTest` kiểm tra race khi đang có 9 lượt, race khởi tạo gate với 12 callers, exact 24h, actor Student/Parent riêng, outer rollback/service restart, lease expiry/late result, lỗi insert/deferred commit và store unavailable. B07 mới bỏ precheck Discovery và bắt buộc reserve trong gateway.

## B07 — Gateway admission và deadline

`AiGatewayService.complete` bắt buộc reserve trước mọi MATCH_EXPLANATION call, kể cả caller gọi thẳng Java gateway. Quota exhausted/unavailable -> safe AiUnavailableException trước provider, không có terminal audit mới cho request bị từ chối. Discovery chỉ gửi AiRequest với actor đã được matching xác thực; không giữ precheck AiUsageQuery hoặc đường complete bỏ qua quota. TUTOR_BIO không nhận BR-25. AiUsageQuery vẫn là thống kê log hoàn tất, không dùng để quyết định matching admission.

Mỗi attempt được nhận có một outcome SUCCESS/ERROR/TIMEOUT. Finally ghi terminal reservation rồi audit qua các transaction độc lập; lỗi một store không hoàn quota hoặc tạo ERROR audit thứ hai. Nếu toàn backend crash trước finish, reservation vẫn đếm và lease hết hạn. Khi DB audit lỗi nhưng quota store còn hoạt động, lần gọi vẫn có kết quả và lượt vẫn bị tiêu. Các store này không phải transaction phân tán với HTTP; không bảo đảm có audit row nếu DB không ghi được.

Deadline provider mặc định 15s bắt đầu sau admission và giới hạn toàn Future chờ HTTP, gồm response trickle. Không phải cam kết tổng HTTP matching <=15s: quota acquisition, outer matching transaction, pool và ghi outcome/audit có overhead riêng. Hủy worker best-effort; kết quả muộn không được thread gọi lấy để thay TIMEOUT hoặc ghi log thêm. Production quota clock/default lease và gateway timeout lấy cùng AiProperties.

`AiMatchingPostgresTest` dùng scorer/ranking và matching transaction thật, repositories/audit/quota thật, Gemini REST adapter tới HTTP server loopback. ProfileFacade được mock để cấp hai tutor/goal fixture; không nhận đây là full-filter/session hoặc toàn luồng Profile HTTP. Test so sánh toàn MatchScore/thứ tự trước/sau hết quota, lần 10/11, gateway direct-call, concurrent matching khi HTTP chưa trả, quota-store unavailable, delayed 15s, trickle 600ms, I/O và HTTP 500. Không gọi Google hoặc cần key thật.
