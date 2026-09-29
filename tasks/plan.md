# Implementation Plan: EduFit Authentication

## Overview

Build the complete authentication scope defined by UC1.1-UC1.4 as one sequence of vertical slices: account registration and email verification, login with a revocable server-side session, logout, forgot/reset password, and authenticated password change. The implementation must cross PostgreSQL, the `iam` Maven module, the Spring Boot `app` composition root, the generated API contract, and the Next.js frontend while preserving the module boundaries in ADR-002.

The target is not merely a collection of screens. Completion means a user can register, verify the email, log in, refresh a protected page without losing the session, log out, reset or change the password, and observe correct invalidation of other sessions.

## Scope

### Included

- UC1.1 Register Account, verify email, and resend verification.
- UC1.2 Login using email and password.
- UC1.3 Logout from the current device.
- UC1.4 Forgot password, reset password, and change password.
- Roles `STUDENT`, `PARENT`, `TUTOR`, and provisioned `ADMIN`.
- Account status, login lockout, rate limiting, CSRF protection, session expiry, and security-event logging.
- Backend API, OpenAPI contract, frontend forms, protected-route behavior, and automated tests.

### Explicitly deferred

- Zalo or other social login. The detailed UC1.2 specification states email/password only.
- Tutor and Student profile creation. Authentication redirects by role; profile-completeness routing is integrated when the `profile` module exists.
- Production email provider selection. Development uses an `AccountEmailSender` abstraction with a local adapter; production SMTP/provider credentials remain environment configuration.
- JWT and refresh tokens. They remain an experimental branch and do not block the primary stateful-session implementation.

## Architecture Decisions

1. **Stateful opaque sessions:** use Spring Session JDBC backed by PostgreSQL. The browser receives only an HttpOnly session cookie; identity and role remain server-side.
2. **Session policy:** expire after 60 minutes of inactivity. Password reset/change invalidates all other sessions; the logged-in password-change flow keeps the current session.
3. **Cookie policy:** use a dedicated cookie name such as `EDUFIT_SESSION`, `HttpOnly`, `SameSite=Lax`, root path, and `Secure=true` outside local development.
4. **CSRF:** do not blanket-ignore `/api/v1/auth/**`. Provide a CSRF bootstrap endpoint and require the token for login, logout, registration, resend, reset, and password change.
5. **Account model:** one immutable role per account; statuses `PENDING_VERIFICATION`, `ACTIVE`, and `DEACTIVATED`; lockout is represented separately by `failed_login_attempts` and `locked_until`.
6. **Password hashing:** retain Argon2. Never log passwords, raw tokens, session IDs, or password hashes.
7. **One-time tokens:** generate at least 256 random bits, return the raw value only through the email link, store only a SHA-256 hash, and atomically mark it used. Verification expires in 24 hours; password reset expires in 30 minutes.
8. **No account enumeration:** login returns a generic credential error; forgot-password returns the same successful response whether an account exists or not.
9. **Email failure behavior:** account creation is committed as `PENDING_VERIFICATION` before delivery is attempted. Delivery failure leaves a recoverable account and allows resend, matching the SRS.
10. **IAM boundary:** public contracts live in `vn.edufit.iam.api`; web, application, domain, and infra remain internal. Other modules may import only `iam.api`.
11. **Same-origin frontend API:** the browser calls `/api/...` through the Next.js proxy/rewrite with `credentials: include`. The backend URL is environment-driven rather than embedded in components.
12. **API contract as source of truth:** expose auth DTOs in OpenAPI and generate the frontend contract. Do not maintain parallel handwritten Java and TypeScript response shapes.

## Target API Contract

