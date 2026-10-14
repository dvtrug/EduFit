# Connection Phase 3 — validation

Branch `feat/connection`, base `ec7d11a`. Phase3 has5 parent tasks T11–T15, implemented as10 executable slices T11a/b, T12a/b/c, T13a/b/c, T14, T15. Each slice is committed/pushed after verify.

## Prerequisite: Discovery-AI integration

Discovery-AI content is already integrated into main through `f07d490` and PR19 (`d208371`). At the audit snapshot, origin/feat/discovery-ai=`f47e2ce`. AI, Discovery and migration directory Git trees on main match that branch byte-for-byte. The branch tip is not an ancestor due separate integration history; dry-run merging the old branch again has7 conflicts involving the removed monolith, adapted Profile test and newer Profile/frontend work. No missing AI implementation requires that redundant merge.

Current Connection includes main; E2E/API/architecture audit verify passed73 tests (Connection HTTP17, Discovery HTTP10, App46 including real PostgreSQL DiscoveryRead30, E2E7 and architecture9), no failures/errors/skips. Log `.codex-doc-review/discovery-ai-connection-audit.log`. DiscoveryQueryService differs only formatting, and existing frontend Connection writer/facade contracts remain compatible.

Current Discovery frontend formats desiredSlots as localized display strings; retain legacy-compatible rollout until client/data inventory permits strict-v1. This is the known T13a rollout requirement, not a reason to enable strict mode now.

## T11a — invitation consent/privacy

- Active actor, current recipient role/status after locks and active opposite-role inviter on accept; profile user/student IDs revalidated. Both invitation directions preserve consent: link created only on recipient accept.
- Pending pair query is bounded by the unique pending-pair invariant; latest directional decline query returns only one row. No full-history scan in invite flow. Unknown/inactive/same-role/self/pending/cooldown/confirmed targets retain identical generic success.
- Existing confirmed link closes pending invitation as WITHDRAWN and returns typed conflict; PostgreSQL proves closure commits before HTTP409 and honors outer rollback.
- Scoped invitation history adds reinviteAfter when present, allowing known-history cooldown visibility. Existing fields and pending/no-cooldown JSON shape stay unchanged; names/roles and batch hydrate remain T18c. This additive field fulfills T11a's cooldown acceptance and will be retained in T18c.
- RED:6 consent cases ran,3 failures for inactive recipient, inactive actor and missing system closure (`phase3-T11a-red.log`). GREEN affected reactor verify:182 tests (Connection143, Notification6, App33), no failures/errors/skips (`phase3-T11a-green.log`). Includes18 focused invitation cases,19 PostgreSQL Connection tests and7 main E2E tests.
- No publisher migration or new schema in T11a. Clock, lock order, default rollback and HTTP route shapes retained. Strict coverage remains a separately tracked gate; checkpoint verification follows the remaining slices.

## T11b — invitation transition events

- Invitation create/accept/decline now publish ID-only events. System already-linked closure publishes WITHDRAWN with a null actor; user withdraw/expiry dispatch remain their later task scope. No direct invitation notification writes remain.
- Mandatory BEFORE_COMMIT consumers create exactly one notification per transition; accepted notifications target the confirmed link, and declined notifications carry no student scope.
- Functional RED: three real PostgreSQL entrypoint cases failed before publisher migration (`phase3-T11b-red-functional.log`). An earlier test-context wiring failure was corrected separately; it is not functional RED evidence.
- GREEN:194 tests (Connection143, Notification6, App45), zero failures/errors/skips (`phase3-T11b-green.log`). Real PostgreSQL tests prove invitation create/accept/decline delivery, terminal retry deduplication, and link/invitation rollback when notification insertion fails. Existing business transaction tests use the production listener/writer through a test-only repository bridge; actual notification persistence is covered separately.
- Affected reactor verify succeeded. Coverage warnings remain tracked for the final checkpoint; this focused run does not establish the strict coverage gate.
