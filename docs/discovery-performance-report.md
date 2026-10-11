# Discovery PostgreSQL Verification

Related handover: [API contract](discovery-api-contract.md), [code/flows and diagrams](discovery-module-flow.md), [documentation index](README.md).
Tables preserve the original successful T12 measurement. Later regression runs
rerun the same fixtures and can produce different latency/plan timings; Surefire
reports are overwritten on each run, not a permanent copy of this snapshot.

## Scope and Reproduction

Run on 2026-10-10, branch `feat/discovery`, T12. Test source:
`backend/app/src/test/java/vn/edufit/discovery/DiscoveryReadPostgresTest.java`.

```powershell
cd backend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
$env:MAVEN_OPTS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT'
.\mvnw.cmd -pl app -am test '-Dtest=DiscoveryReadPostgresTest,vn.edufit.profile.**.*Test,vn.edufit.discovery.**.*Test' '-Dsurefire.failIfNoSpecifiedTests=false' -B -ntp
```

Docker is required. Confirm zero skipped tests; Docker-unavailable skips are not evidence.
The selected regression passed: Profile 44, Discovery 104, app 31 (22 read/performance
cases and 9 matching workflow cases), no failures/errors/skips.

Environment: Windows, AMD Ryzen 7 7435HS (8 cores/16 logical processors), JDK
21.0.10, Docker Desktop 28.3.0 (16 CPUs, approximately 7.66 GiB memory), PostgreSQL
17.11 from `postgres:17-alpine`. All nine real Flyway migrations applied and
Hibernate schema validation passed. Hikari pool maximum size is 5.

Fixtures contain 20, 200 and 240 VERIFIED tutors, two subject-level rows and one
availability slot per tutor, one goal and student. Execution is serial (concurrency
1), with five warm-ups then 30 measured samples per operation. p95 is nearest-rank
sample 29 of 30; timings include assertions/instrumentation. Results are local
observations, not a performance gate or a production capacity estimate.

## Correctness and Query Counts

| Operation | SQL statements | Coverage |
| --- | ---: | --- |
| Search, full page | 5 | Page + count + detail rows + subjects + slots |
| Tutor detail | 3 | Tutor + subjects + slots |
| Bounded candidates | 3 | Capped tutor query + subjects + slots |
| Matching, AI quota exhausted | 8 | Goal/student/goal slots + level catalog + candidate hydration + log insert |
| Empty first search page | 1 | No count or hydration needed |

Counts remain fixed for 20/200/240 tutors, including the search count query.
Scorer-only evaluation issues no SQL. DTO associations are read after service
transactions have ended with no bound EntityManager: this exercises operation
without OSIV, rather than relying on lazy loading during response serialization.
The production configuration also has `spring.jpa.open-in-view=false`.

Tests cover each search filter independently, combined filters, keyword matches
in name/headline/bio/subject name, VERIFIED visibility, missing/unverified detail
404, empty results, touching/non-overlapping slots and pagination over all 23
fixture tutors without duplicates or omissions despite multiple subject rows.

The first PostgreSQL run failed with `function lower(bytea) does not exist` for
null area/keyword parameters. Explicit String casts inside HQL concat expressions
resolve nullable parameter typing; the previously failing reads now pass.

## Local Latency

All values are milliseconds. HTTP rows mean standalone MockMvc using the actual
transactional services and PostgreSQL, not a TCP deployment or security-filter test.

| Operation | Tutors | p50 | p95 | Maximum |
| --- | ---: | ---: | ---: | ---: |
| Search HTTP | 20 | 29.980 | 43.519 | 45.621 |
| Search HTTP | 200 | 20.992 | 26.288 | 26.413 |
| Search HTTP | 240 | 17.873 | 22.897 | 24.989 |
| Detail HTTP | 20 | 11.758 | 14.013 | 14.727 |
| Detail HTTP | 200 | 10.016 | 12.122 | 12.752 |
| Detail HTTP | 240 | 11.613 | 16.942 | 23.584 |
| Match HTTP, no AI | 20 | 30.788 | 44.779 | 67.921 |
| Match HTTP, no AI | 200 | 41.652 | 53.836 | 71.524 |
| Match HTTP, no AI | 240 | 35.279 | 44.259 | 58.335 |
| Candidate DB + hydration | 20 | 10.323 | 12.404 | 12.658 |
| Candidate DB + hydration | 200 | 18.056 | 22.640 | 32.825 |
| Candidate DB + hydration | 240 | 16.685 | 18.668 | 24.441 |
| Scorer only | 20 | 0.419 | 0.570 | 0.617 |
| Scorer only | 200 | 0.724 | 1.023 | 1.204 |
| Scorer only | 240 | 0.319 | 0.484 | 0.486 |

The AI gateway is mocked and quota lookup returns exhausted, so matching exercises
deterministic fallback with a real matching-log insert, not external AI latency or
AI-usage/audit persistence. Candidate and scorer rows isolate those components;
do not subtract independent p95 values to infer other component timings.

SADS NFR-09 targets general API p95 below 300 ms; NFR-14 targets search/matching
below 500 ms. These local observations are below those thresholds but **do not
certify either production NFR**. The testing document's combined search/matching
300 ms reference differs from SADS; this report uses SADS's endpoint-specific
targets. NFR-10 AI 15 s requires separate gateway deadline evidence (T11).

## Representative Query Plans

After ANALYZE on the 240-tutor dataset, tests run EXPLAIN (ANALYZE, BUFFERS) on
equivalent representative SQL shapes, not captured prepared Hibernate statements:

- VERIFIED search ordered by price with LIMIT 10: sequential scan of 240 tutor
  rows, top-N heapsort (30 kB), Limit node; execution 0.096 ms.
- VERIFIED subject/mode candidate query ordered by rating/reviews/created/id with
  LIMIT 200: tutor sequential scan (240 rows), subject scan (480 rows), quicksort
  (86 kB), Limit node; execution 0.634 ms.

Small tables favor sequential scans. These plans do not establish production
index suitability, bound rows scanned, or the plans of every optional filter.
The cap bounds returned/hydrated/scored rows, not all DB scan or sort work.
Rerun with realistic volumes, selectivity, skew and concurrent traffic before
index or capacity decisions. Full plan text and timings are emitted in Surefire
`TEST-vn.edufit.discovery.DiscoveryReadPostgresTest.xml`.

## Candidate Cap and Ranking Loss

An adversarial 240-tutor fixture keeps all tutors eligible for the subject/mode,
but places the only exact-level tutor at a lower rating outside the first 200
preordered candidates. The uncapped oracle loads the same fixture IDs through
the batch detail API and ranks them with the real scorer and level catalog.

| Comparison | Result |
| --- | --- |
| Candidates returned | 200 / 240 |
| Uncapped best score | 97.00 |
| Bounded best score | 78.50 |
| Bounded vs uncapped Top 5 recall | 4 / 5 |

This proves that the cap can lose the global winner. It is not an estimated
production miss rate. The default 200 is a resource guard, **not a guarantee of
global Top N accuracy**. Increasing the configurable cap trades hydration/scoring
cost for recall; changing preordering or introducing staged retrieval requires
separate product/performance validation.

## Remaining Verification

Production-like load, network/serialization/security overhead, realistic large
datasets, concurrent quota reservation, external AI latency, and durable event
delivery are not established here. Separate matching workflow integration tests
cover transactional log/event behavior; T13 covers architectural boundaries.
