# ADR-001: Technology Stack for EduFit

- **Status:** Accepted
- **Related Jira Task:** ES0-1 — Chốt tech stack cho toàn bộ dự án
- **Date:** 2026-09-24
- **Deciders:** Team (4 members)

## Context

EduFit is a course-project SaaS platform matching tutors with students, with a
strong emphasis on trust/verification and continuous progress tracking. The
system involves four roles (Student, Parent, Tutor, Admin), a rule-based
matching engine, mutual-consent scheduling with double-booking protection,
learning-plan/milestone tracking, a review system, and two AI-assisted
features (bio drafting, match explanation) via an external LLM provider.

Constraints shaping this decision:

- Team of 4, studying and working in parallel (part-time effort), 7-week
  timeline, but **the team is not optimizing for shortest possible delivery
  time** — the explicit priority is understanding *why* each decision is made
  and what stage of the system it belongs to, not minimizing effort. AI-
  assisted coding tools reduce the cost of implementing a more thorough
  design, so this ADR does not treat "faster to build" as a decisive
  criterion on its own.
- Several functional/non-functional requirements from the SRS directly
  constrain technical choices (referenced by ID below): NFR-04, NFR-07,
  NFR-11, NFR-15, NFR-17, NFR-24, FR-04, FR-13/17/20/31, NFR-08.
- Development workflow: **local-first**. The team builds and runs the full
  stack (application + database + file storage) on local machines throughout
  development; hosted infrastructure is only introduced at the deployment
  phase near the end of the project.

This ADR covers: frontend framework, backend framework, database, ORM, auth
strategy, and hosting/deployment. It does not cover implementation-level
concurrency control mechanics (e.g., the exact optimistic-locking / DB
constraint design for NFR-17) — that is deferred to a later, implementation-
level design document once Sprint 3–4 scheduling logic is built.

---

## Decision 1: Backend Framework — Spring Boot

**Decision:** Spring Boot (Java) as the core backend framework.

**Rationale:**
- The domain is rule-heavy and stateful: multiple entities behave as explicit
  state machines (Session, Link invitation, Connection Request, Engagement —
  see BR-40, BR-42, BR-45 in the SRS), and many use cases require atomic,
  multi-record transactions (NFR-11). Spring's declarative
  `@Transactional` model maps directly onto this without hand-rolled
  transaction management.
- Verification, audit logging (FR-02, FR-15) and role-based authorization
  (NFR-07) are naturally expressed as cross-cutting concerns (Spring Security
  filters/interceptors, AOP), which a lighter framework would require more
  manual wiring for.
- Aligns with the team's existing learning path (Spring is a deliberate
  learning target for the team, not only a delivery tool).

**Alternatives considered:** Node.js/Express or NestJS (faster to start, but
weaker built-in transaction/authorization primitives for this domain's
complexity); Django (strong ORM/admin, but team has no prior Python
investment and JVM ecosystem was preferred for learning goals).

**Consequences:** Steeper initial learning curve for any team member not
already familiar with Spring; mitigated by treating Sprint 0 as an explicit
ramp-up period rather than assuming instant productivity.

---

## Decision 2: Database — PostgreSQL

**Decision:** PostgreSQL as the single system of record.

**Rationale:**
- The data model is highly relational (User → StudentProfile/TutorProfile →
  LearningGoal/Engagement → Session → SessionNote/Review), with referential
  integrity requirements throughout the SRS's ERD.