| Method | Endpoint | Purpose | Authentication |
|---|---|---|---|
| `GET` | `/api/v1/auth/csrf` | Issue/read CSRF token | Public |
| `POST` | `/api/v1/auth/register` | Create pending account and request verification email | Guest |
| `POST` | `/api/v1/auth/verify-email` | Consume verification token and activate account | Guest |
| `POST` | `/api/v1/auth/resend-verification` | Replace previous token and resend email | Guest |
| `POST` | `/api/v1/auth/login` | Authenticate and persist the SecurityContext in the session | Guest |
| `GET` | `/api/v1/auth/me` | Return current identity and role | Authenticated |
| `POST` | `/api/v1/auth/logout` | Invalidate the current session | Authenticated or expired-safe |
| `POST` | `/api/v1/auth/forgot-password` | Request reset without exposing account existence | Guest |
| `POST` | `/api/v1/auth/reset-password` | Consume reset token and set a new password | Guest |
| `POST` | `/api/v1/auth/change-password` | Change password while logged in | Authenticated |

Success responses use the existing `ApiResponse` envelope; errors use `ApiError` and stable error codes. Passwords and tokens must never appear in response payloads.

## Dependency Graph

```text
Runtime and Docker consistency
        |
        v
Database migration and Spring Session tables
        |
        v
IAM account and token persistence
        |
        +-------------------+
        |                   |
        v                   v
Registration/verification  Login/session/logout
        |                   |
        +---------+---------+
                  v
          Password management
                  |
                  v
       OpenAPI-generated frontend client
                  |
                  v
       Auth screens and route protection
                  |
                  v
        End-to-end security verification
```

## Task Plan

### Phase 1: Runtime and contract foundation

#### Task 1: Make the local runtime reproducible

**Description:** Repair the existing local runtime before auth depends on it. Align Compose database variables with `application.yml`, add the missing backend Dockerfile and Maven Wrapper, and add a development mail service or documented local mail adapter.

**Acceptance criteria:**

- [ ] `docker compose up --build` starts PostgreSQL and the backend with matching database settings.
- [ ] Backend health becomes `UP` without requiring a manually created local database.
- [ ] Account emails can be inspected locally without production credentials.

**Verification:**

- [ ] `docker compose config`
- [ ] `docker compose up --build -d`
- [ ] `GET http://localhost:8080/actuator/health` returns `UP`.

**Dependencies:** None.

**Files likely touched:** `compose.yaml`, `backend/Dockerfile`, `backend/.mvn/**`, `backend/mvnw*`, `backend/app/src/main/resources/application.yml`, `.env.example`.

**Estimated scope:** Medium, split Docker/Maven Wrapper and mail setup into separate commits if necessary.

#### Task 2: Freeze the authentication contract and threat model

**Description:** Record endpoint schemas, response codes, session/cookie behavior, abuse cases, and redirect rules before implementation. Resolve naming once so backend and frontend can work against the same contract.

**Acceptance criteria:**

- [ ] Every endpoint in the target contract has request, success, and error schemas.
- [ ] Abuse cases cover enumeration, brute force, token replay, session fixation, CSRF, open redirect, and concurrent token use.
- [ ] Stable error codes are defined for inactive account, locked account, invalid/expired token, and rate limit.

**Verification:**

- [ ] Contract review confirms no password, hash, token, or session identifier is returned.
- [ ] Redirect parameters accept only local allowlisted paths.

**Dependencies:** Task 1 may proceed in parallel.

**Files likely touched:** `docs/authentication-contract.md`, `backend/platform/shared/.../ErrorCode.java`, OpenAPI annotations/configuration.

**Estimated scope:** Small.

### Checkpoint 1: Foundation

- [ ] Local backend and database start reproducibly.
- [ ] Authentication API and security rules are reviewed before schema/code work.
- [ ] `mvn -B -ntp test` and `npm run build` still pass.

### Phase 2: Persistence and security primitives

#### Task 3: Add account, token, and session migrations

**Description:** Replace the empty initial migration with the minimum schema for accounts, one-time tokens, and Spring Session JDBC. Add database constraints for normalized email uniqueness, one role, token uniqueness, expiry metadata, and valid account states.

**Acceptance criteria:**

