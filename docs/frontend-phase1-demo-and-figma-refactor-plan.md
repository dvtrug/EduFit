# ACTION PLAN: HOÀN THIỆN DỨT ĐIỂM GIAI ĐOẠN 1 — KẾT NỐI DEMO FRONTEND & PHÂN RÃ MÃ NGUỒN FIGMA

> **Mã tài liệu:** EDUFIT-FAP-PHASE1-V1.0  
> **Thuộc văn bản:** Phân kỳ Chi tiết Giai đoạn 1 của [Master Plan Frontend](frontend-master-roadmap-and-architecture-plan.md)  
> **Dự án:** EduFit (SWP391)  
> **Tài liệu tham chiếu:** [SADS](software-architecture-design-specification.md), [ADR-001](ADR-001-tech-stack.md), [ADR-002](ADR-002-multi-module-architecture.md)  
> **Trọng tâm thực hiện:** Kết nối thực tế với 3 module Backend đã có (`iam`, `profile`, `verification`), phân hóa Dashboard theo vai trò người dùng (Student, Tutor, Parent), và phân rã các file TSX nguyên khối xuất từ Figma.

---

## 1. Mục Tiêu Cụ Thể (Goal Description)

1. **Chạy Demo kết nối thực tế với Backend ngay lập tức:**
   * Next.js kết nối trực tiếp với Spring Boot qua Stateful Session Cookie `EDUFIT_SESSION` và API Proxy Rewrites (`/api/:path*` $\rightarrow$ `http://localhost:8080/api/:path*`).
   * Người dùng đăng ký tài khoản $\rightarrow$ Đăng nhập $\rightarrow$ Backend cấp phiên $\rightarrow$ Tự động điều hướng và hiển thị Dashboard tương ứng.
2. **Phân hóa Dashboard theo Vai trò (Role-Adaptive Dashboard):**
   * Nếu là **Học sinh (STUDENT):** Hiển thị thẻ Mục tiêu học tập đang học (Figma Goal Card), thanh tiến độ, 3 thẻ thống kê vi mô và lịch học sắp tới.
   * Nếu là **Gia sư (TUTOR):** Hiển thị banner trạng thái duyệt hồ sơ (Verification Status: VERIFIED, PENDING, REJECTED, UNVERIFIED), tóm lược chuyên môn & môn học nhận dạy từ DB, 3 thẻ số liệu gia sư (học viên, đánh giá, thù lao) và nút nộp hồ sơ bằng cấp.
   * Nếu là **Phụ huynh (PARENT):** Hiển thị bộ chọn con (Child Switcher), tiến độ học tập của từng con, nhận xét mới nhất từ gia sư và số buổi học trong tuần.
3. **Phân rã (Decompose) các File Figma Nguyên khối:**
   * Bóc tách `dashboard/page.tsx` (397 dòng) và `login/page.tsx` (325 dòng) thành các UI components tái sử dụng tại `shared/ui/` và `features/`.
   * Đảm bảo bảo tồn 100% kiểu dáng (Tailwind CSS) của Figma, không làm xô lệch giao diện.
4. **Hỗ trợ Nút Đăng nhập Nhanh (Demo Quick-Login):**
   * Bổ sung 3 nút 1-click tại trang Login: `[Demo Học sinh]`, `[Demo Gia sư]`, `[Demo Phụ huynh]` để người thuyết trình có thể trình diễn các luồng mượt mà.

---

## 2. Chi Tiết Tệp Tin Cần Tạo & Cập Nhật (File Checklist)

```
Bước 1: Tạo Services kết nối Backend
  ├── src/services/catalog.ts
  ├── src/services/profile.ts
  └── src/services/verification.ts
Bước 2: Tạo UI Components dùng chung (bóc tách từ Figma)
  ├── src/shared/ui/Sidebar.tsx
  ├── src/shared/ui/TopHeader.tsx
  └── src/shared/ui/StatCard.tsx
Bước 3: Tạo Feature Components chuyên biệt
  ├── src/features/auth/components/QuickLoginPills.tsx
  ├── src/features/verification/components/TutorVerificationBanner.tsx
  ├── src/features/verification/components/UploadCredentialModal.tsx
  ├── src/features/dashboard/StudentDashboardView.tsx
  ├── src/features/dashboard/TutorDashboardView.tsx
  └── src/features/dashboard/ParentDashboardView.tsx
Bước 4: Cập nhật Dashboard & Login Page
  ├── src/app/login/page.tsx       (Thêm QuickLoginPills)
  └── src/app/dashboard/page.tsx   (Rút gọn xuống < 80 dòng, gọi Sidebar + TopHeader + Role View)
```

---

## 3. Quy Cách Kỹ Thuật Chi Tiết Từng Tệp Tin

