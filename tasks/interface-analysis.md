# Phân tích giao diện tham khảo và hướng áp dụng cho EduFit

## 1. Kết luận thiết kế

Giao diện tham khảo là một learning dashboard theo hướng editorial: nền trung tính, sidebar mảnh, một khối hero navy rất lớn và các thẻ học tập pastel chồng lên khối nền. Thế mạnh của nó nằm ở thứ bậc thị giác rõ, thông tin học tập được trình bày bằng card nhỏ, và việc kết hợp giữa dữ liệu tiến độ với lịch/nhiệm vụ.

Đối với EduFit, không nên sao chép nguyên sidebar và dashboard vào landing page. Nên dùng ngôn ngữ component này để tạo **product showcase**: người truy cập nhìn thấy một phiên bản cô đọng của matching, lịch học, milestone và báo cáo tiến độ ngay trên trang giới thiệu.

## 2. Phân tích bố cục ảnh

### 2.1. Khung tổng thể

- Canvas desktop gần tỷ lệ 4:3, đặt giữa một nền xanh xám dịu.
- Giao diện chính dùng nền trắng/xám rất nhạt, không có shadow nặng.
- Sidebar chiếm khoảng 18% chiều rộng; content area khoảng 82%.
- Content chia thành hai lớp:
  - Lớp trên: search, tiêu đề và hero/progress stage.
  - Lớp dưới: statistics, calendar, assignments và upcoming lesson.
- Khoảng trắng rộng giúp nhiều dữ liệu vẫn dễ đọc.

### 2.2. Điểm neo thị giác

Khối navy “You have completed 6 lessons this week” là điểm neo chính:

- Chiếm gần toàn bộ chiều ngang content.
- Copy lớn nằm bên trái, tạo câu chuyện trước khi đưa dữ liệu.
- Ba thẻ pastel nằm bên phải và tràn nhẹ khỏi khối nền.
- Tương phản navy/pastel làm số liệu quan trọng nổi bật mà không cần biểu đồ phức tạp.

Đây là pattern phù hợp nhất để áp dụng cho EduFit.

### 2.3. Hệ thống card

Giao diện có bốn nhóm card:

1. **Progress cards:** số thứ tự, icon, tên nội dung, số bài, phần trăm và progress bar.
2. **Metric cards:** một con số lớn và một nhãn ngắn.
3. **Assignment rows:** icon màu, tên nhiệm vụ, khóa học, deadline và action menu.
4. **Upcoming card:** thời gian, tên buổi học và affordance mở chi tiết.

Các card dùng radius vừa phải, nội dung ít, spacing nhất quán và gần như không có shadow.

### 2.4. Màu sắc

- Navy dùng cho khối thông tin trọng tâm.
- Lavender, peach và lime dùng cho từng loại học tập/trạng thái.
- Nền và text chủ yếu trung tính.
- Màu chỉ xuất hiện ở khu vực cần phân loại hoặc thu hút chú ý.

Khi áp dụng cho EduFit:

- Navy được thay bằng `--color-ink-black` hoặc một navy-charcoal gần `#1f223d` trong product preview.
- Lavender dùng cho matching/insight.
- Peach/honey dùng cho lịch học hoặc trạng thái đang chờ.
- Lime/green dùng cho milestone hoàn thành và tiến bộ tích cực.
- Phần còn lại vẫn tuân theo cream canvas và stone surface của `DESIGN.md`.

## 3. Điều nên giữ và điều cần thay đổi

### Nên giữ

- Một khối visual lớn làm điểm neo cho mỗi câu chuyện sản phẩm.
- Card pastel chồng nhẹ lên dark stage ở desktop.
- Metric card với con số lớn và nhãn ngắn.
- Progress bar có phần trăm và label.
- Kết hợp dữ liệu tiến độ với lịch/nhiệm vụ sắp tới.
- Icon đơn sắc, đường nét gọn và không trang trí dư thừa.
- Layout bất đối xứng nhưng vẫn theo grid.

### Cần thay đổi

- Không dùng sidebar trên landing page; dùng header ngang.
- Không hiển thị search toàn hệ thống ở hero.
- Không đưa lịch tháng đầy đủ vào landing page; chỉ dùng mini calendar hoặc upcoming lesson card.
- Không trình bày “courses” như nền tảng khóa học; đổi thành mục tiêu, milestone và buổi học.
- Không dùng màu neon lime cho CTA; CTA chính vẫn là dark pill theo `DESIGN.md`.
- Không dùng avatar/dữ liệu giả như social proof thật.

