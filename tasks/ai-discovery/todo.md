# AI + Discovery backend — checklist

Nguồn: [plan.md](plan.md). Ngày khảo sát 2026-10-11; base `feat/discovery@4cc8767`.
Cập nhật 2026-10-12: B00 và Phase 1 (B02–B04) hoàn tất. Các phase sau chưa triển khai; B01 chỉ có phần AI contract trong Phase 1.

## Khảo sát và contract

- [x] Đọc docs Discovery, Connection, kiến trúc, auth, test và SRS; đối chiếu code backend.
- [x] Xác định model lỗi thời, provider mặc định stub, lỗi stub trả bio, goal HTTP thiếu và CSRF/docs lệch nhau.
- [x] Lưu kế hoạch độc lập, giữ plan/checklist Discovery cũ.
- [x] B00: Chốt kết quả baseline hiện tại — xem evidence bên dưới.
- [ ] B01: Chốt schema goal/slots/readiness, quyền, AI/quota/security và Connection contract.
- [ ] Checkpoint A: baseline và contract rõ.

## AI provider

- [x] B02: Gemini model/configuration hợp lệ; tách cách nạp env Compose và Maven/IDE.
- [x] B03: Adapter contract tests; header key; response parsing/errors; secret redaction.
- [x] B04: Stub đúng matching; grounded facts; flag AI được định nghĩa rõ.
- [x] Checkpoint B: AI adapter offline tests và fallback pass.

## Quota và failure isolation

- [x] B05: AI log REQUIRES_NEW và lỗi commit được kiểm chứng/cô lập.
- [x] B06: Quota reservation atomic, rolling 24h, concurrency/stale reservation tests.
- [ ] B07: Matching dùng quota mới; deadline qua fake HTTP; fallback không đổi ranking.
- [ ] Checkpoint C: PostgreSQL tests pass, không skip.

## Đầu vào matching

- [ ] B08: Student create/list/update/archive goals đúng owner và BR-12.
- [ ] B09: Goal availability save nguyên tử; validation và readback.
- [ ] B10: Parent chỉ đọc goals qua confirmed-link authorization; không dependency cycle.
- [ ] Checkpoint D: tạo goal -> slots -> match qua HTTP, không cần SQL thủ công.

## HTTP integration

- [ ] B11: Session thật, CSRF endpoint/validation, security CORS/preflight; docs đồng bộ.
- [ ] B12: Malformed input 400; ApiError thống nhất; search pagination tie-break.
- [ ] Checkpoint E: full filter security và error regression pass.

## Bàn giao

- [ ] B13: Backend HTTP E2E Student/Parent/Tutor, Discovery -> Connection -> class ACTIVE.
- [ ] B14: Runbook + fixture + smoke collection, live Gemini smoke opt-in bằng env key.
- [ ] Checkpoint F: frontend-ready; chưa thay đổi file frontend.
- [ ] Reactor verify/coverage + architecture + migration tests pass; mọi skip được giải thích.
- [ ] `git diff --check`; docs routes/fields/errors/links khớp code.

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
