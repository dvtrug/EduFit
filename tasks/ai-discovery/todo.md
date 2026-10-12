# AI + Discovery backend — checklist

Nguồn: [plan.md](plan.md). Ngày khảo sát 2026-10-11; base `feat/discovery@4cc8767`.
Cập nhật 2026-10-12: B00–B14 và Phase 1–5 đã triển khai trên `feat/discovery-ai`; live Gemini smoke còn pending vì môi trường không có key.

## Khảo sát và contract

- [x] Đọc docs Discovery, Connection, kiến trúc, auth, test và SRS; đối chiếu code backend.
- [x] Xác định model lỗi thời, provider mặc định stub, lỗi stub trả bio, goal HTTP thiếu và CSRF/docs lệch nhau.
- [x] Lưu kế hoạch độc lập, giữ plan/checklist Discovery cũ.
- [x] B00: Chốt kết quả baseline hiện tại — xem evidence bên dưới.
- [x] B01: Chốt schema goal/slots/readiness, quyền, AI/quota/security và Connection contract.
- [x] Checkpoint A: baseline và contract rõ.

## AI provider

- [x] B02: Gemini model/configuration hợp lệ; tách cách nạp env Compose và Maven/IDE.
- [x] B03: Adapter contract tests; header key; response parsing/errors; secret redaction.
- [x] B04: Stub đúng matching; grounded facts; flag AI được định nghĩa rõ.
- [x] Checkpoint B: AI adapter offline tests và fallback pass.

## Quota và failure isolation

- [x] B05: AI log REQUIRES_NEW và lỗi commit được kiểm chứng/cô lập.
- [x] B06: Quota reservation atomic, rolling 24h, concurrency/stale reservation tests.
- [x] B07: Matching dùng quota mới; deadline qua fake HTTP; fallback không đổi ranking.
- [x] Checkpoint C: PostgreSQL tests pass, không skip.

## Đầu vào matching

- [x] B08: Student create/list/update/archive goals đúng owner và BR-12.
- [x] B09: Goal availability save nguyên tử; validation và readback.
- [x] B10: Parent chỉ đọc goals qua confirmed-link authorization; không dependency cycle.
- [x] Checkpoint D: tạo goal -> slots -> match qua HTTP, không cần SQL goal/slot thủ công (account/profile/catalogs vẫn là fixture; CurrentUser kiểm soát trong test).

## HTTP integration

- [x] B11: Session thật, CSRF endpoint/validation, security CORS/preflight; docs đồng bộ.

  B11 evidence: [full-context security + architecture](phase4-b11-final.log), **18 tests pass, 0 fail/error/skip** (7 full-filter PostgreSQL cases + 9 architecture + 2 CurrentUser adapter). [Initial red](phase4-b11-red.log) exposed missing JDBC session auto-configuration; [fixture correction run](phase4-b11-red-http.log) exposed use of the unused Spring Argon2 encoder without BouncyCastle, fixed by seeding with actual IAM Argon2PasswordService. [Intermediate run](phase4-b11-green.log) is a failed harness iteration, not acceptance evidence. [IAM regression](phase4-b11-iam-regression.log). Contract: [Phase 4](../../docs/ai-discovery-phase4-contract.md). No live provider call.