- [ ] A clean PostgreSQL database migrates successfully.
- [ ] Duplicate normalized emails are rejected by the database.
- [ ] Raw verification/reset tokens are never stored.
- [ ] Spring Session tables and cleanup indexes exist.

**Verification:**

- [ ] Testcontainers migration test starts from an empty PostgreSQL database.
- [ ] Constraint tests cover duplicate email and invalid enum/status values.

**Dependencies:** Task 2.

**Files likely touched:** `backend/app/src/main/resources/db/migration/V1__init_schema.sql` plus a migration integration test.

**Estimated scope:** Medium.

#### Task 4: Implement IAM account persistence

**Description:** Create the IAM account model, role/status enums, repositories, email normalization, and Spring `UserDetailsService`. Keep JPA classes in IAM infrastructure and expose only identity DTOs/interfaces through `iam.api`.

**Acceptance criteria:**

- [ ] Account lookup is case-insensitive through one canonical normalized email.
- [ ] `EduFitUserDetails` reflects ACTIVE, locked, and deactivated states correctly.
- [ ] Role comes from persisted server-side data and cannot be supplied during login.

**Verification:**

- [ ] Repository integration tests run against PostgreSQL.
- [ ] Unit tests cover normalization and account-state mapping.

**Dependencies:** Task 3.

**Files likely touched:** `backend/modules/iam/src/main/java/vn/edufit/iam/{api,domain,infra}/**` and `backend/app/.../EduFitUserDetails.java`.

**Estimated scope:** Medium; split persistence and Security adapter if the change exceeds five focused files.

#### Task 5: Implement one-time-token and email primitives

**Description:** Implement secure token generation/hashing, token expiry and consumption, an `AccountEmailSender` port, and a local mail adapter. Ensure replacing a token invalidates the previous one and concurrent consumption succeeds only once.

**Acceptance criteria:**

- [ ] Tokens contain at least 256 bits of randomness and only their hashes are persisted.
- [ ] Expired, used, replaced, and malformed tokens are rejected consistently.
- [ ] Email bodies contain frontend URLs from configuration, never hard-coded deployment hosts.

**Verification:**

- [ ] Unit tests cover token lifecycle and expiry using an injected `Clock`.
- [ ] Concurrency/integration test proves a token cannot be consumed twice.

**Dependencies:** Tasks 3-4.

**Files likely touched:** IAM token domain/application/infra packages and mail configuration.

**Estimated scope:** Medium.

### Checkpoint 2: Security primitives

- [ ] Clean-database migration succeeds.
- [ ] Token and account tests pass.
- [ ] No raw password/token/session values appear in logs or database rows.
- [ ] Correct the ArchUnit package patterns before relying on the architecture test.

### Phase 3: Backend vertical slices

#### Task 6: Deliver registration and email verification

**Description:** Implement register, verify-email, and resend-verification end to end. Registration allows only Student, Parent, or Tutor, creates `PENDING_VERIFICATION`, hashes the password, and attempts email delivery after the account transaction commits.

**Acceptance criteria:**

- [ ] Valid registration creates one pending account with an Argon2 hash.
- [ ] Verification activates the account and invalidates the token atomically.
- [ ] Resend invalidates the old token and is limited to one request per minute.
- [ ] Email failure leaves the account pending and recoverable.

**Verification:**

- [ ] Controller tests cover validation, duplicate email, forbidden ADMIN registration, invalid/expired token, and resend cooldown.
- [ ] Integration test covers register -> verify -> ACTIVE.

**Dependencies:** Tasks 4-5.

**Files likely touched:** IAM registration application services, controller DTOs, controller, and tests.

**Estimated scope:** Break into two Medium commits: registration and verification/resend.

#### Task 7: Deliver login, lockout, and session persistence

**Description:** Authenticate JSON credentials through Spring Security, persist the `SecurityContext` in Spring Session JDBC, rotate the session ID on success, and implement failed-attempt lockout.

