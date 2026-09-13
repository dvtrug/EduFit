# Implementation Plan: EduFit Landing Page

> Phân tích chi tiết giao diện tham khảo mới và quy tắc chuyển đổi sang EduFit nằm tại [`tasks/interface-analysis.md`](interface-analysis.md).

## 1. Mục tiêu

Xây dựng landing page giúp phụ huynh và học sinh nhanh chóng hiểu EduFit, tin tưởng chất lượng gia sư và bắt đầu quy trình tìm gia sư. Trang sử dụng ngôn ngữ thị giác trong `DESIGN.md` (storybook trên nền giấy kem) và chuyển hóa cấu trúc dữ liệu rõ ràng của ảnh tham khảo thành các khối giới thiệu sản phẩm dễ quét.

Landing page không mô phỏng một dashboard hoàn chỉnh và không trình bày “4 vai trò”. Dashboard chỉ xuất hiện dưới dạng product preview để minh họa ba giá trị thiết yếu:

1. Gia sư được xác minh.
2. Matching có lý do rõ ràng.
3. Tiến độ học tập có dữ liệu.

## 2. Phân tích ảnh tham khảo

### 2.1. Ngữ pháp bố cục đáng học hỏi

- **Dark stage làm điểm neo:** một khối navy lớn chứa thông điệp tiến độ, tạo focal point mạnh trước khi người dùng đọc số liệu chi tiết.
- **Card pastel chồng lớp:** ba progress cards nằm trên dark stage, tạo chiều sâu mà không cần shadow nặng.
- **Thông tin theo thứ bậc:** tiêu đề, con số chính, nhãn phụ, sau đó mới đến progress/action.
- **Dashboard theo module:** statistics, calendar, assignments và upcoming lesson đều là component độc lập.
- **Mật độ cân bằng:** giao diện chứa nhiều dữ liệu nhưng vẫn thoáng nhờ grid, khoảng trắng và nhãn ngắn.
- **Màu nhấn có chức năng:** lavender, peach và lime dùng để phân loại nội dung/trạng thái; phần còn lại trung tính.

### 2.2. Những gì không nên sao chép trực tiếp

- Sidebar chỉ phù hợp với portal sau đăng nhập; landing page dùng header ngang, sticky nhẹ.
- Không đưa toàn bộ chức năng hệ thống lên màn hình đầu tiên.
- Không đặt calendar, task list và toàn bộ dashboard vào hero; hero chỉ dùng preview cô đọng.
- Không dùng lavender/lime làm màu CTA chính; CTA vẫn là ink black theo `DESIGN.md`.
- Không để dữ liệu giả quá dày; mỗi preview chỉ cần đủ để chứng minh giá trị.

### 2.3. Cách chuyển hóa cho EduFit

| Thành phần trong ảnh | Chuyển hóa cho landing page EduFit |
|---|---|
| Khung dashboard lớn | Khung product preview ở hero hoặc section “EduFit hoạt động như thế nào” |
| Dark progress hero | `LearningProgressStage` hoặc `MatchingStage` |
| Course progress card | `MilestoneCard` hoặc `MatchingCard` |
| Statistics card | `ProgressMetricCard`: buổi học, quiz, milestone |
| Calendar | `MiniSchedule`: lịch học sắp tới |
| Assignment row | `LearningTaskRow`: bài tập/quiz và deadline |
| Upcoming lesson | `UpcomingLessonCard`: giờ học, gia sư và môn |
| Sidebar | Chỉ dùng trong portal sau đăng nhập, không dùng trên landing page |

## 3. Định hướng trải nghiệm

### 3.1. Đối tượng chính

- **Phụ huynh:** cần sự tin cậy, minh bạch, dễ theo dõi và tiết kiệm thời gian lựa chọn.
- **Học sinh:** cần cảm giác gần gũi, dễ bắt đầu, nhìn thấy mục tiêu và tiến bộ.

### 3.2. Thông điệp cốt lõi

**Tìm đúng gia sư. Đi đúng lộ trình. Nhìn thấy tiến bộ.**

### 3.3. Chuyển đổi chính

