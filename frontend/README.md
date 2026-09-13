# EduFit Frontend

Landing page đầu tiên của EduFit, xây bằng React, TypeScript và Vite. Giao diện chuyển các nguyên tắc trong `../DESIGN.md` thành một trang marketing hiện đại, học thức, dễ đọc cho phụ huynh và học sinh.

## 1. Bản này đã có gì?

- Header desktop và menu mobile.
- Hero với thông điệp chính và mô phỏng kết quả matching.
- Ba cam kết: gia sư xác minh, matching có giải thích, tiến độ có dữ liệu.
- Quy trình tìm gia sư ba bước.
- Danh sách gia sư minh họa, luôn gắn nhãn dữ liệu demo.
- Bảng tiến độ gồm milestone, chỉ số, nhiệm vụ, lịch và buổi học tiếp theo.
- FAQ tương tác và CTA cuối trang.
- Responsive, focus state, reduced-motion và semantic HTML cơ bản.
- Unit/component test bằng Vitest + Testing Library.

Đây là landing page frontend tĩnh. Đăng nhập, form matching, hồ sơ gia sư và dữ liệu thật chưa kết nối backend.

## 2. Công nghệ

| Công nghệ | Vai trò |
|---|---|
| React 19 | Chia giao diện thành component và quản lý trạng thái menu/FAQ |
| TypeScript 6 | Kiểm tra kiểu dữ liệu trước khi chạy |
| Vite 8 | Dev server, hot reload và đóng gói production |
| CSS thuần | Design tokens, layout, responsive và trạng thái tương tác |
| ESLint | Phát hiện lỗi và quy tắc code |
| Vitest | Test runner tương thích Vite |
| Testing Library | Test theo cách người dùng nhìn và tương tác |
| jsdom | Mô phỏng trình duyệt cho test component |

## 3. Chạy dự án

Yêu cầu: Node.js phiên bản tương thích Vite 8 và npm.

```bash
cd frontend
npm install
npm run dev
```

Mở địa chỉ Vite in ra trong terminal, thường là `http://localhost:5173`.

Các lệnh quan trọng:

```bash
npm run dev          # chạy dev server, tự cập nhật khi lưu file
npm test             # chạy toàn bộ test một lần
npm run test:watch   # chạy test và theo dõi thay đổi
npm run lint         # kiểm tra quy tắc code
npm run build        # TypeScript check + build production vào dist/
npm run preview      # xem thử bản đã build
```

Trước khi đẩy code, tối thiểu hãy chạy:

```bash
npm test
npm run lint
npm run build
```

## 4. Cấu trúc thư mục

```text
frontend/
├── index.html                 # HTML gốc, title và SEO description
├── package.json               # dependencies và scripts
├── vite.config.ts             # cấu hình Vite + Vitest
├── src/
│   ├── main.tsx               # entry point, gắn React vào #root
│   ├── App.tsx                # ghép các section thành landing page
│   ├── App.css                # style riêng của component/section
│   ├── index.css              # reset, font và design tokens toàn cục
│   ├── App.test.tsx           # test nội dung, navigation và FAQ
│   ├── data/
│   │   └── landingData.ts     # copy và dữ liệu demo có kiểu
│   ├── components/
│   │   └── landing/
│   │       ├── Logo.tsx
│   │       ├── Header.tsx
│   │       ├── Hero.tsx
│   │       ├── TrustStrip.tsx
│   │       ├── MatchingFlow.tsx
│   │       ├── TutorShortlist.tsx
│   │       ├── ProgressShowcase.tsx
│   │       ├── Faq.tsx
│   │       └── Footer.tsx
│   └── test/
│       └── setup.ts           # cleanup và matcher cho test DOM
└── dist/                      # output sau npm run build, không sửa tay
```

## 5. Luồng khởi động của frontend

1. Trình duyệt tải `index.html`.
2. `index.html` gọi `src/main.tsx` bằng module script.
3. `main.tsx` tạo React root và render `<App />`.
4. `App.tsx` ghép các section theo thứ tự kể chuyện.
5. Component đọc nội dung lặp lại từ `landingData.ts` và tạo DOM.
6. `index.css` cung cấp nền tảng toàn cục; `App.css` áp dụng layout và responsive.

Nếu trang trắng, kiểm tra theo thứ tự: terminal Vite, console trình duyệt, `main.tsx`, rồi import trong `App.tsx`.

## 6. Vai trò từng component

### `App.tsx`

Đây là sơ đồ trang, không nên chứa dữ liệu dài hoặc logic phức tạp. Thứ tự section hiện tại là:

```text
Header
└── main
    ├── Hero
    ├── TrustStrip
    ├── MatchingFlow
    ├── TutorShortlist
    ├── ProgressShowcase
    └── Faq
Footer + Final CTA
```

### `Header.tsx`

Dùng `useState(false)` để lưu menu mobile đang đóng hay mở. `aria-expanded` giúp screen reader hiểu trạng thái. Khi bấm một link, menu được đóng lại.

### `Hero.tsx`

Chứa một `h1`, thông điệp giá trị, hai CTA và `MatchingStage`. Ba card được tạo bằng `.map()` từ `matchCards`; thêm card mới chỉ cần sửa data.

### `TrustStrip.tsx`

Hiển thị ba cam kết ngắn. Không dùng số liệu marketing chưa kiểm chứng.

### `MatchingFlow.tsx`

Dùng danh sách có thứ tự `<ol>` vì đây là một quy trình. Trên desktop là ba cột; dưới breakpoint mobile chuyển thành một cột.

### `TutorShortlist.tsx`