- NFR-17 ("double-booking protection... protected at database level, row
  locking or a constraint") is a requirement that only a transactional RDBMS
  satisfies naturally — via `SELECT ... FOR UPDATE` or an exclusion
  constraint over time ranges. A NoSQL store would require re-implementing
  this guarantee at the application layer, which is both more work and a
  weaker guarantee.
- NFR-24 ("uniqueness of a review per Student–Tutor pair enforced at
  database level") maps directly to a unique constraint.

**Alternatives considered:** MongoDB (rejected — the domain's integrity
requirements are exactly the case relational databases are designed for;
document modeling would fight the domain rather than help it).

**Consequences:** None beyond the standard need for schema migrations to be
managed explicitly (see Decision 6).

---

## Decision 3: ORM — Spring Data JPA / Hibernate

**Decision:** Spring Data JPA with Hibernate as the persistence layer.

**Rationale:**
- Natural pairing with Spring Boot; reduces CRUD boilerplate significantly
  for a team of 4 covering many entities.
- Supports the concurrency-control primitives the SRS's NFRs will require
  (e.g., `@Version` for optimistic locking), so the chosen ORM does not block
  the later concurrency design.

**Explicitly out of scope for this ADR:** the concrete concurrency-control
strategy for NFR-15 (concurrent state changes) and NFR-17 (double booking) —
whether implemented via `@Version` optimistic locking, native
`SELECT ... FOR UPDATE`, or a Postgres `EXCLUDE` constraint — is an
implementation-level decision to be documented separately when the
Scheduling module (Sprint 3–4) is designed in detail. This ADR only fixes
the ORM choice, not its locking mechanics.

**Alternatives considered:** jOOQ (more control over SQL, less ORM "magic",
but higher upfront cost and less idiomatic with the rest of the Spring
ecosystem the team is learning); plain JDBC (rejected — too much
boilerplate for the number of entities involved).

**Consequences:** Team must stay alert to N+1 query patterns, particularly
in UC4.2 (Check Schedule Conflict), where fetch strategy will need explicit
attention during implementation.

---

## Decision 4: Frontend Framework — Next.js

**Decision:** Next.js (App Router) with React.

**Rationale:**
- Most of the application is a role-gated dashboard experience; SSR/SEO
  value is concentrated in one place (UC2.1 Search Tutors, public-facing).
  Next.js is chosen primarily for its file-based routing, project
  conventions, and ecosystem maturity rather than for SSR necessity
  everywhere.
- App Router's middleware model supports checking session/role before
  rendering protected routes, which is a client-side complement to NFR-07
  (server-side authorization remains the actual enforcement point; this is
  only about UX, not security).

**Alternatives considered:** Plain React + Vite (rejected — more manual
routing/config work for no clear benefit at this project's scope); Pages
Router (rejected in favor of App Router for ecosystem currency).

**Consequences:** None significant; standard Next.js conventions apply.

---

## Decision 5: Authentication Strategy

**Decision:** Two parallel tracks are pursued, with one designated as the
primary implementation path and the other as an explicit secondary learning
branch. **Only the primary track blocks feature delivery.**

### Primary track: Server-side session with opaque token

- Mechanism: on login, the server generates a random, non-decodable session
  identifier, stores session state server-side (session store), and returns
  it via an `httpOnly` cookie. Every request looks up the store to resolve
  user identity, role, and session validity.
- **Rationale — this is the track that satisfies the SRS's NFRs precisely:**
  - NFR-04 ("session expires after 60 minutes of inactivity... can be
    invalidated by the server at any time") requires both a sliding
    inactivity window and true instant revocation. A session store gives
    both directly: inactivity is just a "last seen" timestamp update, and
    revocation is a single delete, effective on the very next request — no
    residual validity window.
  - FR-04 ("after a password change, all other active sessions are
    invalidated") is a direct delete-by-user-id operation with no lag.
  - No signing-key management, no token-replay surface beyond standard
    cookie/session hardening (httpOnly, SameSite, CSRF token).
- Trade-off accepted: every request requires a store lookup. At this
  project's scale (course project, low concurrent traffic, likely a single
  backend instance during development and demo), this cost is negligible and
  not a decision-driving factor.

### Secondary track (parallel exploration, non-blocking): JWT + Refresh Token

- Mechanism: short-lived signed JWT access token (5–15 min, verified by
  signature only, no store lookup) + longer-lived refresh token stored
  server-side, rotated on each use with reuse detection.
- **Explicit reason for pursuing this in parallel:** learning JWT-based auth
  is a standard and valuable part of the Spring ecosystem, and the team
  wants direct, hands-on exposure to it rather than only reading about it.
  This branch is treated as a learning exercise / alternative implementation
  to compare against the primary track, not as a candidate to replace it
  before understanding its trade-offs first-hand.
- **Known, accepted limitation of this track:** revocation (logout, password
  change, admin lock) only invalidates the refresh token. Any access token
  already issued remains valid — by design of JWT signature verification —
  until its own short expiry elapses. This means NFR-04's "invalidated at
  any time" is met only up to a bounded delay (the access-token TTL), not
  instantly. If this branch is ever promoted beyond an exploration, this
  deviation from NFR-04's literal wording must be documented and confirmed
  as an accepted trade-off, not silently absorbed.

**Consequences:** The team maintains two auth implementations during
development. This is an accepted, deliberate cost for learning value; the
primary track is what ships in the main feature branches, and the JWT
branch is developed and evaluated separately without gating sprint delivery.

---

## Decision 6: File Storage for Credential Documents (UC1.11)

**Decision:** Local filesystem storage during development; migrate to
Supabase Storage at the deployment phase.

**Rationale:**
- Consistent with the local-first workflow: all data generated during
  development, including uploaded credential documents, stays on local
  machines — no external dependency is introduced before it's needed.
- NFR-08 ("private, access-controlled storage; file type verified by
  content, not extension; sanitized file names") applies regardless of
  backend — these controls are implemented at the application layer
  (validation, access-control checks before serving a file) and are
  independent of whether the underlying store is local disk or Supabase.
- **Architectural guideline (to carry into implementation):** the file
  storage access point should be defined behind a single interface/service
  boundary (e.g., a `CredentialStorageService` with `store()`/`retrieve()`/
  `delete()`), so that switching the local filesystem implementation for a
  Supabase-backed implementation at deploy time is a matter of swapping one
  implementation class, not touching UC1.10/UC1.11 business logic.

**Alternatives considered:** MinIO (S3-compatible local emulation) —
rejected as unnecessary overhead for the volume of files involved (a few
credential documents per tutor); plain local disk is sufficient and simpler
during development.

**Consequences:** The storage-swap point (local → Supabase) must be
explicitly tested before the deployment phase, not assumed to work.

---

## Decision 7: Hosting / Deployment

**Decision:** Local-first during development (Docker Compose for backend +
PostgreSQL + frontend); deploy to hosted infrastructure only near project
completion:
- Backend + PostgreSQL: Render or Railway (managed Postgres, Git-based
  deploy).
- Frontend: Vercel.
- File storage: Supabase Storage (see Decision 6).

**Rationale:**
- Local-first avoids any dependency on external infrastructure while the
  domain logic and data model are still being iterated on, and keeps the
  whole team working against an identical, reproducible environment via
  Docker Compose.
- The chosen hosted targets (Render/Railway, Vercel) are Git-integrated
  PaaS platforms, not a statement of "lower rigor" — they let the team spend
  effort on the parts of the system that carry real engineering weight for
  this project (transaction integrity, authorization, audit logging,
  concurrency control) rather than on infrastructure operations (cluster
  management, TLS provisioning, load balancing) that add no value to this
  specific domain at this scale.

**Consequences / follow-on decisions required:**
1. **Database migration tool is mandatory, not optional**, precisely because
   the team develops locally first: without Flyway or Liquibase in place
   from Sprint 0, four independently-evolving local schemas will diverge,
   and reconciling them at deploy time (Sprint 6–7) becomes a significant
   risk. This is treated as a direct dependency of this ADR, to be set up
   immediately.
2. **CI and CD are decoupled.** GitHub Actions should run build + test on
   every push starting in Sprint 0 (CI), independent of any deployment
   target. The auto-deploy step (CD) to Render/Vercel is only added once the
   team is ready to move to the hosted environment near the end of the
   project. This yields the practical benefit of continuous automated
   testing immediately, without prematurely committing to hosting details.
3. **Production-readiness practices are pursued regardless of hosting
   choice**, since they are what genuinely constitutes "production
   engineering practice" — not self-managing a server. This includes:
   environment separation (local / staging / production via Spring profiles
   and Next.js env files), structured logging to support FR-02/FR-15 audit
   requirements, secrets managed via the hosting platform's secret manager
   (never committed), and relying on the managed Postgres provider's
   automated backups.

**Alternatives considered:** Self-managed VPS / Kubernetes — rejected; the
operational overhead (server hardening, cluster management, manual TLS,
scaling configuration) does not map to any requirement in the SRS and would
consume effort better spent on the domain's actual complexity.

---

## Open Items (deferred to later, implementation-level design docs)

- Concrete concurrency-control mechanism for NFR-15 / NFR-17 (Decision 3).
- Evaluation outcome of the JWT + Refresh Token branch (Decision 5) — to be
  revisited once both tracks have been implemented far enough to compare.
- Exact local → Supabase storage cutover procedure and testing checklist
  (Decision 6).
