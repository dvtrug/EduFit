# Kế hoạch đồ án SWP: EduFit

> **Tên dự án:** EduFit  
> **Tagline:** Đúng gia sư – Đúng mục tiêu – Thấy rõ tiến bộ

## 1. Tổng quan

EduFit là nền tảng web kết nối gia sư với phụ huynh và học sinh THCS từ lớp 6 đến lớp 9. Hệ thống hỗ trợ tìm kiếm và matching gia sư, quản lý quá trình học, tạo quiz bằng AI và phân tích tiến độ để phụ huynh/học sinh dễ theo dõi.

Luồng giá trị chính:

> Đăng ký → tạo hồ sơ và mục tiêu → tìm hoặc match gia sư → kết nối → lập kế hoạch → học và làm quiz → cập nhật tiến độ → AI phân tích → theo dõi dashboard.

## 2. Vấn đề cần giải quyết

### Phụ huynh/học sinh

- Khó tìm gia sư phù hợp đồng thời về môn, khối lớp, mục tiêu, lịch và ngân sách.
- Khó kiểm chứng năng lực và kinh nghiệm của gia sư.
- Không có nơi tập trung để biết học sinh đã học gì và tiến bộ ra sao.
- Nhận xét sau buổi học thường rời rạc và thiếu dữ liệu minh chứng.

### Gia sư

- Khó trình bày hồ sơ chuyên nghiệp và thể hiện đúng thế mạnh.
- Tốn thời gian soạn quiz từ tài liệu học.
- Khó tổng hợp mục tiêu, kết quả quiz và nhiều buổi học để đánh giá học sinh.
- Phải quản lý lịch, lớp và tiến độ bằng nhiều công cụ khác nhau.

### Giải pháp

EduFit kết hợp ba thành phần:

1. Marketplace kết nối gia sư và học sinh.
2. Công cụ quản lý lớp và tiến độ học tập.
3. AI hỗ trợ tạo nội dung và phân tích dữ liệu.

AI chỉ tạo bản nháp và gợi ý. Gia sư vẫn là người duyệt nội dung, xác nhận đánh giá và chịu trách nhiệm chuyên môn.

## 3. Phạm vi

### Trong phạm vi

- Học sinh lớp 6, 7, 8 và 9.
- Các môn THCS do Admin quản lý.
- Học online hoặc offline.
- Bốn nhóm vai trò: học sinh, phụ huynh, gia sư và Admin.
- Hồ sơ, mục tiêu, Search và Matching.
- Yêu cầu kết nối, lớp học và lịch học.
- Learning plan, milestone, báo cáo buổi học và quiz.
- AI hỗ trợ bio, tạo quiz và phân tích tiến độ.
- Dashboard tiến độ cho phụ huynh/học sinh.
- Xác minh hồ sơ gia sư.

### Ngoài phạm vi MVP

- Học sinh ngoài khối 6–9.
- Chat và video call thời gian thực.
- Thanh toán trực tuyến thực tế.
- Ứng dụng di động.
- AI tự chấm hoàn toàn câu tự luận.
- OCR ảnh, xử lý video hoặc audio.
- AI tự công bố nội dung mà không có người duyệt.

## 4. Vai trò

### Học sinh (`STUDENT`)

- Quản lý hồ sơ học sinh và mục tiêu.
- Search hoặc nhận đề xuất gia sư.
- Gửi yêu cầu kết nối.
- Tham gia lớp và xác nhận lịch.
- Xem tài liệu, làm quiz và gửi phản hồi.
- Theo dõi tiến độ và phân tích AI trên dashboard.
- Đánh giá gia sư sau khi đã học.

### Phụ huynh (`PARENT`)

- Liên kết với một hoặc nhiều hồ sơ học sinh bằng lời mời/mã xác nhận.
- Xem mục tiêu, lịch học, báo cáo buổi học, kết quả quiz và tiến độ của học sinh đã liên kết.
- Nhận thông báo liên quan đến lịch học và tiến độ.
- Không được làm quiz hoặc sửa dữ liệu học tập thay cho học sinh.

### Gia sư (`TUTOR`)

- Quản lý hồ sơ chuyên môn và dùng AI hỗ trợ viết bio.
- Khai báo môn, khối lớp, giá, lịch và khu vực.
- Tải minh chứng để Admin duyệt.
- Nhận hoặc từ chối yêu cầu kết nối.
- Quản lý lớp và lịch dạy.
- Tạo learning plan và milestone.
- Tải tài liệu và dùng AI tạo quiz.
- Ghi báo cáo sau buổi học.
- Xem, chỉnh sửa và xác nhận đánh giá do AI đề xuất.