- CTA chính: **Tìm gia sư phù hợp**.
- CTA phụ: **Xem cách EduFit hoạt động**.
- Chuyển đổi hoàn tất khi người dùng bắt đầu form matching hoặc đăng ký tài khoản.

## 4. Kiến trúc trang

### Section 1 — Header

**Mục tiêu:** giúp điều hướng và giữ CTA luôn dễ thấy.

- Logo EduFit bên trái.
- Liên kết giữa: Cách hoạt động, Gia sư, Tiến độ, Câu hỏi thường gặp.
- Bên phải: Đăng nhập dạng ghost, “Tìm gia sư” dạng dark pill.
- Desktop cao 72–80px; mobile chuyển thành logo + CTA + menu button.
- Sticky sau khi người dùng cuộn qua hero; nền cream có blur rất nhẹ hoặc viền đá 1px.

### Section 2 — Hero với product stage

**Mục tiêu:** giải thích giá trị trong 5–8 giây.

- Desktop dùng layout 5/7: copy và CTA bên trái, `MatchingStage` bên phải.
- `MatchingStage` là một dark surface lớn với ba card pastel chồng nhẹ, lấy cảm hứng từ ảnh tham khảo.
- Preview hiển thị tutor match, lịch trùng và mục tiêu tuần; không chứa sidebar hoặc dashboard đầy đủ.
- Illustration storybook chỉ dùng như điểm nhấn nhỏ quanh stage, không cạnh tranh với dữ liệu.

**Copy đề xuất:**

- Eyebrow: `HỌC ĐÚNG CÁCH · TIẾN BỘ RÕ RÀNG`
- H1: `Tìm đúng gia sư, đi đúng lộ trình.`
- Body: `EduFit kết nối học sinh với gia sư đã xác minh, giải thích lý do phù hợp và biến từng buổi học thành tiến bộ có thể nhìn thấy.`
- Reassurance: `Miễn phí khám phá · Chỉ bắt đầu khi bạn thấy phù hợp`

### Section 3 — Trust strip

**Mục tiêu:** xử lý ba lo lắng lớn nhất trước khi giới thiệu tính năng.

Ba trust cards hoặc một hàng ba cột:

- `Gia sư đã xác minh` — hồ sơ và chuyên môn được kiểm tra.
- `Matching có giải thích` — biết rõ vì sao một gia sư phù hợp.
- `Tiến độ có dữ liệu` — quiz, milestone và báo cáo sau buổi học.

Không dùng số liệu chưa được kiểm chứng. Có thể thay bằng số liệu thật khi hệ thống đã có analytics.

### Section 4 — Product story: Từ nhu cầu đến gia sư phù hợp

**Mục tiêu:** trình bày flow thay vì liệt kê tính năng rời rạc.

Layout desktop 2 cột, mobile 1 cột:

- Cột nội dung: ba bước đánh số.
- Cột preview: khung ứng dụng có card, chip và trạng thái.

Ba bước:

1. **Cho EduFit biết mục tiêu:** môn học, cấp độ, lịch rảnh, phong cách học.
2. **Nhận gợi ý có lý do:** danh sách ngắn 2–3 gia sư kèm mức phù hợp.
3. **Bắt đầu và theo dõi:** lịch học, milestone, quiz, nhận xét.

### Section 5 — Tutor matching preview

**Mục tiêu:** biến lợi ích “AI matching” thành bằng chứng trực quan và dễ hiểu.

Component lớn dạng bảng/list như “Top Mentors” trong ảnh:

- Avatar, tên, môn/chuyên môn.
- Badge `94% phù hợp`.
- 2–3 chips lý do: `Mạnh Đại số`, `Dạy bằng sơ đồ`, `Trùng 2 lịch`.
- Kinh nghiệm và rating chỉ hiển thị nếu có dữ liệu thật.
- CTA từng hàng: `Xem hồ sơ`.

Trên mobile, mỗi hàng chuyển thành tutor card dọc; không dùng bảng cuộn ngang cho luồng chính.

### Section 6 — Learning progress showcase

**Mục tiêu:** giải thích giá trị sau khi đã tìm được gia sư.

