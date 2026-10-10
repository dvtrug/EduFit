# Discovery: Commit Checklist

Nguồn chi tiết: `tasks/plan.md`. Mỗi task tương ứng một commit; chỉ đánh dấu xong sau khi kiểm tra và review diff. Code/test của cùng task nằm trong cùng commit.

## Phase 0 — Plan và baseline

- [x] T00: Review plan cuối, kiểm tra branch/dirty files/Connection base và ghi baseline tests. Commit `docs(discovery): finalize phased refactor plan`.

## Phase 1 — Structure và contract

- [x] T01: Move/rename matching log entity + repository; giữ mapping; compile/service tests. Commit `refactor(discovery): normalize matching log persistence packages`.
- [x] T02: Public facade actor-aware + summary DTO + event contract; compile/API dependency review. Commit `feat(discovery): define public matching facade and event contracts`.
- [x] T03: Application input/output tách web DTO; controller mapping và REST JSON regression. Commit `refactor(discovery): decouple application services from HTTP DTOs`.
- [x] Checkpoint 1: Contract và REST regression pass.

## Phase 2 — Candidate data

- [x] T04: Education-level ordering metadata; tests ID không liên tục/exact/adjacent/other. Commit `feat(profile): expose education level ordering for matching`.
- [x] T05: Bounded candidate API/query + batch hydration; PostgreSQL filter/cap/duplicate tests. Commit `perf(profile): add bounded discovery candidate queries`.
- [x] Checkpoint 2: Dữ liệu scorer đủ, query có giới hạn và ordering ổn định.

## Phase 3 — Domain policy

- [x] T06: Move MatchScorer và test sang domain/policy; test hành vi hiện tại. Commit `refactor(discovery): move match scorer into domain policy`.
- [x] T07: MatchWeights + 5-factor scoring + breakdown mapping; parameterized boundaries/time/rating/tie-break tests. Commit `feat(discovery): implement five-factor matching policy`.
- [x] Checkpoint 3: Policy thuần Java và công thức 5 yếu tố đúng.

## Phase 4 — Workflow

- [x] Prerequisite T08: ConnectionFacade confirmed-link implementation hiện diện trên base; ghi commit nguồn và kiểm tra dependency cycle.
- [x] T08: Student ownership + Parent confirmed link; test allowed/denied/link states. Commit `fix(discovery): authorize parent matching through confirmed links`.
- [x] T09: Matching dùng bounded candidates; topN/empty result/order/service tests. Commit `perf(discovery): match against bounded profile candidates`.
- [x] T10: FacadeImpl + matching log/event + transaction commit/rollback tests. Commit `feat(discovery): expose matching facade and publish execution events`.
- [ ] T11: AI Top 1/quota/grounded prompt/15s timeout/fallback verification. Commit `test(discovery): verify grounded AI explanations and fallback`.
- [ ] Checkpoint 4: REST/facade matching, authorization, AI, log và event pass.

## Phase 5 — Integration và architecture

- [ ] T12: PostgreSQL search/detail/filter/pagination/OSIV/query count/p95/candidate cap ranking report. Commit `test(discovery): verify search hydration and candidate performance`.
- [ ] T13: ArchUnit đúng package; module/layer boundaries và dependency cycle checks. Commit `test(discovery): enforce module and layer boundaries`.
- [ ] T14: Xóa matching full-scan API cũ khi hết caller; reactor regression và diff check. Commit `refactor(profile): remove obsolete full-scan matching API`.
- [ ] Checkpoint 5: PostgreSQL evidence, regression và architecture checks hoàn tất.

## Phase 6 — Handover docs

- [ ] T15: Docs package/API/scoring/authorization/AI/log/event/performance/Q&A; diagram ảnh và link verification. Commit `docs(discovery): document matching architecture and execution flows`.

## Kiểm tra trước mỗi commit

- Đọc docs và code liên quan task.
- Chạy test phù hợp; ghi rõ test chưa chạy và nguyên nhân.
- Review diff; chỉ stage file trong task; không stage thay đổi IAM AccountStatus.
- Cập nhật checkbox task và báo SHA + phạm vi + kết quả test cho người review.

## Bàn giao cuối

- [ ] Discovery/Profile/Connection tests và app architecture tests pass.
- [ ] PostgreSQL integration/transaction/query count đã chạy; báo cáo p95 và ảnh hưởng candidate cap có bằng chứng.
- [ ] REST field cũ tương thích, breakdown mới có docs/test.
- [ ] Docs/diagram ảnh khớp code cuối; git diff --check pass.
- [ ] Các task có commit riêng; không có thay đổi ngoài phạm vi trong commits.