### Quản trị viên (`ADMIN`)

- Quản lý tài khoản và danh mục môn học.
- Duyệt hồ sơ, bằng cấp và chứng chỉ gia sư.
- Xử lý báo cáo vi phạm.
- Theo dõi hoạt động và lỗi của chức năng AI.

## 5. Danh mục chức năng

### 5.1 Xác thực và tài khoản

- Đăng ký tài khoản.
- Chọn loại tài khoản học sinh, phụ huynh hoặc gia sư.
- Đăng nhập và đăng xuất.
- Xác minh email.
- Quên và đặt lại mật khẩu.
- Đổi mật khẩu.
- Cập nhật thông tin và ảnh đại diện.
- Phân quyền theo `STUDENT`, `PARENT`, `TUTOR`, `ADMIN`.
- Khóa tài khoản bởi Admin.

### 5.2 Hồ sơ học sinh và liên kết phụ huynh

- Khối lớp 6, 7, 8 hoặc 9.
- Môn cần học.
- Mức hiện tại và mức mong muốn.
- Loại mục tiêu: củng cố kiến thức, cải thiện điểm, bồi dưỡng nâng cao hoặc ôn thi vào lớp 10.
- Thời hạn hoàn thành mục tiêu.
- Hình thức học online/offline.
- Khu vực, lịch rảnh và ngân sách.
- Thông tin phụ huynh liên hệ khi cần.
- Quản lý nhiều mục tiêu theo từng môn.
- Gửi/chấp nhận lời mời liên kết giữa tài khoản phụ huynh và hồ sơ học sinh.
- Một phụ huynh có thể theo dõi nhiều học sinh; chỉ người đã được xác nhận liên kết mới xem được dữ liệu học tập.
- Học sinh hoặc Admin có thể thu hồi liên kết khi cần.

Mục tiêu phải là dữ liệu có cấu trúc, không chỉ là một đoạn bio, để Matching và AI có thể sử dụng.

### 5.3 Hồ sơ gia sư

- Giới thiệu bản thân.
- Trình độ, kinh nghiệm và phương pháp giảng dạy.
- Môn và khối lớp có thể dạy.
- Thế mạnh và mục tiêu giảng dạy.
- Giá dạy, lịch rảnh và hình thức học.
- Khu vực có thể dạy offline.
- Bằng cấp, chứng chỉ và tài liệu minh chứng.
- Trạng thái xác minh.
- Rating và số buổi hoàn thành.

### 5.4 AI hỗ trợ hoàn thiện hồ sơ gia sư

Gia sư nhập thông tin thô; AI hỗ trợ:

- Viết bio rõ ràng và chuyên nghiệp.
- Gợi ý cách trình bày kinh nghiệm và phương pháp dạy.
- Phát hiện thông tin còn thiếu.
- Gợi ý từ khóa theo môn, khối và mục tiêu giảng dạy.

Quy tắc:

- AI không được tự tạo bằng cấp, thành tích hoặc kinh nghiệm.
- Kết quả luôn là bản nháp.
- Gia sư phải duyệt trước khi lưu công khai.
- Hồ sơ vẫn phải qua bước xác minh của Admin.

### 5.5 Search gia sư

Phụ huynh/học sinh có thể tìm gia sư theo:

- Môn học.
- Khối lớp.
- Online hoặc offline.
- Khu vực.
- Khoảng giá.
- Lịch rảnh.
- Kinh nghiệm.
- Rating và trạng thái xác minh.

Kết quả có thể sắp xếp theo giá, rating, kinh nghiệm hoặc độ liên quan.

### 5.6 Matching gia sư

Matching tự sử dụng hồ sơ và mục tiêu của học sinh để tìm gia sư phù hợp.

Điều kiện bắt buộc:

- Đúng môn và khối lớp.
- Đúng hình thức học.
- Có lịch rảnh giao nhau.
- Đúng khu vực nếu học offline.
- Gia sư đã được Admin duyệt.

Công thức khởi đầu:

```text
matchingScore =
    30% * scheduleScore
  + 25% * goalScore
  + 20% * budgetScore
  + 15% * reputationScore
  + 10% * experienceScore
```

Kết quả phải giải thích lý do, ví dụ: đúng Toán lớp 9, trùng ba khung giờ, có kinh nghiệm ôn thi vào lớp 10 và giá nằm trong ngân sách.

Search và Matching dùng chung dữ liệu và bộ lọc. Search cho phép người dùng chủ động tìm; Matching tự điền điều kiện, chấm điểm và xếp hạng.