- Dark `LearningProgressStage` tóm tắt thành quả trong tuần.
- Ba `MilestoneCard` pastel: Toán 9, mục tiêu tuần và quiz gần nhất.
- Hàng dưới chia 8/4: metrics + task list bên trái, mini schedule + upcoming lesson bên phải.
- Insight card màu honey: “Điểm sáng tuần này”.
- Trên mobile bỏ overlap; cards stack và mini calendar chuyển thành upcoming-days list.

### Section 7 — Lợi ích theo đối tượng

**Mục tiêu:** cho hai nhóm người dùng thấy lợi ích riêng mà không biến thành mô tả role system.

Hai card bất đối xứng:

- **Cho học sinh:** hiểu mục tiêu, chuẩn bị trước buổi học, xem tiến độ.
- **Cho phụ huynh:** xem lịch, nhận báo cáo, nắm điểm cần hỗ trợ.

Đây là nội dung marketing theo nhu cầu, không phải danh sách quyền hoặc “4 vai trò”.

### Section 8 — Social proof

**Mục tiêu:** giảm rủi ro cảm nhận trước CTA cuối.

- 3 testimonial cards desktop; horizontal snap hoặc 1 card/slide mobile.
- Avatar, tên viết tắt hoặc tên được đồng ý công khai, vai trò “Phụ huynh/Học sinh”.
- Trích dẫn ngắn dưới 35 từ.
- Có thể bổ sung số liệu thật: tỷ lệ tiếp tục học, rating trung bình, số buổi hoàn thành.

Không dùng dữ liệu hoặc chân dung giả như bằng chứng thật.

### Section 9 — FAQ

**Mục tiêu:** xử lý câu hỏi cản trở chuyển đổi.

Accordion 5–7 câu:

1. EduFit xác minh gia sư như thế nào?
2. Vì sao một gia sư được gợi ý cho tôi?
3. Tôi có được đổi gia sư không?
4. Phụ huynh theo dõi tiến độ bằng cách nào?
5. Chi phí và thanh toán hoạt động ra sao?
6. Nếu lịch học thay đổi thì thế nào?

### Section 10 — Final CTA và footer

**Mục tiêu:** kết thúc bằng một hành động duy nhất.

- Dark inset card trên nền cream.
- Heading: `Bắt đầu bằng một lựa chọn rõ ràng hơn cho việc học.`
- CTA: `Tìm gia sư phù hợp`.
- Footer: Cách hoạt động, Chính sách, Điều khoản, Hỗ trợ, Liên hệ.

## 5. Hệ thống component

### 5.1. Foundation components

| Component | Trách nhiệm | Biến thể |
|---|---|---|
| `Container` | Giới hạn 1200px, padding responsive | default, narrow, wide |
| `Section` | Khoảng cách dọc và semantic heading | cream, stone, dark |
| `Stack` / `Cluster` | Layout dọc/ngang theo spacing scale | gap 8–104 |
| `Heading` | H1–H3 đúng hierarchy | display, section, card |
| `Button` | CTA truy cập được bằng keyboard | primary-dark, secondary-sand, ghost |
| `IconBadge` | Icon nền màu, không tự truyền đạt trạng thái | blue, green, orange, honey, pink |
| `Tag` | Môn học/lý do matching | neutral, info, success |
| `HairlineCard` | Surface trắng, radius 10px, inset border | default, interactive, selected |

### 5.2. Marketing components

| Component | Nội dung |
|---|---|
| `SiteHeader` | Logo, nav, login, CTA, mobile menu |
| `Hero` | Copy, CTA, illustration, product preview |
| `TrustItem` | Label, mô tả ngắn, accent semantic |
| `StepCard` | Số bước, heading, mô tả, trạng thái active |
| `TestimonialCard` | Avatar, identity, quote |
| `FaqAccordion` | Question button + answer region |
| `FinalCta` | Heading, helper text, primary action |
| `SiteFooter` | Sitemap, policy links, copyright |

### 5.3. Product-preview components

