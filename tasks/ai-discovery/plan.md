# AI + Discovery: đánh giá hiện trạng và kế hoạch backend sẵn sàng tích hợp

Ngày kiểm tra: 2026-10-11 (Asia/Ho_Chi_Minh). Base: `feat/discovery`, commit `4cc8767`.
Trạng thái cập nhật 2026-10-12: **Phase 1 (B02–B04) đã triển khai**. Các mục khảo sát bên dưới ghi hiện trạng trước thay đổi; kết quả thực thi mới ở mục 9 và checklist. Chỉ backend, cấu hình, test và tài liệu; không sửa frontend.
Checklist thực thi: [todo.md](todo.md). Kế hoạch Discovery cũ tại `../plan.md` và `../todo.md` được giữ nguyên như lịch sử.

## 1. Kết luận: đã có AI, nhưng chưa phải chỉ thêm API key là xong

Discovery đã nối `MatchExplanationService -> AiGateway -> LlmProvider`. Không cần viết lại thuật toán matching hoặc xây thêm AI server. AI chỉ viết giải thích cho Top 1; điểm và thứ tự do Java tính. Tuy nhiên, cấu hình mặc định và các khoảng trống đầu vào/HTTP chưa đủ để frontend tự chạy toàn bộ nghiệp vụ.

| Hạng mục | Bằng chứng trong code | Đánh giá |
| --- | --- | --- |
| Gateway và provider | `platform/ai`: `AiGatewayService`, `GeminiLlmProvider`, `StubLlmProvider` | Đã có adapter Gemini REST và chế độ offline |
| Chọn provider | `application.yml`, `.env.example`, `compose.yaml`: mặc định `AI_PROVIDER=stub` | Chỉ đặt `GEMINI_API_KEY` vẫn chạy stub; cần `AI_PROVIDER=gemini` |
| Model mặc định | Ba file trên và `AiProperties`: `gemini-1.5-flash` | **Blocker gọi thật**: model đã shutdown 24-09-2025 theo Google; phải đổi model khả dụng |
| Nạp biến môi trường | Compose chuyển AI_PROVIDER/API_KEY/MODEL vào container | `.env` dùng được qua Compose; chạy Maven/IDE phải truyền environment, không có cơ chế tự đọc `.env` trong code đã kiểm tra |
| Deadline | `AiGatewayService.callProvider` dùng Future deadline; `AiClientConfig` có socket timeout | Đã có deadline tổng provider 15s, không chỉ connect/read timeout |
| Fallback | `MatchExplanationService` bắt RuntimeException | Matching vẫn có điểm/explanation theo quy tắc khi AI lỗi hoặc quota hết, nếu phần DB còn hoạt động |
| Quota | Count `MATCH_EXPLANATION` trong 24h theo actor, giới hạn 10 | Có cho request tuần tự; count rồi call không atomic nên có race khi nhiều request đồng thời |
| AI log | `AiRequestLogger`, REQUIRES_NEW; bảng `ai_request_log` | Có SUCCESS/ERROR/TIMEOUT, không lưu prompt/output. Chưa chứng minh lỗi tại transaction commit được cô lập: catch quanh save không bắt được mọi lỗi xảy ra sau khi method return |
| Stub matching | Stub kiểm tra chuỗi `gia sư` trước `giải thích`; prompt matching chứa cả hai | **Lỗi xác định từ nhánh code**: có thể trả bio, còn `aiGenerated=true`; dữ kiện stub cũng bịa lịch/rating |
| Response Gemini | Chỉ đọc `candidates[0].content.parts[0].text` | Thiếu contract test adapter; parts rỗng/phi văn bản/multipart/finishReason chưa xử lý đủ |
| Secret | API key nằm trong query URL; exception message và stack được log ở nhiều tầng | Có nguy cơ lộ key qua exception URL; chuyển header và redaction, không kết luận đã lộ key |
| Search/detail/match | Ba REST routes và Java facade đã hiện thực | Giữ contract hiện tại; topN 5 mặc định, tối đa 10; cap candidates 200 |
| Đầu vào goal | Có `LearningGoal`, slots, repositories, facade đọc; không tìm thấy controller/service HTTP quản lý goals | **Blocker luồng tự phục vụ**: frontend chưa thể tạo/lấy goal cùng lịch để gọi match |
| Quyền Parent | Matching gọi `ConnectionFacade.hasConfirmedParentLink` | Đã có kiểm tra confirmed link; cần test lại qua session/filter thật và revoke thật |
| CORS | `WebConfig` cho localhost/127.0.0.1:3000, credentials=true | Có cấu hình MVC; SecurityConfig chưa gọi `.cors(...)`, cần kiểm chứng OPTIONS qua toàn filter chain |
| CSRF | SecurityConfig ignore `/api/v1/**`; AuthController không có GET `/csrf` | Docs auth/discovery nói có CSRF nhưng runtime đang bỏ qua; phải sửa và đồng bộ contract |
| OpenAPI auth | `OpenApiConfig` khai báo cookie `JSESSIONID`, runtime dùng `EDUFIT_SESSION` | Swagger contract đang sai tên cookie; phải sửa để tránh tích hợp theo schema sai |
| HTTP lỗi đầu vào | GlobalExceptionHandler chưa có mapping riêng malformed JSON/type mismatch | Có nguy cơ trả 500 cho JSON/UUID/query sai; cần regression qua HTTP thật |
| Tính tương thích Connection | Discovery cho lệch level như điểm mềm; Connection yêu cầu tutor dạy đúng subject+level | Có thể match được nhưng gửi request bị chặn. Đây là khác biệt policy cần bàn giao rõ, không tự đổi scorer |
| AI bio UC1.7 | Enum/provider hỗ trợ TUTOR_BIO; không tìm thấy consumer Profile gọi gateway | Chưa hoàn tất AI bio. Không phải điều kiện để hoàn thành AI explanation + Discovery; tách backlog riêng |

