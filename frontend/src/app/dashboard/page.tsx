"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  LayoutDashboard,
  Search,
  BookOpen,
  Calendar,
  TrendingUp,
  LogOut,
  ChevronLeft,
  ChevronRight,
  Sparkles,
  Award,
  Check,
  User,
  Clock,
  ArrowRight,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";

/**
 * Trang Tổng quan (Dashboard Screen - Màn hình chính sau khi xác thực thành công)
 * 
 * =========================================================================
 * ĐỐI SOÁT VỚI THIẾT KẾ FIGMA (docs/figma_code.txt) & QUY CHUẨN KIẾN TRÚC:
 * =========================================================================
 * 1. Thanh điều hướng bên trái (Sidebar - w-60):
 *    - Ánh xạ trực tiếp từ cấu trúc thẻ div trong file `figma_code.txt`:
 *      + Nhóm "TỔNG QUAN": Dashboard (kích hoạt `bg-stone-100 rounded-[10px]`).
 *      + Nhóm "TÌM KIẾM": Tìm gia sư (UC2.1).
 *      + Nhóm "HỌC TẬP": Lớp của tôi (class), Lịch học (tutoring_session), Tiến độ (progress).
 *      + Nút "Thu gọn" (Collapse) ở chân Sidebar.
 * 
 * 2. Thanh tiêu đề phía trên (Top Header):
 *    - Hiển thị danh tính người dùng thực tế lấy từ `useAuth()` (đọc qua session cookie).
 *    - Huy hiệu vai trò (`user.role`: Học sinh, Phụ huynh, Gia sư).
 *    - Nút Đăng xuất (`POST /api/v1/auth/logout`) hủy phiên an toàn.
 * 
 * 3. Nội dung Dashboard:
 *    - Thẻ Mục tiêu trọng tâm (Goal Highlight) với thanh tiến độ % (NFR-23, BR-48).
 *    - 3 Thẻ thống kê vi mô (Cột mốc, Điểm số, Chuỗi ngày học liên tiếp).
 *    - Thẻ Buổi học sắp diễn ra (Upcoming Tutoring Session).
 */