| Component | Props chính | Ghi chú |
|---|---|---|
| `ProductWindow` | `title`, `toolbar`, `children` | Khung mô phỏng sản phẩm; chỉ trang trí ngoài, semantic bên trong |
| `LearnerSummary` | learner, subject, goal, schedule | Không hiển thị dữ liệu cá nhân thật trong demo |
| `TutorMatchCard` | tutor, score, reasons, rating | Score luôn đi với label/lý do, không chỉ dùng màu |
| `TutorMatchList` | tutors, onViewProfile | Desktop row, mobile card |
| `ProgressMetric` | value, label, trend | Giá trị + nhãn rõ ràng |
| `MilestoneProgress` | title, current, total, status | Dùng native progress hoặc ARIA tương đương |
| `WeeklyInsight` | title, body, tone | Honey cho positive insight |
| `VerificationBanner` | label, description, action | Thay thế banner unlock trong mẫu |
| `LearningProgressStage` | summary, milestones, action | Dark stage chủ đạo theo ảnh tham khảo mới |
| `LearningTaskRow` | task, subject, dueDate, status | Hàng bài tập/quiz; mobile chuyển thành card |
| `MiniSchedule` | selectedDate, lessonDates | Lịch cô đọng, không thay thế calendar đầy đủ |
| `UpcomingLessonCard` | time, tutor, subject, href | Buổi học gần nhất với CTA rõ ràng |

## 6. Component tree đề xuất

```text
LandingPage
├── SiteHeader
├── main
│   ├── Hero
│   │   ├── IllustrationCluster
│   │   ├── HeroCopy
│   │   └── ProductWindow
│   │       └── TutorMatchCard
│   ├── TrustStrip
│   │   └── TrustItem × 3
│   ├── MatchingJourney
│   │   ├── StepCard × 3
│   │   └── ProductWindow
│   ├── TutorMatchingPreview
│   │   └── TutorMatchList
│   ├── ProgressPreview
│   │   ├── LearningProgressStage
│   │   │   └── MilestoneCard × 3
│   │   ├── ProgressMetric × 3
│   │   ├── LearningTaskList
│   │   ├── MiniSchedule
│   │   ├── UpcomingLessonCard
│   │   └── WeeklyInsight
│   ├── AudienceBenefits
│   ├── SocialProof
│   ├── FaqAccordion
│   └── FinalCta
└── SiteFooter
```

## 7. Visual system

### 7.1. Màu

- Canvas: `#fbfaf9`.
- Stone surface: `#f2f0ed`.
- Text heading: `#343433`; body: `#474645`.
- Primary action: `#121212`.
- Accent dùng có chủ đích: blue `#0086fc`, green `#00c978`, orange `#ff3e00`, honey `#ffbb26`.
- Product preview được phép dùng tint lavender, peach và lime tương ứng với violet, honey và green; các tint không trở thành màu thương hiệu/CTA.
- Không gradient. Không lấy tím từ ảnh làm màu chủ đạo.
- Viền card: 1px `#e5d5c3` hoặc inset `#f2f0ed`.

### 7.2. Typography

- Display: Family; nếu chưa có webfont thì dùng Bricolage Grotesque làm substitute đã được duyệt về tinh thần.
- Body/UI: Inter.
- H1 desktop 60–68px; tablet 48–56px; mobile 38–44px.
- Section heading desktop 44px; mobile 32–36px.
- Body 16–17px, line-height 1.5.

### 7.3. Shape và elevation

- Card radius 10px; product window 18–24px; pill 9999px.
- Card thường không dùng drop shadow; ưu tiên tonal surface + hairline border.
- Chỉ product window hoặc dark focal card được phép dùng shadow rất nhẹ.
- Mascot hình học xuất hiện ở hero và một vài điểm chuyển section, không chen vào vùng dữ liệu.

## 8. Responsive blueprint

| Breakpoint | Bố cục |
|---|---|
| 320–479px | 1 cột; H1 38–42px; CTA full width; nav menu; tutor row thành card; 1 testimonial/slide |
| 480–767px | 1 cột rộng; product preview tối giản; trust items xếp dọc |
| 768–1023px | Hero 2 cột hoặc copy trên/preview dưới; card grid 2 cột |
| 1024–1279px | Full desktop; container 960px; 3 trust cards; preview 2 cột |
| 1280–1440px+ | Container 1200px; hero ba cột; section gap 96–120px |