- [x] B12: Malformed input 400; ApiError thống nhất; search pagination tie-break.
- [x] Checkpoint E: full filter security và error regression pass.

  B12 evidence: [red](phase4-b12-red.log) có 14/17 input cases fail do 500/lộ rejected value và 8/8 pagination sort cases fail vì thiếu unique order. [Auth status red](phase4-b12-auth-status-red.log) chứng minh INVALID_CREDENTIALS/USER_NOT_ACTIVE/ACCOUNT_LOCKED lệch contract. [Final app regression](phase4-final-regression.log): **152 tests, 0 fail/error/skip, BUILD SUCCESS** sau mọi sửa (23 HTTP input cases, 30 Discovery PostgreSQL cases, 62 goals, 9 matching workflow, 7 full-filter security, 7 handler cũ, 9 architecture, 3 migration, 2 CurrentUser). [Module regression](phase4-module-regression.log): **283 tests, 0 fail/error/skip**. Profile coverage ghép unit + app: **799/901 lines = 88,68%; 182/222 branches = 81,98%**, gate 80% pass ([log](phase4-profile-coverage-merged.log)). App coverage branch baseline **56%**, gate không đạt và được giữ nguyên bằng chứng ([log](phase4-app-coverage.log)); không hạ ngưỡng/exclude class. Contract tổng B01 đã chốt trong [backend contract](../../docs/ai-discovery-backend-contract.md), [Phase 3](../../docs/ai-discovery-phase3-contract.md) và [Phase 4](../../docs/ai-discovery-phase4-contract.md).

## Bàn giao

- [x] B13: Backend HTTP E2E Student/Parent/Tutor, Discovery -> Connection -> class ACTIVE.
- [x] B14: Runbook + fixture + smoke collection, live Gemini smoke opt-in bằng env key.
- [x] Checkpoint F: frontend-ready; chưa thay đổi file frontend.
- [ ] Reactor verify/coverage + architecture + migration tests pass; mọi skip được giải thích.
- [x] `git diff --check`; docs routes/fields/errors/links khớp code.

  B13 evidence: `AiDiscoveryConnectionE2ETest` **7 tests pass, 0 fail/error/skip** với full application, security filters, JDBC session, Flyway và PostgreSQL thật; Gemini fake chạy qua loopback HTTP. Luồng Student đạt profile -> goal/slots -> search/detail/match -> request -> Tutor accept -> một class ACTIVE. Luồng Parent đạt invite/accept -> goal/match/request -> revoke -> 403 và không gọi provider. HTTP 429/blocked/timeout đều fallback và Connection vẫn dùng được; empty candidates không gọi AI; adjacent level được Discovery rank nhưng Connection trả `400 INVALID_OPERATION`. Red concurrency trả `[200,500]` vì stale entity trong Hibernate persistence context; green trả `[200,409]` và đúng một class. Contract: [Phase 5 E2E](../../docs/ai-discovery-phase5-e2e.md). Live Gemini/cold-start/runbook vẫn thuộc B14; không sửa frontend.

  B14 evidence: cold-start `docker compose down` + `up --build -d` giữ nguyên volume đã build đủ 17 Maven modules, Flyway schema ở v1.12 và health UP. [Smoke script](../../scripts/smoke-ai-discovery.ps1) offline pass lúc `2026-10-12T06:56:33Z`: OpenAPI/session cookie/public security, catalogs/search/detail, Student goal+slots+match, anonymous 401, CSRF 403, invalid topN 400, no-result, Parent link/match/revoke 403 và Connection -> class đều pass; stub trả `aiGenerated=false`, AI audit `SUCCESS` với latency 6 ms. Regression cuối **16 tests pass, 0 fail/error/skip**: OpenAPI 2, E2E 7, HTTP security 7. PowerShell parse và `git diff --check` pass. [Runbook](../../docs/ai-discovery-backend-runbook.md) ghi URL, auth/CSRF, accounts/IDs, payload/errors, quota/fallback và lệnh live opt-in. `GEMINI_API_KEY` không có trong process nên live Gemini là **pending**; không ghi nhận live pass và không in/lưu key. Không sửa frontend hoặc main.

## Evidence baseline hiện tại

