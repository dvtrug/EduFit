# Connection — Báo cáo Phase 0

Ngày thực hiện: 11/10/2026 (Asia/Saigon). Workspace duy nhất: `C:/Users/ASUS/Desktop/EduFit/EduFit`; branch `feat/connection`. Checklist: [tasks](../../tasks/connection/todo.md), [plan](../../tasks/connection/plan.md).

## T00 — Bảo toàn dữ liệu và baseline

- Base Connection: `43e7461161404bcbf2fac8ca83bd6eda4a4b8115`.
- Discovery ref giữ nguyên `dd9068e0a14b0e480d1823eab37bc82041166fe5`; main local giữ nguyên `f077e55bae2c4ff1bc74f29aee30610e31c5a53b`.
- Stash Discovery WIP: `6f2a3f29f0cfd5e979a4d8813b2a6df8581e86ee`. Chỉ 3 file Discovery/IAM đang dở, không có docs Connection. Git binary diff trước stash và diff từ stash có SHA256 giống nhau `DEFFE85B00477A595D949D36D67E04E2F349CE7E928043C4D75E11804B43D38E`.
- Backup patch, hashes các file nguyên bản và stash IDs ở `.codex-doc-review/connection-phase0/` (ignored). Không drop/apply stash vào Connection. Bản stash docs riêng bảo toàn trạng thái di chuyển trước switch; docs đã đưa về namespace Connection, không restore nguyên README từ branch Discovery vào base Connection.
- Checkout cũ EduFit-connection đã detached ở `43e7461`, dữ liệu vẫn giữ. Không xóa worktree, không cùng branch ở hai folder.
- Runtime: Java 21.0.10, Maven wrapper 3.9.16; process-local JAVA_HOME, không thay cấu hình máy. HTTPS dùng kho certificate Windows bằng `-Djavax.net.ssl.trustStoreType=Windows-ROOT`, không disable TLS validation.

Lệnh từ `backend/`: `./mvnw.cmd -pl modules/connection -am test -B -ntp -Djavax.net.ssl.trustStoreType=Windows-ROOT`.

| Module | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Shared | 22 | 0 | 0 | 0 |
| AI | 10 | 0 | 0 | 0 |
| IAM | 32 | 0 | 0 | 0 |
| Profile | 26 | 0 | 0 | 0 |
| Connection | 8 | 0 | 0 | 0 |
| Tổng | 98 | 0 | 0 | 0 |

BUILD SUCCESS, 40.301s. Nguồn: `.codex-doc-review/connection-phase0/t00-baseline-unsandboxed.log`, hoàn thành 10:30:51 giờ Việt Nam. Không cộng stale XML `AiDeadlineTest`/`MatchingCandidatesPostgresTest` từ lần chạy Discovery trước đó.

Lần chạy trong sandbox bị Mockito attach pipe lỗi và treo; đã dừng, chạy lại ngoài sandbox. Chưa có PostgreSQL suite ở baseline T00; Docker đang được khởi động để kiểm chứng T00b.

## Cập nhật refs khi bắt đầu thực thi

Fetch thành công: main hiện là `6755121122c79dc9705282225d14ffb76e17c29a`, đã merge frontend-auth PR14. Vì vậy audit main `4eaa333` trong plan là snapshot lịch sử, không còn là tip dùng cho T00a.

- Merge-tree `feat/connection + origin/main` mới báo conflict ở `backend/app/src/test/java/vn/edufit/db/FlywayMigrationScriptTest.java`. Resolve giữ manifest V1.01–V1.09 và kiểm tra V1.09 Connection, không sửa SQL migration.
- Upstream JaCoCo vẫn LINE/BRANCH 80% nhưng `jacoco.haltOnFailure=false`. T00a giữ upstream và chạy thêm `verify -Djacoco.haltOnFailure=true` để báo gap thật; không gọi mặc định BUILD SUCCESS là coverage đạt 80%.
- T00b vẫn dùng Discovery `dd9068e`, giữ APIs/tests đầy đủ khi resolution. Các refs nguồn không bị merge/push ngược.

## T00a / T00b / T01

Chưa hoàn tất. Kết quả và giới hạn được cập nhật trong commit tương ứng, chỉ tick checklist sau kiểm chứng task và push xác nhận.