## 4. Chuyển đổi component sang nghiệp vụ EduFit

| Component tham khảo | Component EduFit | Dữ liệu hiển thị |
|---|---|---|
| Progress hero | `LearningProgressStage` | Lời chào, mục tiêu tuần, milestone đã hoàn thành |
| Course progress card | `MilestoneCard` | Môn học, chủ đề, tiến độ, số hoạt động |
| Statistics card | `ProgressMetricCard` | Buổi học, quiz, milestone, thời lượng học |
| Calendar | `MiniSchedule` | Ngày có lịch, ngày được chọn, số buổi sắp tới |
| Assignment row | `LearningTaskRow` | Bài tập/quiz, môn, deadline, trạng thái |
| Upcoming lesson | `UpcomingLessonCard` | Giờ học, gia sư, môn học, CTA xem chi tiết |
| Search bar | `TutorSearchInput` | Môn, lớp hoặc tên gia sư; dùng ở trang tìm kiếm, không đặt ở hero landing MVP |
| Sidebar | `PortalSidebar` | Chỉ sử dụng trong web portal sau đăng nhập |

## 5. Hướng áp dụng cho landing page EduFit

### Section 1 — Header

- Logo EduFit.
- Cách hoạt động, Gia sư, Theo dõi tiến độ, FAQ.
- Đăng nhập dạng ghost.
- CTA dark pill: `Tìm gia sư`.

### Section 2 — Hero

Layout desktop 5/7:

- Bên trái là copy và CTA.
- Bên phải là `MatchingStage`: một dark stage với ba tutor/matching cards pastel chồng nhẹ.
- Trên mobile, copy đứng trước; cards xếp ngang có scroll snap hoặc stack.

Nội dung đề xuất:

- Eyebrow: `MATCHING MINH BẠCH · HỌC CÓ LỘ TRÌNH`
- H1: `Tìm gia sư phù hợp với cách bạn học.`
- Body: `EduFit chọn lọc gia sư, giải thích lý do phù hợp và giúp gia đình theo dõi tiến độ sau từng buổi học.`
- CTA chính: `Bắt đầu tìm gia sư`.
- CTA phụ: `Xem EduFit hoạt động`.

Ba card trong preview:

1. `94% phù hợp` — Nguyễn Minh Anh — Toán 9.
2. `Trùng 2 lịch học` — Thứ 3 và Thứ 6.
3. `Mục tiêu tuần` — Hoàn thành phương trình bậc hai.

### Section 3 — Trust metrics

Áp dụng metric cards trong ảnh nhưng chỉ dùng dữ liệu đã xác minh:

- Gia sư đã được duyệt.
- Matching có lý do.
- Báo cáo sau buổi học.

Nếu chưa có số liệu production, dùng statement thay vì con số giả.

### Section 4 — Matching flow

- Intro bên trái.
- Bên phải là ba `MatchingStepCard` màu lavender, peach và green.
- Các card có số 01/02/03 như ảnh:
  1. Chia sẻ mục tiêu.
  2. Nhận gợi ý phù hợp.
  3. Chọn gia sư và đặt lịch.
- Progress indicator phía dưới các card dùng để thể hiện bước, không phải phần trăm giả.

### Section 5 — Tutor shortlist

- Danh sách gọn 2–3 gia sư.
- Avatar, tên, chuyên môn, verification badge.
- Match reasons dạng chip.
- CTA `Xem hồ sơ`.
- Desktop dùng hàng ngang; mobile dùng card dọc.

### Section 6 — Learning progress showcase

Đây là section áp dụng trực tiếp nhất từ ảnh:

- Dark `LearningProgressStage` bên trên.
- Ba `MilestoneCard` pastel chồng nhẹ ở desktop.
- Hàng dưới chia 8/4:
  - Trái: `ProgressMetrics` và `LearningTaskList`.
  - Phải: `MiniSchedule` và `UpcomingLessonCard`.
- Copy không nói “khóa học”; dùng milestone, quiz, bài tập và buổi học.

Ví dụ dữ liệu:

- `4/5 milestone`.
- `82% quiz gần nhất`.
- `6 buổi học đã hoàn thành`.
- `8.5 giờ học trong tháng`.
- Bài tập: `Ôn phương trình`, deadline `18/09`.
- Buổi sắp tới: `Toán 9 với cô Minh Anh`, `19:00–20:30`.

### Section 7 — Parent visibility

