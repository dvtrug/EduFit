"use client";

import React, { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { DashboardShell } from "@/shared/ui/DashboardShell";
import { UserRole } from "@/shared/ui/Sidebar";
import {
  StudentDashboardView,
  TutorDashboardView,
  ParentDashboardView,
  AdminDashboardView,
} from "@/features/dashboard/components";

/**
 * Trang Tổng quan Vai Trò Đăng Nhập (Role-Guarded Dashboard)
 *
 * Tự động phân luồng và hiển thị giao diện chính xác dựa trên vai trò (Role)
 * được xác thực từ Session Backend (EDUFIT_SESSION):
 * - STUDENT: StudentDashboardView
 * - TUTOR:   TutorDashboardView
 * - PARENT:  ParentDashboardView
 * - ADMIN:   AdminDashboardView
 */
import { profileService } from "@/services/profile";

export default function DashboardPage() {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();
  const [checkingOnboarding, setCheckingOnboarding] = React.useState(true);

  useEffect(() => {
    if (!isLoading && !user) {
      router.push("/login");
      return;
    }

    if (!isLoading && user) {
      const role = user.role?.toUpperCase();
      if (role === "STUDENT") {
        profileService
          .getMyStudentProfile()
          .then((profile) => {
            if (!profile?.profileComplete) {
              router.push("/onboarding/student");
            } else {
              setCheckingOnboarding(false);
            }
          })
          .catch(() => {
            router.push("/onboarding/student");
          });
      } else if (role === "TUTOR") {
        profileService
          .getMyTutorProfile()
          .then((profile) => {
            const hasOnboarded = Boolean(profile?.bio && profile.subjects?.length > 0);
            if (!hasOnboarded) {
              router.push("/onboarding/tutor");
            } else {
              setCheckingOnboarding(false);
            }
          })
          .catch(() => {
            router.push("/onboarding/tutor");
          });
      } else if (role === "PARENT") {
        const hasOnboarded =
          typeof window !== "undefined" &&
          localStorage.getItem("edufit_parent_onboarding_completed");
        if (!hasOnboarded) {
          router.push("/onboarding/parent");
        } else {
          queueMicrotask(() => setCheckingOnboarding(false));
        }
      } else {
        // ADMIN hoặc role khác: vào thẳng Dashboard
        queueMicrotask(() => setCheckingOnboarding(false));
      }
    }
  }, [user, isLoading, router]);

  const handleLogout = async () => {
    if (user) {
      await logout();
    }
    router.push("/login");
  };

  if (isLoading || !user || checkingOnboarding) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center font-sans">
        <div className="flex flex-col items-center gap-3">
          <div className="size-8 border-3 border-amber-500/30 border-t-amber-500 rounded-full animate-spin" />
          <span className="text-xs text-neutral-500 font-medium">Đang kiểm tra hồ sơ và phiên đăng nhập...</span>
        </div>
      </div>
    );
  }

  const role = (user.role?.toUpperCase() || "STUDENT") as UserRole;

  const renderDashboardView = () => {
    switch (role) {
      case "TUTOR":
        return <TutorDashboardView user={user} />;
      case "PARENT":
        return <ParentDashboardView user={user} />;
      case "ADMIN":
        return <AdminDashboardView user={user} />;
      case "STUDENT":
      default:
        return <StudentDashboardView user={user} />;
    }
  };

  return (
    <DashboardShell user={user} role={role} onLogout={handleLogout}>
      {renderDashboardView()}
    </DashboardShell>
  );
}