### 5.7 Yêu cầu kết nối

- Gửi yêu cầu cho gia sư.
- Chọn môn, mục tiêu và lịch mong muốn.
- Gia sư chấp nhận hoặc từ chối.
- Theo dõi trạng thái yêu cầu.
- Hủy yêu cầu khi chưa được chấp nhận.
- Tạo lớp học sau khi gia sư chấp nhận.

### 5.8 Quản lý lớp học

- Xem danh sách lớp đang hoạt động.
- Quản lý thông tin gia sư, học sinh và môn học.
- Tạo learning plan.
- Chia mục tiêu thành các milestone.
- Theo dõi ngày bắt đầu, ngày dự kiến kết thúc và trạng thái lớp.
- Kết thúc lớp và lưu lý do.

### 5.9 Quản lý lịch học

- Tạo hoặc đề xuất buổi học.
- Học sinh xác nhận lịch.
- Đổi, hủy hoặc đặt lịch học bù.
- Phát hiện lịch trùng của học sinh và gia sư.
- Hiển thị lịch dạng danh sách hoặc calendar.
- Trạng thái: sắp tới, hoàn thành, đã hủy hoặc vắng.
- Gửi thông báo nhắc lịch.

### 5.10 Tài liệu và AI tạo quiz

Gia sư tải PDF, DOCX hoặc TXT và chọn:

- Chủ đề hoặc phạm vi kiến thức.
- Khối lớp.
- Mức độ dễ, trung bình hoặc khó.
- Số lượng câu hỏi.
- Loại câu: trắc nghiệm, đúng/sai hoặc trả lời ngắn.

AI tạo bản nháp gồm:

- Câu hỏi.
- Các lựa chọn.
- Đáp án.
- Giải thích.
- Mức độ khó.
- Đoạn tài liệu làm căn cứ nếu xác định được.

Gia sư chỉnh sửa và duyệt trước khi xuất bản. Hệ thống tự chấm câu trắc nghiệm và đúng/sai; câu trả lời ngắn cần gia sư kiểm tra nếu không khớp chính xác.

Vòng đời quiz:

- Trạng thái nháp, đã xuất bản và đã đóng.
- Thiết lập hạn làm bài và số lần được phép làm.
- Giao quiz cho một lớp hoặc học sinh cụ thể.
- Lưu từng lần làm bài; gia sư chấm thủ công câu trả lời ngắn trước khi công bố điểm cuối cùng.
- Không cho sửa cấu trúc quiz đã có lượt làm; nếu cần thay đổi phải tạo phiên bản mới.

Luồng xử lý:

> Upload tài liệu → trích xuất văn bản → chia nội dung → gửi phần liên quan cho AI → tạo quiz nháp → gia sư duyệt → giao quiz → học sinh làm → lưu kết quả.

### 5.11 Báo cáo buổi học

Gia sư ghi lại:

- Nội dung đã học.
- Milestone liên quan.
- Mức độ hoàn thành.
- Điểm mạnh và phần còn yếu.
- Bài tập về nhà.
- Kế hoạch buổi tiếp theo.

Phụ huynh/học sinh có thể xem, xác nhận đã đọc và gửi phản hồi.

### 5.12 AI hỗ trợ đánh giá học sinh

AI phân tích:

- Mục tiêu và milestone.
- Báo cáo buổi học.
- Điểm và lịch sử làm quiz.
- Tỷ lệ đúng theo chủ đề và độ khó.
- Số buổi hoàn thành, vắng hoặc hủy.
- Nhận xét của gia sư và phản hồi của học sinh.

AI đề xuất:

- Điểm mạnh.
- Kiến thức còn yếu.
- Xu hướng tiến bộ hoặc giảm sút.
- Mức độ bám sát mục tiêu.
- Nội dung nên ôn tiếp.
- Bản nháp nhận xét định kỳ.

Gia sư phải duyệt hoặc chỉnh sửa trước khi công bố. AI không quyết định điểm chính thức hoặc tự đánh dấu học sinh đã hoàn thành mục tiêu. Nếu thiếu dữ liệu, hệ thống phải hiển thị “chưa đủ dữ liệu để đánh giá”.

### 5.13 Dashboard phụ huynh/học sinh

Dashboard chia thành hai phần.

**Dữ liệu thực tế:**

- Mục tiêu và trạng thái.
- Milestone hoàn thành/tổng số milestone.
- Điểm quiz theo thời gian.
- Tỷ lệ đúng theo chủ đề.
- Số buổi đã học, vắng và sắp tới.
- Báo cáo và bài tập gần nhất.

