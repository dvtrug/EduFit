# EDUFIT — TỔNG QUAN DỰ ÁN & KẾ HOẠCH TRIỂN KHAI TOÀN DIỆN

> **Tagline:** Đúng gia sư – Đúng mục tiêu – Thấy rõ tiến bộ  
> **Đối tượng:** Học sinh THCS (Lớp 6 đến Lớp 9), Phụ huynh, Gia sư, Quản trị viên.  
> **Loại đồ án:** Đồ án tốt nghiệp / Đồ án chuyên ngành Công nghệ thông tin (SWP).

---

## MỤC LỤC

1. [Tổng quan Dự án](#1-tổng-quan-dự-án)
2. [Tổng hợp Chức năng Dự án (8 Phân hệ Cốt lõi)](#2-tổng-hợp-chức-năng-dự-án-8-phân-hệ-cốt-lõi)
3. [Lộ trình & Hướng dẫn Xây dựng Dự án (5 Giai đoạn)](#3-lộ-trình--hướng-dẫn-xây-dựng-dự-án-5-giai-đoạn)
4. [Đánh giá Thiếu sót & Các Chức năng Đề xuất Bổ sung](#4-đánh-giá-thiếu-sót--các-chức-năng-đề-xuất-bổ-sung)
5. [Đánh giá & Đề xuất Nâng cấp Tính năng AI](#5-đánh-giá--đề-xuất-nâng-cấp-tính-năng-ai)
6. [Kiến trúc Kỹ thuật & Công nghệ Dự kiến](#6-kiến-trúc-kỹ-thuật--công-nghệ-dự-kiến)
7. [Hướng dẫn Lệnh Git để Push lên GitHub](#7-hướng-dẫn-lệnh-git-để-push-lên-github)

---

## 1. Tổng quan Dự án

EduFit là nền tảng Web thông minh kết nối gia sư với phụ huynh và học sinh THCS (khối 6–9). Khác biệt lớn nhất của EduFit so với các trung tâm hay diễn đàn gia sư truyền thống là:
1. **Gia sư được xác minh chuyên môn và bằng cấp rõ ràng qua Admin.**
2. **Matching có giải thích lý do cụ thể** dựa trên môn học, khối lớp, lịch rảnh, ngân sách và mục tiêu.
3. **Tiến độ học tập minh bạch có dữ liệu thực tế** (quiz, bài tập, milestone, báo cáo buổi học) kết hợp cùng **AI phân tích xu hướng học tập**.

---

## 2. Tổng hợp Chức năng Dự án (8 Phân hệ Cốt lõi)

```
                            ┌────────────────────────┐
                            │    EDUFIT PLATFORM     │
                            └───────────┬────────────┘
        ┌───────────────────┬───────────┴───────────┬───────────────────┐
        ▼                   ▼                       ▼                   ▼
┌───────────────┐   ┌───────────────┐       ┌───────────────┐   ┌───────────────┐
│ Auth & Hồ sơ  │   │  Marketplace  │       │  Lớp & Lịch   │   │  AI & Tiến độ │
│ • RBAC        │   │ • Search      │       │ • Booking     │   │ • AI Bio      │
│ • Parent-Link │   │ • AI/Rule     │       │ • Milestone   │   │ • AI Quiz Gen │
│ • Verification│   │   Matching    │       │ • Báo cáo     │   │ • Dashboard   │
└───────────────┘   └───────────────┘       └───────────────┘   └───────────────┘
```

### 2.1. Phân hệ Xác thực & Phân quyền (Auth & RBAC)
- **Đăng ký tài khoản:** Chọn vai trò `STUDENT` (Học sinh), `PARENT` (Phụ huynh), `TUTOR` (Gia sư).
- **Xác thực & Bảo mật:** Xác minh email qua mã kích hoạt / token; Đăng nhập bằng JWT (Access Token & Refresh Token); Đổi mật khẩu, quên mật khẩu an toàn.
- **Phân quyền 4 vai trò:** Chặn truy cập trái phép cấp API (Spring Security).
- **Quản lý trạng thái tài khoản:** Admin có thể khóa/mở khóa tài khoản khi có vi phạm.

### 2.2. Phân hệ Hồ sơ học sinh & Liên kết Phụ huynh
- **Hồ sơ học sinh (Lớp 6–9):**
  - Khối lớp, môn học cần kèm, học lực hiện tại vs. mục tiêu kỳ vọng (cải thiện điểm, thi vào 10).
  - Khung giờ rảnh, ngân sách học phí/buổi, hình thức học (Online / Offline kèm khu vực).
  - Dữ liệu mục tiêu được chuẩn hóa dạng cấu trúc để phục vụ thuật toán Matching.
- **Liên kết Phụ huynh – Học sinh (Parent-Student Link):**
  - Phụ huynh gửi lời mời liên kết bằng mã xác nhận/email.
  - Học sinh chấp nhận hoặc hủy liên kết bất cứ lúc nào.
  - Phụ huynh liên kết được cấp quyền xem lịch, báo cáo buổi học, bài tập và tiến độ của con (không được làm bài thi thay con).

### 2.3. Phân hệ Hồ sơ Gia sư & Xác minh (Tutor Profile & Verification)
- **Khai báo hồ sơ chuyên môn:** Giới thiệu bản thân, môn và khối lớp có thể dạy, mức học phí, lịch rảnh trong tuần, khu vực dạy offline.
- **Tải minh chứng:** Bằng cấp, bảng điểm đại học, chứng chỉ ngoại ngữ/sư phạm (file PDF hoặc hình ảnh).
- **Trạng thái duyệt:** `PENDING` $\rightarrow$ `VERIFIED` hoặc `REJECTED`. Chỉ gia sư có trạng thái `VERIFIED` mới hiển thị trên Search và Matching.

### 2.4. Phân hệ Tìm kiếm & Matching thông minh (Search & Smart Matching)
- **Tìm kiếm chủ động (Search):** Lọc theo môn, lớp, giá tiền, khu vực, hình thức học, rating sao.
- **Ghép đôi tự động (Smart Matching):** Tự động tính điểm độ tương thích (`matchingScore` từ 0 đến 100%):
  $$\text{MatchingScore} = 30\% \cdot \text{Lịch rảnh} + 25\% \cdot \text{Mục tiêu} + 20\% \cdot \text{Ngân sách} + 15\% \cdot \text{Rating} + 10\% \cdot \text{Kinh nghiệm}$$
- **Giải thích độ phù hợp (Explainability):** Hiển thị rõ lý do match (Ví dụ: *"Trùng 3 khung giờ rảnh, đúng chuyên môn Toán 9 luyện thi vào 10, học phí phù hợp ngân sách"*).

### 2.5. Phân hệ Kết nối, Lớp học & Quản lý Lịch (Classroom & Schedule)
- **Yêu cầu kết nối:** Học sinh/Phụ huynh gửi đề nghị học $\rightarrow$ Gia sư chấp nhận hoặc từ chối.
- **Khởi tạo lớp học (`TutoringClass`):** Thiết lập ngày bắt đầu, dự kiến kết thúc, thông tin thành viên lớp.
- **Lập kế hoạch & Milestone:** Gia sư chia lộ trình thành các cột mốc có trọng số (ví dụ: *Cột mốc 1: Đại số chương 1 - 25%*).
- **Quản lý lịch học:** Lên lịch định kỳ, phát hiện xung đột lịch (Conflict Detection), xin đổi lịch hoặc học bù.

### 2.6. Phân hệ Học liệu & Hệ thống Quiz (Materials & Quizzes)
- **Kho học liệu:** Gia sư tải lên tài liệu bài học (PDF, DOCX, TXT).
- **Sinh Quiz tự động:** Gia sư chọn chủ đề, khối lớp, độ khó $\rightarrow$ AI sinh bộ câu hỏi nháp.
- **Quy trình duyệt (Human-in-the-loop):** Gia sư kiểm tra câu hỏi, sửa đáp án trước khi xuất bản cho học sinh làm bài.
- **Làm bài & Chấm điểm:** Chấm điểm trắc nghiệm tự động; lưu lịch sử các lần làm bài.

### 2.7. Phân hệ Báo cáo buổi học & Phân tích Tiến độ (Session Reports & AI Insights)
- **Báo cáo sau buổi học:** Sau mỗi buổi dạy, gia sư ghi nhận nội dung đã học, đánh giá độ tiếp thu, giao bài tập về nhà.
- **AI phân tích tiến độ định kỳ:** Phân tích điểm quiz, tỷ lệ hoàn thành milestone, số buổi học $\rightarrow$ gợi ý nhận xét về điểm mạnh, điểm yếu và xu hướng tiến bộ.
- **Dashboard 2 lớp:**
  - *Lớp 1 (Dữ liệu định lượng):* Điểm số theo thời gian, % hoàn thành mục tiêu, số buổi chuyên cần.
  - *Lớp 2 (Nhận định định tính từ AI):* Bản tóm tắt nhận xét đã được gia sư kiểm duyệt.

### 2.8. Phân hệ Đánh giá & Quản trị Hệ thống (Review & Admin Portal)
- **Đánh giá:** Học sinh/phụ huynh chấm điểm 1–5 sao và viết review sau khi hoàn thành buổi học.
- **Admin Dashboard:** Theo dõi số lượng người dùng, số lượt ghép đôi, doanh thu/buổi học, kiểm duyệt nội dung vi phạm, theo dõi log hệ thống và chi phí gọi AI API.

---

## 3. Lộ trình & Hướng dẫn Xây dựng Dự án (5 Giai đoạn)

| Giai đoạn | Thời gian dự kiến | Mục tiêu & Hạng mục công việc chính | Sản phẩm bàn giao (Deliverables) |
|---|---|---|---|
| **Phase 1: Nền tảng & Database** | Tuần 1 – 2 | • Thiết kế ERD 18+ thực thể SQL Server.<br>• Khởi tạo Spring Boot 3 & cấu hình Maven/Gradle.<br>• Viết API Auth (JWT, Role `STUDENT`, `PARENT`, `TUTOR`, `ADMIN`). | Script database, API Auth chạy trên Postman, Swagger/OpenAPI docs. |
| **Phase 2: Profile & Matching** | Tuần 3 – 4 | • CRUD Hồ sơ học sinh, phụ huynh và gia sư.<br>• Chức năng Admin duyệt bằng cấp gia sư.<br>• API Search đa tiêu chí và Service thuật toán Matching có giải thích. | Màn hình duyệt gia sư, trang tìm kiếm gia sư có filter và kết quả matching. |
| **Phase 3: Lớp học & Quiz** | Tuần 5 – 6 | • Tạo lớp học, lịch học, kiểm tra trùng lịch.<br>• Lập kế hoạch Milestone và ghi báo cáo buổi học.<br>• Tải tài liệu, làm bài quiz và chấm điểm. | Luồng tạo lớp, đặt lịch, làm quiz và lưu kết quả hoàn chỉnh. |
| **Phase 4: Tích hợp AI & Dashboard** | Tuần 7 – 8 | • Xây dựng `AIService` kết nối LLM (Gemini/OpenAI).<br>• Triển khai AI Bio, AI Quiz Generator (JSON Structured).<br>• Dựng Dual Dashboard với biểu đồ trực quan (Recharts). | Các tính năng AI hoạt động mượt mà; Dashboard tiến độ có số liệu và phân tích. |
| **Phase 5: Kiểm thử & Đóng gói** | Tuần 9 | • Viết Unit Test cho Matching & Progress calculation.<br>• Kiểm tra bảo mật IDOR & phân quyền truy cập chéo.<br>• Tạo kịch bản demo mẫu và hoàn thiện báo cáo đồ án. | Ứng dụng chạy ổn định, không lỗi bảo mật, video/kịch bản demo sẵn sàng. |

---

## 4. Đánh giá Thiếu sót & Các Chức năng Đề xuất Bổ sung

Dự án EduFit có thiết kế nghiệp vụ chuẩn chỉnh. Tuy nhiên, để sản phẩm đạt độ hoàn thiện xuất sắc và thuyết phục tuyệt đối hội đồng phản biện, cần bổ sung **6 điểm khuyết thực tế** sau:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   CÁC ĐIỂM KHUYẾT CẦN BỔ SUNG                          │
├────────────────────────────────────────────────────────────────────────┤
│ 1. Trao đổi sơ bộ (Request Messaging) ── Tránh người dùng rời sang Zalo│
│ 2. Buổi học thử & Điểm danh 2 chiều   ── Ngăn gia sư ghi khống buổi học│
│ 3. Hỗ trợ công thức Toán (KaTeX/LaTeX)── Cực kỳ quan trọng với THCS    │
│ 4. Thông báo tự động qua Email        ── Nhắc lịch học trước 2 giờ     │
│ 5. Quy tắc phạt hủy lịch trễ          ── Bảo vệ quyền lợi cho gia sư   │
│ 6. Thanh toán mô phỏng (Sandbox Escrow)── Hoàn thiện chu trình dịch vụ  │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Trao đổi sơ bộ trước khi nhận lớp (In-app Request Note/Message):**  
   *Thiếu sót:* Nếu chỉ có nút Chấp nhận/Từ chối yêu cầu, phụ huynh không thể hỏi thăm điều kiện học $\rightarrow$ sẽ xin số điện thoại để trao đổi qua Zalo $\rightarrow$ mất khách khỏi nền tảng.  
   *Giải pháp:* Cho phép gửi kèm 1 đoạn tin nhắn trao đổi sơ bộ trong form yêu cầu kết nối.
2. **Buổi học thử (Trial Session) & Điểm danh hai chiều:**  
   *Thiếu sót:* Hiện chỉ có gia sư tự tạo báo cáo buổi học, học sinh không có quyền xác nhận.  
   *Giải pháp:* Thêm cờ `isTrial` cho buổi đầu tiên. Bổ sung nút **"Xác nhận đã học"** cho học sinh/phụ huynh sau mỗi buổi.
3. **Hiển thị công thức Toán & Khoa học tự nhiên (KaTeX/LaTeX):**  
   *Thiếu sót:* Học sinh cấp 2 học Đại số, Hình học, Vật lý, Hóa học. Nếu câu hỏi quiz chỉ là chữ thuần thì không thể hiện được phân số, căn thức hay góc.  
   *Giải pháp:* Tích hợp thư viện `katex` ở Frontend và yêu cầu AI trả về cú pháp LaTeX chuẩn.
4. **Kênh thông báo qua Email (Email Notifications):**  
   *Thiếu sót:* Học sinh và phụ huynh không mở web cả ngày.  
   *Giải pháp:* Tích hợp gửi email tự động khi có lịch học sắp tới (trước 2 tiếng), có báo cáo buổi học mới hoặc kết quả quiz.
5. **Chính sách hủy/đổi lịch (Cancellation Policy):**  
   *Thiếu sót:* Học sinh có thể hủy lịch sát giờ khiến gia sư bị mất công.  
   *Giải pháp:* Chỉ cho phép hủy/đổi lịch miễn phí trước giờ học 4 tiếng.
6. **Thanh toán / Ký quỹ mô phỏng (Sandbox / Escrow Payment):**  
   *Giải pháp:* Tích hợp cổng thanh toán Sandbox (VNPAY / MoMo Test) với cơ chế tạm giữ tiền (Escrow): Phụ huynh nạp tiền $\rightarrow$ Hệ thống tạm giữ $\rightarrow$ Sau khi học sinh bấm "Xác nhận đã học" thì tiền mới chuyển về số dư gia sư.

---

## 5. Đánh giá & Đề xuất Nâng cấp Tính năng AI

### 5.1. Nhận xét về các tính năng AI hiện tại trong dự án

| Tính năng AI hiện tại | Đánh giá ưu điểm | Thách thức & Rủi ro kỹ thuật | Giải pháp khắc phục |
|---|---|---|---|
| **1. AI Hỗ trợ viết Bio gia sư** | Thiết thực, chi phí API rất rẻ, giúp gia sư tự tin hơn với hồ sơ cá nhân. | AI có thể bị "ảo giác" (hallucination), tự bịa thêm bằng cấp hay thành tích ảo. | Áp dụng triệt để nguyên tắc **Human-in-the-loop**: Kết quả chỉ là bản nháp, gia sư phải tự chịu trách nhiệm và Admin phải kiểm duyệt qua văn bằng thực. |
| **2. AI Sinh Quiz từ tài liệu** | Tính năng cốt lõi, tiết kiệm 80% thời gian soạn đề thi cho gia sư. | File PDF tiếng Việt dễ lỗi font; AI có thể trả về câu hỏi sai kiến thức hoặc format không đúng JSON. | • Dùng Apache POI (Word) và PDFBox (PDF) để clean text trước khi gửi prompt.<br>• Bắt buộc dùng **JSON Structured Output**.<br>• Gia sư bắt buộc duyệt lại trước khi xuất bản. |
| **3. AI Phân tích tiến độ học tập** | Giúp phụ huynh nắm bắt được tình hình của con mà không cần đọc từng bài tập chi tiết. | Gửi quá nhiều dữ liệu thô vào LLM sẽ gây tốn tiền API và dễ làm AI đưa ra nhận xét chung chung. | Backend tự tính toán các con số thống kê (Tỷ lệ đúng theo chương, tỷ lệ chuyên cần) rồi mới gửi bảng tóm tắt cho AI phân tích. |

### 5.2. Đề xuất 4 Tính năng AI Bổ sung Đột phá (Tăng điểm đồ án)

#### 🌟 1. AI Personalized Learning Path Generator (Tạo lộ trình cá nhân hóa)
- **Mô tả:** Học sinh nhập mục tiêu (ví dụ: *Lớp 9 học lực Khá, mục tiêu thi vào 10 đạt 8 điểm môn Toán trong 3 tháng*), AI sẽ tự động phân tích ma trận đề thi và sinh ra một **Lộ trình học tập gồm 6–8 Milestones** theo tuần.
- **Giá trị:** Gia sư có sẵn khung giáo án chuẩn, chỉ cần chỉnh sửa nhẹ thay vì phải lập kế hoạch từ đầu.

#### 🌟 2. AI Socratic Homework Assistant (Trợ lý gợi mở bài tập)
- **Mô tả:** Khi học sinh làm bài tập về nhà và gặp câu khó, thay vì đưa luôn đáp án, chatbot AI hoạt động theo **Phương pháp Socratic (hỏi gợi mở)** để học sinh tự tìm ra cách giải:
  > *AI: "Để tính diện tích hình thang, em hãy nhớ lại công thức liên quan đến hai đáy và chiều cao xem nào?"*
- **Giá trị:** Ngăn chặn gian lận chép bài giải, khuyến khích tư duy độc lập.

#### 🌟 3. AI Smart Flashcards & Spaced Repetition (Ôn tập ngắt quãng)
- **Mô tả:** Hệ thống tự động thu thập các câu hỏi học sinh đã làm sai trong các bài Quiz, sau đó AI sinh ra bộ **Flashcards tóm tắt lý thuyết trọng tâm** và nhắc ôn tập theo chu kỳ 1 ngày – 3 ngày – 7 ngày.
- **Giá trị:** Trám đúng lỗ hổng kiến thức cho học sinh cấp 2 trước các kỳ thi quan trọng.

#### 🌟 4. AI Voice-to-Report (Tạo báo cáo buổi học bằng giọng nói)
- **Mô tả:** Sau buổi học, gia sư chỉ cần bấm ghi âm nói nhanh 30 giây tóm tắt buổi dạy. Hệ thống dùng Whisper (Speech-to-Text) kết hợp LLM để chuyển thành một bản báo cáo sư phạm chỉn chu gửi cho phụ huynh.
- **Giá trị:** Giải phóng gia sư khỏi áp lực gõ phím viết báo cáo dài dòng sau giờ dạy mệt mỏi.

---

## 6. Kiến trúc Kỹ thuật & Công nghệ Dự kiến

```
[ Frontend: React + TypeScript + Vite + Tailwind/KaTeX ]
                          │
                   (RESTful API / JWT)
                          ▼
[ Backend: Java 17/21 + Spring Boot 3.x + Spring Security ]
       │                                     │
(Spring Data JPA)                     (Http Client / SDK)
       ▼                                     ▼
[ Database: SQL Server ]            [ AI Service: Gemini / OpenAI API ]
```

---

## 7. Hướng dẫn Lệnh Git để Push lên GitHub

Mở terminal tại thư mục gốc của dự án (`d:\Antigravity\Github\EduFit`) và chạy tuần tự các lệnh sau:

### Bước 1: Kiểm tra trạng thái file
```bash
git status
```
*Bạn sẽ thấy file `PROJECT_SUMMARY.md` nằm trong danh sách "Untracked files".*

### Bước 2: Thêm file vào vùng chờ (Staging Area)
```bash
git add PROJECT_SUMMARY.md
```
*(Hoặc gõ `git add .` nếu bạn muốn thêm toàn bộ các thay đổi khác nếu có).*

### Bước 3: Tạo commit với thông điệp rõ ràng
```bash
git commit -m "docs: add comprehensive project summary, roadmap, gap analysis and AI specifications"
```

### Bước 4: Đẩy code lên GitHub
```bash
git push origin main
```
*(Nếu nhánh của bạn có tên khác như `master`, hãy đổi thành `git push origin <tên-nhánh>`)*.

### Bước 5: Kiểm tra kết quả
Mở trình duyệt truy cập vào repository trên GitHub, bạn sẽ thấy file **`PROJECT_SUMMARY.md`** hiển thị đầy đủ, đẹp mắt và chuyên nghiệp!