- AI/Discovery/Connection reactor và dependencies: **227 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. AI 12, Discovery 104, Connection 13, Profile 44 (gồm 6 PostgreSQL candidate tests), IAM 32, Shared 22. [Log](../ai-discovery-baseline.log).
- App selected tests: **50 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. Architecture 9, CurrentUser 2, FlywayMigrationIT 2, DiscoveryReadPostgresTest 22, MatchingWorkflowPostgresTest 9, exception handler 6. [Log](../ai-discovery-app-baseline.log).
- Tổng hai lượt **277 tests pass**. PostgreSQL chạy qua Testcontainers thật, không dùng H2 thay thế. Các test app hiện tại dùng context chuyên biệt, chưa chứng minh login/CORS/CSRF full application end-to-end; việc đó thuộc B11/B13.
- Lệnh app đã chạy từ backend: `mvn -B -ntp -pl app -am '-Dtest=vn.edufit.discovery.*PostgresTest,vn.edufit.db.FlywayMigrationIT,vn.edufit.Architecture*Test,vn.edufit.web.advice.GlobalExceptionHandlerTest,vn.edufit.config.security.SpringSecurityCurrentUserTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`.
- Chưa chạy toàn reactor verify/coverage hoặc cold-start Compose trong lần lập plan. Whitespace hai tài liệu mới và internal links đã kiểm tra, không phát hiện lỗi.
- Live Gemini: **chưa chạy**, không có bằng chứng model/key thật đã gọi thành công.
- Ghi chú môi trường: đã dùng JDK 21.0.10 thay Java 8 trên PATH; Mockito và Docker cần truy cập ngoài sandbox.
- Bước này chỉ thêm tài liệu/log, không sửa implementation hoặc frontend, không commit/push/merge.

## Evidence Phase 1 — 2026-10-12