Tách phần giải thích và product preview. Tên, môn học, điểm matching và lý do đều lấy từ `tutors`. Dữ liệu hiện tại là minh họa, vì vậy giao diện hiển thị badge `Dữ liệu minh họa`.

### `ProgressShowcase.tsx`

Mô phỏng giá trị sau khi matching: milestone, metric, task list, mini calendar và upcoming lesson. Nó không phải dashboard thật và chưa gọi API.

### `Faq.tsx`

Lưu index câu hỏi đang mở bằng state. Mỗi câu dùng button thật, `aria-expanded`, `aria-controls` và một vùng answer có id tương ứng.

### `Footer.tsx`

Chứa CTA cuối và footer. CTA hiện cuộn tới phần gia sư demo; khi có form matching thật, đổi `href` sang route chính thức.

## 7. Design system trong CSS

Các token nằm ở đầu `src/index.css`:

```css
:root {
  --canvas: #fbfaf8;
  --ink: #121212;
  --blue: #0086fc;
  --green: #00c978;
  --orange: #ff3e00;
  --navy: #242540;
  --font-display: 'Bricolage Grotesque', sans-serif;
  --font-body: 'Inter', sans-serif;
  --container: 1200px;
}
```

Nguyên tắc:

- Dùng token thay vì lặp hex trong component.
- Near-black dành cho CTA chính; màu rực dùng để phân loại và tạo điểm nhấn.
- Bricolage Grotesque dùng cho display/heading, Inter dùng cho nội dung/UI.
- Nền kem và viền ấm tạo cảm giác “family storybook” nhưng bố cục vẫn theo sản phẩm giáo dục chuyên nghiệp.
- Product preview được phép dùng navy + lavender/peach/lime; phần marketing chính giữ màu trung tính.

Breakpoints hiện có:

- `1080px`: các layout lớn chuyển từ hai cột thành một cột.
- `820px`: bật menu mobile, card/process chuyển thành stack.
- `620px`: CTA full width, tutor rows thành card dọc, footer stack.

## 8. Dữ liệu và cách sửa nội dung

Nội dung lặp lại nằm ở `src/data/landingData.ts`. Ví dụ thêm FAQ:

```ts
export const faqItems = [
  // các câu hiện tại
  {
    question: 'Chi phí được tính như thế nào?',
    answer: 'Nội dung đã được product owner xác nhận.',
  },
]
```

Không đưa dữ liệu học sinh thật, số điện thoại, email hoặc lịch học thật vào file demo. Khi có backend, data động phải đến từ API và cần loading, empty, error states.

## 9. Test đang kiểm tra gì?

`src/App.test.tsx` kiểm tra ba hành vi quan trọng:

1. Thông điệp chính và CTA tìm gia sư xuất hiện.
2. Navigation có link tới các section cốt lõi.
3. Người dùng mở FAQ và đọc được câu trả lời.

Testing Library ưu tiên truy vấn theo role và accessible name. Điều này giúp test gần hành vi thật hơn so với tìm class CSS.

Khi sửa component:

1. Viết hoặc sửa test mô tả hành vi mong muốn.
2. Chạy test để thấy nó fail đúng lý do.
3. Sửa code tối thiểu.
4. Chạy lại test, lint và build.

## 10. Kết nối Spring Boot backend sau này

Không để URL API rải trong component. Tạo lớp riêng:

```text
src/
├── api/
│   ├── client.ts
│   └── tutors.ts
├── types/
│   └── tutor.ts
└── features/
    └── matching/
```

Ví dụ biến môi trường local:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Chỉ biến bắt đầu bằng `VITE_` mới được đưa vào code frontend. Không đặt password SQL Server, JWT secret hay thông tin bí mật trong `.env` frontend vì người dùng có thể xem chúng trong bundle.

Luồng dự kiến:

```text
React form → POST /api/matching-requests → Spring Boot
Spring Boot → SQL Server / matching service
Spring Boot → JSON shortlist
React → loading / success / empty / error UI
```

Việc cần thống nhất với backend trước khi nối API:

- URL, HTTP method và request/response schema.
- Quy tắc validation cho môn, lớp, lịch và ngân sách.
- Authentication dùng cookie an toàn hay access token.
- CORS cho origin của Vite lúc development.
- Error format chung và trạng thái loading/empty.

## 11. Việc còn lại trước production

- Chốt CTA chính: form matching hay trang đăng ký.
- Tạo React Router khi bắt đầu có `/login`, `/matching`, `/tutors/:id`.
- Kết nối API và bổ sung loading/error/empty states.
- Dùng logo/font chính thức có giấy phép.
- Thêm privacy, terms, support routes thật.
- Bổ sung Open Graph, canonical và structured data.
- Chạy axe, Lighthouse và visual regression ở 320/768/1024/1440px.
- Thêm analytics sau khi chốt quy tắc dữ liệu; không gửi dữ liệu nhạy cảm của học sinh.
- Chỉ thêm testimonial/rating/số liệu khi đã có nguồn thật và quyền sử dụng.

## 12. Quy ước làm việc cho nhóm

- Một component làm một nhiệm vụ rõ ràng.
- Nội dung lặp lại đặt trong data typed; logic gọi API đặt ngoài component trình bày.
- Không sửa `dist/` bằng tay.
- Không commit `.env` chứa bí mật.
- Không dùng dữ liệu minh họa như bằng chứng thật.
- Giữ một `h1` mỗi trang và không bỏ cấp heading.
- Mọi button/link phải dùng được bằng bàn phím và có focus rõ.
- Mỗi pull request chạy test, lint, build trước khi review.

Kế hoạch tổng thể và checklist vẫn được theo dõi tại `../tasks/plan.md` và `../tasks/todo.md`.