**Acceptance criteria:**

- [ ] Only ACTIVE, non-locked accounts can log in.
- [ ] Five consecutive failures lock the account for 15 minutes; successful login resets the counter and records `last_login_at`.
- [ ] Wrong email and wrong password return the same generic error.
- [ ] A successful session survives page refresh and expires after 60 minutes of inactivity.

**Verification:**

- [ ] Integration tests cover successful login, session fixation protection, generic errors, inactive/deactivated accounts, lockout, unlock, and inactivity expiry.
- [ ] Failed attempts are logged without credentials.

**Dependencies:** Tasks 3-4.

**Files likely touched:** IAM login service/controller, Security configuration, user-details adapter, and integration tests.

**Estimated scope:** Medium.

#### Task 8: Deliver current-user, logout, cookies, and CSRF

**Description:** Add `/me`, expired-safe logout, explicit session-cookie configuration, and a CSRF bootstrap flow. Remove the current blanket CSRF exclusion for auth endpoints.

**Acceptance criteria:**

- [ ] `/me` returns only user ID, email, and role for an authenticated session.
- [ ] Logout invalidates only the current session and clears the cookie.
- [ ] Mutating auth requests without a valid CSRF token are rejected.
- [ ] Production profile sets `Secure`; all profiles set `HttpOnly` and `SameSite=Lax` on the session cookie.

**Verification:**

- [ ] MockMvc tests assert cookie attributes, CSRF rejection/success, `/me`, and logout behavior.
- [ ] A second device/session remains active after ordinary logout.

**Dependencies:** Task 7.

**Files likely touched:** `SecurityConfig`, session configuration, auth controller, and tests.

**Estimated scope:** Medium.

#### Task 9: Deliver forgot, reset, and change password

**Description:** Implement UC1.4 using the same one-time-token primitives. Reset requests always return a neutral response. Password replacement and session invalidation occur atomically.

**Acceptance criteria:**

- [ ] Forgot-password response is indistinguishable for existing and unknown emails.
- [ ] Reset tokens expire after 30 minutes and are single-use.
- [ ] Reset invalidates all active sessions.
- [ ] Authenticated password change verifies the current password, rejects reuse, invalidates other sessions, and keeps the current one.

**Verification:**

- [ ] Integration tests cover enumeration resistance, expiry, replay, password policy, incorrect current password, and session invalidation.
- [ ] Confirmation email is sent without containing password or token data.

**Dependencies:** Tasks 5, 7, and 8.

**Files likely touched:** IAM password application services/controllers, session invalidator, and tests.

**Estimated scope:** Break into two Medium commits: forgot/reset and authenticated change.

### Checkpoint 3: Backend authentication complete

- [ ] UC1.1-UC1.4 backend flows pass against PostgreSQL.
- [ ] OpenAPI documents all auth endpoints and stable error codes.
- [ ] `mvn -B -ntp verify` succeeds.
- [ ] Manual multi-session and token-replay checks succeed.

### Phase 4: Frontend vertical slices

#### Task 10: Establish the frontend auth boundary and generated contract

**Description:** Create the ADR-002 `features/auth` and `shared/api` structure, configure a same-origin API proxy, generate TypeScript types/client from OpenAPI, and centralize CSRF-aware fetch behavior.

**Acceptance criteria:**

- [ ] Components do not hard-code the backend origin.
- [ ] Requests include cookies and the CSRF header when required.
- [ ] Frontend auth DTOs come from the generated contract.
- [ ] No auth token or session identifier is stored in `localStorage` or `sessionStorage`.

**Verification:**

- [ ] Contract generation is deterministic and checked by CI.
- [ ] Unit tests cover API error mapping and CSRF retry/bootstrap behavior.

**Dependencies:** Checkpoint 3 API contract.

**Files likely touched:** `frontend/next.config.ts`, `frontend/src/shared/api/**`, `frontend/src/features/auth/api/**`, package scripts/config.

