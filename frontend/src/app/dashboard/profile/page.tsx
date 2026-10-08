"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { DashboardShell } from "@/shared/ui/DashboardShell";
import { UserRole } from "@/shared/ui/Sidebar";
import { StudentProfileForm } from "@/features/profile/components/StudentProfileForm";
import { TutorProfileForm } from "@/features/profile/components/TutorProfileForm";
import { GraduationCap, Briefcase, Eye } from "lucide-react";

/**
 * =========================================================================
 * SPRING BOOT API ENDPOINTS:
 * =========================================================================
 * - Student Profile: GET/PUT /api/v1/profiles/students/me
 * - Tutor Profile:   GET/PUT /api/v1/profiles/tutors/me
 * - Catalogs:        GET     /api/v1/catalogs/subjects & /education-levels
 * =========================================================================
 */
export default function ProfilePage() {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();

  const userRole = (user?.role?.toUpperCase() || "STUDENT") as UserRole;
  const [activeRole, setActiveRole] = useState<UserRole>(
    ["STUDENT", "TUTOR"].includes(userRole) ? userRole : "STUDENT"
  );

  const effectiveUser = user || {
    id: "demo-user-id",
    email: "demo@edufit.vn",
    fullName: "Nguyễn Văn A (Demo)",
    role: activeRole,
    status: "ACTIVE",
  };

  const handleLogout = async () => {
    if (user) {
      await logout();
    }
    router.push("/login");
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center font-sans">
        <div className="size-8 border-3 border-amber-500/30 border-t-amber-500 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="relative">
      {/* Role Switcher Tab for Demo & Multi-Role Preview */}
      <div className="bg-neutral-900 text-white px-4 py-2 flex flex-wrap items-center justify-between gap-3 text-xs border-b border-neutral-800 z-50 sticky top-0">
        <div className="flex items-center gap-2">
          <Eye className="size-3.5 text-amber-400" />
          <span className="text-neutral-400 font-medium">Xem trước hồ sơ theo vai trò:</span>
        </div>

        <div className="flex items-center gap-1.5 bg-neutral-800/80 p-1 rounded-xl">
          <button
            type="button"
            onClick={() => setActiveRole("STUDENT")}
            className={`px-3 py-1 rounded-lg font-bold text-xs transition-all flex items-center gap-1.5 ${
              activeRole === "STUDENT"
                ? "bg-amber-500 text-neutral-950 shadow-xs"
                : "text-neutral-300 hover:text-white"
            }`}
          >
            <GraduationCap className="size-3.5" />
            <span>Học sinh</span>
          </button>
          <button
            type="button"
            onClick={() => setActiveRole("TUTOR")}
            className={`px-3 py-1 rounded-lg font-bold text-xs transition-all flex items-center gap-1.5 ${
              activeRole === "TUTOR"
                ? "bg-sky-500 text-neutral-950 shadow-xs"
                : "text-neutral-300 hover:text-white"
            }`}
          >
            <Briefcase className="size-3.5" />
            <span>Gia sư</span>
          </button>
        </div>
      </div>

      <DashboardShell user={effectiveUser} role={activeRole} onLogout={handleLogout}>
        <div className="max-w-5xl mx-auto space-y-6">
          {activeRole === "STUDENT" ? <StudentProfileForm /> : <TutorProfileForm />}
        </div>
      </DashboardShell>
    </div>
  );
}