Phân biệt ba mức nghiệm thu:

1. **Test offline:** search/detail/match chạy với dữ liệu hợp lệ, stub/fallback trả nội dung đúng; không cần API key.
2. **Test AI thật:** backend chọn Gemini, model khả dụng, key có quyền/quota, adapter parse đúng; smoke phải chứng minh thực sự gọi provider.
3. **Backend sẵn sàng cho frontend:** thêm goal APIs, session/CSRF/CORS/error contract và test xuyên module. HTTP 200 hay health UP riêng lẻ không chứng minh AI thật hoạt động.

Cấu hình tối thiểu để bật provider thật sau khi xử lý model/adapter:

```dotenv
AI_PROVIDER=gemini
GEMINI_API_KEY=<secret chỉ truyền ở máy/container backend>
GEMINI_MODEL=<model khả dụng với key, hỗ trợ generateContent>
```

Không dùng nguyên placeholder trên để khởi động. Key còn phải có quyền gọi model, project có quota và máy/container truy cập được Google qua HTTPS. Lưu `.env` rồi chạy Maven không tự chuyển các dòng này thành environment; Compose mới có wiring đó hiện tại.

## 2. Nguồn và thứ tự đối chiếu

Đã đọc code AI/Discovery/Profile/Connection, app configuration/security/error handler và các bộ test liên quan, cùng:

- [Discovery API](../../docs/discovery-api-contract.md), [flow](../../docs/discovery-module-flow.md), [performance](../../docs/discovery-performance-report.md).
- [Connection plan](../../docs/connection-implementation-plan.md), [flow](../../docs/connection-module-flow.md), kế hoạch/checklist Discovery cũ.
- [SADS](../../docs/software-architecture-design-specification.md), [ADR-001](../../docs/ADR-001-tech-stack.md), [ADR-002](../../docs/ADR-002-multi-module-architecture.md), [module guide](../../docs/backend-module-structure-guide.md).
- [Authentication contract](../../docs/authentication-contract.md), [testing specification](../../docs/in-development-testing-specification.md), [master test plan](../../docs/edufit_master_test_plan.md), [sprint plan](../../docs/master_sprint_plan_and_ai_learning_framework.md).
- [SRS Edufit.docx](../../docs/SRS%20Edufit.docx): đọc XML text để kiểm tra UC2.3/2.4/2.5, BR-11/12/17/18/25, FR-05/06, NFR-09/10.

Các khác biệt docs cần xử lý có chủ đích:

- SRS gọi AI explanation là **UC2.4**, profile detail là **UC2.5**; một số test docs gắn nhầm UC2.5 cho explanation.
- BR-25 viết “10 explanations/day”; code/contract hiện thực là rolling 24h và count mọi attempt có log, kể cả ERROR/TIMEOUT. Kế hoạch giữ rolling 24h và count attempts để hạn chế retry gây tốn tài nguyên; phải ghi rõ đây là cách cụ thể hóa rule, không nhận là SRS đã chốt chi tiết đó.
- BR-17: Student sửa profile/goals; Parent chỉ đọc khi link confirmed. Không cho Parent ghi goal chỉ vì đã linked.
- BR-12: deadline tương lai, desiredLevel > currentLevel, goal được tham chiếu phải archive thay vì hard delete.
- Authentication contract mô tả error `success:false`, trong khi ApiError runtime không có `success`. Giữ schema runtime hiện hữu, sửa docs và kiểm thử, không đổi toàn bộ response chỉ để khớp tài liệu cũ.
- Giữ matching năm yếu tố, candidate cap và giới hạn ranking đã bàn giao; không copy proposal ba yếu tố hoặc full scan từ tài liệu lịch sử.

