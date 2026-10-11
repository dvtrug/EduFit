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
