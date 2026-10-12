# Phase 4 — HTTP integration contract

## B11: session, CSRF and CORS

Backend uses Spring Boot 4.1.1 / Spring Security 7.1.1. JDBC session auto-configuration requires `spring-boot-starter-session-jdbc`; declaring only `spring-session-jdbc` did not enable it. Flyway owns the session tables, so `spring.session.jdbc.initialize-schema=never` prevents a second schema initializer.

Client sequence (backend contract only; frontend is unchanged):

1. Call `GET /api/v1/auth/csrf` with credentials included. Keep the returned plain `data.token` and `data.headerName` (`X-XSRF-TOKEN`). The response sets `XSRF-TOKEN`, Path `/`, SameSite=Lax, readable by JavaScript, and `Cache-Control: no-store`.
2. Send login JSON with credentials included, the CSRF cookie and `X-XSRF-TOKEN` header. Login, register, forgot/reset password and logout also require CSRF; there are no blanket API exceptions.
3. Successful login rotates an existing session ID, stores the security context in JDBC, sets HttpOnly `EDUFIT_SESSION`, and clears the old CSRF cookie. Fetch `/auth/csrf` again before the next write.
4. Send the session cookie, CSRF cookie and matching header for POST/PUT/PATCH/DELETE, including `/discovery/match` and goal writes.
5. Logout invalidates the JDBC session and clears both cookies. Fetch CSRF again before logging in again. An expired session can still call logout with a valid CSRF pair.

The bootstrap endpoint returns the same deferred **plain** token used by the filter, not the XOR token exposed for HTML forms. Spring Security's SPA configuration resolves plain header tokens while retaining form BREACH protection. Controller-based login explicitly changes the session ID and clears CSRF because authentication filters do not execute that controller's login lifecycle.

Redirect request caching is disabled: rejected API GETs return 401 without creating anonymous JDBC sessions. The explicit CookieSerializer makes the session name/path consistent in the complete application and MockMvc environment.

The session timeout is 3,600 seconds of inactivity. Session/CSRF cookies are host-scoped: use one host consistently (`localhost` or `127.0.0.1`) for API calls in a session. Both `http://localhost:3000` and `http://127.0.0.1:3000` are permitted CORS origins with credentials. Preflight runs before authentication, accepts content-type/X-XSRF-TOKEN headers without a session, and rejects other origins. CORS rejection is Spring's plain HTTP 403, whereas authentication/authorization/CSRF failures use `ApiError` JSON.

Status ordering:

| Request | Result |
| --- | --- |
| Public GET search/detail | 200, or 404 for missing/unverified tutor |
| Protected GET without valid session | 401 `UNAUTHORIZED` |
| Unsafe request missing/wrong CSRF, even without session | 403 `FORBIDDEN` (CSRF filter runs first) |
| Protected unsafe request with valid CSRF but no valid session | 401 `UNAUTHORIZED` |
| Authenticated request with valid CSRF but wrong role/ownership | Business/security 403, or owner-hiding 404 |

Success keeps `ApiResponse`; errors keep `ApiError {code,message,details,timestamp}` without a `success:false` field. Null details follow existing serialization.

Evidence: `DiscoveryHttpSecurityPostgresTest` starts the complete application, PostgreSQL Testcontainers, the JDBC session filter and actual security chain. It uses real IAM password verification and HTTP session cookies; no CurrentUser/service/facade mock. Matching an eligible goal with zero tutors reaches the real matching service and returns an empty list without a provider call. This does not replace Phase 5's populated matching/Connection workflow or live Gemini smoke test.

Existing IAM gap outside the Discovery login/logout scope: change/reset password services currently update passwords but do not revoke other JDBC sessions. The authentication document's multi-device invalidation policy is a target, not implemented evidence. Likewise, controller login does not register the advertised five-session concurrency limit. Neither is claimed as completed by B11.

Sources: [Spring Boot session auto-configuration](https://docs.spring.io/spring-boot/reference/web/spring-session.html), [Spring Security SPA CSRF and token refresh](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

## B12: input errors and pagination

Pending B12 implementation and regression evidence.
