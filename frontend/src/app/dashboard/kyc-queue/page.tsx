"use client";

import React from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { DashboardShell } from "@/shared/ui/DashboardShell";
import { AdminKycQueueView } from "@/features/verification/components/AdminKycQueueView";

/**
 * =========================================================================
 * SPRING BOOT API INTEGRATION:
 * =========================================================================
 * GET  /api/v1/admin/verifications/pending
 * POST /api/v1/admin/verifications/{id}/review
 * Security: @PreAuthorize("hasRole('ADMIN')")
 * =========================================================================
 */
export default function AdminKycQueuePage() {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();

  const effectiveUser = user || {
    id: "demo-admin-id",
    email: "admin.demo@edufit.vn",
    fullName: "Quản Trị Viên (Demo)",
    role: "ADMIN",
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
        <div className="size-8 border-3 border-purple-500/30 border-t-purple-600 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <DashboardShell user={effectiveUser} role="ADMIN" onLogout={handleLogout}>
      <div className="max-w-6xl mx-auto">
        <AdminKycQueueView />
      </div>
    </DashboardShell>
  );
}
