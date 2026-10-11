"use client";

import React, { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { DashboardShell } from "@/shared/ui/DashboardShell";
import { UserRole } from "@/shared/ui/Sidebar";
import { StudentProfileForm } from "@/features/profile/components/StudentProfileForm";
import { TutorProfileForm } from "@/features/profile/components/TutorProfileForm";
import { ParentProfileForm } from "@/features/profile/components/ParentProfileForm";
import { AdminProfileForm } from "@/features/profile/components/AdminProfileForm";

/**
 * =========================================================================
 * TRANG THIẾT LẬP HỒ SƠ TÀI KHOẢN (ROLE-SPECIFIC PROFILE)
 * =========================================================================
 * - Học sinh (STUDENT): Quản lý khối lớp, khu vực học tập, mục tiêu cá nhân.
 * - Gia sư (TUTOR): Quản lý môn dạy, khung giờ rảnh (Availability Matrix), học phí.
 * - Phụ huynh (PARENT): Quản lý thông tin liên hệ, yêu cầu gia sư cho con, liên kết tài khoản.
 * - Quản trị viên (ADMIN): Thông tin điều hành, đặc quyền hệ thống và đổi mật khẩu.
 * =========================================================================
 */
export default function ProfilePage() {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();

  useEffect(() => {
    if (!isLoading && !user) {
      router.push("/login");
    }
  }, [user, isLoading, router]);

  const handleLogout = async () => {
    if (user) {
      await logout();
    }
    router.push("/login");
  };

  if (isLoading || !user) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-3">
          <div className="size-8 border-3 border-amber-500/30 border-t-amber-500 rounded-full animate-spin" />
          <span className="text-xs text-neutral-500 font-medium">Đang tải hồ sơ tài khoản...</span>
        </div>
      </div>
    );
  }

  const role = (user.role?.toUpperCase() || "STUDENT") as UserRole;

  return (
    <DashboardShell user={user} role={role} onLogout={handleLogout}>
      <div className="max-w-5xl mx-auto space-y-6">
        {role === "STUDENT" && <StudentProfileForm />}
        {role === "TUTOR" && <TutorProfileForm />}
        {role === "PARENT" && <ParentProfileForm />}
        {role === "ADMIN" && <AdminProfileForm />}
      </div>
    </DashboardShell>
  );
}
