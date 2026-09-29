# Authentication Implementation Checklist

## Phase 1: Foundation

- [x] T01 Repair Docker, database environment variables, Maven Wrapper, and local mail runtime.
- [x] T02 Freeze the auth API contract, redirects, stable error codes, and threat model.
- [x] Checkpoint: clean local runtime starts and existing builds remain green.

## Phase 2: Persistence and primitives

- [ ] T03 Add PostgreSQL migrations for accounts, one-time tokens, and Spring Session JDBC.
- [ ] T04 Implement IAM account persistence, normalized email, account states, roles, and `UserDetailsService`.
- [ ] T05 Implement secure token generation/hash/expiry/consumption and account-email abstraction.
- [ ] Correct ArchUnit package patterns and prove the rules catch an intentional violation.
- [ ] Checkpoint: migration, repository, token, and architecture tests pass.

## Phase 3: Backend authentication

- [ ] T06 Implement registration, email verification, and resend-verification.
- [ ] T07 Implement login, failed-attempt lockout, and JDBC-backed session persistence.
- [ ] T08 Implement `/me`, logout, cookie policy, and CSRF bootstrap/enforcement.
- [ ] T09 Implement forgot password, reset password, and authenticated password change.
- [ ] Checkpoint: UC1.1-UC1.4 backend integration tests and OpenAPI contract pass.

## Phase 4: Frontend authentication

- [ ] T10 Add `features/auth`, generated API contract, same-origin proxy, and CSRF-aware client.
- [ ] T11 Build registration, check-email, verify-email, and resend screens.
- [ ] T12 Build login, session bootstrap, safe return URL, route guards, and logout.
- [ ] T13 Build forgot-password, reset-password, and change-password screens.
- [ ] Checkpoint: browser E2E covers register -> verify -> login -> protected page -> logout.

## Phase 5: Hardening and release

- [ ] T14 Add rate limiting, security-event logs, security headers, and dependency-audit triage.
- [ ] T15 Run the clean-environment release gate and update documentation/CI.
- [ ] Verify password change invalidates other sessions while preserving the current session.
- [ ] Verify password reset invalidates every active session.
- [ ] Verify no raw passwords, one-time tokens, or session IDs appear in logs or persistence.
- [ ] Final checkpoint: backend verify, frontend lint/test/build, and auth E2E all pass.

## Decisions to Confirm

- [ ] Use Mailpit for local account emails.
- [ ] Keep `409 USER_ALREADY_EXISTS` for registration conflicts.
- [ ] Choose PostgreSQL-backed rate limiting if deployment may run multiple backend instances.
- [ ] Confirm temporary role landing paths: `/student`, `/parent`, `/tutor`, `/admin`.
