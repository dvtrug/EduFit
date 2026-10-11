# Connection Phase 2 — validation

Branch: `feat/connection`. Repository: `C:/Users/ASUS/Desktop/EduFit/EduFit`.
No changes to main/Discovery refs or their task documents. Existing user changes in `.gitignore` and backend implementation documentation are excluded from Connection commits.

## T08a — shared Clock

- Inject the existing IAM Clock into invitation, request, link, quota and query services; no additional production Clock bean.
- Capture one timestamp per operation, after locks where applicable. Invitation creation reuses quota consumption timestamp.
- RED: new fixed-clock quota regression did not compile before Clock injection (`.codex-doc-review/phase2-red-clock.log`).
- GREEN: affected app reactor verify, 58 Connection + 5 real PostgreSQL tests, zero failures/errors/skips (`phase2-T08a.log`). Context tests prove one UTC candidate and fixed override. Additional module verify covers deadline -1ns/equality/+1ns and checks one clock read (`phase2-T08a-module.log`).
- Review: no route/schema/authorization change, no sleep-based timing test, no unconditional Clock bean. Coverage warnings are not a strict coverage pass; full checkpoint verification follows T10b.

## T08b — invitation/link guards

- Central policy preserves 14-day invitation TTL, 7-day decline cooldown and 10 attempts/hour; no new configurable business behavior.
- Mutators reject terminal state and action at/after deadline before touching fields. Expiry remains idempotent. Link revoke requires CONFIRMED.
- RED: 19/25 matrix cases failed against previous mutators (`phase2-red-invitation.log`). GREEN: Connection affected reactor verify, 89 tests, zero failures/errors/skips (`phase2-T08b.log`). Includes 28 invitation/link state, timestamp, expiry and cooldown boundary cases.
- Review: guarded methods cannot change terminal timestamps/cooldown; existing service authorization/lock order remains unchanged. Expiry persistence/conflict semantics deliberately follow in T09.

## T08c — request guards

- Request TTL remains 7 days. Accept/reject/cancel require PENDING and time strictly before expiry; terminal retries leave state, reason and respondedAt unchanged. Expire remains idempotent.
- RED: 18/24 state/deadline cases failed (`phase2-red-request.log`). GREEN: affected Connection reactor verify, 113 tests, zero failures/errors/skips (`phase2-T08c.log`). Tests include every terminal/action combination and -1ns/equality/+1ns for each pending action.
- Review: same central policy as invitations, no schema/new status and no service authorization change.

## T09 — explicit transaction results

- Replace broad `noRollbackFor=ResourceConflictException` on invitation/request writes with sealed Success/Conflict results for expected business conflicts. Controller unwraps after the service proxy returns. Failed required writes throw normally and roll back.
- Request creation also returns typed duplicate/quota/active-class conflicts after expiring stale requests. No DB exception is caught inside a write transaction, no REQUIRES_NEW bypasses caller rollback.
- RED PostgreSQL regression proved previous code committed ACCEPTED when notification threw ResourceConflictException (`phase2-red-rollback.log`: expected PENDING, actual ACCEPTED).
- HTTP constraint/optimistic-lock mapping is scoped to Connection only; raw Hibernate constraint failures from flush and Spring-translated failures from commit are mapped outside the service transaction, without exposing SQL.
- PostgreSQL checks: invitation/request expiry persists despite HTTP409; outer rollback restores PENDING; create conflict persists stale expiry; notification conflict rolls back invitation+link and request+class; outer accept rollback leaves no class; forced class flush and deferred-commit constraint errors roll back and allow a later retry.
- Initial verification exposed raw Hibernate flush errors reaching global500; scoped mapping was corrected, not swallowed in the transaction (`phase2-T09.log`). Final affected reactor verify: 113 Connection + 13 PostgreSQL + 9 architecture tests, zero failures/errors/skips (`phase2-T09-final.log`).
- Review: public facade and ten routes keep existing success shapes; currentStatus remains internal. Returning Success inside an outer transaction is not a promise of committed data.

## T10a — public event contracts

- Four public immutable Java records under `connection.api.event`: invitation/link/request changes and class creation. Required event/aggregate identity, changed status and timestamp; nullable system actor, nullable creation oldStatus, nullable student scope for unresolved invitations.
- Recipient/context IDs support later publisher migrations. Invitation cause is a closed enum, including SYSTEM_ALREADY_LINKED; no free-text reason, email or message is published. A class-created event does not itself imply an additional accepted notification.
- RED: absent records failed contract tests (`phase2-red-events.log`). GREEN: affected app reactor verify, 125 Connection + 13 PostgreSQL + 9 architecture tests, zero failures/errors/skips (`phase2-T10a.log`). Twelve contract cases check exact payload whitelist/types, required envelope, nullable system actor and exact timestamp.
- No publisher migration in this task. Records document in-memory delivery limits, not durable/exactly-once guarantees.

