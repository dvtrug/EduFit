# Connection — sửa lỗi Flyway trên DB local main

Ngày: 11/10/2026. Workspace EduFit, branch `feat/connection`. Đây là hotfix sau Phase1, không bắt đầu Phase2.

## Lỗi và nguyên nhân

DB local `edufit` trên PostgreSQL17.11 (`localhost:5433`) đã có V1.09 `platform/V1.09__seed_demo_accounts_and_catalogs.sql`, checksum **-1594466614**. Manifest Connection trước hotfix dùng cùng version cho `allow_relink_after_revocation`, checksum **1468271677**.

Đã tái hiện `FlywayValidateException: Migration checksum mismatch for migration version 1.09` trên bản sao DB local và regression test lịch sử main. Không dùng repair/clean, không sửa trực tiếp bảng history.

## Manifest hiện hành

| Version | Migration | Checksum |
| --- | --- | --- |
| V1.01–V1.08 | Schema chung hiện hữu | Giữ nguyên |
| V1.09 | Seed main, khôi phục đúng bytes commit main6755121 | -1594466614 |
| V1.10 | Seed đã phát hành trên Connection, giữ nguyên toàn bộ bytes | -1594466614 |
| V1.11 | Connection relink/version/quota, đổi số từ V1.09, SQL nguyên bytes | 1468271677 |

Giữ cả V1.09 và V1.10 để tôn trọng history main và V1.10 đã phát hành. Seed có ON CONFLICT DO NOTHING, không UPDATE tài khoản/catalog/hồ sơ đã có; tests sửa tên user trước nâng cấp và xác nhận tên đó giữ nguyên, không thêm account demo trùng. Không gộp DDL Connection vào seed V1.10.

Code/test commit: `e616f4b49e9b2fa67cafbcfddcc6516af21b56e3`, đã push riêng feat/connection. SQL fixtures lịch sử giữ nguyên bytes/whitespace upstream; diff-check các file mới khác sạch. Fixture nằm ngoài production `db/migration`, không được app quét.

## Verification

Evidence local (ignored): `.codex-doc-review/connection-flyway-fix/`.

- RED `red-history.log`: fixture main V1.09 tái hiện checksum mismatch. Lần RED đầu có duplicate V1.09 do fixture còn quét migration Connection cũ; đã tách fixture lịch sử độc lập trước tái hiện đúng lỗi.
- Clean affected reactor verify: `backend/mvnw.cmd -f backend/pom.xml -pl app -am clean verify -Dtest=FlywayMigrationIT,FlywayMigrationScriptTest,Connection*Test,MatchingWorkflowPostgresTest,Architecture*Test -Dsurefire.failIfNoSpecifiedTests=false -B -ntp -Djavax.net.ssl.trustStoreType=Windows-ROOT`.
- `verify.log`: **75 tests, failures0/errors0/skipped0, BUILD SUCCESS**, 1:50 phút. Connection48, App27; gồm Flyway clean/upgrade2, manifest/checksum2, Connection PostgreSQL5, Discovery matching workflow9 và architecture9. Đây là affected regression, không gọi là full348 tests rerun. Default JaCoCo vẫn warnings như baseline; hotfix không chứng nhận strict80% hoặc đổi ngưỡng.
- Backup DB local trước mọi thay đổi: `edufit-before-fix.dump`, SHA256 `DAFF9469F447EAAD01B1EF9C35700E8C8EBCBC9F9B20E0F3D5A8CE8D4D28436D`, nằm trong thư mục evidence ignored, không commit dữ liệu/password.
- Restore backup vào DB riêng `edufit_connection_flyway_check_20261011`. Flyway migrate2 (V1.10/V1.11), validate OK, rerun0. So counts + fingerprint có thứ tự của toàn bộ31 bảng cũ trước/sau bằng JSONB bỏ trường version mới: giống hoàn toàn. History/quota mới kiểm tra riêng.
- Xác nhận local vẫn khớp backup trước apply. Áp dụng cùng manifest lên DB `edufit`: migrate2, validate OK, rerun0; counts/fingerprints31 bảng cũ không đổi. History V1.01–V1.09 giữ nguyên, thêm V1.10/V1.11; ba cột version là BIGINT, quota table tồn tại.
- Chạy backend jar build mới trên DB local, random port55304, `spring.jpa.hibernate.ddl-auto=validate`: Flyway/schema validate thành công, log `Started EduFitApplication in 19.649 seconds`. GET `/actuator/health` trả `UP`. Process kiểm chứng được dừng sau khi kiểm tra, không đụng process của người dùng.

## Phạm vi tương thích / team rollout

- Đã kiểm chứng DB trắng và DB main có seed V1.09, nâng cấp qua V1.10/V1.11. V1.10 hiện tại giữ nguyên checksum.
- **DB từng chạy Connection DDL dưới V1.09 (checksum1468271677) là lineage khác**, không tự migrate bằng manifest này. Phải backup/đối soát riêng trước chuyển branch; không chạy repair hàng loạt hoặc giả coi migration chưa thực thi là đã chạy. History của DB đó cần phương án reconciliation riêng vì chính version1.09 đã mang nghĩa khác.
- DB có V1.10 nội dung khác hoặc version mới của team phải đối chiếu manifest/checksum trước rollout. Không suy ra mọi DB team giống local đã kiểm chứng.
- Chỉ sửa Connection branch. Main/Discovery refs và stash WIP giữ nguyên; không push ngược các nhánh đó.
- DB clone và backup được giữ lại để người dùng đối chiếu/khôi phục nếu cần. Không tự xóa DB cũ hoặc dữ liệu.