Quy tắc responsive:

- Nội dung quan trọng không phụ thuộc hover.
- Không có horizontal scroll ngoài carousel testimonial có điều khiển rõ ràng.
- Illustration được giảm/bỏ trước khi giảm cỡ chữ dưới ngưỡng dễ đọc.
- Product preview có phiên bản mobile riêng; không scale nguyên dashboard desktop xuống.

## 9. Nội dung và dữ liệu

### Dữ liệu tĩnh ban đầu

- Navigation, hero copy, trust items, steps, FAQ.
- 2–3 tutor demo profiles được đánh dấu là dữ liệu minh họa trong môi trường preview.
- 3 progress metrics và 1 weekly insight minh họa.

### Dữ liệu cần API khi production

- Danh sách gia sư nổi bật/được đề xuất.
- Rating, số buổi học, verification status.
- Thống kê social proof.
- Pricing hoặc chính sách nếu hiển thị động.

### Quy tắc nội dung

- Mỗi section chỉ truyền đạt một ý chính.
- Heading tối đa 10–12 từ; body tối đa 2–3 câu.
- Không dùng thuật ngữ kỹ thuật như “algorithm”, “role”, “permission” trên landing page.
- Không cam kết hiệu quả học tập tuyệt đối.

## 10. Accessibility

- Một `h1`; thứ tự `h2`/`h3` không bỏ cấp.
- Tất cả CTA là `a` hoặc `button` thật, focus-visible rõ.
- Contrast đạt WCAG 2.1 AA; text thường tối thiểu 4.5:1.
- Progress bar có label, giá trị bằng chữ và `aria-valuenow`.
- FAQ dùng button với `aria-expanded` và region được liên kết.
- Mobile menu quản lý focus, Escape để đóng, khóa scroll nền.
- Illustration trang trí dùng `aria-hidden="true"`; ảnh có nội dung có alt cụ thể.
- Hỗ trợ `prefers-reduced-motion`.

## 11. Motion

- Hero mascot: float rất nhẹ 4–6px, chu kỳ 5–7 giây.
- Product cards: reveal theo opacity/translate 8–12px khi vào viewport.
- Progress bar animate một lần, tối đa 500ms.
- Button hover chỉ đổi tone/translate 1px; không bounce.
- Tắt animation không thiết yếu khi reduced motion.

## 12. Performance và SEO

- Mục tiêu Lighthouse: Performance ≥ 90, Accessibility ≥ 95, SEO ≥ 95.
- LCP < 2.5s, CLS < 0.1, INP < 200ms trên mobile trung bình.
- SVG mascot inline hoặc sprite; không dùng ảnh raster lớn cho hero.
- Font subset, preload đúng font display, `font-display: swap`.
- Semantic metadata: title, description, canonical, Open Graph.
- Structured data phù hợp: `Organization`, và chỉ dùng `FAQPage` khi nội dung FAQ hiển thị thật.
- Không tải code dashboard thật cho product preview; preview là component marketing nhẹ.

## 13. Analytics

Theo dõi tối thiểu:

- `landing_view`
- `hero_cta_click`
- `secondary_cta_click`
- `tutor_profile_preview_click`
- `faq_open`
- `final_cta_click`
- `matching_form_start`
- `matching_form_complete`

Mỗi event chỉ gửi dữ liệu hành vi cần thiết; không gửi thông tin học sinh nhạy cảm vào analytics.

## 14. Kiến trúc kỹ thuật đề xuất

Vì repository hiện mới có tài liệu thiết kế, kiến trúc frontend được đề xuất như sau và cần xác nhận trước khi triển khai:

- React + TypeScript.
- Vite nếu đây là SPA độc lập; Next.js nếu cần SSR/SEO và dự án sẽ có nhiều trang marketing.
- CSS variables từ `DESIGN.md`; có thể map sang Tailwind theme nếu dùng Tailwind.
- Component presentation tách khỏi data adapter.
- Content tĩnh tách vào file typed data để copy có thể thay đổi mà không sửa layout.
- Vitest + Testing Library cho component; Playwright cho responsive và hành trình CTA.

