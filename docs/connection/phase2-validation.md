# Connection Phase 2 — validation

Branch: `feat/connection`. Repository: `C:/Users/ASUS/Desktop/EduFit/EduFit`.
No changes to main/Discovery refs or their task documents. Existing user changes in `.gitignore` and backend implementation documentation are excluded from Connection commits.

## T08a — shared Clock

- Inject the existing IAM Clock into invitation, request, link, quota and query services; no additional production Clock bean.
- Capture one timestamp per operation, after locks where applicable. Invitation creation reuses quota consumption timestamp.
- RED: new fixed-clock quota regression did not compile before Clock injection (`.codex-doc-review/phase2-red-clock.log`).
- GREEN: affected app reactor verify, 58 Connection + 5 real PostgreSQL tests, zero failures/errors/skips (`phase2-T08a.log`). Context tests prove one UTC candidate and fixed override. Additional module verify covers deadline -1ns/equality/+1ns and checks one clock read (`phase2-T08a-module.log`).
- Review: no route/schema/authorization change, no sleep-based timing test, no unconditional Clock bean. Coverage warnings are not a strict coverage pass; full checkpoint verification follows T10b.