**Phân tích AI:**

- Tóm tắt tiến độ dễ hiểu.
- Điểm mạnh và nội dung cần cải thiện.
- Cảnh báo khi kết quả giảm hoặc chậm mục tiêu.
- Gợi ý nội dung nên tập trung tiếp theo.

Phân tích AI phải có nhãn và thời điểm cập nhật. Người dùng phải xem được dữ liệu thực tế làm căn cứ.

Công thức tiến độ MVP:

```text
progressPercent =
  tổng trọng số milestone đã hoàn thành
  / tổng trọng số tất cả milestone
  * 100
```

AI diễn giải kết quả nhưng không tự thay đổi phần trăm tiến độ.

### 5.14 Đánh giá và phản hồi

- Học sinh đánh giá gia sư sau ít nhất một buổi hoàn thành.
- Chấm rating và viết nhận xét.
- Báo cáo đánh giá vi phạm.
- Gia sư xem phản hồi.
- Admin xử lý nội dung bị báo cáo.

### 5.15 Thông báo

- Yêu cầu kết nối mới.
- Yêu cầu được chấp nhận hoặc từ chối.
- Lịch học mới, thay đổi hoặc sắp diễn ra.
- Quiz hoặc bài tập mới.
- Quiz đã được chấm.
- Báo cáo buổi học mới.
- Đánh giá tiến độ mới.

### 5.16 Quản trị hệ thống

- Dashboard thống kê.
- Quản lý và khóa tài khoản.
- Duyệt hồ sơ, bằng cấp và chứng chỉ gia sư.
- Quản lý môn học và khối lớp.
- Xử lý báo cáo, đánh giá và tranh chấp.
- Theo dõi lỗi, timeout và mức sử dụng AI.
- Theo dõi số lượt Matching, kết nối, lớp và buổi học.
- Xem nhật ký các thao tác quản trị quan trọng như duyệt gia sư, khóa tài khoản và xử lý báo cáo.

## 6. Ma trận chức năng theo vai trò

| Nhóm chức năng | STUDENT | PARENT | TUTOR | ADMIN |
|---|:---:|:---:|:---:|:---:|
| Quản lý tài khoản cá nhân | ✓ | ✓ | ✓ | ✓ |
| Quản lý hồ sơ học sinh, mục tiêu | ✓ | Xem khi liên kết |  |  |
| Liên kết phụ huynh – học sinh | Xác nhận/thu hồi | Gửi/xác nhận |  | Hỗ trợ |
| Quản lý hồ sơ gia sư |  |  | ✓ | Duyệt |
| AI hỗ trợ bio gia sư |  |  | ✓ | Theo dõi |
| Search và Matching | ✓ | ✓ |  | Theo dõi |
| Yêu cầu kết nối | Gửi | Gửi | Nhận/xử lý | Xem khi cần |
| Lớp và lịch học | Xem/xác nhận | Xem | Tạo/quản lý | Xem khi cần |
| Learning plan, milestone | Xem/xác nhận | Xem | Tạo/cập nhật |  |
| Tài liệu và quiz | Làm quiz | Xem kết quả | Tạo/duyệt/chấm | Xử lý vi phạm |
| Báo cáo buổi học | Xem/phản hồi | Xem/phản hồi | Tạo |  |
| Phân tích AI | Xem bản đã duyệt | Xem bản đã duyệt | Tạo/duyệt | Theo dõi |
| Dashboard tiến độ | ✓ | ✓ | ✓ |  |
| Đánh giá gia sư | Tạo | Tạo khi đủ điều kiện | Xem | Kiểm duyệt |
| Quản trị hệ thống |  |  |  | ✓ |

## 7. Công nghệ dự kiến

- Backend: Java, Spring Boot, Spring Security, Spring Data JPA.
- Database: Microsoft SQL Server.
- Frontend: React hoặc công nghệ nhóm thống nhất.
- API: RESTful API.
- AI: gọi dịch vụ LLM qua một `AIService` riêng để có thể thay nhà cung cấp.
- Xử lý tài liệu: trích xuất văn bản từ PDF, DOCX và TXT trước khi gửi phần cần thiết cho AI.
- Kiểm thử: unit test cho Matching và tính tiến độ; integration test cho API, phân quyền và luồng AI giả lập.

API key phải được lưu trong biến môi trường hoặc secret configuration, không đưa vào source code.

## 8. Thực thể dữ liệu chính