### Cấu trúc file dự kiến

```text
src/
├── pages/
│   └── landing/
│       ├── LandingPage.tsx
│       ├── landing-content.ts
│       └── landing.types.ts
├── components/
│   ├── foundation/
│   ├── marketing/
│   └── product-preview/
├── styles/
│   ├── tokens.css
│   └── globals.css
├── assets/
│   └── illustrations/
└── tests/
    └── landing/
```

## 15. Kế hoạch triển khai

### Phase 1 — Foundation

#### Task 1: Xác nhận stack và khởi tạo app shell

**Acceptance criteria:**

- App chạy local và có route `/`.
- TypeScript strict được bật.
- Có scripts build, test và lint hoạt động.

**Verification:** `npm run build`, `npm test`, `npm run lint`.

**Dependencies:** Không.

**Estimated scope:** M.

#### Task 2: Chuyển `DESIGN.md` thành design tokens

**Acceptance criteria:**

- Color, typography, spacing, radius và shadow có semantic tokens.
- Không dùng raw hex tùy ý trong section components.
- Có trang/dev story để kiểm tra token.

**Verification:** kiểm tra computed styles và visual review ở 320/768/1440px.

**Dependencies:** Task 1.

**Estimated scope:** S.

#### Task 3: Xây foundation components

**Acceptance criteria:**

- Có `Container`, `Section`, `Button`, `Heading`, `Tag`, `HairlineCard`.
- Keyboard/focus state đầy đủ.
- API component nhỏ, composable, typed.

**Verification:** component tests + accessibility scan.

**Dependencies:** Task 2.

**Estimated scope:** M.

### Checkpoint A

- Build/test/lint pass.
- Token khớp `DESIGN.md`.
- Foundation components responsive và accessible.

### Phase 2 — Above the fold

#### Task 4: Header và mobile navigation

**Acceptance criteria:**

- Header desktop/mobile đúng layout.
- Sticky behavior không gây layout shift.
- Menu thao tác được bằng keyboard và Escape.

**Dependencies:** Task 3.

**Estimated scope:** M.

#### Task 5: Hero và product preview đầu trang

**Acceptance criteria:**

- H1, body, hai CTA và reassurance hiển thị đúng.
- Preview dùng dark `MatchingStage` và ba cards pastel, không dùng dashboard/sidebar nguyên bản.
- Illustration không làm giảm LCP hoặc che nội dung.

**Dependencies:** Task 3.

**Estimated scope:** M.

#### Task 6: Trust strip

**Acceptance criteria:**

- Hiển thị đúng ba bằng chứng thiết yếu.
- Không có nội dung “4 vai trò”.
- Card chuyển sang stack sạch ở mobile.

**Dependencies:** Task 3.

**Estimated scope:** S.

### Checkpoint B

- Above-the-fold đúng ở 320/768/1024/1440px.
- CTA đầu trang hoạt động.
- Không có CLS đáng kể khi font/illustration tải.

### Phase 3 — Product story

#### Task 7: Matching journey ba bước

**Acceptance criteria:**

- Ba bước có thứ tự và trạng thái rõ ràng.
- Preview thay đổi theo bước hoặc có bố cục tĩnh dễ hiểu.
- Nội dung không vượt quá giới hạn copy.

**Dependencies:** Task 5.

**Estimated scope:** M.

#### Task 8: Tutor match list

**Acceptance criteria:**

- Desktop list và mobile cards dùng cùng data model.
- Score luôn có label và reasons.
- CTA “Xem hồ sơ” có analytics event.

**Dependencies:** Task 7.

**Estimated scope:** M.

#### Task 9: Progress preview

**Acceptance criteria:**

- Có dark progress stage, ba milestone cards, metrics, task list, mini schedule, upcoming lesson và weekly insight.
- Progress accessible cho screen reader.
- Không tải module dashboard production vào landing page.

**Dependencies:** Task 3.

**Estimated scope:** M.

### Checkpoint C

- Matching và progress được hiểu mà không cần giải thích miệng.
- Responsive không sinh bảng cuộn ngang.
- Component tests và axe scan pass.

