# AI provider contract — Phase 1

## Contract tích hợp đã chốt (B01, Phase 1–4)

- [Goals/slots/readiness và Parent authorization](ai-discovery-phase3-contract.md): schema, validation, payload mẫu và quyền. Goal thiếu slots vẫn ACTIVE, readiness false; không thêm DRAFT.
- [Quota/log/deadline](ai-discovery-phase2-contract.md): 10 attempts/24h cho MATCH_EXPLANATION, provider errors vẫn tính quota.
- [HTTP session/CSRF/CORS và safe input errors](ai-discovery-phase4-contract.md): gọi `/api/v1/auth/csrf` trước login và sau login/logout; mọi write cần session/CSRF theo quyền tương ứng. Các giới hạn IAM hiện hữu được ghi rõ ở đó.
- [Discovery API](discovery-api-contract.md): search/detail/match và Parent goal selection; search có tutorId tie-break, không thay Page schema.

Mapping Discovery -> Connection dùng controller hiện hữu, không thêm endpoint hoặc dependency ngược Profile -> Connection:

1. Chọn `goalId` từ API goals và `tutorId` từ card/match. Gửi `POST /api/v1/connections/requests` với `{ "goalId": "UUID", "tutorId": "UUID", "desiredSlots": ["Monday 09:00-10:00"], "message": "Tùy chọn" }`, session và CSRF.
2. `desiredSlots` hiện là danh sách chuỗi hiển thị, không phải slot UUID hay object availability. Phải có ít nhất một chuỗi không blank, mỗi chuỗi <=100 ký tự; message <=500 ký tự. Connection chưa parse hoặc kiểm tra overlap nội dung chuỗi này. Không coi điểm scheduleFit của Discovery là xác nhận lịch booking.
3. Student phải sở hữu goal; Parent phải có link CONFIRMED. Goal ACTIVE, Tutor VERIFIED và **exact subject + education level**. Discovery có thể rank gia sư lệch cấp; Connection sẽ trả 400 `INVALID_OPERATION` khi không đạt exact eligibility. Không làm mềm quy tắc này trong tích hợp.
4. Response tạo request là 200 ApiResponse chứa requestId/studentId/tutorId/goalId/status/createdAt/expiresAt. Tutor gọi `POST /api/v1/connections/requests/{requestId}/respond` với `{ "accept": true }`; accept tạo class ACTIVE trong cùng transaction và trả classId/studentId/tutorId/goalId. Khi Student thu hồi link, Parent mất quyền ở request tiếp theo.

Nguồn: [ConnectionController](../backend/modules/connection/src/main/java/vn/edufit/connection/web/ConnectionController.java), [ConnectionService](../backend/modules/connection/src/main/java/vn/edufit/connection/application/ConnectionService.java), [Connection flow](connection-module-flow.md). Contract được đọc từ implementation; nghiệm thu HTTP xuyên module/populated workflow và Gemini live vẫn thuộc B13/B14 (Phase 5).

Cập nhật 2026-10-12. Tài liệu này ghi phạm vi Phase 1: B02–B04 trong [kế hoạch](../tasks/ai-discovery/plan.md). Goals/slots/readiness và Parent goal selection hiện được bổ sung trong [Phase 3 contract](ai-discovery-phase3-contract.md); Discovery giữ [API contract](discovery-api-contract.md).

## Cấu hình và khởi động

| Biến | Mặc định | Hành vi |
| --- | --- | --- |
| AI_PROVIDER | stub | Chỉ nhận chính xác `stub` hoặc `gemini`; giá trị sai/rỗng làm startup fail |
| GEMINI_API_KEY | rỗng | Bắt buộc khi provider=gemini; key đơn lẻ không tự bật Gemini |
| GEMINI_MODEL | gemini-3.5-flash-lite | Model ID không gồm `models/`, path hoặc query; ghi đè khi project dùng model khác |
| GEMINI_BASE_URL | https://generativelanguage.googleapis.com | Chỉ override cho môi trường provider/fake server do backend quản lý; không nhận từ HTTP request |
| edufit.ai.timeout | 15s | Deadline chờ provider và connect/read timeout; không bao gồm thời gian DB/log |

