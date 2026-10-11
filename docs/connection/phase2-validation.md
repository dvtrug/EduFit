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