- Bên trái: copy cho phụ huynh.
- Bên phải: report card với trend nhỏ, insight và next action.
- Không dùng biểu đồ chỉ để trang trí; mỗi chart phải trả lời một câu hỏi.

### Section 8 — FAQ và Final CTA

- FAQ dạng accordion.
- Dark final CTA card.
- Footer tối giản.

## 6. Hệ thống component cần xây

### Marketing

- `SiteHeader`
- `HeroCopy`
- `TrustMetric`
- `SectionHeading`
- `FaqAccordion`
- `FinalCta`
- `SiteFooter`

### Product preview

- `ProductStage`
- `MatchingCard`
- `MilestoneCard`
- `ProgressMetricCard`
- `ProgressBar`
- `LearningTaskList`
- `LearningTaskRow`
- `MiniSchedule`
- `UpcomingLessonCard`
- `TutorShortlist`
- `TutorShortlistItem`

### Portal reuse sau này

Các product-preview components nên nhận data qua props và không gắn cứng copy. Khi xây web portal, có thể tái sử dụng phần presentation của:

- `MilestoneCard`
- `ProgressMetricCard`
- `LearningTaskRow`
- `UpcomingLessonCard`
- `TutorShortlistItem`

Landing page dùng dữ liệu demo typed; portal dùng API adapter.

## 7. Quy tắc layout

- Container tối đa 1200px.
- Grid desktop 12 cột, gutter 24px.
- Section gap 96–120px desktop; 64–80px tablet; 48–64px mobile.
- Dark stage radius 24px.
- Data cards radius 14–18px; marketing hairline cards vẫn 10px theo `DESIGN.md`.
- Card overlap tối đa 24–36px và chỉ dùng từ 1024px trở lên.
- Text width hero tối đa 560px.
- Mỗi data card chỉ có một primary value và một action.

## 8. Responsive

### Desktop 1280px+

- Hero 5/7.
- Dark stage + overlapping cards.
- Progress showcase có 8/4 split.
- Tutor shortlist dùng row.

### Tablet 768–1279px

- Hero copy trên, preview dưới hoặc 1/1 split.
- Ba cards chuyển thành grid 3 hoặc 2+1.
- Calendar xuống dưới metrics.
- Không overlap nếu chiều rộng dưới 1024px.

### Mobile 320–767px

- Header rút gọn.
- Hero 1 cột, CTA full-width.
- Preview cards stack hoặc scroll snap có pagination label.
- Stats grid 2 cột; metric rộng dùng full row.
- Tutor list và task list thành cards.
- Mini calendar có thể thay bằng danh sách ba ngày sắp tới.

## 9. Accessibility

- Dark stage và pastel cards phải đạt contrast WCAG AA.
- Lime sáng không dùng với text trắng; dùng ink black.
- Progress bar có label và giá trị bằng text/ARIA.
- Calendar dùng button có accessible name theo ngày/tháng.
- Action menu có `aria-label` cụ thể, không chỉ icon ba chấm.
- Danh sách bài tập dùng list/table semantics phù hợp với viewport.
- Không truyền trạng thái chỉ bằng màu; luôn có text/icon.
- Card overlap không được làm thay đổi thứ tự focus.

## 10. Motion

- Card hero xuất hiện stagger 60–90ms.
- Hover card dịch tối đa 2px, không phóng to mạnh.
- Progress bar chạy một lần trong 400–500ms.
- Upcoming lesson có focus/hover border rõ.
- Tắt translate/stagger khi `prefers-reduced-motion`.

## 11. Những điểm cần tránh

- Sao chép logo, nội dung hoặc nhận diện Caplen.
- Dùng neon lime làm CTA chính.
- Đặt sidebar lên landing page.
- Nhồi calendar, charts và task list vào hero.
- Biến landing page thành ảnh dashboard tĩnh không responsive.
- Hiển thị match percentage mà không giải thích lý do.
- Dùng số liệu/testimonial giả như dữ liệu thực.

## 12. Tiêu chí nghiệm thu giao diện

- Người dùng hiểu EduFit làm gì trong dưới 8 giây.
- CTA chính nhìn thấy ngay ở 320px và 1440px.
- Product preview thể hiện matching và progress, không giống nền tảng bán khóa học.
- Không có sidebar trong landing page.
- Các component dữ liệu có thể tái sử dụng cho web portal.
- Không có horizontal overflow ở 320px.
- Contrast, keyboard navigation và reduced motion đạt yêu cầu.
- Visual language vẫn nhất quán với `DESIGN.md`.
