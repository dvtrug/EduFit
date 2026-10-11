# Connection — Báo cáo Phase 1

Workspace: `C:/Users/ASUS/Desktop/EduFit/EduFit`, branch `feat/connection`; base `cd75bae`. Chỉ commit/push Connection, không đổi refs main/Discovery. Evidence logs: `.codex-doc-review/connection-phase1/` (ignored). JDK21, Maven wrapper, Windows-ROOT trust store, PostgreSQL17 Testcontainers.

## T02 — Normalize persistence packages

- Chuyển 5 entities/5 repositories sang `infra/persistence/entity` và `repository`; giữ nguyên cả 10 class bodies sau khi loại package/import, xác minh bằng so sánh source với HEAD trước task. Không thay SQL/checksum/schema.
- Cập nhật imports và entity/repository scans của MatchingWorkflowPostgresTest cùng commit.
- Reactor verify: `-pl app -am verify -Dtest=ConnectionServiceTest,MatchingWorkflowPostgresTest,ArchitectureTest,ArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false`. Connection13 + MatchingWorkflow9 + Architecture9 =31 tests, 0 fail/error/skip; PostgreSQL thực có Hibernate validate và FK/rollback assertions. Log `t02-verify.log`.
- JaCoCo default upstream có warnings baseline như Phase0, không chứng minh strict80% đạt. Checkpoint cuối sẽ clean verify toàn reactor để loại target classes cũ sau package move.

## T03 — Public facade implementation

- T02 commit/push `e08e96c`. ConnectionFacade signatures/nested ActiveClassView giữ nguyên; đúng một ConnectionFacadeImpl read-only dùng 2 repositories. Service dùng facade qua injection, không tự gọi public facade method trên chính nó.
- Discovery PostgreSQL fixture @Import chuyển cùng task. Chuyển 5 confirmed-link cases sang facade suite; 8 service scenarios vẫn còn.
- RED: facade test compile fail do implementation chưa tồn tại (`t03-red.log`). GREEN: clean reactor verify selector ConnectionServiceTest/ConnectionFacadeImplTest/MatchingWorkflowPostgresTest/ArchitectureTest/ArchitectureBoundaryTest, **35 tests**, 0 fail/error/skip. Facade9 gồm confirmed/revoked/missing, active/ended/missing và context đúng một facade bean. PostgreSQL workflow9 pass, architecture9 pass (`t03-verify.log`).

## T04 — Application / HTTP DTO boundary

- T03 commit/push `09d62da`. Extract 4 application result records, 5 request records và 5 response records. Service map entities sang result trong transaction; web không import persistence, application không import web. Giữ toàn bộ field names/types cũ, 200/list/null behavior; chưa thêm deadline/consent/page fields của phase sau.
- Cả hai accept DTO dùng Boolean + NotNull. RED đầu tiên fail vì Jackson3 baseline báo HttpMessageNotReadableException cho primitive missing/null, không phải Bean Validation; evidence không chứng minh HTTP200/auto-decline trên runtime này. GREEN xác minh đúng MethodArgumentNotValidException, HTTP400, không gọi service với omitted/null accept; explicit false vẫn decline/reject.
- Module reactor verify selector `Connection*Test`: service8 + facade9 + HTTP17 =34 tests, 0 fail/error/skip (`t04-verify.log`). HTTP test gồm đủ10routes, accept/decline/class/null, exact data schemas và Tutor whitelist (không email/name/IDs nội bộ). Đây là standalone MockMvc, chưa thay thế security-filter E2E T21.
- Default coverage report vẫn warnings, không đánh dấu strict80% pass. Import cleanup chỉ loại unused imports, không thay behavior đã test.

## T05 — Transactional locking / invitation quota

- T04 commit/push `c558b9e`. Port ConnectionLock và PostgresConnectionLock giữ SQL/key namespace cũ, canonical UUID pair, propagation MANDATORY và guard joined EntityManager transaction. Quota service khóa actor trước đọc/ghi, giữ timestamp sau khóa và trả Consumption để invitation dùng đúng timestamp đó; 10 attempts/window cố định, unknown email vẫn tính.
- Accept invitation resolve Student scope trước khóa, student -> pair -> row; reload/revalidate scope sau khóa. Nếu profile mất/đổi thì không tạo link trong scope chưa khóa. Decline/withdraw chỉ pair -> row; request/revoke student -> row. Không có pair -> student path.
- RED missing adapter/service types (`t05-red.log`). GREEN 42 unit/HTTP tests pass (`t05-verify.log`), gồm key namespaces/direction, no transaction guard, quota10/11/reset ±1ns và service lock order.
- PostgreSQL fixture lúc đầu bị ambiguity EntityManager field, đã chuyển field sang PersistenceContext. Focused reactor verify (`t05-postgres-verify.log`):2 tests pass không skip; proxy lock/quota ngoài transaction bị từ chối; hai transactions opposite pair có SQLSTATE55P03 lock timeout thật, transaction kết thúc thì lấy lại lock được. Tổng verification44 tests pass qua hai lần chạy; không gọi lần đầu full command là pass.

## T06 — Invitation / parent link services

- T05 commit/push `71a08ab`. Tách LinkInvitationService và ParentStudentLinkService; controller gọi các bean độc lập, service cũ chỉ giữ request. So sánh source xác nhận cả 6 operation bodies và transaction annotations giống T05; giữ noRollbackFor hiện hữu đến Phase2.
- Reactor verify selector `Connection*Test,MatchingWorkflowPostgresTest,ArchitectureTest,ArchitectureBoundaryTest`: **57 tests**, 0 failure/error/skip, BUILD SUCCESS (`t06-verify.log`). HTTP17 và 9 use-case regressions vẫn pass; Discovery workflow PostgreSQL9 và architecture9 pass.
- Đổi tên PostgreSQL fixture thành ConnectionTransactionPostgresTest, tổng4 tests: quota commit qua invitation entry point thực; revoke rollback khi audit lỗi rồi commit khi retry thành công; assertions sink nằm trong transaction thật; giữ2 lock tests của T05. Context component scan wire các constructor/service thật.
- Review: dependency qua facade/port, DTO boundary giữ nguyên, không SQL/schema/security/Discovery production changes. Quota → pair và student → pair → row giữ nguyên. Không kết luận strict coverage pass từ default verify.
