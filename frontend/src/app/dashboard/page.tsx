"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { DashboardShell } from "@/shared/ui/DashboardShell";
import {
  StudentDashboardView,
  TutorDashboardView,
  ParentDashboardView,
  AdminDashboardView,
} from "@/features/dashboard/components";
import { Eye } from "lucide-react";

type RoleType = "STUDENT" | "TUTOR" | "PARENT" | "ADMIN";

/**
 * Trang Tổng quan Đa Vai Trò (Multi-Role Dashboard)
 *
 * =========================================================================
 * KHỚP NỐI 100% VỚI 12 THIẾT KẾ FIGMA GIAI ĐOẠN 1:
 * =========================================================================
 * 1. 8 Shared Shell Components:
 *    - Student Sidebar (36:2507) & Topbar (36:2573)
 *    - Tutor Sidebar (36:6507) & Topbar (36:6576)
 *    - Parent Sidebar (36:5675) & Topbar (36:5742)
 *    - Admin Sidebar (36:7561) & Topbar (36:7633)
 *
 * 2. 4 Role Dashboard Views:
 *    - Student Dashboard View (36:2595)
 *    - Tutor Dashboard View (36:6598)
 *    - Parent Dashboard View (142:8478)
 *    - Admin Dashboard View (36:7660)
 *
 * 3. Đồng bộ Session Backend:
 *    - Đọc người dùng thực tế từ `useAuth()` (cookie `EDUFIT_SESSION`).
 *    - Tích hợp Role Switcher tiện lợi để demo và kiểm thử trực quan cả 4 vai trò.
 */
export default function DashboardPage() {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();
  const [activeRole, setActiveRole] = useState<RoleType>("STUDENT");

  // Đồng bộ role mặc định từ tài khoản người dùng đăng nhập
  useEffect(() => {
    if (user && user.role) {
      const validRole = user.role.toUpperCase() as RoleType;
      if (["STUDENT", "TUTOR", "PARENT", "ADMIN"].includes(validRole)) {
        setActiveRole(validRole);
      }
    }
  }, [user]);

  // Nếu có user thực tế từ backend, dùng thông tin đó. Nếu chưa đăng nhập, dùng user demo để xem trước Figma
  const effectiveUser = user || {
    id: "demo-user-id",
    email: "demo@edufit.vn",
    fullName: "Minh Anh (Demo Preview)",
    role: activeRole,
    status: "ACTIVE",
  };

  // Xử lý đăng xuất
  const handleLogout = async () => {
    if (user) {
      await logout();
    }
    router.push("/login");
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-3">
          <div className="size-8 border-3 border-amber-500/30 border-t-amber-500 rounded-full animate-spin" />
          <span className="text-xs text-neutral-500 font-medium">Đang tải dữ liệu phiên làm việc EduFit...</span>
        </div>
      </div>
    );
  }

  // Render View tương ứng theo vai trò đang chọn
  const renderDashboardView = () => {
    switch (activeRole) {
      case "TUTOR":
        return <TutorDashboardView user={effectiveUser} />;
      case "PARENT":
        return <ParentDashboardView user={effectiveUser} />;
      case "ADMIN":
        return <AdminDashboardView user={effectiveUser} />;
      case "STUDENT":
      default:
        return <StudentDashboardView user={effectiveUser} />;
    }
  };

  return (
    <div className="relative">
      {/* Thanh Demo Role Switcher (Giúp chuyển đổi và kiểm thử nhanh 4 thiết kế Figma) */}
      <div className="bg-neutral-900 text-white px-4 py-2 flex flex-wrap items-center justify-between gap-3 text-xs border-b border-neutral-800 z-50 sticky top-0">
        <div className="flex items-center gap-2">
          <Eye className="size-3.5 text-amber-400" />
          <span className="text-neutral-400 font-medium">Xem trước giao diện Figma:</span>
        </div>

        <div className="flex items-center gap-1.5 bg-neutral-800/80 p-1 rounded-xl">
          <button
            type="button"
            onClick={() => setActiveRole("STUDENT")}
            className={`px-3 py-1 rounded-lg font-bold text-xs transition-all ${
              activeRole === "STUDENT"
                ? "bg-amber-500 text-neutral-950 shadow-xs"
                : "text-neutral-300 hover:text-white"
            }`}
          >
            Học sinh (36:2595)
          </button>
          <button
            type="button"
            onClick={() => setActiveRole("TUTOR")}
            className={`px-3 py-1 rounded-lg font-bold text-xs transition-all ${
              activeRole === "TUTOR"
                ? "bg-sky-500 text-neutral-950 shadow-xs"
                : "text-neutral-300 hover:text-white"
            }`}
          >
            Gia sư (36:6598)
          </button>
          <button
            type="button"
            onClick={() => setActiveRole("PARENT")}
            className={`px-3 py-1 rounded-lg font-bold text-xs transition-all ${
              activeRole === "PARENT"
                ? "bg-emerald-500 text-neutral-950 shadow-xs"
                : "text-neutral-300 hover:text-white"
            }`}
          >
            Phụ huynh (142:8478)
          </button>
          <button
            type="button"
            onClick={() => setActiveRole("ADMIN")}
            className={`px-3 py-1 rounded-lg font-bold text-xs transition-all ${
              activeRole === "ADMIN"
                ? "bg-purple-500 text-neutral-950 shadow-xs"
                : "text-neutral-300 hover:text-white"
            }`}
          >
            Quản trị viên (36:7660)
          </button>
        </div>
      </div>

      {/* Khung giao diện chính (Sidebar + Topbar + Content View) */}
      <DashboardShell user={effectiveUser} role={activeRole} onLogout={handleLogout}>
        {renderDashboardView()}
      </DashboardShell>
    </div>
  );
}