### BƯỚC 1: Lớp API Services Kết Nối Backend

#### 1. `frontend/src/services/catalog.ts`
* Kết nối endpoint `/api/v1/catalogs`:
  * `getCatalog()`: Lấy toàn bộ danh mục gồm cấp học (`educationLevels`) và môn học (`subjects`).
  * `getSubjects()`: `GET /api/v1/catalogs/subjects`.
  * `getEducationLevels()`: `GET /api/v1/catalogs/education-levels`.

#### 2. `frontend/src/services/profile.ts`
* Kết nối module `profile`:
  * `getMyProfile()`: `GET /api/v1/profiles/me` (Lấy thông tin tổng hợp `MyProfileComposite`).
  * `getMyStudentProfile()`: `GET /api/v1/profiles/students/me` (Lấy `StudentProfileData`).
  * `updateStudentProfile(payload)`: `PUT /api/v1/profiles/students/me`.
  * `getMyTutorProfile()`: `GET /api/v1/profiles/tutors/me` (Lấy `TutorProfileData`).
  * `updateTutorProfile(payload)`: `PUT /api/v1/profiles/tutors/me`.
  * `addTutorSubject(payload)`: `POST /api/v1/profiles/tutors/me/subjects`.
  * `removeTutorSubject(subjectId, levelId)`: `DELETE /api/v1/profiles/tutors/me/subjects`.
  * `setAvailabilitySlots(payload)`: `PUT /api/v1/profiles/tutors/me/availability-slots`.

#### 3. `frontend/src/services/verification.ts`
* Kết nối module `verification`:
  * `getMyRequest()`: `GET /api/v1/verifications/my-request`.
  * `submitVerification(payload, files)`: `POST /api/v1/verifications` (Multipart `FormData` đính kèm JSON Blob `payload` và danh sách tệp `files`).

---

### BƯỚC 2: UI Components Dùng Chung (Bóc Tách từ Figma)

#### 1. `frontend/src/shared/ui/Sidebar.tsx`
* Bóc tách thanh sidebar 100 dòng từ `dashboard/page.tsx`.
* Hỗ trợ prop `role?: UserRole` (`STUDENT`, `TUTOR`, `PARENT`) để hiển thị đúng nhóm menu:
  * `STUDENT`: Dashboard, Tìm gia sư, Lớp của tôi, Lịch học, Tiến độ học tập.
  * `TUTOR`: Dashboard, Lớp đang dạy, Lịch dạy tuần này, Xác minh & Bằng cấp, Thù lao giảng dạy.
  * `PARENT`: Dashboard, Hồ sơ các con, Tìm gia sư cho con, Báo cáo tiến độ.
* Giữ nguyên nút Thu gọn/Mở rộng (Collapse/Expand) ở chân sidebar.

#### 2. `frontend/src/shared/ui/TopHeader.tsx`
* Bóc tách thanh header từ `dashboard/page.tsx`.
* Hiển thị avatar ký tự đầu, họ tên, email, huy hiệu màu sắc vai trò (`STUDENT`: sky, `PARENT`: emerald, `TUTOR`: amber, `ADMIN`: purple), và nút Đăng xuất an toàn.

#### 3. `frontend/src/shared/ui/StatCard.tsx`
* Component thẻ thống kê vi mô tái sử dụng với icon tròn màu sắc (`emerald-100`, `sky-100`, `orange-100`), nhãn, giá trị số lớn, suffix và dòng ghi chú nhỏ.

---

### BƯỚC 3: Feature Components Tùy Biến Theo Vai Trò

#### 1. `frontend/src/features/auth/components/QuickLoginPills.tsx`
* 3 nút bấm nhanh đặt tại trang Login:
  * `[Học sinh]`: email `student.demo@edufit.vn`, mật khẩu `Password123@`
  * `[Gia sư]`: email `tutor.demo@edufit.vn`, mật khẩu `Password123@`
  * `[Phụ huynh]`: email `parent.demo@edufit.vn`, mật khẩu `Password123@`
* Nhấp 1-click tự động điền form và thực hiện đăng nhập.

#### 2. `frontend/src/features/verification/components/TutorVerificationBanner.tsx`
* Hiển thị banner trạng thái kiểm duyệt hồ sơ cho Gia sư:
  * `VERIFIED`: Khung màu xanh ngọc bích "Hồ sơ đã được xác minh chính thức" kèm huy hiệu uy tín.
  * `PENDING`: Khung màu xanh dương "Hồ sơ đang chờ Quản trị viên xét duyệt (thường xử lý trong 24h)".
  * `REJECTED`: Khung màu đỏ hồng "Hồ sơ xác minh chưa được duyệt" kèm lý do từ chối và nút "Nộp lại bằng cấp".
  * `DRAFT` / `UNVERIFIED`: Khung màu cam cảnh báo "Tài khoản gia sư chưa được xác minh" kèm nút "Nộp hồ sơ ngay".