**Estimated scope:** Medium.

#### Task 11: Deliver registration and verification screens

**Description:** Implement role-aware registration, check-email state, verification result page, and resend action while preserving non-password field values on validation errors.

**Acceptance criteria:**

- [ ] Registration exposes only Student, Parent, and Tutor roles.
- [ ] Field errors map to the correct inputs; password fields are cleared after failure.
- [ ] Verification and resend show explicit success, expired, already-used, cooldown, and delivery-failure states.

**Verification:**

- [ ] Component tests cover validation and server error mapping.
- [ ] E2E test covers register -> inspect local email -> verify -> login link.

**Dependencies:** Tasks 6 and 10.

**Files likely touched:** `frontend/src/app/(auth)/register/**`, `verify-email/**`, and `frontend/src/features/auth/**`.

**Estimated scope:** Break into registration and verification/resend commits.

#### Task 12: Deliver login, session bootstrap, route protection, and logout

**Description:** Build the login screen, resolve the session through `/me`, protect role routes, preserve a safe local return URL, and add logout. Keep authorization enforcement on the backend; frontend guards are UX only.

**Acceptance criteria:**

- [ ] Refreshing a protected page retains authentication through the server session.
- [ ] Unauthenticated access redirects to login with an allowlisted return path.
- [ ] Login redirects to the correct role dashboard; unauthorized role routes do not render protected content.
- [ ] Logout clears the current session and returns to the public page.

**Verification:**

- [ ] Tests cover every role, invalid credentials, lockout display, expired session, unsafe return URL, and logout.
- [ ] Browser inspection confirms no credential/session token is exposed to client storage.

**Dependencies:** Tasks 7-8 and 10.

**Files likely touched:** auth login feature, auth layouts, route guards, role landing placeholders, and tests.

**Estimated scope:** Break route protection from login/logout if needed.

#### Task 13: Deliver password-management screens

**Description:** Implement forgot-password, reset-password, and authenticated change-password flows with neutral request messaging and clear expired-token recovery.

**Acceptance criteria:**

- [ ] Forgot-password always shows the same confirmation text.
- [ ] Reset page handles invalid, expired, and already-used tokens without revealing account data.
- [ ] Change-password confirms success and explains that other sessions were signed out.

**Verification:**

- [ ] Component and E2E tests cover happy paths and all SRS exceptions.
- [ ] Browser back/refresh does not repopulate password fields.

**Dependencies:** Tasks 9-10.

**Files likely touched:** auth password routes/components and tests.

**Estimated scope:** Medium.

### Phase 5: Hardening and delivery gate

#### Task 14: Add abuse protection and security regression coverage

**Description:** Complete per-IP/per-account limits for signup, resend, login, and reset; add security headers and audit-safe structured events; run dependency and browser-level security checks.

**Acceptance criteria:**

- [ ] Rate limits persist sufficiently for the intended deployment topology and do not rely only on client state.
- [ ] Security logs contain event type, time, account identifier where safe, and IP, but no credentials or raw tokens.
- [ ] CSP, HSTS in production, frame protection, content-type protection, CORS, cookie, and cache headers are verified.

**Verification:**

- [ ] Automated tests cover rate-limit boundaries and reset behavior.
- [ ] `npm audit` and Maven dependency audit findings are triaged rather than force-fixed.
- [ ] Browser DevTools confirms cookie and security-header behavior.

**Dependencies:** Tasks 6-13.

**Files likely touched:** backend rate-limit/security configuration, frontend headers, tests, and CI workflow.

**Estimated scope:** Medium.

#### Task 15: Run the authentication release gate

**Description:** Execute a clean environment test of all UC1.1-UC1.4 flows, verify architecture rules, and update project documentation so commands and implemented capabilities match the repository.

**Acceptance criteria:**

- [ ] Clean clone/bootstrap follows documented commands without manual database edits.
- [ ] All four roles have correct authentication and route behavior.
- [ ] Documentation no longer claims missing CI, wrappers, scripts, or security controls.
- [ ] Authentication changes are ready for code review without unrelated working-tree changes.