Tài liệu Google đối chiếu ngày kiểm tra:

- [Model 1.5 shutdown](https://firebase.google.com/docs/ai-logic/faq-and-troubleshooting): `gemini-1.5-flash` ngừng 24-09-2025.
- [Model lifecycle](https://ai.google.dev/gemini-api/docs/deprecations): kiểm tra lại khi triển khai, không thay bằng một model cũ khác theo trí nhớ.
- [Models API](https://ai.google.dev/api/models): xác nhận model nhìn thấy bằng key và hỗ trợ `generateContent`.
- [GenerateContent schema](https://ai.google.dev/api/generate-content), [API authentication](https://ai.google.dev/api): header `x-goog-api-key`, parts, candidates, finishReason, usageMetadata.

## 3. Phạm vi và quyết định thiết kế

**Phải hoàn thành:** AI explanation cho Discovery; đầu vào goal tối thiểu; quyền Student/Parent; HTTP readiness; handoff sang Connection; bộ test, fixture, runbook backend.

**Ngoài phạm vi:** sửa frontend, AI bio hoàn chỉnh, chatbot/RAG/vector DB, review/scheduling/notification mới, deploy production, thay thuật toán scoring, merge/push tự động.

Luồng đích:

```text
Session + CSRF -> Catalog + Profile -> Student tạo goal có slots
                                      -> Discovery search/detail/match
Parent confirmed -> đọc goal được cấp quyền -> cùng matching service
Matching -> rule-based rank -> Top 1 AI gateway -> Gemini hoặc fallback
         -> matching log + event commit
Chọn tutor + goal -> Connection request -> Tutor accept -> class ACTIVE
```

Ranh giới bắt buộc:

- `platform/ai` không biết goal/tutor/scorer, không expose controller AI tổng quát cho client gửi prompt tùy ý.
- `discovery` dùng `profile.api`, `connection.api`, `ai.api`; domain tiếp tục thuần Java.
- Profile sở hữu ghi goal và slots; tránh `profile -> connection` vì Connection đã phụ thuộc Profile. Parent đọc goals qua endpoint tại Discovery, nơi đã có quyền gọi ConnectionFacade, rồi lấy DTO từ ProfileFacade.
- Không đổi public AiGateway/AiResponse chỉ để sửa stub nếu test double/fallback facts có thể giải quyết gọn. Stub dùng response sample trung thực, không suy đoán lịch/rating; provenance `aiGenerated` phải được định nghĩa rõ cho offline mode.
- Không giữ lock DB suốt cuộc gọi LLM. Nếu làm quota atomic, dùng reservation transaction ngắn trước provider; finalize sau provider, có expiry cho reservation bị treo.
- Không tự retry 429/5xx trong vòng 15s trước khi có retry budget rõ; fallback nhanh là mặc định.

Thiết kế quota dự kiến cho B06: bảng reservation mới có reservationId, userId, feature, reservedAt, expiresAt và trạng thái. Cùng userId+feature dùng PostgreSQL transaction advisory lock, đếm attempts đã finalize trong cửa sổ 24h cộng reservations còn hiệu lực, rồi insert reservation nếu dưới 10; commit trước khi gọi HTTP. Finalization cập nhật attempt và ghi log trong transaction ngắn, tránh count hai lần cùng reservation. Reservation hết lease được giải phóng khi đọc/ghi quota; failure trước provider release, failure sau khi đã gọi provider vẫn tính attempt. Cần test crash giữa từng bước. Tái sử dụng pattern advisory-lock Connection khi phù hợp nhưng AI không import implementation Connection.

Handoff IDs phải phân biệt: actor là account userId, goal là goalId, học sinh nghiệp vụ là studentId, gia sư nghiệp vụ là tutorId. Client không gửi actor/role. Với offline/BOTH, area phải có theo BR-11; student educationLevelId không thay cho currentLevel/desiredLevel. Response goal cần goalId, subject/level, budget, mode/area, status, slots và trạng thái sẵn sàng cùng danh sách field còn thiếu; tên trường readiness chốt ở B01, không nhận là response hiện có.

## 4. Kế hoạch theo phase và task

Mỗi task là một thay đổi tập trung gồm code + test liên quan. Đường dẫn dưới đây tương đối với repository, file test mới ghi tên dự kiến; có thể tách class theo convention khi triển khai, tránh gom hai subsystem độc lập vào một commit. Chỉ commit/push khi có yêu cầu thực thi tương ứng.

### Phase 0 — Chốt baseline và contract

**B00 — Khảo sát và lưu bằng chứng** (S; phụ thuộc: không)

- Làm: ghi base SHA, working tree, JDK/Maven/Docker, chạy tests AI/Discovery/Profile/Connection; ghi riêng app/PostgreSQL tests và live provider smoke.
- File: plan/checklist này; `tasks/ai-discovery-baseline.log`, báo cáo app baseline nếu chạy được.
- Đạt: mỗi kết luận phân biệt code inspection, test hiện tại, evidence lịch sử và phần chưa chạy; không suy diễn “AI thật OK” từ mock.
- Kiểm chứng: lệnh ở mục 6; tổng hợp run/fail/skip và lỗi môi trường.

**B01 — Chốt contract đầu vào và readiness** (M; phụ thuộc: B00)

- Làm: tài liệu mới `docs/ai-discovery-backend-contract.md`: route goal/slots, quyền, AI modes, response/error, CSRF, quota, mapping sang Connection.
- Đề xuất Student: POST/GET `/api/v1/profiles/students/me/goals`; PUT `/api/v1/profiles/students/me/goals/{goalId}`; PUT `.../{goalId}/availability`; POST `.../{goalId}/archive`.
- Đề xuất Parent: GET `/api/v1/discovery/students/{studentId}/goals`, trả goal + lịch tối thiểu để chọn, có confirmed-link authorization. Các route này **chưa tồn tại**.
- Goal create/update: subjectId, currentLevel, desiredLevel, goalType, deadline, mode, area, budgetMin/budgetMax. Cấp học lấy từ Student profile; không nhầm với current/desired skill level.
- Đạt: có schema mẫu, validation/error table, phân biệt DRAFT/incomplete readiness bằng dữ liệu hợp lệ hiện có (không tự thêm goal status mới); frontend biết khi nào cần hoàn thiện profile/slots.
- Kiểm chứng: review từng field với entity/migration/SRS; kiểm tra dependency graph không cycle. Không viết frontend types trong task này.

Checkpoint A: contract và baseline đủ rõ trước khi đổi behavior.

### Phase 1 — Làm provider AI đáng tin cậy

**B02 — Cấu hình Gemini dùng được** (M; phụ thuộc: B01)

- File: `AiProperties`, `application.yml`, `.env.example`, `compose.yaml`, configuration tests mới.
- Làm: chọn model stable còn khả dụng sau khi đối chiếu docs và Models API; đồng bộ defaults, giữ env override; validate provider không hợp lệ và thiếu key khi chọn gemini bằng thông báo an toàn.
- Đạt: key-only không bị hiểu là tự bật Gemini; stub không cần key; runtime Maven/IDE và Compose có hướng dẫn khác nhau. Base URL dùng cho fake provider được truyền nhất quán nếu runbook cần.
- Kiểm chứng: Spring context tests stub/gemini/invalid provider/missing config, kiểm tra cấu hình Compose không in secret. Không coi key không blank là key có quyền.
- Live model availability chỉ xác nhận được khi có secret qua môi trường; bước đó để B14, không yêu cầu dán key vào chat.

**B03 — Adapter Gemini và bảo vệ secret** (M; phụ thuộc: B02)

- File: `GeminiLlmProvider`, adapter contract tests mới, fixture JSON; redaction/helper chỉ thêm khi cần.
- Làm: key qua header thay query; parse text parts có kiểm tra kiểu/rỗng; tránh nhận thought/non-text parts làm explanation; xử lý finishReason/safety, null/invalid JSON và HTTP errors nhất quán.
- Đạt: không có key/prompt/output/contact trong log/error; multi-part text hợp lệ được nối đúng; 401/403/404/429/5xx và safety block thành lỗi gateway để Discovery fallback, không bịa output.
- Kiểm chứng: local HTTP fake server nhận request, assert URL/header/body; capture logs với key giả; fixtures single/multi/empty/non-text/blocked/truncated/invalid response. Token metadata thiếu vẫn chạy.

**B04 — Stub đúng ngữ cảnh và explanation có nghĩa** (M; phụ thuộc: B03)

- File: `StubLlmProvider`, `MatchExplanationService`, tests tương ứng; chỉ đổi API metadata nếu thật sự cần provenance.
- Làm: sửa ưu tiên routing; test bằng prompt matching thật; loại sample bịa 100% lịch/rating/kinh nghiệm. Offline mode phải mô tả rõ là giả lập hoặc dùng rule-based facts.
- Đạt: prompt matching không trả bio; chỉ Top 1 đi qua gateway; Top 2..N giữ fallback; response rỗng/lỗi không thành `aiGenerated=true`. Định nghĩa flag cho stub trong contract, không dùng flag đó một mình để chứng minh gọi Gemini.
- Kiểm chứng: real prompt -> stub -> gateway -> explanation test, success/failure/quota; capture prompt xác nhận không PII, không dữ liệu tự do chưa xác minh.

Checkpoint B: adapter có contract tests không cần internet; stub/fallback giúp test offline có ý nghĩa.

### Phase 2 — Log, deadline và quota

**B05 — Cô lập lỗi log AI tại commit** (M; phụ thuộc: B04)

- File: `AiRequestLogger`, `AiGatewayService`, transaction integration test tại app; tách collaborator transactional nếu cần để catch nằm ngoài proxy boundary.
- Làm: xác minh failure tại flush/commit, không chỉ repository.save mock; mỗi attempt có đúng một status kết thúc khi DB khả dụng. Outer matching rollback không xóa log provider attempt đã commit.
- Đạt: lỗi logger không làm mất kết quả rule-based; không ghi ERROR thứ hai chỉ vì ghi SUCCESS lỗi; không tuyên bố lưu log thành công khi DB hỏng. Quota fail-closed: usage store lỗi thì không gọi provider và vẫn trả fallback.
- Kiểm chứng: PostgreSQL test success/timeout/provider error/outer rollback/constraint hoặc commit failure; kiểm tra không để transaction matching bị rollback-only do AI logger.

**B06 — Quota atomic khi gọi đồng thời** (M; phụ thuộc: B05)

- File: public quota API nhỏ trong `ai.api`, reservation implementation/repository trong AI, migration mới tại app. Tách persistence contract và wiring thành hai commit con nếu vượt ~5 file.
- Làm: reservation theo actor + MATCH_EXPLANATION, rolling 24h; count attempts/reservations theo policy mục 2, kiểm tra ở transaction ngắn, không giữ DB lock trong lúc HTTP. Chuyển caller ở B07.
- Đạt: tại 9 lượt, hai request đồng thời chỉ một request được phép gọi; reservation stale có timeout/expiry; không sửa migration đã chạy; không thêm Redis chỉ để xử lý quota.
- Kiểm chứng: PostgreSQL concurrency với barrier, rollback/restart/stale reservation, boundary đúng 24h bằng Clock kiểm soát được; quota Student và Parent tách theo actor.

**B07 — Nối quota và kiểm chứng deadline end-to-end** (M; phụ thuộc: B06)

- File: `MatchExplanationService`, gateway/usage implementation cần điều chỉnh, deadline/quota tests.
- Làm: thay count-then-call bằng reservation; bỏ đường gọi có thể bypass; kiểm tra fake HTTP provider chậm và lỗi I/O cùng logger thật.
- Đạt: call provider bị giới hạn 15s; fallback không đổi score/rank; hết quota không gọi provider. Nêu riêng DB/pool/log overhead: deadline 15s của provider không đồng nghĩa toàn HTTP request hoàn tất tuyệt đối trong 15s.
- Kiểm chứng: lần 10 được gọi/lần 11 fallback, lỗi usage fallback, concurrent matching, fake server delayed/trickle response; không rely chỉ vào Mockito throw timeout.

Checkpoint C: failure isolation và quota có chứng cứ PostgreSQL; không đánh dấu pass nếu test bị skip.

### Phase 3 — Đủ dữ liệu để gọi matching từ frontend

**B08 — Student quản lý goal** (M; phụ thuộc: B01)

- File: controller/service goal mới, DTO request/response, `LearningGoalRepository`, tests. Nếu quá 5 file, chia tạo/đọc trước rồi update/archive.
- Làm: create/list/update/archive theo contract B01; actor lấy từ session, không tin studentId client; validate BR-12, catalogs, budget >= 0 và max > 0, min <= max, mode/area.
- Đạt: Student chỉ sửa goal của mình; Parent không ghi; deadline tương lai, desired > current theo scale schema/SRS; dùng archive để bảo toàn request/class lịch sử. Goal chưa slots được trả với lý do chưa sẵn sàng match, không giả báo profile complete.
- Kiểm chứng: service + HTTP validation + PostgreSQL persistence; owner/non-owner/Parent/Tutor; goal archived không match và không gửi Connection request mới được.

**B09 — Lưu lịch goal nguyên tử** (M; phụ thuộc: B08)

- File: `GoalAvailabilitySlot`, repository, goal service/controller, slot tests.
- Làm: endpoint replace slots; day 1..7, start < end, cùng ngày; quy định xử lý duplicate/overlap theo scorer, không đổi ý nghĩa phút overlap.
- Đạt: save all hoặc rollback all; response/query đọc lại slots; update goal của người khác bị chặn; không mất slot cũ khi payload mới sai.
- Kiểm chứng: time biên/chạm biên/duplicate/overlap/rỗng và DB rollback; tạo goal qua HTTP rồi set slots -> gọi match không cần insert SQL thủ công.

**B10 — Parent chọn goal đúng quyền** (M; phụ thuộc: B09)

- File: query endpoint tại Discovery, `ProfileFacade` + DTO/read implementation, authorization tests. Chia contract/read và HTTP wiring nếu cần giữ task nhỏ.
- Làm: lấy danh sách goal + readiness theo studentId sau khi xác nhận quyền; dùng ConnectionFacade hiện có. Không thêm dependency Profile -> Connection.
- Đạt: Parent linked đọc được; pending/rejected/revoked/unlinked bị chặn; revoke có hiệu lực ở lần đọc/match tiếp theo; dữ liệu trả tối thiểu, không contact của gia đình khác.
- Kiểm chứng: PostgreSQL link thật + query/match; architecture test không cycle; Student không đọc goal người khác bằng route này.

Checkpoint D: Student và Parent có thể lấy goalId hợp lệ qua HTTP, không phụ thuộc seed goal để hoàn thành luồng chính.

### Phase 4 — HTTP sẵn sàng tích hợp

**B11 — Session, CSRF và CORS nhất quán** (M; phụ thuộc: B01, B10)

- File: `SecurityConfig`, `WebConfig`, IAM CSRF endpoint nhỏ, full-filter security tests, auth contract. OpenAPI cookie schema sửa riêng cùng tài liệu ở B14 để giữ task nhỏ.
- Làm: bật CORS ở Security filter chain; bỏ blanket ignore `/api/v1/**`; hiện thực GET `/api/v1/auth/csrf` theo auth contract, materialize token/cookie; cấu hình SPA token handler phù hợp Spring Security đang dùng. Kiểm tra refresh token sau login/logout và cookie path.
- Đạt: allowed-origin preflight không cần session; protected POST cần session + token; thiếu/sai token 403 JSON; guest protected request có status được mô tả theo thứ tự filter; anonymous search/detail vẫn public. Chỉ exception CSRF thật sự cần thiết, không bỏ bảo vệ toàn API.
- Kiểm chứng: MockMvc full Spring context/filter, login lấy cookie thật -> csrf -> match; OPTIONS/credentials/header cho localhost:3000, 127.0.0.1:3000, origin lạ; login/logout session invalidation; không mock CurrentUser cho bằng chứng này.
- Bàn giao rõ host cookie: không trộn localhost và 127.0.0.1 trong một phiên. Đây là sửa backend contract, chưa sửa client.

**B12 — HTTP error và pagination regression** (M; phụ thuộc: B11)

- File: `GlobalExceptionHandler`, tests handler/controller; search sorting trong Profile nếu test chứng minh thiếu stable tie-break.
- Làm: map malformed JSON, UUID/type/enum/time query sai thành 400 nhất quán; giữ ApiError hiện hữu. Thêm unique tutorId tie-break cho search khi sort key bằng nhau.
- Đạt: invalid input không 500, không log secret/rejected password; page không duplicate/mất tutor trong dataset đứng yên có sort ties; không đổi tên field hoặc cấu trúc Page trong đợt tích hợp này.
- Kiểm chứng: HTTP parameterized matrix; PostgreSQL page 0/1/last với ties, blank filters, no results, unverified detail 404. Dataset có ghi mới đồng thời vẫn có giới hạn offset paging, phải ghi rõ.

Checkpoint E: full filter/security và HTTP payload/error có bằng chứng, docs không còn mô tả CSRF trái runtime.

### Phase 5 — Xuyên module và bàn giao

**B13 — Backend E2E Discovery -> Connection** (M; phụ thuộc: B07, B12)

- File: app HTTP integration test mới, fixture nhỏ và API collection/script. Tách test harness và business matrix nếu vượt ~5 file.
- Luồng: đăng nhập Student -> profile/cấp học -> tạo goal/slots -> search/detail/match -> gửi connection request -> Tutor accept -> class ACTIVE. Parent: invite/accept -> đọc goal/match/request -> revoke -> bị chặn.
- Đạt: gọi các service thật với PostgreSQL; provider fake HTTP có kiểm soát; AI/log/quota thực, không mock facade để che wiring. Khi Gemini 429/timeout/blocked, core flow vẫn đi tiếp với fallback.
- Kiểm chứng: đúng một request/class khi accept đồng thời; unauthorized không gọi provider; matching log/event sau commit đúng actor/goal; empty candidates không gọi AI; no N+1/cap regression bằng bộ tests hiện có.
- Test riêng lệch level: Discovery có thể rank tutor, nhưng Connection từ chối vì exact subject+level. Ghi eligibility rule và error trong contract; không làm mềm Connection hoặc siết hard filter Discovery âm thầm.

**B14 — Live Gemini smoke và runbook bàn giao** (M; phụ thuộc: B13)

- File: `scripts/smoke-ai-discovery.ps1` hoặc collection tương đương, `docs/ai-discovery-backend-runbook.md`, update docs index/API/flow.
- Làm: fixture local có Student/Parent/Tutor VERIFIED, catalogs, goals/slots và IDs rõ; không tự tạo Admin production, không reset DB/volume. Script nhận key qua env, không in key hoặc lưu vào collection; kiểm tra Models API rồi chạy một matching thật có quota trống. Sửa OpenApiConfig dùng EDUFIT_SESSION và public operation security cho search/detail/catalogs để schema khớp runtime.
- Đạt: ghi provider/model (không key), thời gian, response `aiGenerated`, AI SUCCESS log và explanation đúng facts; phân biệt live pass với mock pass. Restart chế độ stub và test fallback cũng có hướng dẫn. Nếu chưa có key, bàn giao offline pass + live pending, không ghi “AI hoàn tất”.
- Kiểm chứng: cold start Compose + Flyway + health, student/parent flow, forbidden/invalid/no-result/quota/fallback collection; đối chiếu route/DTO với code/OpenAPI; docs links và `git diff --check`.

Checkpoint F: frontend nhận được URL, auth/CSRF sequence, sample payload, test accounts/IDs local, errors và collection chạy được; không có thay đổi frontend.

## 5. Dependency và thứ tự triển khai

```text
B00 -> B01 -> B02 -> B03 -> B04 -> B05 -> B06 -> B07
          \-> B08 -> B09 -> B10 -> B11 -> B12
                    B07 + B12 -> B13 -> B14
```

Ưu tiên xử lý model/adapter/stub sớm để xác định khả năng gọi AI. Goal APIs có thể làm sau checkpoint B, nhưng phải xong trước nghiệm thu frontend-ready. Đề xuất thực thi tuần tự để hạn chế xung đột contract; không cần thêm agent/worktree cho bước lập kế hoạch hiện tại.

Nếu cần demo nhanh: B02-B04 + fixture có thể chứng minh matching offline/live riêng lẻ; **chưa đạt** backend-ready khi B08-B13 chưa xong. Không bỏ quota/security/ownership khỏi tiêu chí hoàn tất chỉ để demo chạy.

## 6. Kiểm chứng và Definition of Done

Chạy từ `backend`, JDK 21. Maven hiện trên PATH của máy có thể dùng Java 8; chỉ override env trong tiến trình test, không sửa cấu hình máy toàn cục.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn -B -ntp -pl platform/ai,modules/discovery,modules/connection -am test
mvn -B -ntp -pl app -am test
mvn -B -ntp -pl app -am verify
git diff --check
```

Không coi `test` tương đương `verify`: JaCoCo check ở verify nhưng POM hiện mặc định `jacoco.haltOnFailure=false`; BUILD SUCCESS không chứng minh đạt coverage 80%. Báo cáo coverage của module thay đổi, chạy gate bật bằng `-Djacoco.haltOnFailure=true` khi nghiệm thu và ghi rõ baseline module ngoài phạm vi nếu fail; không sửa toàn reactor chỉ để phủ coverage cho các module chưa xây. `FlywayMigrationIT` cần kiểm tra cấu hình runner và chạy explicit nếu không có Failsafe; tên *IT không mặc nhiên được Surefire default chạy. Khi dùng selector kèm `-am`, thêm `-Dsurefire.failIfNoSpecifiedTests=false` ở dạng quoting phù hợp PowerShell để module dependency không fail vì không có test trùng tên.

Ma trận tối thiểu:

| Nhóm | Ca bắt buộc | Bằng chứng |
| --- | --- | --- |
| Provider | success, multipart, empty, malformed, block, 401/403/404/429/5xx, chậm | Local HTTP server + log redaction assertions |
| Quota | 0/9/10, 24h boundary, concurrent, stale reservation, usage DB lỗi | Unit clock + PostgreSQL concurrency |
| Log/deadline | REQUIRES_NEW survive rollback, flush/commit failure, delayed/trickle HTTP | Spring proxy + PostgreSQL + fake HTTP |
| Goals | create/list/update/slots/archive, owner/Parent denied, invalid catalog/time/budget | HTTP + DB, không chỉ mock |
| Security | guest, Student, Parent confirmed/pending/revoked, Tutor, OPTIONS, CSRF missing/invalid/valid | Full security filter + session thật |
| Discovery | search/detail/match, topN, no results, VERIFIED-only, tie pagination, cap, stable scores | Existing tests + new integration regression |
| Connection | request exact-level eligible/ineligible, accept, duplicate/race, Parent revoke | Cross-module HTTP/DB |
| Live | model visible, real successful generation, correct facts, sanitized logs | Opt-in smoke bằng key môi trường |

Nghiệm thu cuối: không sửa frontend; mỗi task có evidence và test pass; không còn blocker mục 1; offline suite và core HTTP workflow chạy; live status ghi trung thực; docs khớp runtime; không đổi migration đã chạy; không merge main trong công việc này.

## 7. Rủi ro và giới hạn cần giữ trong bàn giao

- Key/model/project quota: chỉ live smoke xác nhận được. Model mặc định mới phải được kiểm lại tại thời điểm triển khai.
- Quota/log DB có ảnh hưởng pool: matching outer transaction + REQUIRES_NEW cần connection thứ hai; test đồng thời, không chỉ chạy một request.
- AI explanation bị hallucinate: prompt grounded và schema hợp lệ chưa chứng minh nội dung đúng; smoke review theo facts, fallback nếu output không dùng được. Không cần xây semantic verification engine ở phạm vi này.
- Candidate cap 200 không bảo đảm global best; giữ báo cáo ranking-loss hiện có.
- Session/CSRF thay đổi ảnh hưởng caller hiện tại: cung cấp sequence và collection trước khi frontend tích hợp, regression IAM/Connection/Profile cùng lúc.
- AiRequestLog có tokenCount trong response nhưng bảng hiện không lưu token/model; FR-05 chỉ yêu cầu time/user/status/latency. Không tự thêm billing dashboard hoặc migration token accounting vào scope.
- Notification/audit ngoài luồng matching vẫn có phần chưa hoàn tất theo Connection docs; không coi bàn giao này hoàn thành toàn hệ thống.

## 8. Baseline lần khảo sát này

- Đã kiểm tra repository sạch trước khảo sát, branch/base như đầu tài liệu; chưa gọi Gemini thật và chưa đọc/in secret.
- Lần chạy Maven đầu dùng Java 8 sai môi trường; đã chuyển JDK 21 cho tiến trình test.
- Trong sandbox, Mockito attach named pipe bị Access denied; chạy lại test ngoài sandbox để thu kết quả thực. Docker engine được kiểm tra read-only: 28.3.0.
- Kết quả baseline: **227 tests reactor module + 50 tests app chọn lọc = 277 pass, 0 fail/error/skip**. Có PostgreSQL/Testcontainers thật cho candidates, Discovery read/workflow và Flyway upgrade. Chi tiết và lệnh exact ở [checklist](todo.md); logs tại `../ai-discovery-baseline.log` và `../ai-discovery-app-baseline.log`.
- Chưa kiểm chứng Gemini live, full-application login/CORS/CSRF HTTP, cold-start Compose hoặc verify/coverage trong lượt này. Các app tests hiện có dùng context chuyên biệt; không dùng chúng để tuyên bố security filter end-to-end đã pass.
- Whitespace và link nội bộ của tài liệu mới đã kiểm tra, không phát hiện lỗi; working tree chỉ thêm plan/checklist và hai baseline logs, không sửa implementation/frontend.

## 9. Thực thi Phase 1 — 2026-10-12

Theo yêu cầu thực hiện Phase 1, đã hoàn thành B02–B04. B01 mới chốt phần AI trong [contract Phase 1](../../docs/ai-discovery-backend-contract.md); contract tổng goal/slots/security vẫn pending, không đánh dấu B01 hoàn tất.

- Đồng bộ default model `gemini-3.5-flash-lite` trong Java/YAML/Compose/example; provider sai hoặc Gemini thiếu key fail startup, model ID không nhận path/query. Compose có wiring base URL; Maven/IDE cần environment riêng.
- Adapter dùng header key; ghép text parts, bỏ thought/non-text, từ chối blocked/incomplete/empty response; phân biệt HTTP/I/O/timeout, không đưa raw body/cause vào exception hoặc log.
- Gateway truyền provenance. Stub không bịa facts; Discovery dùng breakdown thực và `aiGenerated=false` với stub/blank response. Ranking/scorer và quota hiện tại không đổi.
- Contract tests dùng HTTP server loopback thật, không gọi Google. Có TDD red/green logs; `phase1-stub-red.log` chủ động dùng test-failure-ignore để thu cả hai module nên phải đọc failure counts, không suy từ BUILD SUCCESS.
- Kết quả và giới hạn coverage ở [checklist](todo.md). Chưa live Gemini, chưa cold-start Compose hoặc full-filter security E2E; Phase 2 trở đi còn nguyên. Không sửa frontend, không commit/push/merge.