- Contract bàn giao: [AI provider Phase 1](../../docs/ai-discovery-backend-contract.md). B01 tổng thể chưa hoàn tất; không đổi goal/Connection/security contract trong phase này.
- TDD: [config red](phase1-config-red.log), [adapter red](phase1-adapter-red.log), [adapter green](phase1-adapter-green.log), [stub red](phase1-stub-red.log), [stub green](phase1-stub-green.log). Lượt stub red bật `maven.test.failure.ignore=true` để thu failure cả AI/Discovery; BUILD SUCCESS của lượt red không phải nghiệm thu.
- Regression reactor AI/Discovery/Connection và dependencies: **265 tests, 0 fail/error/skip, BUILD SUCCESS**. Shared 22, AI 47, IAM 32, Profile 44 (gồm PostgreSQL candidates), Connection 13, Discovery 107. [Log](phase1-regression.log). Lượt này trước khi bổ sung các test adapter/config cuối; production code không đổi sau lượt này.
- App selected regression: **42 tests app, 0 fail/error/skip**, gồm architecture 9, Flyway 2, Discovery read PostgreSQL 22, matching workflow PostgreSQL 9. Reactor cùng lượt chạy thêm 52 test AI pass. [Log](phase1-app-regression.log).
- AI final suite sau mọi bổ sung: **61 tests AI + 22 tests Shared, 0 fail/error/skip, BUILD SUCCESS**. [Log](phase1-ai-final-tests.log). Không cộng các lượt thành số test duy nhất vì có test chạy lặp.
- Coverage gate bật thật, AI **82,14% lines / 82,50% branches**; Discovery **92,90% lines / 84,34% branches**, đều đạt 80%. [AI gate](phase1-ai-coverage.log), [Discovery gate](phase1-discovery-coverage.log).
- Reactor `verify -Djacoco.haltOnFailure=true` **chưa pass**: dừng ở baseline Shared (line ratio 0,75; branch ratio 0,71), trước khi chạy AI. [Log](phase1-ai-verify.log). Không sửa Shared ngoài phạm vi; hai module thay đổi đã kiểm tra gate riêng. Không nhận rằng toàn reactor verify/coverage đã pass.
- Lệnh chính từ backend, JDK 21:
  - `mvn -B -ntp -pl platform/ai,modules/discovery,modules/connection -am test`
  - `mvn -B -ntp -pl app -am '-Dtest=Ai*Test,GeminiLlmProviderTest,StubLlmProviderTest,Architecture*Test,FlywayMigrationIT,DiscoveryReadPostgresTest,MatchingWorkflowPostgresTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
  - `mvn -B -ntp -pl platform/ai -am test`
  - `mvn -B -ntp -pl platform/ai '-Djacoco.haltOnFailure=true' jacoco:check@check-coverage-gate` (tương tự `-pl modules/discovery`).
- `git diff --check` và `docker compose --env-file .env.example config --quiet` pass. Không có diff frontend; docs Discovery và index đã đồng bộ. Đã rà diff Java/config và error/provenance paths.
- **Chưa gọi Gemini thật**, chưa cold-start Compose, chưa full-filter session/CSRF/CORS E2E. Test adapter chạy HTTP loopback; Testcontainers dùng PostgreSQL thật. Phase 2 quota/log isolation và các phase HTTP còn pending. Không commit/push/merge.

## Evidence Phase 2

- B05 hoàn tất: `AiLogPostgresTest` **7 tests, 0 fail/error/skip**; selected AI regression pass trong cùng reactor. [Red](b05-red.log): 2 failures + 1 error chứng minh lỗi commit/constraint/usage transaction; [green](b05-green.log): BUILD SUCCESS. Deferred trigger gây lỗi thật tại commit, sequence chứng minh chỉ một insert attempt; outer rollback giữ log; usage store lỗi trả fallback không gọi provider và outer transaction vẫn commit được. [Contract](../../docs/ai-discovery-phase2-contract.md).
- Lệnh B05 từ backend/JDK 21: `mvn -B -ntp -pl app -am '-Dtest=AiLogPostgresTest,Ai*Test,GeminiLlmProviderTest,StubLlmProviderTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`.
- Nhánh `feat/discovery-ai`; mỗi task hoàn tất được commit và push riêng theo yêu cầu. Không thay đổi frontend/main; chưa gọi Gemini live.
- B06 persistence slice: V1.12 + AiMatchQuota API; **3 Flyway tests + 9 app architecture checks pass, không skip**. [Schema red](b06-schema-red.log): 3 failures vì chưa có V1.12; [schema green](b06-schema-green.log): clean migration, legacy upgrade, quota backfill và rerun no-op pass. Schema/API được commit riêng trước implementation/wiring; B06 tổng thể chưa đánh dấu xong ở slice này.
- B06 implementation hoàn tất: [quota red](b06-quota-red.log) 13 cases fail với empty-admission scaffold; [quota green](b06-quota-green.log) 13 quota + 7 logger + 9 app architecture checks pass (AI config/architecture cũng pass); [quota final](b06-quota-final.log) **15 quota cases, 0 fail/error/skip**, gồm bổ sung deferred commit và terminal-store failure. PostgreSQL thật, barrier đồng thời, Clock kiểm soát mốc 24h, actor riêng, outer rollback/restart và stale lease. Lệnh: `mvn -B -ntp -pl app -am '-Dtest=AiQuotaPostgresTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`. Caller vẫn dùng đường cũ cho đến B07; không nhận B06 riêng là runtime quota đã được enforce.
- B07 hoàn tất: [red](b07-red.log) 8 cases chưa pass (6 failures liên quan admission/outcome, 2 errors gồm async cleanup làm deadlock fixture); đã thêm await worker shutdown trong cleanup. [green](b07-green.log): **8 HTTP/PostgreSQL matching + 8 logger tests pass**, gateway/deadline unit 8 và Discovery explanation/controller 20 cũng pass. Không bật failure-ignore. Lần 10/11, gọi gateway trực tiếp, concurrent matching trong lúc HTTP còn treo, delayed mặc định 15s, trickle 600ms, I/O và HTTP 500 được kiểm chứng; toàn MatchScore/thứ tự giữ nguyên khi fallback.
- Final module regression: **280 tests, 0 fail/error/skip, BUILD SUCCESS**. Shared 22, AI 63, IAM 32, Profile 44, Connection 13, Discovery 106. [Log](phase2-module-regression.log). Lệnh: `mvn -B -ntp -pl platform/ai,modules/discovery,modules/connection -am test`.
- Final app regression: **82 selected tests đều pass ở kết quả cuối**, gồm architecture 9, logger 8, quota 15, matching HTTP/DB 8, CurrentUser 2, Flyway 3, DiscoveryRead 22, MatchingWorkflow 9, exception handler 6. [Lượt rộng](phase2-app-regression.log) ban đầu có 6 failures trong DiscoveryRead do assertion cũ đòi gateway không được gọi; 60 tests ngoài class đó đã pass. Đã cập nhật offline fixture trả non-model response và xác nhận gateway được gọi cho Top 1 (provider vẫn mock); [rerun cả 22 DiscoveryRead](phase2-read-regression-final.log) pass 0 fail/error/skip. Không coi lượt rộng ban đầu là BUILD SUCCESS. Các test ngoài class lỗi không bị thay đổi nên không chạy lặp lại.
- App command: `mvn -B -ntp -pl app -am '-Dtest=AiLogPostgresTest,AiQuotaPostgresTest,AiMatchingPostgresTest,Architecture*Test,FlywayMigrationIT,DiscoveryReadPostgresTest,MatchingWorkflowPostgresTest,GlobalExceptionHandlerTest,SpringSecurityCurrentUserTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`; rerun dùng `-Dtest=DiscoveryReadPostgresTest`. Cả hai dùng JDK 21, Docker PostgreSQL thật. Không cộng thêm XML FlywayMigrationScriptTest cũ ngoài selector vào số 82.
- **362 test cases khác nhau đã pass** (280 module + 82 app); các lượt TDD/regression trùng nhau không cộng thêm.
- Coverage unit + app integration: AI **296/318 lines = 93,08%; 111/132 branches = 84,09%**. Discovery **338/360 lines = 93,89%; 169/196 branches = 86,22%**. Gate 80% bật `jacoco.haltOnFailure=true` pass ở cả hai module, không exclude class. [Coverage log](phase2-coverage.log). Unit-only execution data không chứa các nhánh PostgreSQL chạy ở app; đã dùng [merge helper](Phase2MergeCoverage.java) + JaCoCo ExecFileLoader ghép ba file exec thật, giữ bản unit gốc ở target/jacoco-unit-phase2.exec rồi chạy report/check trên file ghép. Xem reproduction dưới đây.
- Đã rà diff/API boundaries, SQL parameterization, trạng thái terminal/expiry và secret patterns; không phát hiện key thật. Whitespace và docs links được kiểm tra trước commit. Không sửa frontend/main, không gọi Gemini thật, không cold-start Compose/full-filter HTTP E2E. Full reactor verify vẫn chưa được nhận pass (baseline coverage Shared đã ghi ở Phase 1); B14 giữ pending.