**Verification:**

- [ ] `./mvnw -B -ntp verify`
- [ ] Frontend frozen install, lint, unit tests, and production build succeed.
- [ ] Authentication E2E suite succeeds against Docker Compose.
- [ ] Corrected ArchUnit rules fail against a deliberate boundary violation and pass after it is removed.

**Dependencies:** All prior tasks.

**Files likely touched:** CI workflow, README, project overview, auth runbook/test documentation.

**Estimated scope:** Medium.

## Test Matrix

| Flow | Minimum automated coverage |
|---|---|
| Register | Valid input, invalid role, weak password, duplicate normalized email, concurrent duplicate signup, email failure |
| Verify email | Valid, expired, malformed, already used, replaced token, concurrent double consume |
| Resend | Pending account, active account neutral behavior, cooldown, old token invalidation |
| Login | Success, generic invalid credentials, pending/deactivated account, five-failure lockout, expiry, successful reset of counter |
| Session | Refresh persistence, inactivity expiry, server revocation, cookie flags, session fixation rotation |
| Logout | Current session invalidated, expired-safe behavior, other sessions preserved |
| Forgot/reset | Existing/unknown account same response, rate limit, expiry, replay, weak password, all-session invalidation |
| Change password | Wrong current password, password reuse, success, current session preserved, other sessions revoked |
| Authorization | Anonymous 401, wrong-role 403, server-side role source, direct API access cannot bypass UI |
| CSRF | Missing/invalid token rejected; valid same-origin form succeeds |

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Existing Compose and application DB variables disagree | High | Complete Task 1 before persistence work. |
| Current ArchUnit rules target the wrong package prefix | High | Correct and mutation-test the rules at Checkpoint 2. |
| Email is sent inside the account transaction | High | Commit the pending account first, then attempt delivery; make resend recoverable. |
| Concurrent verification/reset requests reuse a token | High | Hash tokens, use row locking/atomic update, and test double consumption. |
| Session invalidation is difficult with container-local sessions | High | Use Spring Session JDBC and query/delete sessions by principal. |
| CSRF is disabled for all auth endpoints | High | Replace blanket exclusion with explicit CSRF bootstrap and tests. |
| Frontend and backend DTOs drift | Medium | Generate the TypeScript contract from OpenAPI and fail CI on drift. |
| Profile-completeness redirect couples IAM to Profile | Medium | Keep IAM identity-only; add profile routing when the Profile API exists. |
| Rate limiting works only on one JVM | Medium | Use a DB-backed strategy or explicitly document single-instance scope for the course deployment. |
| User enumeration through timing or messages | Medium | Generic responses, similar code paths, normalized timing tests, and no account-specific reset response. |

## Open Decisions Before Implementation

1. Select the local mail approach: Mailpit in Compose is recommended; a log-only sender is acceptable only for automated tests.
2. Confirm whether registration conflicts should return `409 USER_ALREADY_EXISTS` as written in UC1.1, despite NFR-06 applying explicitly to login/reset. The current plan follows UC1.1 and returns the conflict.
3. Confirm the rate-limit persistence level for deployment: PostgreSQL-backed is recommended if more than one backend instance may run.
4. Confirm placeholder role landing URLs until dashboards exist: proposed `/student`, `/parent`, `/tutor`, and `/admin`.

## Definition of Done

- [ ] All acceptance criteria in `tasks/todo.md` are checked.
- [ ] Backend unit, integration, architecture, migration, and security tests pass.
- [ ] Frontend unit/component/E2E tests, lint, typecheck, and production build pass.
- [ ] UC1.1-UC1.4 work from a clean Docker Compose environment.
- [ ] No plaintext password, raw token, or session identifier exists in code, logs, responses, or persisted token records.
- [ ] API documentation and repository documentation match the implemented behavior.

