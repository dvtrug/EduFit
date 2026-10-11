# Connection — Báo cáo Phase 1

Workspace: `C:/Users/ASUS/Desktop/EduFit/EduFit`, branch `feat/connection`; base `cd75bae`. Chỉ commit/push Connection, không đổi refs main/Discovery. Evidence logs: `.codex-doc-review/connection-phase1/` (ignored). JDK21, Maven wrapper, Windows-ROOT trust store, PostgreSQL17 Testcontainers.

**Sau checkpoint:** [Hotfix Flyway](flyway-fix-validation.md) thay đổi manifest để hỗ trợ DB local main đã seed V1.09. Các kết quả Phase1 dưới đây là snapshot trước hotfix.

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

## T07 — Request / scoped queries

- T06 commit/push `0b6e81c`. ConnectionRequestService sở hữu create/cancel/respond; ConnectionQueryService sở hữu invitation/link/request lists và Tutor inbox metadata. TutorInboxResult là application DTO theo whitelist; controller chỉ lấy actor, validate và map HTTP DTO, không còn IAM/Profile/persistence dependency.
- Xóa ConnectionService; đổi expected class duy nhất trong ArchitectureBoundaryTest sang ConnectionFacadeImpl cùng task, không đổi các rule/assertions Discovery khác. Scan backend source (loại target) không còn caller/service cũ. ConnectionUseCaseTest giữ đủ9 regressions từ service cũ.
- So source:3 request writer bodies/transaction annotations và2 invitation/link query bodies/annotations không đổi. Tutor metadata chuyển vào query transaction theo yêu cầu; nếu mapping metadata thất bại thì expiry dirty-check cùng transaction cũng rollback. Giữ direct notification/audit và noRollbackFor hiện hữu cho các task sau, chưa triển khai events/Clock/guards/batch/paging/security changes.
- RED thiếu ConnectionQueryService (`t07-red.log`); GREEN module Connection 55 tests pass (HTTP17, facade9, use cases9, query13, quota5, lock2), upstream affected modules cùng reactor pass (`t07-unit.log`). Query13 xác minh Student bỏ scope ngoại lai, Parent confirmed/revoked/missing scope, role guards, Tutor owns inbox, expiry/legacy slots/sender fallback và invitation/link scopes.
- PostgreSQL fixture bổ sung accept request → class: notification failure rollback request về PENDING và không có class; retry thành công tạo đúng1 ACTIVE class. Callback metadata/sink assert transaction active. Đây là atomicity smoke, không thay toàn bộ race/failure matrix T22.

## Review / giới hạn còn lại

- Persistence class bodies/mappings và migration bytes không đổi trong Phase1. Public facade signatures giữ nguyên; controller/application DTO tách biệt, native SQL chỉ trong lock adapter. Service constructor/context wiring được PostgreSQL fixture component scan kiểm chứng.
- Tests HTTP ở module dùng standalone MockMvc; security filter chain/CSRF và full race matrix chưa được triển khai, thuộc T21/T22. Baseline slot strings được giữ; không tự bật strict parser.
- Coverage chưa đạt 80%: báo cáo Connection sau clean run có LINE311/414 (75.12%), BRANCH84/176 (47.73%). Baseline Phase0 `t00b-verify.log` đã cảnh báo Connection0.41/0.22; coverage được cải thiện nhưng vẫn là gap cần bổ sung trong các task tests sau. Shared LINE78/103 (75.73%), BRANCH20/28 (71.43%) giữ gap baseline. Không hạ gate, không gọi BUILD SUCCESS mặc định là strict coverage pass.

## Checkpoint 1 — kết quả cuối

- Clean toàn reactor: `backend/mvnw.cmd -f backend/pom.xml clean verify -Dtest=*,FlywayMigrationIT -Dsurefire.failIfNoSpecifiedTests=false -B -ntp -Djavax.net.ssl.trustStoreType=Windows-ROOT`. Log `checkpoint-clean-verify.log`: **348 tests, failures0/errors0/skipped0, BUILD SUCCESS**, 3:52 phút. Tổng đối chiếu từ XML Surefire sau clean: Shared22, AI12, Storage6, IAM32, Profile44, Verification16, Connection 55, Discovery104, App 57.
- PostgreSQL thật: ConnectionTransaction5, MatchingWorkflow9, DiscoveryRead22, MatchingCandidates6 (Profile) và FlywayMigrationIT2 clean/upgrade đều không skip; architecture9 pass. Full clean loại compiled ConnectionService/old test và persistence packages cũ khỏi target.
- Strict: `backend/mvnw.cmd -f backend/pom.xml verify -DskipTests -Djacoco.haltOnFailure=true -B -ntp -Djavax.net.ssl.trustStoreType=Windows-ROOT`, tái sử dụng execution data từ 348 tests vừa chạy, không gọi đây là một lượt chạy tests khác. Log `checkpoint-strict-verify.log`: **BUILD FAILURE**, dừng tại Shared LINE75.73%/BRANCH71.43%; các module sau bị skip ở lượt strict. Connection counters ở mục trên là report module từ full run, không phải một strict pass.
- Scope review: không đổi migrations, Discovery production source hay API ConnectionFacade. Discovery tasks/plan.md/tasks/todo.md khớp bytes `dd9068e`. Local main `f077e55` và feat/discovery `dd9068e` giữ nguyên; WIP stash `6f2a3f29f0cfd5e979a4d8813b2a6df8581e86ee` vẫn còn, không apply/drop. Chỉ push refs/heads/feat/connection.
- T02–T06 commits đã push: `e08e96c`, `09d62da`, `c558b9e`, `71a08ab`, `0b6e81c`; T07 là commit chứa báo cáo này với message `refactor(connection): split request and scoped query services`.
- Kết luận phạm vi: hoàn tất các task cấu trúc T02–T07 và regression checkpoint; **strict coverage quality gate còn FAIL**, không hạ ngưỡng hay đánh dấu pass. Chưa bắt đầu Phase2/T08a hoặc các contract đích của phase sau.
