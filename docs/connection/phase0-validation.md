# Connection — Báo cáo Phase 0

Ngày thực hiện: 11/10/2026 (Asia/Saigon). Workspace duy nhất: `C:/Users/ASUS/Desktop/EduFit/EduFit`; branch `feat/connection`. Checklist: [tasks](../../tasks/connection/todo.md), [plan](../../tasks/connection/plan.md).

## T00 — Bảo toàn dữ liệu và baseline

- Base Connection: `43e7461161404bcbf2fac8ca83bd6eda4a4b8115`.
- Discovery ref giữ nguyên `dd9068e0a14b0e480d1823eab37bc82041166fe5`; main local giữ nguyên `f077e55bae2c4ff1bc74f29aee30610e31c5a53b`.
- Stash Discovery WIP: `6f2a3f29f0cfd5e979a4d8813b2a6df8581e86ee`. Chỉ 3 file Discovery/IAM đang dở, không có docs Connection. Git binary diff trước stash và diff từ stash có SHA256 giống nhau `DEFFE85B00477A595D949D36D67E04E2F349CE7E928043C4D75E11804B43D38E`.
- Backup patch, hashes các file nguyên bản và stash IDs ở `.codex-doc-review/connection-phase0/` (ignored). Không drop/apply stash vào Connection. Bản stash docs riêng bảo toàn trạng thái di chuyển trước switch; docs đã đưa về namespace Connection, không restore nguyên README từ branch Discovery vào base Connection.
- Checkout cũ EduFit-connection đã detached ở `43e7461`, dữ liệu vẫn giữ. Không xóa worktree, không cùng branch ở hai folder.
- Runtime: Java 21.0.10, Maven wrapper 3.9.16; process-local JAVA_HOME, không thay cấu hình máy. HTTPS dùng kho certificate Windows bằng `-Djavax.net.ssl.trustStoreType=Windows-ROOT`, không disable TLS validation.

Lệnh từ `backend/`: `./mvnw.cmd -pl modules/connection -am test -B -ntp -Djavax.net.ssl.trustStoreType=Windows-ROOT`.

| Module | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Shared | 22 | 0 | 0 | 0 |
| AI | 10 | 0 | 0 | 0 |
| IAM | 32 | 0 | 0 | 0 |
| Profile | 26 | 0 | 0 | 0 |
| Connection | 8 | 0 | 0 | 0 |
| Tổng | 98 | 0 | 0 | 0 |

BUILD SUCCESS, 40.301s. Nguồn: `.codex-doc-review/connection-phase0/t00-baseline-unsandboxed.log`, hoàn thành 10:30:51 giờ Việt Nam. Không cộng stale XML `AiDeadlineTest`/`MatchingCandidatesPostgresTest` từ lần chạy Discovery trước đó.

Lần chạy trong sandbox bị Mockito attach pipe lỗi và treo; đã dừng, chạy lại ngoài sandbox. Chưa có PostgreSQL suite ở baseline T00; Docker đang được khởi động để kiểm chứng T00b.

## Cập nhật refs khi bắt đầu thực thi

Fetch thành công: main hiện là `6755121122c79dc9705282225d14ffb76e17c29a`, đã merge frontend-auth PR14. Vì vậy audit main `4eaa333` trong plan là snapshot lịch sử, không còn là tip dùng cho T00a.

- Merge-tree `feat/connection + origin/main` mới báo conflict ở `backend/app/src/test/java/vn/edufit/db/FlywayMigrationScriptTest.java`. Ngoài conflict văn bản, phát hiện hai migration cùng V1.09: Connection relink và main seed demo. Giữ nguyên Connection V1.09; chuyển seed mới nhập sang V1.10, giữ nguyên bytes SQL. Manifest kiểm tra 10 migration và uniqueness version xuyên module.
- Upstream JaCoCo vẫn LINE/BRANCH 80% nhưng `jacoco.haltOnFailure=false`. T00a giữ upstream và chạy thêm `verify -Djacoco.haltOnFailure=true` để báo gap thật; không gọi mặc định BUILD SUCCESS là coverage đạt 80%.
- T00b vẫn dùng Discovery `dd9068e`, giữ APIs/tests đầy đủ khi resolution. Các refs nguồn không bị merge/push ngược.

## T00a — Tích hợp main

T00 đã commit/push `1fcad8f13ffee85aa64ec6075fcf512bd128fd04`. Merge nguồn main `6755121122c79dc9705282225d14ffb76e17c29a` chỉ vào Connection; frontend-auth là delta upstream, không viết thêm frontend.

- Clean full reactor: `./mvnw.cmd clean verify -B -ntp -Dtest=*,FlywayMigrationIT -Dsurefire.failIfNoSpecifiedTests=false -Djavax.net.ssl.trustStoreType=Windows-ROOT`. BUILD SUCCESS, 133 tests, 0 failure/error/skip, 2:03 phút. Log `t00a-verify.log` trong thư mục evidence ignored.
- Sau thêm upgrade assertion: `./mvnw.cmd -pl app -am test -B -ntp -Dtest=FlywayMigrationIT,FlywayMigrationScriptTest -Dsurefire.failIfNoSpecifiedTests=false -Djavax.net.ssl.trustStoreType=Windows-ROOT`. 4 tests pass, không skip: clean PostgreSQL 17, upgrade target 1.09 -> 1.10, validate, rerun no-op, manifest/unique versions. Log `t00a-migration-upgrade.log`.
- Strict: `./mvnw.cmd verify -B -ntp -Djacoco.haltOnFailure=true -Djavax.net.ssl.trustStoreType=Windows-ROOT`. FAIL ở Shared, LINE 75%/BRANCH 71%; các module sau không được strict-verify. Đây là baseline, không đổi code Shared. Log `t00a-strict-verify.log`.
- Default verify còn warnings: AI 34%/14%, Storage BRANCH 64%, IAM 54%/49%, Profile 67%/58%, Connection 41%/22%, App 26%/41% (LINE/BRANCH). Modules không có execution data bị JaCoCo skip, không coi là coverage đạt.
- `git diff --cached --check` ở T00a có whitespace warnings nhập nguyên từ main (`docs/refactor/auth_refactor_plan.md`, EOF `frontend/src/lib/api.ts`). Các file resolution/Connection không có warning; không trộn sửa format upstream vào task integration.