Default mới được đối chiếu với [danh sách model stable](https://ai.google.dev/gemini-api/docs/models) và [Gemini 3.5 Flash-Lite](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite) ngày 2026-10-12. Chưa gọi Models API bằng key thật trong Phase 1; quyền model/quota của project phải được kiểm tra trong live smoke B14. Model cũ `gemini-1.5-flash` không còn dùng làm default.

**Docker Compose:** `.env` ở root repository được Compose đọc và chuyển các biến AI vào backend container. Sửa `.env` theo `.env.example`, chọn provider=gemini và cấu hình key chỉ trên máy; chạy `docker compose up --build -d backend`. Không dùng `docker compose config` để gửi output vào chat/log vì output có thể chứa secret đã resolve.

**Maven/IDE:** Spring Boot hiện không tự đọc `.env`. Truyền biến vào environment của process hoặc Run Configuration, dùng Java 21. Không đặt key trong command-line argument, source, fixture hay collection. Chỉ điền key sẽ tiếp tục chạy stub nếu chưa đặt AI_PROVIDER=gemini. Khi chọn gemini nhưng thiếu key, backend chủ động fail startup; lỗi provider sau startup vẫn có fallback.

## Public Java API và provenance

`AiGateway.complete(AiRequest)` giữ chữ ký. `AiResponse` bổ sung `generatedByModel` bên cạnh text/latencyMs/tokenCount:

- Gemini response hoàn tất, có text hợp lệ: true.
- Stub: false, tokenCount=null vì không gọi model thật.
- Constructor cũ `new AiResponse(text, latencyMs, tokenCount)` và factory `AiResponse.of(...)` vẫn có để caller/test double cũ biên dịch, mặc định true. Constructor này chỉ dùng cho kết quả model-backed; caller tạo stub phải truyền false bằng constructor bốn tham số.
- `LlmResult` nội bộ có cùng metadata, giữ constructor hai tham số tương thích source.
- Đây là contract trong JVM, không phải endpoint AI công khai hoặc field mới trên HTTP Discovery. Rebuild reactor khi thay JAR; không cam kết binary compatibility với JAR đã biên dịch bằng record cũ.

Discovery không dò chuỗi văn bản hoặc kiểm tra tên provider để quyết định flag. Nếu generatedByModel=false hoặc text rỗng, `MatchExplanationService` dùng năm điểm thực trong ruleBased và trả `aiGenerated=false`. AI thật trả text hợp lệ mới có `aiGenerated=true`. Top 2..N tiếp tục dùng rule-based; scorer/ranking không đổi.

## Gemini HTTP boundary

- POST `/v1beta/models/{model}:generateContent`, key qua header `x-goog-api-key`; không đặt key trong query URL.
- Body gồm systemInstruction tùy chọn, contents/user text và generationConfig.temperature khi có. Không tự gửi contact/bio/PII; Discovery chỉ gửi mode, học phí và breakdown đã xác minh.
- Chỉ nhận candidate đầu tiên với finishReason=STOP. Thiếu finishReason, MAX_TOKENS, safety block hoặc lý do kết thúc khác đều bị coi là chưa có câu trả lời dùng được.
- Ghép text parts theo thứ tự, bỏ thought parts và non-text parts; không biến số/object thành văn bản. Không có text hoặc chỉ whitespace -> unavailable. Token metadata thiếu/sai kiểu/âm -> null, không làm hỏng text hợp lệ.
- 401/403/404/429/5xx -> AiUnavailableException; timeout kể cả khi đọc body -> AiTimeoutException. Không retry tự động. Resource I/O/JSON sai/empty/safety block -> unavailable; Discovery bắt lỗi và dùng fallback.
- Không truyền response body, prompt/output, URL lỗi hoặc raw cause ra khỏi adapter. Log gateway/matching chỉ chứa loại lỗi, status/latency và actor/feature cần audit; AiProperties.toString che cấu hình Gemini. Không bật wire/debug logging của HTTP client khi dùng secret thật.
- Theo [GenerateContent schema](https://ai.google.dev/api/generate-content) và [API authentication](https://ai.google.dev/api). Strict STOP là chính sách của EduFit để tránh hiển thị câu giải thích bị cắt, không phải yêu cầu rằng mọi API consumer phải từ chối MAX_TOKENS.

## Offline behavior, quota và giới hạn

Stub nhận diện explanation trước bio vì prompt matching cũng chứa “gia sư”. Sample stub không bịa kinh nghiệm, lịch hoặc rating; Discovery thay sample bằng rule-based facts. Stub vẫn đi qua gateway, log SUCCESS và cách tính quota hiện có để test workflow nhất quán. SUCCESS ở chế độ stub không chứng minh model thật đã chạy; live smoke phải xác nhận cấu hình provider và request thật.

Tại thời điểm Phase 1, quota còn count-then-call. [Phase 2](ai-discovery-phase2-contract.md) thay bằng reservation atomic bắt buộc trong gateway và xử lý logger commit failure/DB failure isolation. Phase 1 riêng lẻ không giải quyết các race này; đọc contract Phase 2 cho runtime hiện tại.

Backend chưa frontend-ready chỉ nhờ Phase 1–3: còn full-filter CSRF/CORS/session, HTTP validation matrix và cross-module E2E trong kế hoạch. Goals/slots và Parent goal selection đã có contract Phase 3 riêng. AI bio UC1.7 chưa có consumer HTTP hoàn chỉnh.

## Kiểm chứng

Tests: AiConfigurationTest (Spring provider selection/startup validation), GeminiLlmProviderTest (HTTP server thật trên loopback, request/response/errors/timeout/log privacy), StubLlmProviderTest và real stub -> gateway -> MatchExplanationService test. DB logger/usage được mock trong pipeline test; không nhận đây là PostgreSQL transaction test hoặc live Gemini smoke.

Chạy từ backend bằng JDK 21:

```powershell
mvn -B -ntp -pl platform/ai,modules/discovery,modules/connection -am test
```

Kết quả thực thi và lệnh app/coverage được lưu trong [checklist](../tasks/ai-discovery/todo.md).
