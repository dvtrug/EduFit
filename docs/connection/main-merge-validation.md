# Connection — merge main mới

Ngày: 14/10/2026. Repository `C:/Users/ASUS/Desktop/EduFit/EduFit`; branch `feat/connection`.

- First parent: `e8b8b1635aef9c7505393f819ea6cbc7d92a8980` (bao gồm commit local DiscoveryQueryService của người dùng).
- Main được tích hợp: `d20837148e5e5f3b31d29afcfe2cf10f737c623f`; fetch lại trước resolve xác nhận main không đổi.
- Scope: resolve merge và thích nghi Connection với main; không triển khai Phase 3, không merge/push ngược main/Discovery.

## Conflict và cách resolve

| File | Quyết định |
| --- | --- |
| SecurityConfig | Theo main: public GET danh sách/chi tiết Discovery, SPA CSRF, session và request-cache policy của main. Bỏ rule trùng phía Connection. |
| FlywayMigrationScriptTest | Theo main:13 version và seed V1.13; giữ checksum assertions V1.09/V1.10/V1.11. |
| FlywayMigrationIT | Theo main: clean 13, upgrade từ 1.09 chạy 4, demo 9 account; giữ upgrade/idempotency/data-preservation và test quota mới. |
| ConnectionService monolith | Giữ xóa sau refactor. Không đưa lại bean facade thứ hai hoặc noRollbackFor rộng. Port phần sửa lỗi main sang persistence fragment. |

`LearningGoalPostgresTest` auto-merge nhưng còn import/call service cũ: chuyển request/revoke sang service mới, import facade implementation/lock/shared Clock và chuẩn hóa repo scan.

## Sửa lỗi cache/row lock

- Main thêm detach trước lock/reload ở respond/withdraw invitation, revoke link, cancel/respond request.
- `FreshConnectionRowRepository` được ba repository invitation/request/link dùng chung: tìm initial entity, detach riêng entity đó, reload với PESSIMISTIC_WRITE trong transaction MANDATORY.
- Không EntityManager trong application service, không clear toàn persistence context; giữ advisory lock order, Clock, terminal guards, typed result và rollback semantics Phase 2.
- RED: ba test PostgreSQL cache initial entity, để transaction khác commit terminal status/version rồi lấy row lock. Code cũ ném ObjectOptimisticLockingFailureException cả ba loại (`.codex-doc-review/connection-main-merge-red-cache.log`).
- Thêm kiểm chứng missing-row/transaction requirement và việc reload không làm mất transition ở entity khác chưa flush.

## Manifest và security hiện hành

SQL/checksum V1.01–V1.11 không bị sửa. Giữ V1.12/V1.13 nguyên bản main; không rename migration, repair/clean hoặc migrate DB local. Migration tests dùng PostgreSQL container riêng.

CSRF contract và T21 được cập nhật theo main đã bật SPA CSRF; không đánh dấu T21 hoàn tất. Production publishers vẫn chưa chuyển sang events, Phase 3 chưa bắt đầu.


## Kết quả kiểm chứng

| Lượt chạy | Kết quả |
| --- | --- |
| Focused affected-reactor verify | PASS: Connection125, Notification6, App113 tests; có cache regression3, Flyway IT3, E2E7, LearningGoal62, security7 và architecture9. Log `.codex-doc-review/connection-main-merge-focused.log`. |
| Full clean verify, `-Dtest=*Test,*IT -Dsurefire.failIfNoSpecifiedTests=false` | PASS: **656 tests, 0 failures, 0 errors, 0 skips**. App219, Connection125, Discovery114, IAM32, Notification6, Profile53, Verification16, AI63, Shared22, Storage6. Log `.codex-doc-review/connection-main-merge-full.log`. |
| Connection PostgreSQL trong full run |18 tests pass: thêm3 cache regressions và2 test giữ unrelated mutation/transaction requirement; các expiry, rollback, deferred constraint và lock tests Phase2 vẫn pass. |
| FlywayMigrationIT trong full run |3 tests pass: clean13, upgrade lịch sử main từ1.09 và quota upgrade. |
| Strict coverage dùng full-run data, `verify -DskipTests -Djacoco.haltOnFailure=true` | FAIL tại Shared; các module tiếp theo bị Maven skip. Không hạ threshold. Log `.codex-doc-review/connection-main-merge-strict.log`. |

| Module coverage | LINE | BRANCH |
| --- | --- | --- |
| Shared |78/103 =75.73% |20/28 =71.43% |
| Connection |385/494 =77.94% |99/196 =50.51% |
| Notification |17/23 =73.91% |3/4 =75.00% |

Ngưỡng vẫn80%. Module-local JaCoCo report không cộng execution data của App integration tests; fragment đã được kiểm chứng bằng PostgreSQL ở App. Full build default không đồng nghĩa strict coverage pass.

## Review và phạm vi

- Bốn conflict được resolve; không còn unmerged paths. Không đưa lại monolith, Clock bean trùng, facade thứ hai hay noRollbackFor rộng.
- Port main bugfix qua repository fragment, không thay public HTTP success shape/authorization/TTL; khóa row vẫn PESSIMISTIC_WRITE trong cùng transaction, advisory lock order giữ nguyên.
- `LearningGoalPostgresTest` được thích nghi wiring/service calls; các assertions nghiệp vụ của main vẫn giữ. Main E2E concurrent accept xác nhận đúng một class và kết quả200/409.
- SecurityConfig và hai Flyway test conflict theo main. Mọi SQL migration giữ nguyên bytes main; không sửa DB local. Contract/flow/T21 notes được cập nhật theo baseline mới.
- Commit local `e8b8b16`, task docs Discovery và các stash hiện có giữ nguyên. Main/Discovery branches không bị merge/push ngược.
- Whitespace check cho thay đổi owned pass. Full staged check có whitespace đã có nguyên bản main ở TutorBioService.java và docs/refactor/role_portal_and_features_refactor_plan.md; blob index bằng blob origin/main cho cả hai, không sửa formatting ngoài scope.
- Code review bao gồm correctness/cache/transaction, readability, module boundaries, security và lock/query behavior. Frontend incoming giữ nguyên main; không chạy browser/frontend tests trong lượt này. Backend SpringBoot E2E chạy production beans với PostgreSQL và fake external AI provider.

Đây là merge tích hợp sau Phase2, không đánh dấu Phase3/T11a/T21 hoàn tất.