### Migration lineage phải kiểm tra trước rollout

SQL seed chuyển V1.09 -> V1.10 có Git blob `9e3b3d94df95e90b5814d521a49fe5a597f7a95f`, trùng bytes upstream. Connection V1.09 vẫn blob `cca5b0a4d145cd5bda22adbe7c755573836b9d35`.

Clean DB và Connection-history V1.09 đã được kiểm chứng. Database đã chạy **seed main V1.09** có history khác: không thể dùng thẳng manifest Connection này mà tự động validate. Trước dùng nhánh Connection trên DB đó phải đối chiếu script/checksum/history, backup và lập migration reconciliation riêng; không dùng `repair`, đổi history hoặc clean DB thật trong Phase 0. Không có database người dùng nào bị thay đổi; chỉ Testcontainers cô lập.

## T00b — Tích hợp Discovery

T00a đã commit/push `e5871742bdcee9a024db29b8191f4fe9ab6a27b7`. Nguồn Discovery `dd9068e0a14b0e480d1823eab37bc82041166fe5`.

Base main mới làm xuất hiện 6 conflicts thay vì 3 trong audit ban đầu:

1. SecurityConfig: union role matchers của main và public GET tutor search/detail của Discovery; authentication fallback giữ nguyên.
2. FlywayMigrationIT: giữ manifest 10 file, 4 demo accounts và upgrade assertion T00a.
3. FlywayMigrationScriptTest: giữ Connection V1.09, seed V1.10 và kiểm tra version duy nhất.
4. ConnectionServiceTest: giữ 8 tests ban đầu + 5 invocations confirmed-link authorization của Discovery.
5. ProfileFacadeImpl: giữ constructor 8 arguments/candidate cap/catalog/bounded queries của Discovery; giữ `findGoalForConnection` và `tutorTeaches` của Connection.
6. ProfileFacadeImplTest: giữ tests/mocks và constructor Discovery, bỏ imports trùng do merge.

`tasks/plan.md` và `tasks/todo.md` giữ nguyên bytes từ Discovery. README docs chỉ đổi link flow Connection vào namespace mới ở T00b. Ba đường dẫn code từng có WIP hiện khớp source committed Discovery; thay đổi đang dở vẫn nằm nguyên trong stash riêng, không đưa dirty formatting vào nhánh Connection.

Lưu ý baseline SecurityConfig main miễn CSRF toàn `/api/v1/**`, trong khi Discovery cũ chỉ miễn auth. T00b giữ chính sách upstream để integration không âm thầm đổi auth; T01 ghi rõ delta và task khôi phục bảo vệ riêng Connection trước checkpoint security.

Verification: clean full reactor cùng lệnh T00a, log `t00b-verify.log`: **301 tests**, failures/errors/skipped đều0, BUILD SUCCESS, 3:58 phút. Shared22, AI12, Storage6, IAM32, Profile44, Verification16, Connection13, Discovery104, App52. PostgreSQL: MatchingWorkflow9, DiscoveryRead22, MatchingCandidates6, FlywayMigrationIT2 (clean+upgrade) đều pass không skip; Flyway script2, ArchitectureBoundary6, Architecture3 pass. Các suite App đã chạy qua selector `*,FlywayMigrationIT`, không dựa assumption `*IT` mặc định.

Strict full verify `-Djacoco.haltOnFailure=true` vẫn fail tại Shared75%/71%, các module sau chưa strict pass; `t00b-strict-verify.log`. T00b `git diff --cached --check` sạch. Các failures coverage của T00a không được gọi là regression do resolution.

## T01 — Chốt contract, chưa refactor runtime

T00b đã commit/push `12c1e9b025cb049790a9db2d8021c187ab59dbf6`; main và Discovery đều là ancestors của Connection. [Contract](connection-api-contract.md) ghi đủ 10 routes cũ, schemas/role scopes/error envelope, mandatory accept, slot grammar/legacy rollout, consent/cooldown, system closure, inbox deadline/privacy và routes page/Admin mới. T18c/T20/T21 được cập nhật cho các thiếu sót tìm thấy khi đối chiếu SRS/main mới.

Source inventory slots chỉ là fixture `Mon 18:00`/`Tue 19:00`; không có Connection API writer trong frontend hiện tại. Chưa inventory DB thật; strict parser activation và client CSRF token là điều kiện rollout ghi rõ, không tự bật tại Phase 0.

Verification T01: parse thành công 8 fixtures JSON; không có broken links trong 7 tài liệu kiểm tra, route/schema so code/SRS và review diff; `git diff --check` sạch. Task chỉ đổi docs, không chạy lại backend suite không cần thiết sau khi 301 tests T00b pass. Kiểm tra cuối: main local vẫn `f077e55`, Discovery local vẫn `dd9068e`; stash Discovery WIP vẫn tồn tại. Không merge/push feature vào các nhánh nguồn.

Phase 0 hoàn tất với baseline coverage gap và DB lineage limitation nêu trên. Chưa bắt đầu Phase 1; contract mới là đích, không phải tuyên bố runtime đã đáp ứng.