### Reproduce coverage Phase 2

Chạy hai nhóm test ở trên trước. Từ backend, Java 21; JaCoCo 0.8.12 core JAR được Maven tải trong lượt test:

```powershell
$jacocoCore = Join-Path $env:USERPROFILE '.m2/repository/org/jacoco/org.jacoco.core/0.8.12/org.jacoco.core-0.8.12.jar'
Copy-Item platform/ai/target/jacoco.exec platform/ai/target/jacoco-unit-phase2.exec
Copy-Item modules/discovery/target/jacoco.exec modules/discovery/target/jacoco-unit-phase2.exec
java -cp $jacocoCore ../tasks/ai-discovery/Phase2MergeCoverage.java target/phase2-merged.exec platform/ai/target/jacoco.exec modules/discovery/target/jacoco.exec app/target/jacoco.exec
Copy-Item target/phase2-merged.exec platform/ai/target/jacoco.exec
Copy-Item target/phase2-merged.exec modules/discovery/target/jacoco.exec
mvn -B -ntp -pl platform/ai,modules/discovery '-Djacoco.haltOnFailure=true' jacoco:report jacoco:check@check-coverage-gate
```

`jacoco:check` đọc dataFile mặc định của module (không dùng property override của report), nên bước copy là có chủ đích. Đây là evidence coverage kết hợp unit + PostgreSQL integration, không phải tuyên bố unit-only hoặc toàn reactor verify đã đạt gate. Báo cáo local nằm trong target/site/jacoco, không commit build outputs.