## T10b — transactional notification foundation

- Add one-way notification -> Connection public event dependency. Internal writer uses MANDATORY; listener uses BEFORE_COMMIT with fallbackExecution=false. Event notification uses the business occurredAt; legacy sink construction keeps its existing timestamp behavior.
- Only invitation-created dispatch is prepared; there is no production event publisher in Phase 2. Existing direct sinks remain intact. Expand other transitions/recipients with T11b/T12c and request/expiry publisher tasks. Class-created has no accepted-notification consumer. This is infrastructure evidence, not a claim that the final recipient matrix or notification read authorization is complete.
- RED: missing listener/writer failed compile (`phase2-red-listener.log`). GREEN: affected app reactor verify, 125 Connection + 13 Connection PostgreSQL + 7 notification PostgreSQL + 9 architecture tests, no failures/errors/skips (`phase2-T10b.log`).
- Real PostgreSQL verifies delayed BEFORE_COMMIT keeps both rows invisible until commit, publication outside transaction has no fallback, writer rejects missing transaction, outer rollback suppresses listener, notification FK/flush failure and deferred constraint failure roll back the business row. Synthetic class-created/unmigrated transitions do not duplicate notifications.
- Additional module tests assert notification recipient/student/target/type/timestamp and dormant transition dispatch; included in final clean reactor checkpoint.
- Review: listener imports public event API only, no reverse dependency, no REQUIRES_NEW/async/external side effect, no changes to the existing direct sink. Event IDs are not a durable replay/deduplication store.

## Checkpoint 2 — final evidence

Functional implementation T08a–T10b is complete. Phase 3 has not started.

| Verification | Result |
| --- | --- |
| `backend/mvnw.cmd -f backend/pom.xml clean verify -B -ntp` (JDK21, Windows-ROOT truststore) | PASS: 437 tests, 0 failures, 0 errors, 0 skips; log `.codex-doc-review/phase2-full-verify.log` |
| Connection module | 125 tests pass |
| Notification module | 6 tests pass |
| App module | 70 tests pass, including 13 Connection PostgreSQL and 7 Notification PostgreSQL tests; default verify also covers Discovery integration and architecture |
| `verify -DskipTests -Djacoco.haltOnFailure=true` using full-run data | FAIL at Shared coverage gate; subsequent modules skipped by Maven; log `.codex-doc-review/phase2-strict-verify.log` |

The default build has `jacoco.haltOnFailure=false`; its success is not a strict coverage pass. Full-run module counters:

| Module | Lines | Branches |
| --- | --- | --- |
| Shared (unchanged baseline) | 78/103 = 75.73% | 20/28 = 71.43% |
| Connection | 385/486 = 79.22% | 99/194 = 51.03% |
| Notification | 17/23 = 73.91% | 3/4 = 75.00% |

All three remain below the configured80% thresholds. App integration execution data is not aggregated into module-local JaCoCo reports. No threshold/exclusion was changed. Comprehensive remaining use-case/state/recipient/race coverage continues in the planned tasks; this checkpoint does not certify the strict gate.

Default Surefire discovery does not select `FlywayMigrationIT`; it was not rerun in Phase2. Flyway SQL is unchanged, and its clean/legacy-main-upgrade evidence remains in the separate hotfix report. No runtime startup smoke, frontend/browser testing, scheduler, final recipient matrix, CSRF change or Phase3 behavior is claimed by this report.

### Branch isolation and review

- Local main remains `f077e55bae2c4ff1bc74f29aee30610e31c5a53b`; origin/main tracking ref remains `6755121122c79dc9705282225d14ffb76e17c29a`.
- Local/origin feat/discovery remain `dd9068e0a14b0e480d1823eab37bc82041166fe5`.
- `tasks/plan.md` and `tasks/todo.md` compare byte-for-byte equal to Discovery. Stashes `dffaedcf20f8081f0fe1af68c4b3070a5db1f917`, `6f2a3f29f0cfd5e979a4d8813b2a6df8581e86ee`, `fdd92d7a849547b731fad81b105b54330d35357a` are preserved.
- Existing user `.gitignore` and backend implementation document changes are excluded. Only feat/connection is pushed; no merge to another branch.
- Review covered state/time correctness, transaction and privacy boundaries, dependency direction, error mapping, and unchanged lock ordering. No schema/migration/public HTTP success shape change; no new external side effect or query loop. `git diff --check` passes for owned changes (user `.gitignore` excluded).

### Per-task commits

| Task | Commit |
| --- | --- |
| T08a | `76824a5` — reuse shared clock |
| T08b | `f6ff997` — invitation/link guards |
| T08c | `09619a8` — request guards |
| T09 | `612fa82` — explicit expiry/write rollback |
| T10a | `74db6a8` — event contracts |
| T10b | This report accompanies `feat(notification): consume connection transitions transactionally` |

Each executable task has its own commit and push; no parent T08/T10 aggregation commit.
