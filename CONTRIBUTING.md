# Hướng dẫn đóng góp cho EduFit

## Kiểm tra chất lượng trên máy

Sử dụng Java 21 và Maven Wrapper đi kèm dự án. Chạy toàn bộ quality gate trước khi mở pull request:

```bash
cd backend
sh ./mvnw -B -ntp verify
```

Trên Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd -B -ntp verify
```

Dùng `sh ./mvnw spotless:apply` hoặc `.\mvnw.cmd spotless:apply` để định dạng Java, Markdown, YAML và XML. CI chạy Spotless và Checkstyle ở chế độ chỉ kiểm tra; mã nguồn sẽ bị từ chối nếu sai định dạng, dùng wildcard import, có import không sử dụng hoặc đặt tên Java sai quy ước.

Để chứng minh quy ước được thực thi bằng công cụ thay vì chỉ được mô tả bằng văn bản, chạy `sh ./scripts/verify-lint-enforcement.sh` trên macOS/Linux hoặc `.\scripts\verify-lint-enforcement.ps1` trên Windows. Script tạm thời tạo một file Java cố ý sai quy ước, xác nhận lint từ chối file đó rồi tự xóa file tạm.

## Quy ước Java

- Package dùng chữ thường và nằm dưới namespace `com.edufit.backend`.
- Kiểu dữ liệu và class dùng `UpperCamelCase`; method, parameter, field và biến cục bộ dùng `lowerCamelCase`; hằng số dùng `UPPER_SNAKE_CASE`.
- Google Java Format là quy chuẩn chính thức cho khoảng trắng và xuống dòng.
- Không được dùng wildcard import hoặc để import không sử dụng.
- Phần triển khai nội bộ phải nằm trong module sở hữu. Giao tiếp giữa các module phải đi qua contract công khai của module đích.
- Mọi thay đổi cơ sở dữ liệu phải được thêm bằng Flyway migration mới. Không chỉnh sửa migration đã được chia sẻ với thành viên khác hoặc đã chạy trên môi trường dùng chung.

## Branch và pull request

- Tạo branch từ branch mặc định hiện tại và giữ thời gian tồn tại của branch ngắn.
- Dùng một trong các mẫu tên: `feature/<mo-ta-ngan>`, `fix/<mo-ta-ngan>`, `chore/<mo-ta-ngan>` hoặc `refactor/<mo-ta-ngan>`.
- Mọi thay đổi phải được merge thông qua pull request, có CI thành công và ít nhất một lượt phê duyệt.
- Khi hợp lý, tách thay đổi hành vi, refactor và chỉ định dạng thành các commit riêng.
- Sau khi push repository, cấu hình GitHub ruleset theo `docs/github-branch-protection.md`.

## Commit message

Dùng quy ước Conventional Commits:

```text
<type>(<scope-khong-bat-buoc>): <mo-ta-ngan-gon>
```

Các type được chấp nhận gồm `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, `ci` và `perf`.

Ví dụ:

```text
feat(identity): thêm contract đăng ký bằng email
fix(scheduling): từ chối các buổi học đã xác nhận bị trùng lịch
ci: thêm cache cho dependency Maven
```

Phần mô tả nên dùng câu mệnh lệnh, ngắn gọn và giải thích đúng phạm vi thay đổi. Nếu cần phần nội dung chi tiết, hãy giải thích lý do của thay đổi thay vì lặp lại những gì đã thể hiện trong mã nguồn.