- `User`, `Role`.
- `StudentProfile`, `ParentStudentLink`, `TutorProfile`, `TutorDocument`.
- `Subject`, `GradeLevel`, `Availability`.
- `LearningGoal`, `ConnectionRequest`, `TutoringClass`.
- `LearningPlan`, `Milestone`.
- `StudySession`, `SessionReport`.
- `LearningMaterial`, `Quiz`, `QuizAssignment`, `Question`, `QuizAttempt`, `Answer`.
- `AIRequest`, `AIResult`, `ProgressReview`.
- `Review`, `Notification`, `Report`, `AuditLog`.

## 9. Quy tắc nghiệp vụ quan trọng

- Chỉ hỗ trợ lớp 6–9.
- Chỉ gia sư đã được duyệt mới xuất hiện trong Search và Matching.
- Không cho phép lịch học trùng của học sinh hoặc gia sư.
- Phụ huynh chỉ được xem dữ liệu của học sinh khi liên kết đã được xác nhận và còn hiệu lực.
- Chỉ thành viên lớp được xem tài liệu, quiz và tiến độ của lớp.
- Nội dung AI phải được gia sư duyệt trước khi công bố.
- AI không được tự thêm thành tích vào hồ sơ.
- AI không được tự quyết định điểm hoặc trạng thái hoàn thành mục tiêu.
- Dữ liệu thực tế và nhận định AI phải được phân biệt trên dashboard.
- Không gửi thông tin cá nhân không cần thiết của học sinh tới dịch vụ AI.
- Website vẫn phải dùng được các chức năng cốt lõi khi dịch vụ AI lỗi hoặc timeout.

## 10. Chức năng MVP

- Đăng ký, đăng nhập, quên mật khẩu và phân quyền.
- Hồ sơ học sinh, hồ sơ gia sư và mục tiêu.
- Liên kết phụ huynh – học sinh và quyền xem tiến độ.
- AI hỗ trợ viết bio gia sư.
- Admin xác minh gia sư.
- Search và Matching có giải thích.
- Yêu cầu kết nối, lớp và lịch học.
- Learning plan, milestone và báo cáo buổi học.
- Upload tài liệu và AI tạo quiz nháp.
- Học sinh làm quiz, lưu kết quả và gia sư chấm câu trả lời ngắn.
- AI tạo bản nháp phân tích tiến độ.
- Gia sư duyệt đánh giá.
- Dashboard phụ huynh/học sinh.
- Đánh giá gia sư và thông báo trong hệ thống.
- Nhật ký các thao tác quản trị quan trọng.

## 11. Tiêu chí thành công

- Gia sư dùng AI tạo được bio nháp từ thông tin thật và chỉnh sửa trước khi lưu.
- Search trả về đúng bộ lọc; Matching trả về gia sư phù hợp và giải thích được lý do.
- Hai bên kết nối, tạo lớp và quản lý lịch không bị trùng.
- Phụ huynh chỉ xem được dữ liệu của học sinh đã liên kết hợp lệ.
- Gia sư tải được tài liệu và nhận quiz nháp đúng cấu trúc.
- Học sinh làm quiz và kết quả được lưu chính xác.
- AI phân tích được tiến độ và chỉ ra dữ liệu làm căn cứ.
- Gia sư có thể sửa hoặc từ chối nhận xét AI.
- Dashboard hiển thị riêng dữ liệu thực tế và nhận định AI.
- Người ngoài lớp không truy cập được dữ liệu học tập.
- Khi AI không hoạt động, các chức năng hồ sơ, Search, Matching, lớp và tiến độ cơ bản vẫn sử dụng được.

## 12. Kịch bản demo

1. Gia sư đăng ký, nhập thông tin ngắn và dùng AI tạo bio.
2. Admin kiểm tra minh chứng và duyệt hồ sơ gia sư.
3. Học sinh lớp 9 tạo mục tiêu đạt 8 điểm Toán trong kỳ thi vào lớp 10.
4. Hệ thống Matching và giải thích lý do đề xuất gia sư.
5. Học sinh gửi yêu cầu; gia sư chấp nhận và tạo lớp.
6. Gia sư tạo learning plan, milestone và lịch học.
7. Gia sư tải tài liệu hàm số; AI tạo quiz để gia sư duyệt.
8. Học sinh làm quiz qua các buổi học.
9. Gia sư ghi báo cáo; AI phân tích kết quả và tạo nhận xét nháp.
10. Gia sư xác nhận nhận xét.
11. Phụ huynh/học sinh xem điểm, milestone, xu hướng và phân tích AI trên dashboard.

Kịch bản này thể hiện ba điểm mạnh của đề tài: **matching đúng người, AI hỗ trợ gia sư và tiến độ học tập có dữ liệu minh chứng**.