### Phase 4 — Trust và conversion

#### Task 10: Audience benefits và social proof

**Acceptance criteria:**

- Lợi ích tách theo nhu cầu học sinh/phụ huynh.
- Testimonial dùng dữ liệu đã được phép hoặc gắn nhãn minh họa.
- Carousel mobile điều khiển được bằng keyboard/touch.

**Dependencies:** Task 3.

**Estimated scope:** M.

#### Task 11: FAQ

**Acceptance criteria:**

- Có 5–7 câu hỏi cản trở chuyển đổi.
- Accordion có ARIA chính xác.
- FAQ schema chỉ xuất khi nội dung hiển thị trên trang.

**Dependencies:** Task 3.

**Estimated scope:** S.

#### Task 12: Final CTA, footer và analytics

**Acceptance criteria:**

- CTA dẫn đến form/route thống nhất.
- Footer có policy/support links.
- Các event trong mục Analytics được kiểm tra.

**Dependencies:** Tasks 4–11.

**Estimated scope:** M.

### Phase 5 — QA và release readiness

#### Task 13: Responsive, accessibility và visual regression

**Acceptance criteria:**

- Kiểm tra 320, 768, 1024, 1440px.
- Không có lỗi axe nghiêm trọng.
- Có visual snapshots cho hero, tutor list, progress và CTA.

**Dependencies:** Tasks 4–12.

**Estimated scope:** M.

#### Task 14: Performance và SEO

**Acceptance criteria:**

- Đạt mục tiêu Lighthouse và Core Web Vitals đã nêu.
- Metadata/OG/canonical đúng.
- Assets và fonts được tối ưu.

**Dependencies:** Task 13.

**Estimated scope:** M.

### Checkpoint D — Definition of Done

- Build, tests và lint pass.
- Landing page responsive, accessible và không có console error.
- Nội dung đã được product owner duyệt.
- Không có số liệu/testimonial chưa xác minh được trình bày như dữ liệu thật.
- CTA chính hoạt động end-to-end và analytics được ghi nhận.
- Sẵn sàng review trước khi deploy.

## 16. Rủi ro và giảm thiểu

| Rủi ro | Mức độ | Giảm thiểu |
|---|---|---|
| Landing page trông như dashboard | Cao | Dùng dashboard chỉ trong product window; giữ narrative sections và whitespace lớn |
| Quá nhiều màu/mascot làm giảm độ tin cậy | Trung bình | Accent chỉ ở illustration, status và data markers; CTA vẫn near-black |
| Matching score bị hiểu là cam kết tuyệt đối | Cao | Luôn hiển thị reasons và copy “gợi ý”; người dùng quyết định cuối cùng |
| Dữ liệu social proof chưa có | Cao | Không bịa số; dùng block ẩn hoặc nội dung minh họa có nhãn |
| Preview desktop không dùng được trên mobile | Cao | Xây mobile variant riêng cho tutor list và progress card |
| Font Family chưa có web license/file | Trung bình | Xác nhận license; tạm dùng Bricolage Grotesque rồi thay qua token |
| Landing page tải nặng do dashboard/illustration | Trung bình | Product preview thuần presentation, SVG tối ưu, lazy-load below fold |

## 17. Câu hỏi cần chốt trước khi code

1. Frontend hiện tại dùng React/Vite, Next.js hay stack khác?
2. CTA chính sẽ mở form matching, trang đăng ký hay trang tìm gia sư?
3. EduFit đã có dữ liệu thật cho rating, số gia sư và testimonial chưa?
4. Font Family đã có file webfont và quyền sử dụng chưa?
5. Cần song ngữ Việt/Anh ngay từ bản đầu không?
6. Có brand assets/logo SVG chính thức không?

## 18. Phạm vi MVP đề xuất

Để ra landing page đầu tiên nhanh nhưng đủ thuyết phục, MVP nên gồm Header, Hero, Trust strip, Matching journey, Tutor match preview, Progress preview, FAQ, Final CTA và Footer. Audience benefits và social proof có thể thêm ở vòng hai nếu chưa có nội dung/dữ liệu được xác minh.