## Evidence Phase 3

- Goal/readiness contract của B01 được chốt trong [Phase 3 contract](../../docs/ai-discovery-phase3-contract.md); phần security tổng thể B01 vẫn pending cho Phase 4. B08 hoàn tất: create/list/update/archive, active subject, BR-12, budget/mode/area, owner từ session, archive idempotent và giữ matching history. Real Matching/Connection từ chối archived goal.
- [B08 red](b08-red.log): chạy controller scaffold không có routes, 18 failures/0 errors chứng minh thiếu API. Test đã viết trước implementation; do Docker ban đầu chưa chạy, sau khi khởi động engine đã tái lập scaffold để có red HTTP thật. [Docker unavailable](phase3-docker-unavailable.log), [Docker đang khởi động](phase3-docker-starting.log) là lỗi môi trường, không tính là business red. [Fixture proxy](b08-fixture-proxy.log): 20/21 goal tests pass, một failure do test lookup concrete ConnectionService qua JDK proxy; sửa fixture sang class proxy.
- [B08 green](b08-green.log): **21 goal HTTP/service/PostgreSQL cases + 9 app architecture checks, 0 fail/error/skip, BUILD SUCCESS**. CurrentUser kiểm soát trong fixture; chưa nhận full-filter auth/CSRF E2E đã pass. Migrations PostgreSQL thật, không skip/H2. Lệnh backend/JDK 21: `mvn -B -ntp -pl app -am '-Dtest=LearningGoalPostgresTest,Architecture*Test' '-Dsurefire.failIfNoSpecifiedTests=false' test`.
- B09 hoàn tất: [red](b09-red.log) 18 failures/0 errors do thiếu availability route; [body error red](b09-unreadable-body-red.log) 38/39 goal cases pass, JSON null bị trả 500. Đã sửa tối thiểu HttpMessageNotReadableException -> 400 an toàn, thêm handler regression; B12 tổng thể vẫn pending.
- [B09 green](b09-green.log): **39 goal cases + 9 app architecture + 7 handler tests = 55 app tests, 0 fail/error/skip, BUILD SUCCESS**. Replace giữ owner goal lock qua commit; concurrent second request chờ transaction thứ nhất, kết quả không trộn slots. Deferred PostgreSQL trigger gây lỗi commit sau delete/insert, old slot ID vẫn còn nguyên. Duplicate/overlap/null/day/time/minute precision/100-slot limit/empty clear/touching boundary đều kiểm tra. Create goal -> replace slots -> POST match chạy qua MockMvc với facade/repository thật, không seed SQL goal/slot; fixture vẫn seed account/profile/catalogs và CurrentUser được kiểm soát.
- Lệnh B09 backend/JDK 21: `mvn -B -ntp -pl app -am '-Dtest=LearningGoalPostgresTest,GlobalExceptionHandlerTest,Architecture*Test' '-Dsurefire.failIfNoSpecifiedTests=false' test`.
- B10 hoàn tất: [red](b10-red.log) 13 failures/0 errors do thiếu Parent route; [green](b10-green.log) **53 goal cases + 9 architecture + 7 handler = 69 app tests pass**. Profile chỉ expose read facade/DTO; Discovery kiểm tra confirmed link trước khi đọc. Real ConnectionService revoke do Student thực hiện, Parent bị chặn ở lần đọc/match tiếp theo. Pending/declined/expired/withdrawn/revoked/unlinked và role không phải Parent bị chặn. DTO không chứa contact/email/account ID; active/incomplete/archived và empty-list readiness được kiểm chứng. 3 goals dùng đúng 4 business SQL queries, không N+1; không tính auth/session filter.
- Final module regression: **280 tests, 0 fail/error/skip, BUILD SUCCESS**. Shared 22, AI 63, IAM 32, Profile 44 (gồm PostgreSQL candidates), Connection 13, Discovery 106. [Log](phase3-module-regression.log). Lệnh backend/JDK 21: `mvn -B -ntp -pl platform/ai,modules/discovery,modules/connection -am test`.
- [Final app regression](phase3-app-regression.log): **106 tests, 0 fail/error/skip, BUILD SUCCESS**, gồm goal 54 (thêm direct service validation), architecture 9, Flyway 3, DiscoveryRead 22, MatchingWorkflow 9, CurrentUser 2, handler 7. Lượt này đã chạy lại sau khi chuyển nested slot @Valid sang type argument theo Hibernate Validator 9 và bổ sung linked-Parent write denial; production code không thay đổi sau lượt này.
- Gate Profile ban đầu **793/895 lines, 168/218 branches (77,06%)**, chưa đạt branch 80%: [log ban đầu](phase3-profile-coverage-before-boundaries.log). Chỉ bổ sung goal/slot boundary tests trong phạm vi mới: null currentLevel/budgetMax/mode, BOTH area trắng, endTime null, nano/second precision, cùng giờ khác ngày, validation direct Java caller. [Final goal suite](phase3-goal-boundary-final.log): **62 cases pass, 0 fail/error/skip, BUILD SUCCESS**. 52 app cases ngoài goal class đã pass trong lượt 106 và không đổi; tổng cuối là **114 app cases khác nhau đã pass**. Không cộng các lượt chạy trùng thành số test mới. Tổng module + app: **394 cases khác nhau pass**, không skip.
- App command: `mvn -B -ntp -pl app -am '-Dtest=LearningGoalPostgresTest,Architecture*Test,FlywayMigrationIT,DiscoveryReadPostgresTest,MatchingWorkflowPostgresTest,GlobalExceptionHandlerTest,SpringSecurityCurrentUserTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`; final goal rerun dùng selector `LearningGoalPostgresTest`. Không cộng các XML app cũ nằm ngoài selector.
- Coverage ghép unit + PostgreSQL integration cuối: **Profile 793/895 lines = 88,60%; 178/218 branches = 81,65%**. **Discovery 355/377 lines = 94,16%; 177/204 branches = 86,76%**. [Final gate](phase3-coverage-final.log): cả hai module pass `jacoco.haltOnFailure=true`, không exclude class/hạ ngưỡng. Exec là dữ liệu test thật tích lũy; giữ source backup unit ở target/jacoco-unit-phase3.exec, ghép với app.exec bằng [helper](Phase2MergeCoverage.java). Không tuyên bố unit-only coverage hoặc toàn reactor verify đã pass (baseline Shared ghi ở Phase 1).
- Không sửa frontend/main, không sửa migration cũ, không gọi Gemini live hoặc cold-start Compose/full-filter session/CSRF/CORS E2E. Phase 4/5 và B01 phần security vẫn pending. Docs/whitespace/secret-pattern checks thực hiện trước commit/push B10.

### Reproduce coverage Phase 3

Chạy module/app commands phía trên trước; dùng JDK 21 từ backend. Có thể chạy app selector toàn bộ một lượt với bộ goal hiện tại (62 cases) để tái lập 114 app cases.

```powershell
$jacocoCore = Join-Path $env:USERPROFILE '.m2/repository/org/jacoco/org.jacoco.core/0.8.12/org.jacoco.core-0.8.12.jar'
Copy-Item modules/profile/target/jacoco.exec modules/profile/target/jacoco-unit-phase3.exec
Copy-Item modules/discovery/target/jacoco.exec modules/discovery/target/jacoco-unit-phase3.exec
java -cp $jacocoCore ../tasks/ai-discovery/Phase2MergeCoverage.java target/phase3-merged.exec modules/profile/target/jacoco-unit-phase3.exec modules/discovery/target/jacoco-unit-phase3.exec app/target/jacoco.exec
Copy-Item target/phase3-merged.exec modules/profile/target/jacoco.exec
Copy-Item target/phase3-merged.exec modules/discovery/target/jacoco.exec
mvn -B -ntp -pl modules/profile,modules/discovery '-Djacoco.haltOnFailure=true' jacoco:report jacoco:check@check-coverage-gate
```