export default function DashboardPage() {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  // Bảo vệ tuyến đường (Route Guard): Nếu phiên đã hết hạn hoặc chưa đăng nhập -> chuyển về /login
  useEffect(() => {
    if (!isLoading && !user) {
      router.push("/login");
    }
  }, [isLoading, user, router]);

  // Xử lý đăng xuất (UC1.3)
  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-3">
          <div className="size-8 border-3 border-sky-600/30 border-t-sky-600 rounded-full animate-spin" />
          <span className="text-xs text-neutral-500 font-medium">Đang đồng bộ dữ liệu phiên làm việc...</span>
        </div>
      </div>
    );
  }

  if (!user) {
    return null;
  }

  // Chuyển đổi mã Role sang nhãn hiển thị tiếng Việt thân thiện
  const roleLabels: Record<string, { label: string; color: string }> = {
    STUDENT: { label: "Học sinh", color: "bg-sky-100 text-sky-800" },
    PARENT: { label: "Phụ huynh", color: "bg-emerald-100 text-emerald-800" },
    TUTOR: { label: "Gia sư", color: "bg-amber-100 text-amber-900" },
    ADMIN: { label: "Quản trị viên", color: "bg-purple-100 text-purple-900" },
  };

  const currentRole = roleLabels[user.role] || { label: user.role, color: "bg-stone-100 text-neutral-800" };

  return (
    <div className="min-h-screen bg-stone-50 flex font-sans text-neutral-900 selection:bg-orange-200">
      {/* =================================================================== */}
      {/* 1. SIDEBAR (KHỚP TỶ LỆ & CSS FIGMA TẠI docs/figma_code.txt)           */}
      {/* =================================================================== */}
      <aside
        className={`${
          sidebarCollapsed ? "w-20" : "w-64"
        } bg-white border-r border-stone-200/80 transition-all duration-300 flex flex-col justify-between shrink-0 h-screen sticky top-0 z-40`}
      >
        <div className="p-4 sm:p-5 flex flex-col gap-6">
          {/* Logo EduFit */}
          <Link href="/" className="flex items-center gap-2.5 overflow-hidden px-1">
            <div className="size-9 rounded-xl bg-gradient-to-tr from-sky-500 via-emerald-400 to-orange-400 flex items-center justify-center shadow-xs shrink-0">
              <span className="text-white font-bold text-lg font-heading">E</span>
            </div>
            {!sidebarCollapsed && (
              <span className="font-heading text-xl font-bold tracking-tight text-neutral-900 truncate">
                EduFit
              </span>
            )}
          </Link>

          {/* Danh mục Menu Điều hướng */}
          <nav className="flex flex-col gap-5 overflow-hidden">
            {/* Nhóm 1: TỔNG QUAN */}
            <div className="flex flex-col gap-1.5">
              {!sidebarCollapsed && (
                <div className="px-3 text-[10px] font-semibold text-neutral-400 uppercase tracking-wider">
                  TỔNG QUAN
                </div>
              )}
              <Link
                href="/dashboard"
                className="h-11 px-3 bg-stone-100 rounded-xl flex items-center gap-2.5 text-zinc-900 font-semibold text-xs transition-colors"
              >
                <LayoutDashboard className="size-4 shrink-0 text-zinc-800" />
                {!sidebarCollapsed && <span>Dashboard</span>}
              </Link>
            </div>

            {/* Nhóm 2: TÌM KIẾM (UC2.1) */}
            <div className="flex flex-col gap-1.5">
              {!sidebarCollapsed && (
                <div className="px-3 text-[10px] font-semibold text-neutral-400 uppercase tracking-wider">
                  TÌM KIẾM
                </div>
              )}
              <button
                type="button"
                className="h-11 px-3 rounded-xl flex items-center gap-2.5 text-neutral-600 hover:bg-stone-50 font-medium text-xs transition-colors w-full text-left"
              >
                <Search className="size-4 shrink-0 text-neutral-500" />
                {!sidebarCollapsed && <span>Tìm gia sư</span>}
              </button>
            </div>

            {/* Nhóm 3: HỌC TẬP (UC3, UC4, UC5) */}
            <div className="flex flex-col gap-1.5">
              {!sidebarCollapsed && (
                <div className="px-3 text-[10px] font-semibold text-neutral-400 uppercase tracking-wider">
                  HỌC TẬP
                </div>
              )}
              <button
                type="button"
                className="h-11 px-3 rounded-xl flex items-center gap-2.5 text-neutral-600 hover:bg-stone-50 font-medium text-xs transition-colors w-full text-left"
              >
                <BookOpen className="size-4 shrink-0 text-neutral-500" />
                {!sidebarCollapsed && <span>Lớp của tôi</span>}
              </button>
              <button
                type="button"
                className="h-11 px-3 rounded-xl flex items-center justify-between text-neutral-600 hover:bg-stone-50 font-medium text-xs transition-colors w-full text-left"
              >
                <div className="flex items-center gap-2.5">
                  <Calendar className="size-4 shrink-0 text-neutral-500" />
                  {!sidebarCollapsed && <span>Lịch học</span>}
                </div>
                {!sidebarCollapsed && (
                  <span className="size-5 bg-amber-400 text-neutral-900 rounded-full flex items-center justify-center text-[10px] font-bold">
                    2
                  </span>
                )}
              </button>
              <button
                type="button"
                className="h-11 px-3 rounded-xl flex items-center gap-2.5 text-neutral-600 hover:bg-stone-50 font-medium text-xs transition-colors w-full text-left"
              >
                <TrendingUp className="size-4 shrink-0 text-neutral-500" />
                {!sidebarCollapsed && <span>Tiến độ</span>}
              </button>
            </div>
          </nav>
        </div>

        {/* Chân Sidebar: Nút Thu gọn / Mở rộng */}
        <div className="p-4 border-t border-stone-100">
          <button
            type="button"
            onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
            className="w-full h-10 px-3 bg-stone-50 hover:bg-stone-100 rounded-xl flex items-center justify-center gap-2 text-neutral-600 text-xs font-medium transition-colors"
          >
            {sidebarCollapsed ? (
              <ChevronRight className="size-4" />
            ) : (
              <>
                <ChevronLeft className="size-4" />
                <span>Thu gọn</span>
              </>
            )}
          </button>
        </div>
      </aside>

      {/* =================================================================== */}
      {/* 2. KHU VỰC NỘI DUNG CHÍNH (MAIN WORKSPACE)                          */}
      {/* =================================================================== */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top Header */}
        <header className="h-16 px-6 sm:px-8 bg-white border-b border-stone-200/80 flex items-center justify-between sticky top-0 z-30">
          <div className="flex items-center gap-3">
            <h1 className="font-heading text-lg font-semibold text-neutral-900">
              Dashboard
            </h1>
            <span className="text-stone-300">/</span>
            <span className="text-xs text-neutral-500">Tổng quan học tập</span>
          </div>

          {/* User Profile Pill & Logout */}
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2.5 pl-3 border-l border-stone-200/60">
              <div className="size-9 rounded-full bg-orange-100 text-orange-800 flex items-center justify-center font-bold text-xs font-heading">
                {user.fullName.charAt(0).toUpperCase()}
              </div>
              <div className="hidden sm:flex flex-col text-left">
                <span className="text-xs font-semibold text-neutral-900 leading-tight">
                  {user.fullName}
                </span>
                <span className="text-[10px] text-neutral-500">{user.email}</span>
              </div>
              <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ml-1 ${currentRole.color}`}>
                {currentRole.label}
              </span>
            </div>

            {/* Nút Đăng xuất (UC1.3) */}
            <button
              type="button"
              onClick={handleLogout}
              title="Đăng xuất khỏi hệ thống"
              className="size-9 rounded-xl border border-stone-200 hover:bg-red-50 hover:border-red-200 hover:text-red-600 flex items-center justify-center text-neutral-500 transition-colors"
            >
              <LogOut className="size-4" />
            </button>
          </div>
        </header>

        {/* Nội dung Dashboard */}
        <main className="p-6 sm:p-8 max-w-6xl w-full mx-auto space-y-8">
          {/* Lời chào */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h2 className="font-heading text-2xl sm:text-3xl font-semibold text-neutral-900">
                Chào {user.fullName} 👋
              </h2>
              <p className="text-sm text-neutral-600 mt-1">
                Tuần này bạn đang tiến bộ rất tích cực. Hãy tiếp tục duy trì nhé!
              </p>
            </div>
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 bg-emerald-50 text-emerald-800 rounded-full border border-emerald-200/60 text-xs font-semibold">
              <Award className="size-4 text-emerald-600" />
              <span>Tài khoản đã kích hoạt ({user.status})</span>
            </div>
          </div>

          {/* Lưới tổng quan 2 cột */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Cột trái: Thẻ mục tiêu đang học (Figma Goal Card) */}
            <div className="lg:col-span-7 bg-white p-6 sm:p-7 rounded-3xl border border-stone-200/80 shadow-xs flex flex-col justify-between gap-6">
              <div>
                <div className="flex justify-between items-center mb-4">
                  <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-stone-100 rounded-full text-xs font-semibold text-neutral-700">
                    <Sparkles className="size-3.5 text-amber-500" />
                    <span>Mục tiêu đang tập trung</span>
                  </div>
                  <span className="px-2.5 py-0.5 bg-orange-300 rounded-full text-neutral-900 text-xs font-bold">
                    Lớp 9
                  </span>
                </div>

                <h3 className="font-heading text-xl font-semibold text-neutral-900 mb-2">
                  Đạt 8+ điểm Toán trong kỳ thi vào lớp 10
                </h3>
                <p className="text-xs text-neutral-500">
                  Lộ trình học 1-1 cùng cô Nguyễn Hoàng Lan · 2 buổi/tuần
                </p>
              </div>

              {/* Progress Bar (NFR-23) */}
              <div className="space-y-2">
                <div className="flex justify-between items-center text-xs font-semibold">
                  <span className="text-neutral-600">Tiến độ hoàn thành</span>
                  <span className="text-emerald-600 text-sm">68%</span>
                </div>
                <div className="w-full h-3 bg-stone-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-emerald-500 rounded-full transition-all duration-700"
                    style={{ width: "68%" }}
                  />
                </div>
                <div className="flex justify-between items-center text-[11px] text-neutral-400">
                  <span>8 / 12 cột mốc đã vượt qua</span>
                  <span>Còn 4 cột mốc</span>
                </div>
              </div>

              <div className="pt-2 flex items-center justify-between border-t border-stone-100 text-xs">
                <span className="text-neutral-500">Buổi học kế tiếp: T5, 19:00</span>
                <button className="text-sky-600 hover:text-sky-700 font-semibold flex items-center gap-1">
                  Xem chi tiết lộ trình <ArrowRight className="size-3.5" />
                </button>
              </div>
            </div>

            {/* Cột phải: 3 Số liệu vi mô (Micro metrics) */}
            <div className="lg:col-span-5 flex flex-col gap-4">
              <div className="p-5 bg-white rounded-2xl border border-stone-200/80 shadow-xs flex items-center gap-4">
                <div className="size-12 rounded-2xl bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0">
                  <Check className="size-6" />
                </div>
                <div>
                  <div className="text-xs text-neutral-500 font-medium">Cột mốc hoàn thành</div>
                  <div className="text-2xl font-bold font-heading text-neutral-900">8 / 12</div>
                </div>
              </div>

              <div className="p-5 bg-white rounded-2xl border border-stone-200/80 shadow-xs flex items-center gap-4">
                <div className="size-12 rounded-2xl bg-sky-100 text-sky-700 flex items-center justify-center shrink-0">
                  <TrendingUp className="size-6" />
                </div>
                <div>
                  <div className="text-xs text-neutral-500 font-medium">Điểm quiz trung bình</div>
                  <div className="text-2xl font-bold font-heading text-neutral-900">8.4 <span className="text-xs font-normal text-neutral-400">/ 10</span></div>
                </div>
              </div>

              <div className="p-5 bg-white rounded-2xl border border-stone-200/80 shadow-xs flex items-center gap-4">
                <div className="size-12 rounded-2xl bg-orange-100 text-orange-700 flex items-center justify-center shrink-0">
                  <Calendar className="size-6" />
                </div>
                <div>
                  <div className="text-xs text-neutral-500 font-medium">Chuỗi ngày học tập</div>
                  <div className="text-2xl font-bold font-heading text-neutral-900">6 ngày <span className="text-xs font-normal text-emerald-600">liên tiếp</span></div>
                </div>
              </div>
            </div>
          </div>

          {/* Buổi học sắp tới */}
          <div className="bg-white p-6 sm:p-7 rounded-3xl border border-stone-200/80 shadow-xs">
            <div className="flex justify-between items-center mb-5">
              <h3 className="font-heading text-lg font-semibold text-neutral-900">
                Lịch học sắp tới (tutoring_session)
              </h3>
              <span className="text-xs text-sky-600 font-semibold hover:underline cursor-pointer">
                Xem toàn bộ lịch
              </span>
            </div>

            <div className="p-4 bg-stone-50 border border-stone-200/60 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <div className="size-12 rounded-2xl bg-orange-300 text-neutral-900 flex flex-col items-center justify-center shrink-0">
                  <span className="text-[10px] font-bold uppercase">T5</span>
                  <span className="text-base font-bold leading-none">18</span>
                </div>
                <div>
                  <div className="text-sm font-semibold text-neutral-900">
                    Bài 14: Đồ thị hàm số bậc nhất và ứng dụng
                  </div>
                  <div className="flex items-center gap-3 text-xs text-neutral-500 mt-1">
                    <span className="flex items-center gap-1">
                      <Clock className="size-3.5" /> 19:00 - 20:30
                    </span>
                    <span>·</span>
                    <span className="flex items-center gap-1">
                      <User className="size-3.5" /> Cô Nguyễn Hoàng Lan
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2.5">
                <span className="px-3 py-1 bg-sky-50 text-sky-700 rounded-full text-xs font-semibold border border-sky-200/50">
                  SCHEDULED
                </span>
                <button className="px-4 py-2 bg-stone-200/80 hover:bg-stone-300 text-neutral-800 text-xs font-semibold rounded-xl transition">
                  Vào phòng học
                </button>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
