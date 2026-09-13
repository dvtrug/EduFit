# EduFit Landing Page — Task Checklist

## Foundation

- [x] Phân tích giao diện dashboard tham khảo và lập mapping component cho EduFit.
- [ ] Duyệt `tasks/interface-analysis.md` trước khi bắt đầu code.
- [ ] Chốt frontend stack và đích đến của CTA chính.
- [ ] Khởi tạo app shell, route `/`, TypeScript strict, lint và test.
- [ ] Chuyển `DESIGN.md` thành semantic design tokens.
- [ ] Xây `Container`, `Section`, `Heading`, `Button`, `Tag`, `HairlineCard`.
- [ ] Kiểm tra token/component ở 320px, 768px và 1440px.

## Above the fold

- [ ] Xây header desktop và mobile navigation accessible.
- [ ] Xây hero 5/7 với copy EduFit và dark `MatchingStage`.
- [ ] Tạo illustration cluster SVG theo phong cách Family.
- [ ] Tạo ba matching cards pastel có overlap trên desktop và stack trên mobile.
- [ ] Xây trust strip: xác minh, matching rõ ràng, tiến độ có dữ liệu.
- [ ] Checkpoint: build/test/lint và axe pass.

## Product story

- [ ] Xây hành trình matching ba bước.
- [ ] Tạo `TutorMatchCard` và `TutorMatchList`.
- [ ] Tạo mobile card variant thay cho desktop row.
- [ ] Tạo `ProgressMetric`, `MilestoneProgress`, `WeeklyInsight`.
- [ ] Tạo `LearningProgressStage` và `MilestoneCard`.
- [ ] Tạo `LearningTaskList` và `LearningTaskRow` responsive.
- [ ] Tạo `MiniSchedule` và `UpcomingLessonCard`.
- [ ] Xây progress showcase theo bố cục 8/4 desktop, 1 cột mobile.
- [ ] Checkpoint: người dùng hiểu flow matching → học → theo dõi.

## Trust and conversion

- [ ] Xây lợi ích theo nhu cầu học sinh/phụ huynh.
- [ ] Thu thập và xác minh testimonial/số liệu thật.
- [ ] Xây social proof section hoặc ẩn section nếu chưa có dữ liệu.
- [ ] Xây FAQ accessible.
- [ ] Xây final CTA và footer.
- [ ] Gắn analytics cho CTA, tutor preview, FAQ và matching form.

## QA

- [ ] Test responsive ở 320, 768, 1024 và 1440px.
- [ ] Test keyboard navigation, focus và reduced motion.
- [ ] Chạy axe; không có lỗi nghiêm trọng.
- [ ] Tạo visual regression snapshots.
- [ ] Xác nhận không có nội dung “4 vai trò”.
- [ ] Xác nhận không có số liệu/testimonial giả được trình bày như thật.
- [ ] Kiểm tra Lighthouse và Core Web Vitals.
- [ ] Kiểm tra metadata, canonical, Open Graph và FAQ schema.
- [ ] Product owner duyệt nội dung và CTA end-to-end.