#### 3. `frontend/src/features/verification/components/UploadCredentialModal.tsx`
* Hộp thoại modal cho phép Gia sư nộp hồ sơ xác minh:
  * Lựa chọn loại bằng cấp (`DEGREE`, `TEACHING_LICENCE`, `STUDENT_CARD`, `CERTIFICATE`, `OTHER`).
  * Nhập tên đơn vị/trường cấp bằng, năm tốt nghiệp, ghi chú.
  * Bộ chọn tệp tài liệu hỗ trợ JPG, PNG, PDF (kiểm tra giới hạn dung lượng < 10MB).
  * Gọi `verificationService.submitVerification` với hiệu ứng loading và phản hồi thành công.

#### 4. `frontend/src/features/dashboard/StudentDashboardView.tsx`
* Màn hình tổng quan của Học sinh: Thẻ mục tiêu đang học, thanh tiến độ, 3 thẻ thống kê vi mô và lịch học sắp tới. Kết nối với `profileService.getMyStudentProfile()`.

#### 5. `frontend/src/features/dashboard/TutorDashboardView.tsx`
* Màn hình tổng quan của Gia sư: Tích hợp `TutorVerificationBanner`, tóm lược hồ sơ chuyên môn, danh sách môn học nhận dạy, 3 chỉ số thống kê gia sư và lịch dạy hôm nay.

#### 6. `frontend/src/features/dashboard/ParentDashboardView.tsx`
* Màn hình tổng quan của Phụ huynh: Bộ chọn con (Child Switcher), tiến độ mục tiêu của từng con, nhận xét mới nhất từ gia sư và chỉ số chuyên cần.

---

### BƯỚC 4: Tích Hợp Lại Trang `dashboard/page.tsx` & `login/page.tsx`

#### 1. `frontend/src/app/login/page.tsx`
* Nhập và hiển thị `<QuickLoginPills />` phía trên biểu mẫu đăng nhập để người thuyết trình hoặc tester có thể truy cập ngay lập tức.

#### 2. `frontend/src/app/dashboard/page.tsx`
* Rút gọn trang từ 397 dòng xuống **dưới 80 dòng**:
```tsx
export default function DashboardPage() {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();
  // Route Guard kiểm tra phiên...

  return (
    <div className="min-h-screen bg-stone-50 flex font-sans text-neutral-900">
      <Sidebar role={user.role} currentPath="/dashboard" />
      <div className="flex-1 flex flex-col min-w-0">
        <TopHeader user={user} onLogout={handleLogout} />
        <main className="p-6 sm:p-8 max-w-6xl w-full mx-auto">
          {user.role === "TUTOR" ? (
            <TutorDashboardView user={user} />
          ) : user.role === "PARENT" ? (
            <ParentDashboardView user={user} />
          ) : (
            <StudentDashboardView user={user} />
          )}
        </main>
      </div>
    </div>
  );
}
```

---

## 4. Hướng Dẫn Thực Hiện Dành Cho Thành Viên Đội Ngũ (Team Execution Guide)

Khi nhận task thực hiện Giai đoạn 1, thành viên nhóm tiến hành theo quy trình Git chuẩn:

### Bước 1: Tạo Nhánh Tính Năng Mới
```bash
git checkout main
git pull origin main
git checkout -b feature/frontend-phase1-demo
```

### Bước 2: Triển Khai Tuần Tự
1. Viết các API services tại `frontend/src/services/` (`catalog.ts`, `profile.ts`, `verification.ts`).
2. Viết các UI components dùng chung tại `frontend/src/shared/ui/` (`Sidebar.tsx`, `TopHeader.tsx`, `StatCard.tsx`).
3. Viết các feature components tại `frontend/src/features/` (`QuickLoginPills.tsx`, `TutorVerificationBanner.tsx`, `UploadCredentialModal.tsx`, các dashboard views).
4. Cập nhật `frontend/src/app/login/page.tsx` và `frontend/src/app/dashboard/page.tsx`.

### Bước 3: Kiểm Tra Biên Dịch & Chạy Thử
```bash
cd frontend
npm run build
npm run dev
```
Xác nhận `npm run build` thoát với mã 0 (exit code 0), không có lỗi TypeScript hay lỗi cú pháp Next.js Turbopack.

### Bước 4: Tạo Pull Request
* Commit với thông điệp rõ ràng theo Conventional Commits:  
  `feat(frontend): implement role-adaptive dashboard and decompose figma components (phase 1 demo)`
* Mở Pull Request vào nhánh `main` và đối soát với Checklist tại Mục 2 của tài liệu này.
